package com.scripthost.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.scripthost.TestApplication
import com.scripthost.config.ConfigStore
import com.scripthost.config.PlaintextCipher
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The multiline config editor: prefills from ConfigStore, Save persists
 * under the field key, and values round-trip with line breaks intact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class ConfigValueEditorActivityTest {

    private fun launch(key: String, label: String): ActivityScenario<ConfigValueEditorActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ConfigValueEditorActivity::class.java
        ).apply {
            putExtra(ConfigValueEditorActivity.EXTRA_FIELD_KEY, key)
            putExtra(ConfigValueEditorActivity.EXTRA_FIELD_LABEL, label)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun opensPrefilledWithStoredValue() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ConfigStore(context.filesDir, PlaintextCipher).put("PLAN", "line1\nline2\nline3")

        launch("PLAN", "Plan").use { scenario ->
            scenario.onActivity { activity ->
                assertThat(findEditText(activity).text.toString()).isEqualTo("line1\nline2\nline3")
            }
        }
    }

    @Test
    fun savePersistsValueWithLineBreaks() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        launch("PLAN", "Plan").use { scenario ->
            scenario.onActivity { activity ->
                findEditText(activity).setText("a\nb\nc\nd")
                findButton(activity, "Save").performClick()
            }
        }

        val stored = ConfigStore(context.filesDir, PlaintextCipher).get("PLAN")
        assertThat(stored).isEqualTo("a\nb\nc\nd")
    }

    private fun findEditText(activity: ConfigValueEditorActivity): android.widget.EditText {
        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val found = mutableListOf<android.widget.EditText>()
        fun walk(v: android.view.View) {
            if (v is android.widget.EditText) found.add(v)
            if (v is android.view.ViewGroup) repeat(v.childCount) { walk(v.getChildAt(it)) }
        }
        repeat(root.childCount) { walk(root.getChildAt(it)) }
        assertThat(found).isNotEmpty()
        return found[0]
    }

    private fun findButton(activity: ConfigValueEditorActivity, text: String): android.widget.Button {
        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        val found = mutableListOf<android.widget.Button>()
        fun walk(v: android.view.View) {
            if (v is android.widget.Button) found.add(v)
            if (v is android.view.ViewGroup) repeat(v.childCount) { walk(v.getChildAt(it)) }
        }
        repeat(root.childCount) { walk(root.getChildAt(it)) }
        return found.first { it.text.toString().equals(text, ignoreCase = true) }
    }
}
