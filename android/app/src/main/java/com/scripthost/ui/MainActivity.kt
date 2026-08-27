package com.scripthost.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.scripthost.R
import com.scripthost.ScriptHostApplication
import com.scripthost.models.InstallResult
import com.scripthost.models.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Main Activity - Discord-style drawer layout.
 *
 * Left drawer (~280dp): fixed "SAM" header (with "script aggregation mobile"
 * subtitle) and a "Scripts" section label; the installed script list scrolls
 * beneath them; a compact icon row ("+" add script, gear settings) sits
 * directly below the list. Right content: ONE header bar (~72dp) above the
 * script runtime surface ([ScriptRuntimeFragment]) with a welcome state when
 * nothing is running. A running script's ROOT page is the content home — the
 * bar's left button is ☰ (opens the drawer) at the root page and swaps to ✕
 * on pushed script pages; leaving/stopping a script happens from the drawer.
 * The title shows "SAM / script aggregation mobile" idle or the script name
 * while running. The drawer starts open on cold start (no animation; honors
 * the "open drawer on launch" app option) and dims the content while open
 * via the DrawerLayout scrim — tapping the dimmed content closes the drawer.
 * No system ActionBar is used (NoActionBar theme); the system status bar is
 * tinted to match the theme background.
 */
class MainActivity : AppCompatActivity(), ScriptRuntimeFragment.Host {

    private var flingDownX = 0f
    private var flingTracker: android.view.VelocityTracker? = null

    /**
     * Rightward-fling handling for the content area.
     *
     * The fling is detected here, at the activity level, because DrawerLayout's
     * drag handling can reroute a gesture mid-stream, so a container deep in
     * the fragment never reliably sees ACTION_UP (observed on device). The
     * fragment-level container in SubScreenChrome still serves the standalone
     * hosts. Gestures starting in the drawer edge zone are ignored so opening
     * the drawer does not pop a script page. With a script running, the
     * fragment decides: pop a pushed page, or open the drawer at the root
     * page; with no script running, a right fling opens the drawer.
     */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        val edgePx = 24 * resources.displayMetrics.density
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                flingDownX = ev.x
                flingTracker?.recycle()
                flingTracker = if (ev.x < edgePx) null
                else android.view.VelocityTracker.obtain().apply { addMovement(ev) }
            }
            android.view.MotionEvent.ACTION_MOVE -> flingTracker?.addMovement(ev)
            android.view.MotionEvent.ACTION_UP -> {
                flingTracker?.let { tracker ->
                    tracker.addMovement(ev)
                    tracker.computeCurrentVelocity(1000)
                    val vx = tracker.xVelocity
                    val vy = tracker.yVelocity
                    val minDistancePx = 64 * resources.displayMetrics.density
                    if (ev.x - flingDownX > minDistancePx &&
                        vx > 800f && vx > kotlin.math.abs(vy) * 1.5f &&
                        !drawerLayout.isDrawerOpen(drawerPanel)
                    ) {
                        val handled =
                            (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
                                as? ScriptRuntimeFragment)?.let {
                                it.onCloseGesture(); true
                            } ?: false
                        if (!handled) {
                            // Idle content: right fling opens the drawer. This
                            // doubles as the edge-swipe path on systems (e.g.
                            // MIUI) whose back gesture owns the screen edge.
                            drawerLayout.openDrawer(drawerPanel)
                        }
                    }
                }
                flingTracker?.recycle()
                flingTracker = null
            }
            android.view.MotionEvent.ACTION_CANCEL -> {
                flingTracker?.recycle()
                flingTracker = null
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerPanel: LinearLayout
    private lateinit var scriptListView: ListView
    private lateinit var emptyStateView: View
    private lateinit var runtimeContainer: FrameLayout

    /** Header bar slots: the left button and the title swap as a pair. */
    private lateinit var hamburgerButton: View
    private lateinit var headerCloseButton: View
    private lateinit var headerIdleBlock: View
    private lateinit var headerScriptName: TextView

    private val app get() = application as ScriptHostApplication
    private val scriptManager get() = app.scriptManager

    private lateinit var scriptAdapter: ScriptAdapter

    /** Id of the script currently running in the content area, if any. */
    var runningScriptId: String? = null
        private set

    /** SAF file picker for ".js" import (specific MIME types with wildcard fallback). */
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importScriptFromUri(it) }
    }

    /** Back closes the drawer first; otherwise default back behavior. */
    private val drawerBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (drawerLayout.isDrawerOpen(drawerPanel)) {
                drawerLayout.closeDrawer(drawerPanel)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyStatusBarStyle()
        setupUI()
        loadScripts()
        onBackPressedDispatcher.addCallback(this, drawerBackCallback)

        if (savedInstanceState == null && app.appSettings.openDrawerOnLaunch) {
            // Cold start: drawer starts open, without animation
            drawerLayout.openDrawer(drawerPanel, false)
        }

        handleViewIntent(intent)

        installGuideOnFirstLaunch()
    }

    /**
     * On the very first launch, quietly install the bundled guide script and
     * run it, so a new user lands on working onboarding content instead of an
     * empty list. The flag lives in its own prefs file and is also set on
     * failure so a broken install does not retry every launch.
     */
    private fun installGuideOnFirstLaunch() {
        val prefs = getSharedPreferences(PREFS_APP_STATE, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_GUIDE_INSTALLED, false)) return

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val dest = File(File(filesDir, "imports").apply { mkdirs() }, GUIDE_EXAMPLE_FILE)
                    assets.open("$BUILTIN_EXAMPLES_DIR/$GUIDE_EXAMPLE_FILE").use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    scriptManager.installScriptFromFile(dest, verifySignature = false)
                } catch (e: Exception) {
                    InstallResult.Failure(e.message ?: "guide install failed")
                }
            }
            prefs.edit().putBoolean(KEY_GUIDE_INSTALLED, true).apply()
            loadScripts()
            (result as? InstallResult.Success)?.let { runScript(it.script) }
        }
    }

    /**
     * Tint the system status bar to the theme background and pick light/dark
     * status-bar icons for contrast, following the current night mode. The
     * activity recreates on theme changes, so this stays in sync.
     */
    private fun applyStatusBarStyle() {
        val background = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, background, true)
        window.statusBarColor = background.data

        val nightMode = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isLightBackground = nightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = isLightBackground
    }

    private fun setupUI() {
        drawerLayout = DrawerLayout(this)

        // ---- Right content area: slim header + script runtime surface ----
        val contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentColumn.addView(buildHeaderRow())
        // Hairline divider under the header
        contentColumn.addView(View(this).apply {
            setBackgroundColor(0x22000000)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))

        val contentFrame = FrameLayout(this)

        emptyStateView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            addView(TextView(context).apply {
                text = getString(R.string.welcome_title)
                textSize = 30f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = getString(R.string.welcome_subtitle)
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
            })
        }
        contentFrame.addView(emptyStateView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        runtimeContainer = FrameLayout(this).apply {
            id = View.generateViewId()
            visibility = View.GONE
        }
        contentFrame.addView(runtimeContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        contentColumn.addView(contentFrame, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        drawerLayout.addView(contentColumn, DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT,
            DrawerLayout.LayoutParams.MATCH_PARENT
        ))

        // ---- Left drawer panel ----
        drawerPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 40, 24, 24)
        }
        // Match the theme background so the panel is opaque
        val background = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, background, true)
        drawerPanel.setBackgroundColor(background.data)

        // 1. App title header + subtitle (fixed; the list scrolls beneath it)
        drawerPanel.addView(TextView(this).apply {
            text = "SAM"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 0)
        })
        drawerPanel.addView(TextView(this).apply {
            text = getString(R.string.app_subtitle)
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(8, 0, 8, 16)
        })

        // 2. Section label
        drawerPanel.addView(TextView(this).apply {
            text = getString(R.string.drawer_scripts_label)
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(8, 8, 8, 8)
        })

        // 3. Script list (scrolls independently under the fixed header)
        scriptListView = ListView(this)

        // 4. Compact icon row appended right after the last script entry:
        // "+" add, gear settings. Added as a list footer so it follows the
        // items instead of pinning to the panel bottom.
        val iconRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        iconRow.addView(TextView(this).apply {
            text = "+"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setPadding(24, 12, 24, 12)
            contentDescription = getString(R.string.cd_add_script)
            setOnClickListener { showAddScriptDialog() }
        })
        iconRow.addView(TextView(this).apply {
            text = "⚙"
            textSize = 22f
            setPadding(24, 12, 24, 12)
            contentDescription = getString(R.string.cd_settings)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        })
        scriptListView.addFooterView(iconRow, null, false)

        drawerPanel.addView(scriptListView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        val drawerWidth = (280 * resources.displayMetrics.density).toInt()
        drawerLayout.addView(drawerPanel, DrawerLayout.LayoutParams(
            drawerWidth,
            DrawerLayout.LayoutParams.MATCH_PARENT,
            Gravity.START
        ))

        // Dim the right content while the drawer is open; tapping the dim
        // closes the drawer (default DrawerLayout behavior)
        drawerLayout.setScrimColor(0x99000000.toInt())

        setContentView(drawerLayout)

        // Tap a script to run it; long-press for manage options
        scriptListView.setOnItemClickListener { _, _, position, _ ->
            runScript(scriptAdapter.getItem(position) as Script)
        }
        scriptListView.setOnItemLongClickListener { _, _, position, _ ->
            showScriptDetails(scriptAdapter.getItem(position) as Script)
            true
        }
    }

    /**
     * The single header bar (~72dp): exactly one left button at all times —
     * ☰ (opens the drawer; edge swipe also works, DrawerLayout default) when
     * idle, ✕ (close page/session) while a script runs. The title swaps
     * between the idle "SAM / script aggregation mobile" block and the
     * running script's name. Glyphs keep their size; extra height is padding.
     */
    private fun buildHeaderRow(): View {
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (HEADER_HEIGHT_DP * resources.displayMetrics.density).toInt()
            )
        }

        // Left button slot: ☰ and ✕ share the same spot, never both visible
        val leftSlot = FrameLayout(this)
        hamburgerButton = TextView(this).apply {
            text = "☰"
            textSize = 22f
            setPadding(20, 12, 20, 12)
            contentDescription = getString(R.string.drawer_open)
            setOnClickListener { drawerLayout.openDrawer(drawerPanel) }
        }
        headerCloseButton = SubScreenChrome.closeButton(this) { requestRuntimeClose() }.apply {
            visibility = View.GONE
        }
        leftSlot.addView(hamburgerButton)
        leftSlot.addView(headerCloseButton)
        headerRow.addView(leftSlot)

        // Title slot: idle block and script name share the same spot
        val titleSlot = FrameLayout(this)
        headerIdleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            addView(TextView(context).apply {
                text = "SAM"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = getString(R.string.app_subtitle)
                textSize = 11f
                setTextColor(Color.GRAY)
            })
        }
        headerScriptName = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 8)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
            )
        }
        titleSlot.addView(headerIdleBlock)
        titleSlot.addView(headerScriptName)
        headerRow.addView(titleSlot)

        return headerRow
    }

    /** Forward a header ✕ tap to the running fragment (pop a pushed page). */
    private fun requestRuntimeClose() {
        (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScriptRuntimeFragment)
            ?.onCloseGesture()
    }

    private fun loadScripts() {
        scriptAdapter = ScriptAdapter(this, scriptManager.getAllScripts())
        scriptListView.adapter = scriptAdapter
    }

    // ------------------------------------------------------------------
    // Script runtime hosting (drawer content area)
    // ------------------------------------------------------------------

    private fun runScript(script: Script) {
        drawerLayout.closeDrawer(drawerPanel)
        runningScriptId = script.id
        loadScripts() // refresh the drawer to highlight the running script
        emptyStateView.visibility = View.GONE
        runtimeContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(runtimeContainer.id, ScriptRuntimeFragment.newInstance(script.id), FRAGMENT_TAG)
            .commit()
    }

    override fun onScriptSessionStarted(scriptName: String) {
        headerScriptName.text = scriptName
        // Root page = content home: ☰ stays, ✕ only appears on pushed pages
        hamburgerButton.visibility = View.VISIBLE
        headerCloseButton.visibility = View.GONE
        headerIdleBlock.visibility = View.GONE
        headerScriptName.visibility = View.VISIBLE
    }

    override fun onScriptSessionEnded() {
        runningScriptId = null
        loadScripts() // clear the running-script highlight
        supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let { fragment ->
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
        runtimeContainer.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        hamburgerButton.visibility = View.VISIBLE
        headerCloseButton.visibility = View.GONE
        headerIdleBlock.visibility = View.VISIBLE
        headerScriptName.visibility = View.GONE
    }

    override fun onScriptPageDepthChanged(depth: Int) {
        // ☰ at the root page (depth 1), ✕ on pushed pages — same slot
        val onSubPage = depth > 1
        hamburgerButton.visibility = if (onSubPage) View.GONE else View.VISIBLE
        headerCloseButton.visibility = if (onSubPage) View.VISIBLE else View.GONE
    }

    override fun openDrawer(): Boolean {
        drawerLayout.openDrawer(drawerPanel)
        return true
    }

    override fun closeDrawerIfOpen(): Boolean {
        return if (drawerLayout.isDrawerOpen(drawerPanel)) {
            drawerLayout.closeDrawer(drawerPanel)
            true
        } else {
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        app.permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // ------------------------------------------------------------------
    // Add / import scripts
    // ------------------------------------------------------------------

    private fun showAddScriptDialog() {
        val options = arrayOf(
            getString(R.string.create_new_script),
            getString(R.string.import_from_file),
            getString(R.string.builtin_examples)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_script_dialog_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ScriptEditorActivity::class.java))
                    1 -> importLauncher.launch(
                        arrayOf("text/javascript", "application/javascript", "*/*")
                    )
                    2 -> showBuiltinExamplesDialog()
                }
            }
            .show()
    }

    /** Picker for the examples bundled in assets (see builtin_examples/). */
    private fun showBuiltinExamplesDialog() {
        val files = try {
            assets.list(BUILTIN_EXAMPLES_DIR)?.sorted() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        if (files.isEmpty()) {
            Toast.makeText(this, R.string.import_read_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val displayNames = files.map { builtinDisplayName(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.builtin_examples_title))
            .setItems(displayNames) { _, which -> installBuiltinExample(files[which]) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /** "todo_list.js" -> "Todo List" */
    private fun builtinDisplayName(filename: String): String {
        return filename.removeSuffix(".js").split('_')
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
    }

    /**
     * Install a bundled example: copy the asset into filesDir/imports, then
     * install it via [com.scripthost.engine.ScriptManager.installScriptFromFile]
     * (same pattern as the URI import).
     */
    private fun installBuiltinExample(filename: String) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val dest = File(File(filesDir, "imports").apply { mkdirs() }, filename)
                    assets.open("$BUILTIN_EXAMPLES_DIR/$filename").use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    scriptManager.installScriptFromFile(dest, verifySignature = false)
                } catch (e: Exception) {
                    InstallResult.Failure(getString(R.string.import_failed, e.message ?: ""))
                }
            }

            when (result) {
                is InstallResult.Success -> {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.script_installed_toast, result.script.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    if (filename == AGENT_EXAMPLE_FILE) {
                        // The agent chat needs API config to be useful
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.agent_config_hint),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    loadScripts()
                }
                is InstallResult.Failure -> {
                    Toast.makeText(this@MainActivity, result.error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Handle "open with" VIEW intents for .js files (see manifest). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { importScriptFromUri(it) }
            // Consume so a configuration change doesn't re-import
            intent.data = null
        }
    }

    /**
     * Copy the picked file into app-private storage, then install it via
     * [com.scripthost.engine.ScriptManager.installScriptFromFile].
     */
    private fun importScriptFromUri(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val displayName = queryDisplayName(uri)
                        ?: "imported_${System.currentTimeMillis()}.js"
                    // Only script sources and script packages are importable
                    val lowerName = displayName.lowercase()
                    if (!lowerName.endsWith(".js") && !lowerName.endsWith(".json")) {
                        return@withContext InstallResult.Failure(
                            getString(R.string.import_unsupported, displayName)
                        )
                    }
                    val importsDir = File(filesDir, "imports").apply { mkdirs() }
                    val dest = File(importsDir, File(displayName).name)
                    contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@withContext InstallResult.Failure(
                        getString(R.string.import_read_failed)
                    )

                    scriptManager.installScriptFromFile(dest, verifySignature = false)
                } catch (e: Exception) {
                    InstallResult.Failure(getString(R.string.import_failed, e.message ?: ""))
                }
            }

            when (result) {
                is InstallResult.Success -> {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.script_installed_toast, result.script.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadScripts()
                }
                is InstallResult.Failure -> {
                    Toast.makeText(this@MainActivity, result.error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------
    // Script management (long-press)
    // ------------------------------------------------------------------

    private fun showScriptDetails(script: Script) {
        // A running script can also be stopped from here (the other ways out
        // are picking another script or the debug-mode Stop Script button)
        val isRunning = script.id == runningScriptId
        val options = if (isRunning) {
            arrayOf(
                getString(R.string.action_stop),
                getString(R.string.action_edit),
                getString(R.string.action_export),
                getString(R.string.action_delete)
            )
        } else {
            arrayOf(
                getString(R.string.action_edit),
                getString(R.string.action_export),
                getString(R.string.action_delete)
            )
        }

        // NOTE: AlertDialog cannot show a message and an items list together —
        // the message hides the items, so details go into the title instead.
        AlertDialog.Builder(this)
            .setTitle("${script.name}  v${script.version}")
            .setItems(options) { _, which ->
                when (options[which]) {
                    getString(R.string.action_stop) -> stopRunningScript()
                    getString(R.string.action_edit) -> editScript(script)
                    getString(R.string.action_export) -> exportScript(script)
                    getString(R.string.action_delete) -> deleteScript(script)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /** Stop the script running in the content area, if any. */
    private fun stopRunningScript() {
        (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScriptRuntimeFragment)
            ?.requestStop()
    }

    private fun editScript(script: Script) {
        val intent = Intent(this, ScriptEditorActivity::class.java)
        intent.putExtra("SCRIPT_ID", script.id)
        startActivity(intent)
    }

    private fun exportScript(script: Script) {
        // Minimal export: write the script source to the app-specific
        // external directory and show where it landed.
        try {
            val dir = File(getExternalFilesDir(null), "exports").apply { mkdirs() }
            val out = File(dir, "${script.id}.js")
            out.writeText(script.sourceCode)
            Toast.makeText(this, getString(R.string.exported_toast, out.absolutePath), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteScript(script: Script) {
        AlertDialog.Builder(this)
            .setTitle("Delete Script")
            .setMessage("Are you sure you want to delete ${script.name}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    scriptManager.uninstallScript(script.id)
                    loadScripts()
                    Toast.makeText(this@MainActivity, "Script deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadScripts()
    }

    companion object {
        private const val FRAGMENT_TAG = "script_runtime"

        /** Header bar height (~1.8x the original slim bar). */
        private const val HEADER_HEIGHT_DP = 72

        /** Asset directory holding the bundled example scripts. */
        private const val BUILTIN_EXAMPLES_DIR = "builtin_examples"

        /** Bundled agent-chat example; needs API keys configured in Settings. */
        private const val AGENT_EXAMPLE_FILE = "agent_conversation.js"

        /** Bundled onboarding guide, auto-installed and run on first launch. */
        private const val GUIDE_EXAMPLE_FILE = "guide.js"

        /** App-level UI state (first-launch flags); separate from AppSettings. */
        private const val PREFS_APP_STATE = "app_state"
        private const val KEY_GUIDE_INSTALLED = "guide_installed"
    }
}

/**
 * Adapter for script list
 */
class ScriptAdapter(
    private val context: MainActivity,
    private val scripts: List<Script>
) : BaseAdapter() {

    override fun getCount(): Int = scripts.size

    override fun getItem(position: Int): Any = scripts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val script = scripts[position]
        val isRunning = script.id == context.runningScriptId

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            // Translucent accent tint marks the currently running script;
            // readable in both light and dark themes
            if (isRunning) setBackgroundColor(RUNNING_BACKGROUND)
        }

        val nameText = TextView(context).apply {
            text = script.name
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            if (isRunning) setTextColor(RUNNING_ACCENT)
        }
        layout.addView(nameText)

        val descText = TextView(context).apply {
            text = script.description
            textSize = 14f
        }
        layout.addView(descText)

        val infoText = TextView(context).apply {
            text = "v${script.version} by ${script.author}"
            textSize = 12f
            setTextColor(Color.GRAY)
        }
        layout.addView(infoText)

        return layout
    }

    companion object {
        private const val RUNNING_BACKGROUND = 0x22007AFF
        private const val RUNNING_ACCENT = 0xFF007AFF.toInt()
    }
}
