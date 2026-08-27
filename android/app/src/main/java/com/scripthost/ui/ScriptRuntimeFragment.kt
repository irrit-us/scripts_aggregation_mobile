package com.scripthost.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.scripthost.R
import com.scripthost.ScriptHostApplication
import com.scripthost.bridge.ConfigBridge
import com.scripthost.bridge.NotificationBridge
import com.scripthost.bridge.SSHBridge
import com.scripthost.bridge.SystemBridge
import com.scripthost.bridge.UIBridge
import com.scripthost.config.AppSettings
import com.scripthost.engine.ExecutionResult
import com.scripthost.engine.JavaScriptEngine
import com.scripthost.models.ScriptContext
import com.scripthost.util.AndroidLogger
import kotlinx.coroutines.launch

/**
 * Script Runtime Fragment - Execute scripts with UI rendering.
 *
 * Hosted either by [MainActivity] (embedded in the drawer content area) or by
 * [ScriptRuntimeActivity] (standalone). The script's ROOT page is the content
 * home (it replaces the host's welcome state), not a closable sub-screen:
 * the ✕ / right-fling / Back gestures only pop PUSHED pages; at the root
 * page they defer to the host (drawer hosts open the drawer, standalone
 * hosts end the session). The host owns the single header bar and swaps its
 * left button ☰↔✕ via [Host.onScriptPageDepthChanged].
 */
class ScriptRuntimeFragment : Fragment() {

    /** Callbacks the hosting activity provides for session lifecycle. */
    interface Host {
        /** A script started running; the host shows the running header state. */
        fun onScriptSessionStarted(scriptName: String) {}

        /** The script set its own display title via UI.setTitle. */
        fun onScriptTitle(title: String) {}

        /** The script session ended (stop button, X/back at the root page). */
        fun onScriptSessionEnded()

        /** Close the navigation drawer when it is open; true when it was. */
        fun closeDrawerIfOpen(): Boolean = false

        /**
         * Script page depth changed (pushPage/popPage). Hosts swap the left
         * header button: ☰ at the root page (depth 1), ✕ on pushed pages.
         */
        fun onScriptPageDepthChanged(depth: Int) {}

        /**
         * Root-page close gesture: drawer hosts open the drawer (leaving the
         * script is done from the drawer) and return true; standalone hosts
         * return false so the session ends instead.
         */
        fun openDrawer(): Boolean = false
    }

    private val logger = AndroidLogger()

    private lateinit var scriptContainer: FrameLayout
    private lateinit var consoleOutput: TextView
    private lateinit var stopButton: Button

    private val app get() = requireActivity().application as ScriptHostApplication
    private val scriptManager get() = app.scriptManager
    private val permissionManager get() = app.permissionManager
    private val host get() = activity as? Host

    private var scriptEngine: JavaScriptEngine? = null
    private var scriptContext: ScriptContext? = null
    private var uiBridge: UIBridge? = null

    /** Back closes the drawer, then pops a script page, then opens the drawer. */
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (host?.closeDrawerIfOpen() == true) return
            onCloseGesture()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return buildUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        // App option: keep the screen on for the duration of the session
        if (AppSettings(requireContext()).keepScreenOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val scriptId = arguments?.getString(ARG_SCRIPT_ID)
        if (scriptId != null) {
            loadAndRunScript(scriptId)
        } else {
            Toast.makeText(requireContext(), "No script specified", Toast.LENGTH_SHORT).show()
            endSession()
        }
    }

    private fun buildUI(): View {
        val context = requireContext()
        // Outermost container observes all touches (even ones children
        // consume): a quick rightward fling = same as the X button
        val swipeContainer = SubScreenChrome.swipeRightCloseContainer(context) {
            onCloseGesture()
        }
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // NOTE: no header here — the host activity owns the single slim
        // header bar (✕ + script name while running), so bars never stack.

        // Page host for script UI (UIBridge stacks pages inside it), wrapped
        // in a ScrollView so long script content scrolls. fillViewport keeps
        // short content filling the screen; the bridge's page model is
        // untouched — pages still live directly in scriptContainer.
        scriptContainer = FrameLayout(context)
        scriptContainer.setPadding(16, 16, 16, 16)
        val pageScroll = ScrollView(context).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        pageScroll.addView(scriptContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        rootLayout.addView(pageScroll)

        // Debug chrome (console + stop button) is only visible in debug mode;
        // sessions can otherwise be left/stopped from the drawer.
        val debugMode = AppSettings(context).debugMode
        val debugChrome = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (debugMode) View.VISIBLE else View.GONE
        }

        // Console output
        val consoleLabel = TextView(context).apply {
            text = context.getString(R.string.console_label)
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.LTGRAY)
        }
        debugChrome.addView(consoleLabel)

        val consoleScroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200
            )
        }

        consoleOutput = TextView(context).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setPadding(16, 8, 16, 8)
        }
        consoleScroll.addView(consoleOutput)
        debugChrome.addView(consoleScroll)

        // Stop button
        stopButton = Button(context).apply {
            text = context.getString(R.string.stop_script)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        debugChrome.addView(stopButton)

        rootLayout.addView(debugChrome)

        stopButton.setOnClickListener {
            stopScript()
        }

        swipeContainer.addView(rootLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        return swipeContainer
    }

    /**
     * Close gesture (✕ / right fling / Back): pops a PUSHED page; at the root
     * page it defers to the host — drawer hosts open the drawer (the root
     * page is the content home, not a closable sub-screen), standalone hosts
     * end the session. Public so the host's header button can trigger it.
     */
    fun onCloseGesture() {
        val bridge = uiBridge
        if (bridge != null && bridge.popPage()) {
            return
        }
        if (host?.openDrawer() != true) {
            endSession()
        }
    }

    /** Stop the running script and end the session (debug stop / drawer Stop). */
    fun requestStop() {
        stopScript()
    }

    private fun loadAndRunScript(scriptId: String) {
        val script = scriptManager.getScript(scriptId)
        if (script == null) {
            Toast.makeText(requireContext(), "Script not found", Toast.LENGTH_SHORT).show()
            endSession()
            return
        }
        if (script.sourceCode.isBlank()) {
            // Metadata entry exists but the source file is missing/empty —
            // running it would silently "succeed" doing nothing.
            Toast.makeText(
                requireContext(),
                getString(R.string.script_source_missing, script.name),
                Toast.LENGTH_LONG
            ).show()
            endSession()
            return
        }
        host?.onScriptSessionStarted(script.name)

        // Request permissions
        permissionManager.requestPermissions(requireActivity(), script) { granted, denied ->
            if (!isAdded) return@requestPermissions
            if (denied.isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Some permissions were denied: ${denied.joinToString { it.name }}",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Create script context
            scriptContext = ScriptContext(
                script = script,
                grantedPermissions = granted.toMutableSet(),
                startTime = System.currentTimeMillis()
            )

            // Run script
            runScript()
        }
    }

    private fun runScript() {
        val context = scriptContext ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Initialize engine
                scriptEngine = JavaScriptEngine(requireContext())

                // Register bridges
                val scriptId = context.script.id
                val uiBridge = UIBridge(requireContext(), scriptContainer)
                this@ScriptRuntimeFragment.uiBridge = uiBridge
                // Report page-depth changes so the host can swap ☰ ↔ ✕
                uiBridge.onPageDepthChanged = { depth ->
                    activity?.runOnUiThread {
                        if (isAdded) host?.onScriptPageDepthChanged(depth)
                    }
                }
                // Forward script-declared display titles to the top bar
                uiBridge.onSetTitle = { title ->
                    activity?.runOnUiThread {
                        if (isAdded) host?.onScriptTitle(title)
                    }
                }
                val systemBridge = SystemBridge(requireContext(), permissionManager, scriptId)
                val configBridge = ConfigBridge(
                    app.configStore, app.scriptConfigSchemas, permissionManager,
                    scriptId, context.script.name
                )
                val notificationBridge = NotificationBridge(requireContext(), permissionManager, scriptId)
                val sshBridge = SSHBridge(permissionManager, scriptId)

                scriptEngine?.registerBridge(uiBridge)
                scriptEngine?.registerBridge(systemBridge)
                scriptEngine?.registerBridge(configBridge)
                scriptEngine?.registerBridge(notificationBridge)
                scriptEngine?.registerBridge(sshBridge)

                // Execute script
                appendConsole("Starting script: ${context.script.name}")

                val result = scriptEngine?.execute(context)

                when (result) {
                    is ExecutionResult.Success -> {
                        appendConsole("Script completed successfully")
                        appendConsole("Output: ${result.output}")
                    }
                    is ExecutionResult.Error -> {
                        appendConsole("Script error: ${result.message}")
                        Toast.makeText(
                            requireContext(),
                            "Error: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    null -> {
                        appendConsole("Script execution failed")
                    }
                }

            } catch (e: Exception) {
                appendConsole("Exception: ${e.message}")
                logger.error(TAG, "Script execution failed", e)
            }
        }
    }

    private fun stopScript() {
        scriptContext?.let { context ->
            scriptEngine?.stop(context.script.id)
            appendConsole("Script stopped by user")
        }
        endSession()
    }

    /** End the session and hand control back to the host. */
    private fun endSession() {
        host?.onScriptSessionEnded() ?: activity?.finish()
    }

    private fun appendConsole(message: String) {
        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            val current = consoleOutput.text.toString()
            consoleOutput.text = if (current.isEmpty()) {
                message
            } else {
                "$current\n$message"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        scriptEngine?.release()
        scriptEngine = null
        uiBridge = null
    }

    companion object {
        private const val TAG = "ScriptRuntimeFragment"
        private const val ARG_SCRIPT_ID = "SCRIPT_ID"

        fun newInstance(scriptId: String): ScriptRuntimeFragment {
            return ScriptRuntimeFragment().apply {
                arguments = Bundle().apply { putString(ARG_SCRIPT_ID, scriptId) }
            }
        }
    }
}
