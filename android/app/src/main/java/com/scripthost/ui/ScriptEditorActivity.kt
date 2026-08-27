package com.scripthost.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scripthost.ScriptHostApplication
import com.scripthost.models.Permission
import com.scripthost.models.ScriptCategory
import kotlinx.coroutines.launch

/**
 * Script Editor Activity - Create and edit scripts
 */
class ScriptEditorActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var versionEditText: EditText
    private lateinit var authorEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var codeEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var categorySpinner: Spinner

    private val app get() = application as ScriptHostApplication
    private val scriptManager get() = app.scriptManager

    private var scriptId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()

        // Load existing script if editing
        scriptId = intent.getStringExtra("SCRIPT_ID")
        scriptId?.let { loadScript(it) }
    }

    private fun setupUI() {
        val scrollView = ScrollView(this)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Title
        val titleText = TextView(this).apply {
            text = if (scriptId != null) "Edit Script" else "New Script"
            textSize = 24f
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(titleText)

        // Name
        rootLayout.addView(TextView(this).apply { text = "Name:" })
        nameEditText = EditText(this).apply {
            hint = "Script name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(nameEditText)

        // Version
        rootLayout.addView(TextView(this).apply { text = "Version:" })
        versionEditText = EditText(this).apply {
            hint = "1.0.0"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(versionEditText)

        // Author
        rootLayout.addView(TextView(this).apply { text = "Author:" })
        authorEditText = EditText(this).apply {
            hint = "Your name"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(authorEditText)

        // Category
        rootLayout.addView(TextView(this).apply { text = "Category:" })
        categorySpinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val categories = ScriptCategory.values().map { it.name }
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        rootLayout.addView(categorySpinner)

        // Description
        rootLayout.addView(TextView(this).apply { text = "Description:" })
        descriptionEditText = EditText(this).apply {
            hint = "What does this script do?"
            minLines = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(descriptionEditText)

        // Code
        rootLayout.addView(TextView(this).apply { text = "Code:" })
        codeEditText = EditText(this).apply {
            hint = "// Write your JavaScript code here\nconsole.log('Hello, World!');"
            minLines = 10
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(codeEditText)

        // Save button
        saveButton = Button(this).apply {
            text = "Save Script"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(saveButton)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        // Event listeners
        saveButton.setOnClickListener {
            saveScript()
        }
    }

    private fun loadScript(scriptId: String) {
        val script = scriptManager.getScript(scriptId) ?: return

        nameEditText.setText(script.name)
        versionEditText.setText(script.version)
        authorEditText.setText(script.author)
        descriptionEditText.setText(script.description)
        codeEditText.setText(script.sourceCode)

        val categoryIndex = ScriptCategory.values().indexOf(script.category)
        if (categoryIndex >= 0) {
            categorySpinner.setSelection(categoryIndex)
        }
    }

    private fun saveScript() {
        val name = nameEditText.text.toString().trim()
        val version = versionEditText.text.toString().trim()
        val author = authorEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val code = codeEditText.text.toString()

        if (name.isEmpty() || version.isEmpty() || author.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val category = ScriptCategory.values()[categorySpinner.selectedItemPosition]

        // Preserve the existing script's permissions when editing; new scripts
        // default to INTERNET (can be extended with permission selector UI)
        val permissions = scriptId?.let { scriptManager.getScript(it)?.permissions }
            ?: listOf(Permission.INTERNET)

        lifecycleScope.launch {
            val result = scriptManager.installScript(
                name = name,
                version = version,
                author = author,
                description = description,
                permissions = permissions,
                sourceCode = code,
                category = category,
                verifySignature = false
            )

            when (result) {
                is com.scripthost.models.InstallResult.Success -> {
                    Toast.makeText(this@ScriptEditorActivity, "Script saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is com.scripthost.models.InstallResult.Failure -> {
                    Toast.makeText(this@ScriptEditorActivity, "Error: ${result.error}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
