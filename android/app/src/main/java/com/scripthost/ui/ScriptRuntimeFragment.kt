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
import com.scripthost.ScriptHostApplication
import com.scripthost.bridge.ConfigBridge
import com.scripthost.bridge.NotificationBridge
import com.scripthost.bridge.SSHBridge
import com.scripthost.bridge.SystemBridge
import com.scripthost.bridge.UIBridge
import com.scripthost.engine.ExecutionResult
import com.scripthost.engine.JavaScriptEngine
import com.scripthost.models.ScriptContext
import com.scripthost.util.AndroidLogger
import kotlinx.coroutines.launch

/**
 * Script Runtime Fragment - Execute scripts with UI rendering.
 *
 * Hosted either by [MainActivity] (embedded in the drawer content area) or by
 * [ScriptRuntimeActivity] (standalone). The host owns the single slim header
 * bar (✕ + script name while running, reported via [Host]); the fragment
 * itself adds no header so bars never stack. A rightward swipe on the
 * surface closes the current script page (popPage); at the root page it ends
 * the session and the host returns to its empty state.
 */
class ScriptRuntimeFragment : Fragment() {

    /** Callbacks the hosting activity provides for session lifecycle. */
    interface Host {
        /** A script started running; the host shows the running header state. */
        fun onScriptSessionStarted(scriptName: String) {}

        /** The script session ended (stop button, X/back at the root page). */
        fun onScriptSessionEnded()

        /** Close the navigation drawer when it is open; true when it was. */
        fun closeDrawerIfOpen(): Boolean = false
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

    /** Back closes the drawer, then pops a script page, then ends the session. */
    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (host?.closeDrawerIfOpen() == true) return
            closePageOrSession()
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
            closePageOrSession()
        }
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // NOTE: no header here — the host activity owns the single slim
        // header bar (✕ + script name while running), so bars never stack.

        // Page host for script UI (UIBridge stacks pages inside it)
        scriptContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(16, 16, 16, 16)
        }
        rootLayout.addView(scriptContainer)

        // Console output
        val consoleLabel = TextView(context).apply {
            text = "Console:"
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.LTGRAY)
        }
        rootLayout.addView(consoleLabel)

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
        rootLayout.addView(consoleScroll)

        // Stop button
        stopButton = Button(context).apply {
            text = "Stop Script"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(stopButton)

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
     * Pop the top script page; at the root page, end the whole session.
     * Public so the host's header ✕ button can trigger the same behavior.
     */
    fun closePageOrSession() {
        val bridge = uiBridge
        if (bridge == null || !bridge.popPage()) {
            endSession()
        }
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
            Toast.makeText(requireContext(), "脚本源文件缺失: ${script.name}", Toast.LENGTH_LONG).show()
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
                val systemBridge = SystemBridge(requireContext(), permissionManager, scriptId)
                val configBridge = ConfigBridge(app.configStore, permissionManager, scriptId)
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
