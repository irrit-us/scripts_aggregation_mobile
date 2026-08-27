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
 * Left drawer (~280dp): "SAM" header (with "script aggregation mobile"
 * subtitle), a "脚本列表" section label, the installed script list, and
 * bottom-pinned "添加脚本" / "设置" buttons. Right content: ONE slim header
 * bar ([☰] + title) above the script runtime surface
 * ([ScriptRuntimeFragment]) with an empty/welcome state when nothing is
 * running. The header title shows "SAM / script aggregation mobile" when
 * idle and "✕ + script name" while a script runs (same bar, no stacking).
 * The drawer starts open on cold start (no animation) and dims the content
 * while open via the DrawerLayout scrim — tapping the dimmed content closes
 * the drawer. No system ActionBar is used (NoActionBar theme).
 */
class MainActivity : AppCompatActivity(), ScriptRuntimeFragment.Host {

    private var flingDownX = 0f
    private var flingTracker: android.view.VelocityTracker? = null

    /**
     * Rightward-fling-to-close for the embedded script runtime.
     *
     * The fling is detected here, at the activity level, because DrawerLayout's
     * drag handling can reroute a gesture mid-stream, so a container deep in
     * the fragment never reliably sees ACTION_UP (observed on device). The
     * fragment-level container in SubScreenChrome still serves the standalone
     * hosts. Gestures starting in the drawer edge zone are ignored so opening
     * the drawer does not close a script page.
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
                                it.closePageOrSession(); true
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
    private lateinit var emptyStateView: TextView
    private lateinit var runtimeContainer: FrameLayout

    /** Header title blocks: exactly one is visible at a time. */
    private lateinit var headerIdleBlock: View
    private lateinit var headerRunningBlock: View
    private lateinit var headerScriptName: TextView

    private val app get() = application as ScriptHostApplication
    private val scriptManager get() = app.scriptManager

    private lateinit var scriptAdapter: ScriptAdapter

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
        setupUI()
        loadScripts()
        onBackPressedDispatcher.addCallback(this, drawerBackCallback)

        if (savedInstanceState == null) {
            // Cold start: drawer starts open, without animation
            drawerLayout.openDrawer(drawerPanel, false)
        }

        handleViewIntent(intent)
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

        emptyStateView = TextView(this).apply {
            text = "从左侧列表选择一个脚本开始运行"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
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

        // 1. App title header + subtitle
        drawerPanel.addView(TextView(this).apply {
            text = "SAM"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setPadding(8, 8, 8, 0)
        })
        drawerPanel.addView(TextView(this).apply {
            text = "script aggregation mobile"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(8, 0, 8, 16)
        })

        // 2. Section label
        drawerPanel.addView(TextView(this).apply {
            text = "脚本列表"
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(8, 8, 8, 8)
        })

        // 3. Script list
        scriptListView = ListView(this)
        drawerPanel.addView(scriptListView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        // 4. Bottom-pinned controls
        drawerPanel.addView(Button(this).apply {
            text = "添加脚本"
            setOnClickListener { showAddScriptDialog() }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        drawerPanel.addView(Button(this).apply {
            text = "设置"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
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
     * The single slim header bar: ☰ opens the drawer (edge swipe also works,
     * DrawerLayout default); the title area swaps between the idle
     * "SAM / script aggregation mobile" block and the running
     * "✕ + script name" block.
     */
    private fun buildHeaderRow(): View {
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 8, 0)
        }

        headerRow.addView(TextView(this).apply {
            text = "☰"
            textSize = 22f
            setPadding(20, 12, 20, 12)
            contentDescription = getString(R.string.drawer_open)
            setOnClickListener { drawerLayout.openDrawer(drawerPanel) }
        })

        headerIdleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            addView(TextView(context).apply {
                text = "SAM"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = "script aggregation mobile"
                textSize = 11f
                setTextColor(Color.GRAY)
            })
        }
        headerRow.addView(headerIdleBlock)

        headerRunningBlock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            addView(SubScreenChrome.closeButton(context) { requestRuntimeClose() })
            headerScriptName = TextView(context).apply {
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            }
            addView(headerScriptName)
        }
        headerRow.addView(headerRunningBlock)

        return headerRow
    }

    /** Forward a header ✕ tap to the running fragment (pop page / end session). */
    private fun requestRuntimeClose() {
        (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScriptRuntimeFragment)
            ?.closePageOrSession()
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
        emptyStateView.visibility = View.GONE
        runtimeContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(runtimeContainer.id, ScriptRuntimeFragment.newInstance(script.id), FRAGMENT_TAG)
            .commit()
    }

    override fun onScriptSessionStarted(scriptName: String) {
        headerScriptName.text = scriptName
        headerIdleBlock.visibility = View.GONE
        headerRunningBlock.visibility = View.VISIBLE
    }

    override fun onScriptSessionEnded() {
        supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let { fragment ->
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
        runtimeContainer.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        headerIdleBlock.visibility = View.VISIBLE
        headerRunningBlock.visibility = View.GONE
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
        val options = arrayOf("创建新脚本", "从文件导入")

        AlertDialog.Builder(this)
            .setTitle("添加脚本")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ScriptEditorActivity::class.java))
                    1 -> importLauncher.launch(
                        arrayOf("text/javascript", "application/javascript", "*/*")
                    )
                }
            }
            .show()
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
                            "仅支持导入 .js 脚本或 .json 脚本包: $displayName"
                        )
                    }
                    val importsDir = File(filesDir, "imports").apply { mkdirs() }
                    val dest = File(importsDir, File(displayName).name)
                    contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@withContext InstallResult.Failure("无法读取所选文件")

                    scriptManager.installScriptFromFile(dest, verifySignature = false)
                } catch (e: Exception) {
                    InstallResult.Failure("导入失败: ${e.message}")
                }
            }

            when (result) {
                is InstallResult.Success -> {
                    Toast.makeText(
                        this@MainActivity,
                        "脚本已安装: ${result.script.name}",
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
        val options = arrayOf("Edit", "Export", "Delete")

        // NOTE: AlertDialog cannot show a message and an items list together —
        // the message hides the items, so details go into the title instead.
        AlertDialog.Builder(this)
            .setTitle("${script.name}  v${script.version}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editScript(script)
                    1 -> exportScript(script)
                    2 -> deleteScript(script)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            Toast.makeText(this, "已导出: ${out.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
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

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val nameText = TextView(context).apply {
            text = script.name
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
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
}
