package com.scripthost.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.scripthost.ScriptHostApplication

/**
 * Settings Activity - Manage configured API keys and other key/value settings.
 *
 * Values configured here are readable by scripts that declare the CONFIG
 * permission through the `Config` bridge (e.g. `Config.get("OPENAI_API_KEY")`).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var configListView: ListView
    private lateinit var emptyText: TextView
    private lateinit var configAdapter: ConfigAdapter

    private val app get() = application as ScriptHostApplication
    private val configStore get() = app.configStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        loadConfig()
    }

    private fun setupUI() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        rootLayout.addView(TextView(this).apply {
            text = "Settings"
            textSize = 24f
            setPadding(0, 0, 0, 8)
        })

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

        configListView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        rootLayout.addView(configListView)

        rootLayout.addView(Button(this).apply {
            text = "Add Key"
            setOnClickListener { showEditDialog(null) }
        })

        setContentView(rootLayout)

        configListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            showEditDialog(configAdapter.getItem(position))
        }
    }

    private fun loadConfig() {
        val entries = configStore.all().entries.sortedBy { it.key }
        val isEmpty = entries.isEmpty()

        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        configListView.visibility = if (isEmpty) View.GONE else View.VISIBLE

        configAdapter = ConfigAdapter(this, entries)
        configListView.adapter = configAdapter
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
