package com.scripthost.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scripthost.ScriptHostApplication
import com.scripthost.bridge.ConfigBridge
import com.scripthost.bridge.SystemBridge
import com.scripthost.bridge.UIBridge
import com.scripthost.engine.ExecutionResult
import com.scripthost.engine.JavaScriptEngine
import com.scripthost.models.ScriptContext
import com.scripthost.models.ScriptState
import kotlinx.coroutines.launch

/**
 * Script Runtime Activity - Execute scripts with UI rendering
 */
class ScriptRuntimeActivity : AppCompatActivity() {

    private lateinit var scriptContainer: LinearLayout
    private lateinit var consoleOutput: TextView
    private lateinit var stopButton: Button

    private val app get() = application as ScriptHostApplication
    private val scriptManager get() = app.scriptManager
    private val permissionManager get() = app.permissionManager

    private var scriptEngine: JavaScriptEngine? = null
    private var scriptContext: ScriptContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()

        val scriptId = intent.getStringExtra("SCRIPT_ID")
        if (scriptId != null) {
            loadAndRunScript(scriptId)
        } else {
            Toast.makeText(this, "No script specified", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Script container (where UI components are added)
        scriptContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(16, 16, 16, 16)
        }
        rootLayout.addView(scriptContainer)

        // Console output
        val consoleLabel = TextView(this).apply {
            text = "Console:"
            setPadding(16, 8, 16, 8)
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }
        rootLayout.addView(consoleLabel)

        val consoleScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200
            )
        }

        consoleOutput = TextView(this).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
            setPadding(16, 8, 16, 8)
        }
        consoleScroll.addView(consoleOutput)
        rootLayout.addView(consoleScroll)

        // Stop button
        stopButton = Button(this).apply {
            text = "Stop Script"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(stopButton)

        setContentView(rootLayout)

        stopButton.setOnClickListener {
            stopScript()
        }
    }

    private fun loadAndRunScript(scriptId: String) {
        val script = scriptManager.getScript(scriptId)
        if (script == null) {
            Toast.makeText(this, "Script not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Request permissions
        permissionManager.requestPermissions(this, script) { granted, denied ->
            if (denied.isNotEmpty()) {
                Toast.makeText(
                    this,
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

        lifecycleScope.launch {
            try {
                // Initialize engine
                scriptEngine = JavaScriptEngine(this@ScriptRuntimeActivity)

                // Register bridges
                val uiBridge = UIBridge(this@ScriptRuntimeActivity, scriptContainer)
                val systemBridge = SystemBridge(this@ScriptRuntimeActivity, permissionManager)
                val configBridge = ConfigBridge(app.configStore, permissionManager)

                scriptEngine?.registerBridge(uiBridge)
                scriptEngine?.registerBridge(systemBridge)
                scriptEngine?.registerBridge(configBridge)

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
                            this@ScriptRuntimeActivity,
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
                e.printStackTrace()
            }
        }
    }

    private fun stopScript() {
        scriptContext?.let { context ->
            scriptEngine?.stop(context.script.id)
            appendConsole("Script stopped by user")
        }
        finish()
    }

    private fun appendConsole(message: String) {
        runOnUiThread {
            val current = consoleOutput.text.toString()
            consoleOutput.text = if (current.isEmpty()) {
                message
            } else {
                "$current\n$message"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        scriptEngine?.release()
    }
}
