package com.scripthost.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.scripthost.R
import com.scripthost.ScriptHostApplication

/**
 * Settings Activity - full-screen sub-screen for app configuration.
 *
 * Sections:
 *  1. App: debug-mode toggle (script console mirrored to Logcat) and
 *     light/dark appearance — app-only preferences persisted in
 *     [com.scripthost.config.AppSettings] (SharedPreferences).
 *  2. App-level config keys (API keys etc.) backed by [com.scripthost.config.ConfigStore],
 *     values masked in the list. Scripts with the CONFIG permission read them
 *     through the `Config` bridge (e.g. `Config.get("OPENAI_API_KEY")`).
 *  3. Installed scripts: declared permissions plus granted permissions with
 *     per-permission revoke.
 *
 * Chrome follows the sub-screen spec: a top-left X closes the screen, and a
 * rightward swipe anywhere on it does the same.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var configListContainer: LinearLayout
    private lateinit var scriptsContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var configAdapter: ConfigAdapter

    private val app get() = application as ScriptHostApplication
    private val configStore get() = app.configStore
    private val scriptManager get() = app.scriptManager
    private val permissionManager get() = app.permissionManager
    private val appSettings get() = app.appSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        loadConfig()
        loadScriptPermissions()
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

        rootLayout.addView(TextView(this).apply {
            text = "Configure API keys and settings. Scripts with the CONFIG " +
                "permission can read these values, so only install trusted scripts."
            textSize = 13f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 0, 0, 16)
        })

        emptyText = TextView(this).apply {
            text = "No keys configured. Tap \"Add Key\" to get started."
            textSize = 14f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(16, 24, 16, 24)
        }
        rootLayout.addView(emptyText)

        configListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootLayout.addView(configListContainer)

        rootLayout.addView(Button(this).apply {
            text = "Add Key"
            setOnClickListener { showEditDialog(null) }
        })

        // Per-installed-script permissions section
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

        scrollView.addView(rootLayout)
        swipeContainer.addView(scrollView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(swipeContainer)
    }

    /** "App" section: debug-mode toggle and light/dark appearance. */
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
            setPadding(0, 8, 0, 16)
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
    }

    private fun loadConfig() {
        val entries = configStore.all().entries.sortedBy { it.key }

        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE

        configAdapter = ConfigAdapter(this, entries)
        configListContainer.removeAllViews()
        for (i in 0 until configAdapter.count) {
            val row = configAdapter.getView(i, null, configListContainer)
            val key = configAdapter.getItem(i)
            row.setOnClickListener { showEditDialog(key) }
            configListContainer.addView(row)
        }
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

    private fun showEditDialog(key: String?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 0)
        }

        val keyInput = EditText(this).apply {
            hint = "Key (e.g. OPENAI_API_KEY)"
            setText(key ?: "")
            // The key is the identity of an entry; rename via delete + re-add.
            isEnabled = key == null
        }
        val valueInput = EditText(this).apply {
            hint = "Value (e.g. sk-...) or leave empty"
            setText(key?.let { configStore.get(it).orEmpty() } ?: "")
        }
        layout.addView(keyInput)
        layout.addView(valueInput)

        AlertDialog.Builder(this)
            .setTitle(if (key == null) "Add Key" else "Edit Key")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val entryKey = key ?: keyInput.text.toString().trim()
                val entryValue = valueInput.text.toString()
                if (entryKey.isEmpty()) {
                    Toast.makeText(this, "Key cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    configStore.put(entryKey, entryValue)
                    loadConfig()
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                if (key != null) {
                    configStore.remove(key)
                    loadConfig()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadConfig()
        loadScriptPermissions()
    }
}

/**
 * Adapter for config entries. Values are masked in the list so secrets are
 * not shown on screen; the full value is visible while editing.
 */
class ConfigAdapter(
    private val context: SettingsActivity,
    private val entries: List<Map.Entry<String, String>>
) : BaseAdapter() {

    override fun getCount(): Int = entries.size

    override fun getItem(position: Int): String = entries[position].key

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val entry = entries[position]

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        layout.addView(TextView(context).apply {
            text = entry.key
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        layout.addView(TextView(context).apply {
            text = maskValue(entry.value)
            textSize = 13f
            setTextColor(android.graphics.Color.GRAY)
        })

        return layout
    }

    private fun maskValue(value: String): String {
        if (value.isEmpty()) return "(empty)"
        return if (value.length <= 6) "••••••" else value.take(4) + "••••••"
    }
}
