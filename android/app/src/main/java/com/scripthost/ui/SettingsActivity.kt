package com.scripthost.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.scripthost.R
import com.scripthost.ScriptHostApplication

/**
 * Settings Activity - full-screen sub-screen for app configuration.
 *
 * Sections (top to bottom):
 *  1. App: debug mode, appearance, script timeout, keep screen on, open
 *     drawer on launch — app-only preferences persisted in
 *     [com.scripthost.config.AppSettings] (SharedPreferences).
 *  2. Installed scripts: declared permissions plus granted permissions with
 *     per-permission revoke.
 *  3. Script config sections (bottom): for every installed script that
 *     declared a config schema via `Config.schema(...)` (see
 *     [com.scripthost.config.ScriptConfigSchemas]), one labeled control per
 *     field with a per-section Save button persisting to
 *     [com.scripthost.config.ConfigStore] under the field keys.
 *
 * Chrome follows the sub-screen spec: a top-left X closes the screen, and a
 * rightward swipe anywhere on it does the same.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var scriptsContainer: LinearLayout
    private lateinit var scriptConfigContainer: LinearLayout

    private val app get() = application as ScriptHostApplication
    private val configStore get() = app.configStore
    private val scriptManager get() = app.scriptManager
    private val permissionManager get() = app.permissionManager
    private val appSettings get() = app.appSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        loadScriptPermissions()
        loadScriptConfigSections()
    }

    private fun setupUI() {
        // Outermost container observes all touches: a quick rightward fling
        // anywhere on the screen closes it
        val swipeContainer = SubScreenChrome.swipeRightCloseContainer(this) { finish() }

        val scrollView = ScrollView(this)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Header: X at top-left closes the screen
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        headerRow.addView(SubScreenChrome.closeButton(this) { finish() })
        headerRow.addView(TextView(this).apply {
            text = getString(R.string.settings_title)
            textSize = 24f
        })
        rootLayout.addView(headerRow)

        // ---- App-level preferences ----
        setupAppSection(rootLayout)

        // ---- Per-installed-script permissions ----
        rootLayout.addView(TextView(this).apply {
            text = getString(R.string.installed_scripts_section)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 32, 0, 8)
        })

        scriptsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootLayout.addView(scriptsContainer)

        // ---- Script-provided config sections (bottom) ----
        scriptConfigContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootLayout.addView(scriptConfigContainer)

        scrollView.addView(rootLayout)
        swipeContainer.addView(scrollView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(swipeContainer)
    }

    /** "App" section: app-level preferences, applied immediately. */
    private fun setupAppSection(rootLayout: LinearLayout) {
        rootLayout.addView(TextView(this).apply {
            text = getString(R.string.settings_app_section)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        })

        // Debug mode: mirror script console.* messages to Logcat
        rootLayout.addView(Switch(this).apply {
            text = getString(R.string.debug_mode_label)
            textSize = 15f
            isChecked = appSettings.debugMode
            setOnCheckedChangeListener { _, isChecked ->
                appSettings.debugMode = isChecked
            }
        })

        // Appearance: follow system / light / dark
        val modes = listOf(
            getString(R.string.theme_follow_system) to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            getString(R.string.theme_light) to AppCompatDelegate.MODE_NIGHT_NO,
            getString(R.string.theme_dark) to AppCompatDelegate.MODE_NIGHT_YES
        )

        val appearanceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }
        appearanceRow.addView(TextView(this).apply {
            text = getString(R.string.appearance_label)
            textSize = 15f
            setPadding(0, 0, 16, 0)
        })

        var spinnerReady = false
        val modeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_spinner_item,
                modes.map { it.first }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        modeSpinner.setSelection(modes.indexOfFirst { it.second == appSettings.nightMode }
            .coerceAtLeast(0))
        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // setSelection fires the listener once during setup; skip it
                if (!spinnerReady) {
                    spinnerReady = true
                    return
                }
                val mode = modes[position].second
                appSettings.nightMode = mode
                AppCompatDelegate.setDefaultNightMode(mode)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        appearanceRow.addView(modeSpinner)
        rootLayout.addView(appearanceRow)

        // Script timeout (seconds): persisted as soon as a valid number is typed
        val timeoutRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }
        timeoutRow.addView(TextView(this).apply {
            text = getString(R.string.script_timeout_label)
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })
        timeoutRow.addView(EditText(this).apply {
            setText(appSettings.engineTimeoutSeconds.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine()
            setEms(4)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val seconds = s?.toString()?.trim()?.toIntOrNull() ?: return
                    if (seconds >= 1) {
                        appSettings.engineTimeoutSeconds = seconds
                    }
                }
            })
        })
        rootLayout.addView(timeoutRow)

        // Keep screen on while a script is running
        rootLayout.addView(Switch(this).apply {
            text = getString(R.string.keep_screen_on_label)
            textSize = 15f
            isChecked = appSettings.keepScreenOn
            setOnCheckedChangeListener { _, isChecked ->
                appSettings.keepScreenOn = isChecked
            }
        })

        // Open the script-list drawer automatically on launch
        rootLayout.addView(Switch(this).apply {
            text = getString(R.string.open_drawer_on_launch_label)
            textSize = 15f
            isChecked = appSettings.openDrawerOnLaunch
            setOnCheckedChangeListener { _, isChecked ->
                appSettings.openDrawerOnLaunch = isChecked
            }
        })
    }

    /** List installed scripts with declared permissions and revocable grants. */
    private fun loadScriptPermissions() {
        scriptsContainer.removeAllViews()

        val scripts = scriptManager.getAllScripts()
        if (scripts.isEmpty()) {
            scriptsContainer.addView(TextView(this).apply {
                text = getString(R.string.no_scripts_installed)
                textSize = 14f
                setTextColor(android.graphics.Color.GRAY)
                setPadding(16, 16, 16, 16)
            })
            return
        }

        scripts.forEach { script ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            card.addView(TextView(this).apply {
                text = script.name
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(this).apply {
                text = "v${script.version} by ${script.author}"
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            })
            card.addView(TextView(this).apply {
                val declared = script.permissions.joinToString { it.name }
                text = getString(
                    R.string.declared_permissions,
                    declared.ifEmpty { getString(R.string.none_value) }
                )
                textSize = 13f
                setPadding(0, 8, 0, 0)
            })

            val granted = permissionManager.getGrantedPermissions(script.id)
            if (granted.isEmpty()) {
                card.addView(TextView(this).apply {
                    text = getString(R.string.granted_none)
                    textSize = 13f
                    setTextColor(android.graphics.Color.GRAY)
                    setPadding(0, 4, 0, 0)
                })
            } else {
                granted.sortedBy { it.name }.forEach { permission ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(0, 8, 0, 0)
                    }
                    row.addView(TextView(this).apply {
                        text = "${permission.name} - ${permission.description}"
                        textSize = 13f
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    })
                    row.addView(Button(this).apply {
                        text = getString(R.string.revoke)
                        setOnClickListener {
                            permissionManager.revokePermission(script.id, permission)
                            Toast.makeText(
                                this@SettingsActivity,
                                getString(R.string.permission_revoked, script.name, permission.name),
                                Toast.LENGTH_SHORT
                            ).show()
                            loadScriptPermissions()
                        }
                    })
                    card.addView(row)
                }
            }

            scriptsContainer.addView(card)
        }
    }

    /**
     * Render one config section per installed script that declared a schema
     * via `Config.schema(...)`. Values live in ConfigStore under the field
     * keys (global namespace); keys not present in the schema are untouched.
     */
    private fun loadScriptConfigSections() {
        scriptConfigContainer.removeAllViews()

        val installedIds = scriptManager.getAllScripts().map { it.id }.toSet()
        app.scriptConfigSchemas.all().forEach { (scriptId, schema) ->
            if (scriptId !in installedIds || schema.fields.isEmpty()) return@forEach

            scriptConfigContainer.addView(TextView(this).apply {
                text = schema.name
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 32, 0, 8)
            })

            // Field key -> read current control value
            val readers = mutableListOf<Pair<String, () -> String>>()

            schema.fields.forEach { field ->
                scriptConfigContainer.addView(TextView(this).apply {
                    text = field.label
                    textSize = 15f
                    setPadding(0, 8, 0, 0)
                })

                val stored = configStore.get(field.key)
                when (field.type) {
                    "boolean" -> {
                        val toggle = Switch(this).apply {
                            isChecked = (stored ?: field.default).toBoolean()
                        }
                        scriptConfigContainer.addView(toggle)
                        readers += field.key to { toggle.isChecked.toString() }
                    }
                    "select" -> {
                        val options = field.options.orEmpty()
                        val spinner = Spinner(this).apply {
                            adapter = ArrayAdapter(
                                this@SettingsActivity,
                                android.R.layout.simple_spinner_item,
                                options
                            ).apply {
                                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            }
                        }
                        val selected = options.indexOf(stored ?: field.default)
                        spinner.setSelection(selected.coerceAtLeast(0))
                        scriptConfigContainer.addView(spinner)
                        readers += field.key to {
                            options.getOrElse(spinner.selectedItemPosition) { options.first() }
                        }
                    }
                    else -> {
                        val input = EditText(this).apply {
                            setText(stored ?: field.default.orEmpty())
                            setSingleLine()
                            inputType = when (field.type) {
                                "password" -> InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD
                                "number" -> InputType.TYPE_CLASS_NUMBER or
                                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                                    InputType.TYPE_NUMBER_FLAG_SIGNED
                                else -> InputType.TYPE_CLASS_TEXT
                            }
                        }
                        scriptConfigContainer.addView(input)
                        readers += field.key to { input.text.toString().trim() }
                    }
                }
            }

            scriptConfigContainer.addView(Button(this).apply {
                text = getString(R.string.save)
                setOnClickListener {
                    readers.forEach { (key, read) -> configStore.put(key, read()) }
                    Toast.makeText(
                        this@SettingsActivity,
                        getString(R.string.script_config_saved, schema.name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        loadScriptPermissions()
        loadScriptConfigSections()
    }
}
