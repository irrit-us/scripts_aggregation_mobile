package com.scripthost.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scripthost.ui.markdown.MarkdownRenderer
import java.nio.charset.Charset
import kotlin.concurrent.thread

/**
 * Markdown Viewer - full-screen sub-screen for viewing `.md` / `.markdown`
 * files opened from other apps (file managers, downloads, mail).
 *
 * The file content is read once off the main thread (content:// and file://
 * URIs both go through the ContentResolver; no persistable permission is
 * taken) and rendered with the built-in [MarkdownRenderer]. Chrome follows
 * the sub-screen spec: a top-left X and a rightward swipe both close.
 */
class MarkdownViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri == null) {
            Toast.makeText(this, "No file to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val displayName = resolveDisplayName(uri)
        thread {
            val content = try {
                contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charset.forName("UTF-8"))
                }
            } catch (e: Exception) {
                null
            }

            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (content == null) {
                    Toast.makeText(this, "Unable to read file", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    setupUI(displayName, content)
                }
            }
        }
    }

    private fun setupUI(fileName: String, content: String) {
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
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(SubScreenChrome.closeButton(this) { finish() })
        headerRow.addView(TextView(this).apply {
            text = fileName
            textSize = 20f
        })
        rootLayout.addView(headerRow)

        rootLayout.addView(TextView(this).apply {
            textSize = 15f
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
            text = MarkdownRenderer.render(content, resources.displayMetrics.density)
        })

        scrollView.addView(rootLayout)
        swipeContainer.addView(scrollView)
        setContentView(swipeContainer)
    }

    /** Best-effort display name: DISPLAY_NAME for content URIs, else the last path segment. */
    private fun resolveDisplayName(uri: Uri): String {
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) {
                        val name = cursor.getString(index)
                        if (!name.isNullOrEmpty()) return name
                    }
                }
            } catch (e: Exception) {
                // Fall through to the path segment
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Markdown"
    }
}
