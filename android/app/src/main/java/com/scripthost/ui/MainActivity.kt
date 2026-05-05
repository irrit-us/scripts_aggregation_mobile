package com.scripthost.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scripthost.ScriptHostApplication
import com.scripthost.models.Script
import com.scripthost.models.ScriptCategory
import kotlinx.coroutines.launch

/**
 * Main Activity - Script library and management
 */
class MainActivity : AppCompatActivity() {

    private lateinit var scriptListView: ListView
    private lateinit var addScriptButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var searchEditText: EditText

    private val app get() = application as ScriptHostApplication
    private val scriptManager get() = app.scriptManager

    private var currentCategory: ScriptCategory? = null
    private lateinit var scriptAdapter: ScriptAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        loadScripts()
    }

    private fun setupUI() {
        // Create layout programmatically
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Title
        val titleText = TextView(this).apply {
            text = "ScriptHost"
            textSize = 24f
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(titleText)

        // Search bar
        searchEditText = EditText(this).apply {
            hint = "Search scripts..."
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(searchEditText)

        // Category filter
        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val categoryLabel = TextView(this).apply {
            text = "Category: "
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        categoryLayout.addView(categoryLabel)

        categorySpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        categoryLayout.addView(categorySpinner)

        rootLayout.addView(categoryLayout)

        // Script list
        scriptListView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        rootLayout.addView(scriptListView)

        // Add script button
        addScriptButton = Button(this).apply {
            text = "Add Script"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(addScriptButton)

        setContentView(rootLayout)

        // Setup category spinner
        val categories = listOf("All") + ScriptCategory.values().map { it.name }
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Event listeners
        addScriptButton.setOnClickListener {
            showAddScriptDialog()
        }

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = if (position == 0) null else ScriptCategory.values()[position - 1]
                loadScripts()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterScripts(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        scriptListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val script = scriptAdapter.getItem(position) as Script
            showScriptDetails(script)
        }
    }

    private fun loadScripts() {
        val scripts = if (currentCategory != null) {
            scriptManager.getScriptsByCategory(currentCategory!!)
        } else {
            scriptManager.getAllScripts()
        }

        scriptAdapter = ScriptAdapter(this, scripts)
        scriptListView.adapter = scriptAdapter
    }

    private fun filterScripts(query: String) {
        if (query.isEmpty()) {
            loadScripts()
        } else {
            val scripts = scriptManager.searchScripts(query)
            scriptAdapter = ScriptAdapter(this, scripts)
            scriptListView.adapter = scriptAdapter
        }
    }

    private fun showAddScriptDialog() {
        val options = arrayOf("Create New Script", "Import from File")

        AlertDialog.Builder(this)
            .setTitle("Add Script")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ScriptEditorActivity::class.java))
                    1 -> importScriptFromFile()
                }
            }
            .show()
    }

    private fun importScriptFromFile() {
        // In production, use file picker
        Toast.makeText(this, "File import not yet implemented", Toast.LENGTH_SHORT).show()
    }

    private fun showScriptDetails(script: Script) {
        val options = arrayOf("Run", "Edit", "Export", "Delete")

        AlertDialog.Builder(this)
            .setTitle(script.name)
            .setMessage("${script.description}\n\nVersion: ${script.version}\nAuthor: ${script.author}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> runScript(script)
                    1 -> editScript(script)
                    2 -> exportScript(script)
                    3 -> deleteScript(script)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun runScript(script: Script) {
        val intent = Intent(this, ScriptRuntimeActivity::class.java)
        intent.putExtra("SCRIPT_ID", script.id)
        startActivity(intent)
    }

    private fun editScript(script: Script) {
        val intent = Intent(this, ScriptEditorActivity::class.java)
        intent.putExtra("SCRIPT_ID", script.id)
        startActivity(intent)
    }

    private fun exportScript(script: Script) {
        val json = scriptManager.exportScript(script.id)
        if (json != null) {
            // In production, save to file or share
            Toast.makeText(this, "Script exported", Toast.LENGTH_SHORT).show()
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
            setTypeface(null, android.graphics.Typeface.BOLD)
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
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(infoText)

        return layout
    }
}
