package com.scripthost.bridge

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * UI Bridge - Exposes native UI components to scripts
 * Allows scripts to create buttons, labels, lists, and other UI elements
 */
class UIBridge(private val context: Context, private val rootView: ViewGroup) : ScriptBridge {

    private var runtime: V8? = null
    private val viewRegistry = ConcurrentHashMap<Int, View>()
    private var nextViewId = 1000

    override fun register(runtime: V8) {
        this.runtime = runtime

        // Create UI namespace
        val uiObject = V8Object(runtime)
        runtime.add("UI", uiObject)

        // Register UI methods
        uiObject.registerJavaMethod(this, "addView", "addView", arrayOf(V8Object::class.java))
        uiObject.registerJavaMethod(this, "removeView", "removeView", arrayOf(Int::class.java))
        uiObject.registerJavaMethod(this, "clearViews", "clearViews", emptyArray())

        // Register component constructors
        runtime.registerJavaMethod(this, "createButton", "Button", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createLabel", "Label", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createTextField", "TextField", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createListView", "ListView", emptyArray())
        runtime.registerJavaMethod(this, "createImageView", "ImageView", emptyArray())
        runtime.registerJavaMethod(this, "createSwitch", "Switch", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createSlider", "Slider", emptyArray())
        runtime.registerJavaMethod(this, "createScrollView", "ScrollView", emptyArray())

        // Helper functions
        runtime.registerJavaMethod(this, "showAlert", "showAlert",
            arrayOf(String::class.java, String::class.java))
        runtime.registerJavaMethod(this, "showToast", "showToast", arrayOf(String::class.java))

        uiObject.release()
    }

    override fun unregister() {
        viewRegistry.clear()
        runtime = null
    }

    /**
     * Create a Button component
     */
    @Suppress("unused")
    fun createButton(text: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val button = Button(context).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val viewId = registerView(button)

        // Create JavaScript object wrapper
        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)
        jsObject.add("text", text)

        // Register methods
        jsObject.registerJavaMethod(this, "setButtonText", "setText",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setButtonColor", "setBackgroundColor",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setButtonTextColor", "setTextColor",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setButtonOnTap", "setOnTap",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a Label (TextView) component
     */
    @Suppress("unused")
    fun createLabel(text: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val label = TextView(context).apply {
            this.text = text
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val viewId = registerView(label)

        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)
        jsObject.add("text", text)

        jsObject.registerJavaMethod(this, "setLabelText", "setText",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setLabelColor", "setTextColor",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setLabelSize", "setTextSize",
            arrayOf(Int::class.java, Float::class.java))

        return jsObject
    }

    /**
     * Create a TextField (EditText) component
     */
    @Suppress("unused")
    fun createTextField(hint: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val textField = EditText(context).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val viewId = registerView(textField)

        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)
        jsObject.add("hint", hint)

        jsObject.registerJavaMethod(this, "getTextFieldValue", "getValue",
            arrayOf(Int::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldValue", "setValue",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a ListView component
     */
    @Suppress("unused")
    fun createListView(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val listView = android.widget.ListView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val viewId = registerView(listView)

        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)

        jsObject.registerJavaMethod(this, "setListViewItems", "setItems",
            arrayOf(Int::class.java, V8Array::class.java))
        jsObject.registerJavaMethod(this, "setListViewOnItemTap", "setOnItemTap",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create an ImageView component
     */
    @Suppress("unused")
    fun createImageView(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val viewId = registerView(imageView)

        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)

        return jsObject
    }

    /**
     * Create a Switch component
     */
    @Suppress("unused")
    fun createSwitch(text: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val switch = androidx.appcompat.widget.SwitchCompat(context).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val viewId = registerView(switch)

        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)

        jsObject.registerJavaMethod(this, "setSwitchOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a Slider (SeekBar) component
     */
    @Suppress("unused")
    fun createSlider(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val slider = SeekBar(context).apply {
            max = 100
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val viewId = registerView(slider)

        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)

        jsObject.registerJavaMethod(this, "setSliderValue", "setValue",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setSliderOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a ScrollView component
     */
    @Suppress("unused")
    fun createScrollView(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val viewId = registerView(scrollView)

        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)

        return jsObject
    }

    // Component property setters

    @Suppress("unused")
    fun setButtonText(viewId: Int, text: String) {
        (viewRegistry[viewId] as? Button)?.text = text
    }

    @Suppress("unused")
    fun setButtonColor(viewId: Int, color: String) {
        (viewRegistry[viewId] as? Button)?.setBackgroundColor(parseColor(color))
    }

    @Suppress("unused")
    fun setButtonTextColor(viewId: Int, color: String) {
        (viewRegistry[viewId] as? Button)?.setTextColor(parseColor(color))
    }

    @Suppress("unused")
    fun setButtonOnTap(viewId: Int, callback: V8Function) {
        (viewRegistry[viewId] as? Button)?.setOnClickListener {
            callback.call(runtime, null)
        }
    }

    @Suppress("unused")
    fun setLabelText(viewId: Int, text: String) {
        (viewRegistry[viewId] as? TextView)?.text = text
    }

    @Suppress("unused")
    fun setLabelColor(viewId: Int, color: String) {
        (viewRegistry[viewId] as? TextView)?.setTextColor(parseColor(color))
    }

    @Suppress("unused")
    fun setLabelSize(viewId: Int, size: Float) {
        (viewRegistry[viewId] as? TextView)?.textSize = size
    }

    @Suppress("unused")
    fun getTextFieldValue(viewId: Int): String {
        return (viewRegistry[viewId] as? EditText)?.text?.toString() ?: ""
    }

    @Suppress("unused")
    fun setTextFieldValue(viewId: Int, value: String) {
        (viewRegistry[viewId] as? EditText)?.setText(value)
    }

    @Suppress("unused")
    fun setTextFieldOnChange(viewId: Int, callback: V8Function) {
        (viewRegistry[viewId] as? EditText)?.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val params = V8Array(runtime).push(s.toString())
                    callback.call(runtime, params)
                    params.release()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            }
        )
    }

    @Suppress("unused")
    fun setListViewItems(viewId: Int, items: V8Array) {
        val listView = viewRegistry[viewId] as? android.widget.ListView ?: return
        val itemList = mutableListOf<String>()

        for (i in 0 until items.length()) {
            itemList.add(items.getString(i))
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, itemList)
        listView.adapter = adapter
    }

    @Suppress("unused")
    fun setListViewOnItemTap(viewId: Int, callback: V8Function) {
        (viewRegistry[viewId] as? android.widget.ListView)?.setOnItemClickListener { _, _, position, _ ->
            val params = V8Array(runtime).push(position)
            callback.call(runtime, params)
            params.release()
        }
    }

    @Suppress("unused")
    fun setSwitchOnChange(viewId: Int, callback: V8Function) {
        (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.setOnCheckedChangeListener { _, isChecked ->
            val params = V8Array(runtime).push(isChecked)
            callback.call(runtime, params)
            params.release()
        }
    }

    @Suppress("unused")
    fun setSliderValue(viewId: Int, value: Int) {
        (viewRegistry[viewId] as? SeekBar)?.progress = value
    }

    @Suppress("unused")
    fun setSliderOnChange(viewId: Int, callback: V8Function) {
        (viewRegistry[viewId] as? SeekBar)?.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val params = V8Array(runtime).push(progress)
                    callback.call(runtime, params)
                    params.release()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )
    }

    /**
     * Add view to root container
     */
    @Suppress("unused")
    fun addView(jsObject: V8Object) {
        val viewId = jsObject.getInteger("_viewId")
        val view = viewRegistry[viewId] ?: return

        (context as? android.app.Activity)?.runOnUiThread {
            rootView.addView(view)
        }
    }

    /**
     * Remove view from container
     */
    @Suppress("unused")
    fun removeView(viewId: Int) {
        val view = viewRegistry[viewId] ?: return

        (context as? android.app.Activity)?.runOnUiThread {
            rootView.removeView(view)
        }

        viewRegistry.remove(viewId)
    }

    /**
     * Clear all views
     */
    @Suppress("unused")
    fun clearViews() {
        (context as? android.app.Activity)?.runOnUiThread {
            rootView.removeAllViews()
        }
        viewRegistry.clear()
    }

    /**
     * Show alert dialog
     */
    @Suppress("unused")
    fun showAlert(title: String, message: String) {
        (context as? android.app.Activity)?.runOnUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    /**
     * Show toast message
     */
    @Suppress("unused")
    fun showToast(message: String) {
        (context as? android.app.Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Helper methods

    private fun registerView(view: View): Int {
        val viewId = nextViewId++
        viewRegistry[viewId] = view
        return viewId
    }

    private fun parseColor(colorString: String): Int {
        return try {
            Color.parseColor(colorString)
        } catch (e: Exception) {
            Color.BLACK
        }
    }
}
