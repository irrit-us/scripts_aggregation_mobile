package com.scripthost.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scripthost.R
import com.scripthost.ScriptHostApplication

/**
 * Config Value Editor Activity - full-screen editor for long (multiline)
 * script config values.
 *
 * Opened from the preview row of a multiline field in Settings. The large
 * editable area is prefilled with the current ConfigStore value; Save
 * (top-right) persists under the field's key and closes. Closing via the
 * top-left ✕ or a rightward fling discards any edits.
 *
 * Chrome follows the sub-screen spec ([SubScreenChrome]).
 */
class ConfigValueEditorActivity : AppCompatActivity() {

    /**
     * Config backing store. Falls back to a plaintext store when the host
     * application is not ScriptHostApplication (Robolectric tests).
     */
    private val configStore by lazy {
        (application as? com.scripthost.ScriptHostApplication)?.configStore
            ?: com.scripthost.config.ConfigStore(filesDir, com.scripthost.config.PlaintextCipher)
    }

    private lateinit var valueInput: EditText
    private var fieldKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fieldKey = intent.getStringExtra(EXTRA_FIELD_KEY)
        val fieldLabel = intent.getStringExtra(EXTRA_FIELD_LABEL) ?: fieldKey
        if (fieldKey == null) {
            finish()
            return
        }

        setupUI(fieldLabel ?: "")
    }

    private fun setupUI(fieldLabel: String) {
        // Outermost container observes all touches: a quick rightward fling
        // closes the editor (discarding unsaved edits)
        val swipeContainer = SubScreenChrome.swipeRightCloseContainer(this) { finish() }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 16)
        }

        // Header: ✕ left (discard + close), field label, Save top-right
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(SubScreenChrome.closeButton(this) { finish() })
        headerRow.addView(TextView(this).apply {
            text = fieldLabel
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })
        headerRow.addView(Button(this).apply {
            text = getString(R.string.save)
            setOnClickListener { saveAndClose() }
        })
        rootLayout.addView(headerRow)

        // Large editable area, vertically scrollable
        valueInput = EditText(this).apply {
            setText(configStore.get(fieldKey!!) ?: "")
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.TOP
            isVerticalScrollBarEnabled = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        rootLayout.addView(valueInput)

        swipeContainer.addView(rootLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(swipeContainer)
    }

    /** Persist the edited value under the field's key, toast, and close. */
    private fun saveAndClose() {
        val key = fieldKey ?: return
        // Multiline values keep their line breaks; only outer whitespace goes
        configStore.put(key, valueInput.text.toString().trim())
        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        const val EXTRA_FIELD_KEY = "FIELD_KEY"
        const val EXTRA_FIELD_LABEL = "FIELD_LABEL"
    }
}
