package com.scripthost.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scripthost.ScriptHostApplication

/**
 * Script Runtime Activity - thin standalone host for [ScriptRuntimeFragment].
 *
 * Kept for compatibility (manifest entry, external `SCRIPT_ID` intents); the
 * main UI embeds the same fragment directly in the drawer content area. Like
 * MainActivity, it owns a single slim header bar (✕ + script name) — the
 * fragment itself renders no header.
 */
class ScriptRuntimeActivity : AppCompatActivity(), ScriptRuntimeFragment.Host {

    private val app get() = application as ScriptHostApplication

    private lateinit var headerScriptName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scriptId = intent.getStringExtra("SCRIPT_ID")
        if (scriptId == null) {
            Toast.makeText(this, "No script specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Single slim header: ✕ pops a pushed page; at the root page the
        // fragment defers to the host, and this standalone host ends the
        // session (there is no drawer to open here)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 8, 0)
        }
        headerRow.addView(SubScreenChrome.closeButton(this) { requestRuntimeClose() })
        headerScriptName = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }
        headerRow.addView(headerScriptName)
        rootLayout.addView(headerRow)
        rootLayout.addView(View(this).apply {
            setBackgroundColor(0x22000000)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))

        val container = FrameLayout(this).apply { id = View.generateViewId() }
        rootLayout.addView(container, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        setContentView(rootLayout)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(container.id, ScriptRuntimeFragment.newInstance(scriptId), FRAGMENT_TAG)
                .commit()
        }
    }

    /** Forward a header ✕ tap to the fragment (pop page / end session). */
    private fun requestRuntimeClose() {
        (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScriptRuntimeFragment)
            ?.onCloseGesture()
    }

    override fun onScriptSessionStarted(scriptName: String) {
        headerScriptName.text = scriptName
    }

    override fun onScriptSessionEnded() {
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        app.permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val FRAGMENT_TAG = "script_runtime"
    }
}
