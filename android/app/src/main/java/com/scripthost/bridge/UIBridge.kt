package com.scripthost.bridge

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.eclipsesource.v8.JavaCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Function
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 * UI Bridge - Exposes native UI components to scripts.
 *
 * Every component exposes a common set of configuration methods (visibility,
 * enabled state, padding, margins, width/height, opacity, background color,
 * corner radius) plus component-specific methods. Animations are intentionally
 * not part of the interface; all properties are applied statically.
 *
 * All V8 access happens on the main thread (J2V8 thread affinity); view
 * mutation is marshaled through [onUiThread].
 */
class UIBridge(private val context: Context, private val rootView: ViewGroup) : ScriptBridge {

    /** Per-view background style (color + corner radius) applied together. */
    private data class ViewStyle(var backgroundColor: Int? = null, var cornerRadiusDp: Float = 0f)

    /** Per-view text style state for bold/italic toggling. */
    private data class TextState(var bold: Boolean = false, var italic: Boolean = false)

    private var runtime: V8? = null
    private val viewRegistry = ConcurrentHashMap<Int, View>()
    private val viewStyles = ConcurrentHashMap<Int, ViewStyle>()
    private val textStates = ConcurrentHashMap<Int, TextState>()
    private var nextViewId = 1000
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun register(runtime: V8) {
        this.runtime = runtime

        // Create UI namespace
        val uiObject = V8Object(runtime)
        runtime.add("UI", uiObject)

        // Register UI methods
        uiObject.registerJavaMethod(this, "addView", "addView", arrayOf(V8Object::class.java))
        uiObject.registerJavaMethod(this, "removeView", "removeView", arrayOf(Int::class.java))
        uiObject.registerJavaMethod(this, "clearViews", "clearViews", emptyArray())
        uiObject.registerJavaMethod(this, "setRootBackgroundColor", "setBackgroundColor",
            arrayOf(String::class.java))

        // Register component constructors
        runtime.registerJavaMethod(this, "createButton", "Button", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createLabel", "Label", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createTextField", "TextField", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createListView", "ListView", emptyArray())
        runtime.registerJavaMethod(this, "createImageView", "ImageView", emptyArray())
        runtime.registerJavaMethod(this, "createSwitch", "Switch", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createSlider", "Slider", emptyArray())
        runtime.registerJavaMethod(this, "createScrollView", "ScrollView", emptyArray())
        runtime.registerJavaMethod(this, "createCheckBox", "CheckBox", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createSpinner", "Spinner", emptyArray())
        runtime.registerJavaMethod(this, "createProgressBar", "ProgressBar", emptyArray())
        runtime.registerJavaMethod(this, "createLayout", "Layout", arrayOf(String::class.java))

        // Dialog helpers (global functions)
        runtime.registerJavaMethod(this, "showAlert", "showAlert",
            arrayOf(String::class.java, String::class.java))
        runtime.registerJavaMethod(this, "showConfirm", "showConfirm",
            arrayOf(String::class.java, String::class.java, V8Function::class.java))
        runtime.registerJavaMethod(this, "showPrompt", "showPrompt",
            arrayOf(String::class.java, String::class.java, V8Function::class.java))
        runtime.registerJavaMethod(this, "showListPicker", "showListPicker",
            arrayOf(String::class.java, V8Array::class.java, V8Function::class.java))
        // showToast(message) and showToast(message, "long")
        runtime.registerJavaMethod(JavaCallback { _, parameters ->
            showToast(parameters)
            null
        }, "showToast")

        uiObject.release()
    }

    override fun unregister() {
        viewRegistry.clear()
        viewStyles.clear()
        textStates.clear()
        runtime = null
    }

    // ------------------------------------------------------------------
    // Component factories
    // ------------------------------------------------------------------

    /**
     * Create a Button component.
     */
    @Suppress("unused")
    fun createButton(text: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val button = onUiThread {
            Button(context).apply {
                this.text = text
                layoutParams = defaultLayoutParams()
            }
        }

        val viewId = registerView(button)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.add("text", text)
        registerTextMethods(jsObject)
        jsObject.registerJavaMethod(this, "setButtonText", "setText",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setButtonOnTap", "setOnTap",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a Label (TextView) component.
     */
    @Suppress("unused")
    fun createLabel(text: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val label = onUiThread {
            TextView(context).apply {
                this.text = text
                textSize = 16f
                layoutParams = defaultLayoutParams()
            }
        }

        val viewId = registerView(label)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.add("text", text)
        registerTextMethods(jsObject)
        jsObject.registerJavaMethod(this, "setLabelText", "setText",
            arrayOf(Int::class.java, String::class.java))

        return jsObject
    }

    /**
     * Create a TextField (EditText) component.
     */
    @Suppress("unused")
    fun createTextField(hint: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val textField = onUiThread {
            EditText(context).apply {
                this.hint = hint
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(16), dp(16), dp(16), dp(16))
                }
            }
        }

        val viewId = registerView(textField)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.add("hint", hint)
        registerTextMethods(jsObject)
        jsObject.registerJavaMethod(this, "getTextFieldValue", "getValue",
            arrayOf(Int::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldValue", "setValue",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldHint", "setHint",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldHintColor", "setHintTextColor",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldInputType", "setInputType",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setTextFieldMaxLength", "setMaxLength",
            arrayOf(Int::class.java, Int::class.java))

        return jsObject
    }

    /**
     * Create a ListView component.
     */
    @Suppress("unused")
    fun createListView(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val listView = onUiThread {
            android.widget.ListView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        val viewId = registerView(listView)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "setListViewItems", "setItems",
            arrayOf(Int::class.java, V8Array::class.java))
        jsObject.registerJavaMethod(this, "setListViewOnItemTap", "setOnItemTap",
            arrayOf(Int::class.java, V8Function::class.java))
        jsObject.registerJavaMethod(this, "setListViewSelection", "setSelection",
            arrayOf(Int::class.java, Int::class.java))

        return jsObject
    }

    /**
     * Create an ImageView component.
     */
    @Suppress("unused")
    fun createImageView(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val imageView = onUiThread {
            ImageView(context).apply {
                layoutParams = defaultLayoutParams()
            }
        }

        val viewId = registerView(imageView)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "setImageBase64", "setImageBase64",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setImageScaleType", "setScaleType",
            arrayOf(Int::class.java, String::class.java))

        return jsObject
    }

    /**
     * Create a Switch component.
     */
    @Suppress("unused")
    fun createSwitch(text: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val switch = onUiThread {
            androidx.appcompat.widget.SwitchCompat(context).apply {
                this.text = text
                layoutParams = defaultLayoutParams()
            }
        }

        val viewId = registerView(switch)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.add("text", text)
        registerTextMethods(jsObject)
        jsObject.registerJavaMethod(this, "setSwitchText", "setText",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setSwitchChecked", "setChecked",
            arrayOf(Int::class.java, Boolean::class.java))
        jsObject.registerJavaMethod(this, "getSwitchChecked", "getChecked",
            arrayOf(Int::class.java))
        jsObject.registerJavaMethod(this, "setSwitchOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a Slider (SeekBar) component.
     */
    @Suppress("unused")
    fun createSlider(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val slider = onUiThread {
            SeekBar(context).apply {
                max = 100
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(16), dp(16), dp(16), dp(16))
                }
            }
        }

        val viewId = registerView(slider)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "setSliderValue", "setValue",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "getSliderValue", "getValue",
            arrayOf(Int::class.java))
        jsObject.registerJavaMethod(this, "setSliderMax", "setMax",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setSliderMin", "setMin",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setSliderOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a ScrollView component. Holds a single content view; add more
     * children by wrapping them in a [Layout].
     */
    @Suppress("unused")
    fun createScrollView(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val scrollView = onUiThread {
            ScrollView(context).apply {
                isFillViewport = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        val viewId = registerView(scrollView)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "addChildToScrollView", "addView",
            arrayOf(Int::class.java, V8Object::class.java))
        jsObject.registerJavaMethod(this, "removeChildFromScrollView", "removeView",
            arrayOf(Int::class.java, V8Object::class.java))
        jsObject.registerJavaMethod(this, "setScrollViewFillViewport", "setFillViewport",
            arrayOf(Int::class.java, Boolean::class.java))

        return jsObject
    }

    /**
     * Create a CheckBox component.
     */
    @Suppress("unused")
    fun createCheckBox(text: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val checkBox = onUiThread {
            CheckBox(context).apply {
                this.text = text
                layoutParams = defaultLayoutParams()
            }
        }

        val viewId = registerView(checkBox)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.add("text", text)
        registerTextMethods(jsObject)
        jsObject.registerJavaMethod(this, "setCheckBoxText", "setText",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setCheckBoxChecked", "setChecked",
            arrayOf(Int::class.java, Boolean::class.java))
        jsObject.registerJavaMethod(this, "getCheckBoxChecked", "getChecked",
            arrayOf(Int::class.java))
        jsObject.registerJavaMethod(this, "setCheckBoxOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a Spinner (dropdown) component.
     */
    @Suppress("unused")
    fun createSpinner(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val spinner = onUiThread {
            Spinner(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(16), dp(16), dp(16), dp(16))
                }
            }
        }

        val viewId = registerView(spinner)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "setSpinnerItems", "setItems",
            arrayOf(Int::class.java, V8Array::class.java))
        jsObject.registerJavaMethod(this, "setSpinnerSelection", "setSelection",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "getSpinnerSelection", "getSelection",
            arrayOf(Int::class.java))
        jsObject.registerJavaMethod(this, "setSpinnerOnChange", "setOnChange",
            arrayOf(Int::class.java, V8Function::class.java))

        return jsObject
    }

    /**
     * Create a horizontal ProgressBar component.
     */
    @Suppress("unused")
    fun createProgressBar(): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val progressBar = onUiThread {
            ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(16), dp(16), dp(16), dp(16))
                }
            }
        }

        val viewId = registerView(progressBar)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "setProgressBarMax", "setMax",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setProgressBarValue", "setProgress",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "getProgressBarValue", "getProgress",
            arrayOf(Int::class.java))
        jsObject.registerJavaMethod(this, "setProgressBarIndeterminate", "setIndeterminate",
            arrayOf(Int::class.java, Boolean::class.java))

        return jsObject
    }

    /**
     * Create a Layout (LinearLayout) container. Orientation is
     * "vertical" (default) or "horizontal". Child views are reparented into
     * the container with [addChildToLayout].
     */
    @Suppress("unused")
    fun createLayout(orientation: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val layout = onUiThread {
            LinearLayout(context).apply {
                this.orientation = if (orientation.equals("horizontal", true)) {
                    LinearLayout.HORIZONTAL
                } else {
                    LinearLayout.VERTICAL
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(16), dp(16), dp(16), dp(16))
                }
            }
        }

        val viewId = registerView(layout)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "addChildToLayout", "addView",
            arrayOf(Int::class.java, V8Object::class.java))
        jsObject.registerJavaMethod(this, "removeChildFromLayout", "removeView",
            arrayOf(Int::class.java, V8Object::class.java))
        jsObject.registerJavaMethod(this, "setLayoutOrientation", "setOrientation",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setLayoutGravity", "setGravity",
            arrayOf(Int::class.java, String::class.java))

        return jsObject
    }

    // ------------------------------------------------------------------
    // Common view configuration methods (registered on every component)
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun setViewVisible(viewId: Int, visible: Boolean) {
        onUiThread {
            viewRegistry[viewId]?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    @Suppress("unused")
    fun setViewEnabled(viewId: Int, enabled: Boolean) {
        onUiThread { viewRegistry[viewId]?.isEnabled = enabled }
    }

    @Suppress("unused")
    fun setViewPadding(viewId: Int, left: Int, top: Int, right: Int, bottom: Int) {
        onUiThread {
            viewRegistry[viewId]?.setPadding(dp(left), dp(top), dp(right), dp(bottom))
        }
    }

    @Suppress("unused")
    fun setViewMargin(viewId: Int, left: Int, top: Int, right: Int, bottom: Int) {
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(
                dp(left), dp(top), dp(right), dp(bottom)
            )
        }
    }

    @Suppress("unused")
    fun setViewWidth(viewId: Int, width: Int) {
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            view.layoutParams?.let {
                it.width = resolveDimension(width)
                view.requestLayout()
            }
        }
    }

    @Suppress("unused")
    fun setViewHeight(viewId: Int, height: Int) {
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            view.layoutParams?.let {
                it.height = resolveDimension(height)
                view.requestLayout()
            }
        }
    }

    @Suppress("unused")
    fun setViewAlpha(viewId: Int, alpha: Float) {
        onUiThread { viewRegistry[viewId]?.alpha = alpha.coerceIn(0f, 1f) }
    }

    @Suppress("unused")
    fun setViewBackgroundColor(viewId: Int, color: String) {
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            val style = viewStyles.getOrPut(viewId) { ViewStyle() }
            style.backgroundColor = parseColor(color)
            applyBackground(view, style)
        }
    }

    @Suppress("unused")
    fun setViewCornerRadius(viewId: Int, radiusDp: Float) {
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            val style = viewStyles.getOrPut(viewId) { ViewStyle() }
            style.cornerRadiusDp = radiusDp
            applyBackground(view, style)
        }
    }

    @Suppress("unused")
    fun getViewId(viewId: Int): Int = viewId

    // ------------------------------------------------------------------
    // Text configuration methods (registered on text-capable components)
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun setTextViewSize(viewId: Int, size: Float) {
        onUiThread { (viewRegistry[viewId] as? TextView)?.textSize = size }
    }

    @Suppress("unused")
    fun setTextViewColor(viewId: Int, color: String) {
        onUiThread { (viewRegistry[viewId] as? TextView)?.setTextColor(parseColor(color)) }
    }

    @Suppress("unused")
    fun setTextViewBold(viewId: Int, bold: Boolean) {
        onUiThread {
            val view = viewRegistry[viewId] as? TextView ?: return@onUiThread
            textStates.getOrPut(viewId) { TextState() }.bold = bold
            applyTypeface(view, textStates[viewId] ?: TextState())
        }
    }

    @Suppress("unused")
    fun setTextViewItalic(viewId: Int, italic: Boolean) {
        onUiThread {
            val view = viewRegistry[viewId] as? TextView ?: return@onUiThread
            textStates.getOrPut(viewId) { TextState() }.italic = italic
            applyTypeface(view, textStates[viewId] ?: TextState())
        }
    }

    @Suppress("unused")
    fun setTextViewAlign(viewId: Int, align: String) {
        onUiThread {
            (viewRegistry[viewId] as? TextView)?.gravity = gravityFor(align)
        }
    }

    @Suppress("unused")
    fun setTextViewAllCaps(viewId: Int, allCaps: Boolean) {
        onUiThread { (viewRegistry[viewId] as? TextView)?.isAllCaps = allCaps }
    }

    // ------------------------------------------------------------------
    // Component-specific setters
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun setButtonText(viewId: Int, text: String) {
        onUiThread { (viewRegistry[viewId] as? Button)?.text = text }
    }

    @Suppress("unused")
    fun setButtonOnTap(viewId: Int, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
            (viewRegistry[viewId] as? Button)?.setOnClickListener {
                callback.call(runtime, null)
            }
        }
    }

    @Suppress("unused")
    fun setLabelText(viewId: Int, text: String) {
        onUiThread { (viewRegistry[viewId] as? TextView)?.text = text }
    }

    @Suppress("unused")
    fun getTextFieldValue(viewId: Int): String {
        return onUiThread { (viewRegistry[viewId] as? EditText)?.text?.toString() ?: "" }
    }

    @Suppress("unused")
    fun setTextFieldValue(viewId: Int, value: String) {
        onUiThread { (viewRegistry[viewId] as? EditText)?.setText(value) }
    }

    @Suppress("unused")
    fun setTextFieldOnChange(viewId: Int, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
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
    }

    @Suppress("unused")
    fun setTextFieldHint(viewId: Int, hint: String) {
        onUiThread { (viewRegistry[viewId] as? EditText)?.hint = hint }
    }

    @Suppress("unused")
    fun setTextFieldHintColor(viewId: Int, color: String) {
        onUiThread { (viewRegistry[viewId] as? EditText)?.setHintTextColor(parseColor(color)) }
    }

    @Suppress("unused")
    fun setTextFieldInputType(viewId: Int, type: String) {
        onUiThread {
            val field = viewRegistry[viewId] as? EditText ?: return@onUiThread
            field.inputType = when (type.lowercase()) {
                "number" -> InputType.TYPE_CLASS_NUMBER
                "phone" -> InputType.TYPE_CLASS_PHONE
                "email" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                "password" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                "multiline" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                else -> InputType.TYPE_CLASS_TEXT
            }
        }
    }

    @Suppress("unused")
    fun setTextFieldMaxLength(viewId: Int, maxLength: Int) {
        onUiThread {
            val field = viewRegistry[viewId] as? EditText ?: return@onUiThread
            field.filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
        }
    }

    @Suppress("unused")
    fun setListViewItems(viewId: Int, items: V8Array) {
        val listView = viewRegistry[viewId] as? android.widget.ListView ?: return
        val itemList = mutableListOf<String>()

        for (i in 0 until items.length()) {
            itemList.add(items.getString(i))
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, itemList)
        onUiThread { listView.adapter = adapter }
    }

    @Suppress("unused")
    fun setListViewOnItemTap(viewId: Int, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
            (viewRegistry[viewId] as? android.widget.ListView)?.setOnItemClickListener { _, _, position, _ ->
                val params = V8Array(runtime).push(position)
                callback.call(runtime, params)
                params.release()
            }
        }
    }

    @Suppress("unused")
    fun setListViewSelection(viewId: Int, index: Int) {
        onUiThread { (viewRegistry[viewId] as? android.widget.ListView)?.setSelection(index) }
    }

    @Suppress("unused")
    fun setImageBase64(viewId: Int, base64: String) {
        onUiThread {
            val imageView = viewRegistry[viewId] as? ImageView ?: return@onUiThread
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Invalid base64 or image data; leave the view unchanged
            }
        }
    }

    @Suppress("unused")
    fun setImageScaleType(viewId: Int, scaleType: String) {
        onUiThread {
            val imageView = viewRegistry[viewId] as? ImageView ?: return@onUiThread
            imageView.scaleType = when (scaleType.lowercase()) {
                "center" -> ImageView.ScaleType.CENTER
                "center_crop" -> ImageView.ScaleType.CENTER_CROP
                "center_inside" -> ImageView.ScaleType.CENTER_INSIDE
                "fit_center" -> ImageView.ScaleType.FIT_CENTER
                "fit_start" -> ImageView.ScaleType.FIT_START
                "fit_end" -> ImageView.ScaleType.FIT_END
                "fit_xy", "stretch" -> ImageView.ScaleType.FIT_XY
                else -> ImageView.ScaleType.FIT_CENTER
            }
        }
    }

    @Suppress("unused")
    fun setSwitchText(viewId: Int, text: String) {
        onUiThread { (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.text = text }
    }

    @Suppress("unused")
    fun setSwitchChecked(viewId: Int, checked: Boolean) {
        onUiThread { (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.isChecked = checked }
    }

    @Suppress("unused")
    fun getSwitchChecked(viewId: Int): Boolean {
        return onUiThread { (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.isChecked ?: false }
    }

    @Suppress("unused")
    fun setSwitchOnChange(viewId: Int, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
            (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.setOnCheckedChangeListener { _, isChecked ->
                val params = V8Array(runtime).push(isChecked)
                callback.call(runtime, params)
                params.release()
            }
        }
    }

    @Suppress("unused")
    fun setSliderValue(viewId: Int, value: Int) {
        onUiThread { (viewRegistry[viewId] as? SeekBar)?.progress = value }
    }

    @Suppress("unused")
    fun getSliderValue(viewId: Int): Int {
        return onUiThread { (viewRegistry[viewId] as? SeekBar)?.progress ?: 0 }
    }

    @Suppress("unused")
    fun setSliderMax(viewId: Int, max: Int) {
        onUiThread { (viewRegistry[viewId] as? SeekBar)?.max = max }
    }

    @Suppress("unused")
    fun setSliderMin(viewId: Int, min: Int) {
        onUiThread {
            val slider = viewRegistry[viewId] as? SeekBar ?: return@onUiThread
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                slider.min = min
            }
        }
    }

    @Suppress("unused")
    fun setSliderOnChange(viewId: Int, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
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
    }

    @Suppress("unused")
    fun setCheckBoxText(viewId: Int, text: String) {
        onUiThread { (viewRegistry[viewId] as? CheckBox)?.text = text }
    }

    @Suppress("unused")
    fun setCheckBoxChecked(viewId: Int, checked: Boolean) {
        onUiThread { (viewRegistry[viewId] as? CheckBox)?.isChecked = checked }
    }

    @Suppress("unused")
    fun getCheckBoxChecked(viewId: Int): Boolean {
        return onUiThread { (viewRegistry[viewId] as? CheckBox)?.isChecked ?: false }
    }

    @Suppress("unused")
    fun setCheckBoxOnChange(viewId: Int, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
            (viewRegistry[viewId] as? CheckBox)?.setOnCheckedChangeListener { _, isChecked ->
                val params = V8Array(runtime).push(isChecked)
                callback.call(runtime, params)
                params.release()
            }
        }
    }

    @Suppress("unused")
    fun setSpinnerItems(viewId: Int, items: V8Array) {
        val spinner = viewRegistry[viewId] as? Spinner ?: return
        val itemList = mutableListOf<String>()
        for (i in 0 until items.length()) {
            itemList.add(items.getString(i))
        }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, itemList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        onUiThread { spinner.adapter = adapter }
    }

    @Suppress("unused")
    fun setSpinnerSelection(viewId: Int, index: Int) {
        onUiThread { (viewRegistry[viewId] as? Spinner)?.setSelection(index) }
    }

    @Suppress("unused")
    fun getSpinnerSelection(viewId: Int): Int {
        return onUiThread { (viewRegistry[viewId] as? Spinner)?.selectedItemPosition ?: 0 }
    }

    @Suppress("unused")
    fun setSpinnerOnChange(viewId: Int, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
            (viewRegistry[viewId] as? Spinner)?.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val label = parent?.getItemAtPosition(position)?.toString() ?: ""
                        val params = V8Array(runtime).push(position).push(label)
                        callback.call(runtime, params)
                        params.release()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    @Suppress("unused")
    fun setProgressBarMax(viewId: Int, max: Int) {
        onUiThread { (viewRegistry[viewId] as? ProgressBar)?.max = max }
    }

    @Suppress("unused")
    fun setProgressBarValue(viewId: Int, value: Int) {
        onUiThread { (viewRegistry[viewId] as? ProgressBar)?.progress = value }
    }

    @Suppress("unused")
    fun getProgressBarValue(viewId: Int): Int {
        return onUiThread { (viewRegistry[viewId] as? ProgressBar)?.progress ?: 0 }
    }

    @Suppress("unused")
    fun setProgressBarIndeterminate(viewId: Int, indeterminate: Boolean) {
        onUiThread { (viewRegistry[viewId] as? ProgressBar)?.isIndeterminate = indeterminate }
    }

    // ------------------------------------------------------------------
    // Container methods
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun addChildToLayout(layoutId: Int, childObject: V8Object) {
        val childId = childObject.getInteger("_viewId")
        val child = viewRegistry[childId] ?: return
        val layout = viewRegistry[layoutId] as? ViewGroup ?: return
        onUiThread {
            (child.parent as? ViewGroup)?.removeView(child)
            layout.addView(child)
        }
    }

    @Suppress("unused")
    fun removeChildFromLayout(layoutId: Int, childObject: V8Object) {
        val childId = childObject.getInteger("_viewId")
        val child = viewRegistry[childId] ?: return
        val layout = viewRegistry[layoutId] as? ViewGroup ?: return
        onUiThread { layout.removeView(child) }
    }

    @Suppress("unused")
    fun setLayoutOrientation(viewId: Int, orientation: String) {
        onUiThread {
            val layout = viewRegistry[viewId] as? LinearLayout ?: return@onUiThread
            layout.orientation = if (orientation.equals("horizontal", true)) {
                LinearLayout.HORIZONTAL
            } else {
                LinearLayout.VERTICAL
            }
        }
    }

    @Suppress("unused")
    fun setLayoutGravity(viewId: Int, gravity: String) {
        onUiThread {
            (viewRegistry[viewId] as? LinearLayout)?.gravity = gravityFor(gravity)
        }
    }

    @Suppress("unused")
    fun addChildToScrollView(scrollViewId: Int, childObject: V8Object) {
        val childId = childObject.getInteger("_viewId")
        val child = viewRegistry[childId] ?: return
        val scrollView = viewRegistry[scrollViewId] as? ScrollView ?: return
        onUiThread {
            (child.parent as? ViewGroup)?.removeView(child)
            scrollView.removeAllViews()
            scrollView.addView(child)
        }
    }

    @Suppress("unused")
    fun removeChildFromScrollView(scrollViewId: Int, childObject: V8Object) {
        val childId = childObject.getInteger("_viewId")
        val child = viewRegistry[childId] ?: return
        val scrollView = viewRegistry[scrollViewId] as? ScrollView ?: return
        onUiThread { scrollView.removeView(child) }
    }

    @Suppress("unused")
    fun setScrollViewFillViewport(viewId: Int, fillViewport: Boolean) {
        onUiThread { (viewRegistry[viewId] as? ScrollView)?.isFillViewport = fillViewport }
    }

    // ------------------------------------------------------------------
    // UI namespace methods
    // ------------------------------------------------------------------

    /**
     * Add view to root container. Reparents the view if it is already
     * attached elsewhere.
     */
    @Suppress("unused")
    fun addView(jsObject: V8Object) {
        val viewId = jsObject.getInteger("_viewId")
        val view = viewRegistry[viewId] ?: return

        onUiThread {
            (view.parent as? ViewGroup)?.removeView(view)
            rootView.addView(view)
        }
    }

    /**
     * Remove view from container.
     */
    @Suppress("unused")
    fun removeView(viewId: Int) {
        val view = viewRegistry[viewId] ?: return

        onUiThread {
            rootView.removeView(view)
        }

        viewRegistry.remove(viewId)
    }

    /**
     * Clear all views.
     */
    @Suppress("unused")
    fun clearViews() {
        onUiThread {
            rootView.removeAllViews()
        }
        viewRegistry.clear()
        viewStyles.clear()
        textStates.clear()
    }

    /**
     * Set the background color of the root container.
     */
    @Suppress("unused")
    fun setRootBackgroundColor(color: String) {
        onUiThread { rootView.setBackgroundColor(parseColor(color)) }
    }

    // ------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------

    /**
     * Show an alert dialog.
     */
    @Suppress("unused")
    fun showAlert(title: String, message: String) {
        onUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    /**
     * Show a confirm dialog; callback receives `(confirmed: boolean)`.
     */
    @Suppress("unused")
    fun showConfirm(title: String, message: String, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { _, _ ->
                    invokeCallback(callback, runtime) { it.push(true) }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    invokeCallback(callback, runtime) { it.push(false) }
                }
                .show()
        }
    }

    /**
     * Show a prompt dialog; callback receives `(value: string, cancelled: boolean)`.
     */
    @Suppress("unused")
    fun showPrompt(title: String, message: String, callback: V8Function) {
        val runtime = this.runtime ?: return
        onUiThread {
            val input = EditText(context)
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    invokeCallback(callback, runtime) {
                        it.push(input.text.toString()).push(false)
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    invokeCallback(callback, runtime) { it.push("").push(true) }
                }
                .show()
        }
    }

    /**
     * Show a list picker dialog; callback receives `(index: number, label: string)`.
     */
    @Suppress("unused")
    fun showListPicker(title: String, items: V8Array, callback: V8Function) {
        val runtime = this.runtime ?: return
        val labels = (0 until items.length()).map { items.getString(it) }.toTypedArray()
        onUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(labels) { _, which ->
                    invokeCallback(callback, runtime) {
                        it.push(which).push(labels[which])
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /**
     * Show a toast. Supports `showToast(message)` and
     * `showToast(message, "long")`.
     */
    fun showToast(parameters: V8Array) {
        if (parameters.length() < 1) return
        val message = parameters.getString(0)
        val long = parameters.length() > 1 && parameters.getString(1).equals("long", ignoreCase = true)
        onUiThread {
            Toast.makeText(context, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------

    private fun newJsObject(runtime: V8, viewId: Int): V8Object {
        val jsObject = V8Object(runtime)
        jsObject.add("_viewId", viewId)
        jsObject.add("viewId", viewId)
        registerCommonMethods(jsObject)
        return jsObject
    }

    private fun registerCommonMethods(jsObject: V8Object) {
        jsObject.registerJavaMethod(this, "setViewVisible", "setVisible",
            arrayOf(Int::class.java, Boolean::class.java))
        jsObject.registerJavaMethod(this, "setViewEnabled", "setEnabled",
            arrayOf(Int::class.java, Boolean::class.java))
        jsObject.registerJavaMethod(this, "setViewPadding", "setPadding",
            arrayOf(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setViewMargin", "setMargin",
            arrayOf(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setViewWidth", "setWidth",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setViewHeight", "setHeight",
            arrayOf(Int::class.java, Int::class.java))
        jsObject.registerJavaMethod(this, "setViewAlpha", "setAlpha",
            arrayOf(Int::class.java, Float::class.java))
        jsObject.registerJavaMethod(this, "setViewBackgroundColor", "setBackgroundColor",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setViewCornerRadius", "setCornerRadius",
            arrayOf(Int::class.java, Float::class.java))
        jsObject.registerJavaMethod(this, "getViewId", "getViewId", arrayOf(Int::class.java))
    }

    private fun registerTextMethods(jsObject: V8Object) {
        jsObject.registerJavaMethod(this, "setTextViewSize", "setTextSize",
            arrayOf(Int::class.java, Float::class.java))
        jsObject.registerJavaMethod(this, "setTextViewColor", "setTextColor",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setTextViewBold", "setBold",
            arrayOf(Int::class.java, Boolean::class.java))
        jsObject.registerJavaMethod(this, "setTextViewItalic", "setItalic",
            arrayOf(Int::class.java, Boolean::class.java))
        jsObject.registerJavaMethod(this, "setTextViewAlign", "setTextAlign",
            arrayOf(Int::class.java, String::class.java))
        jsObject.registerJavaMethod(this, "setTextViewAllCaps", "setAllCaps",
            arrayOf(Int::class.java, Boolean::class.java))
    }

    /**
     * Run [block] on the Android main thread, blocking the caller until it
     * completes. This is a no-op when already on the main thread.
     */
    private fun <T> onUiThread(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return block()
        }
        val result = AtomicReference<T>()
        val latch = CountDownLatch(1)
        mainHandler.post {
            try {
                result.set(block())
            } finally {
                latch.countDown()
            }
        }
        latch.await()
        return result.get()
    }

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

    private fun defaultLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp(16), dp(16), dp(16), dp(16))
        }
    }

    private fun applyBackground(view: View, style: ViewStyle) {
        if (style.backgroundColor == null && style.cornerRadiusDp <= 0f) return
        val drawable = GradientDrawable().apply {
            cornerRadius = dp(style.cornerRadiusDp)
            setColor(style.backgroundColor ?: Color.TRANSPARENT)
        }
        view.background = drawable
    }

    private fun applyTypeface(view: TextView, state: TextState) {
        var style = Typeface.NORMAL
        if (state.bold) style = style or Typeface.BOLD
        if (state.italic) style = style or Typeface.ITALIC
        view.setTypeface(Typeface.DEFAULT, style)
    }

    private fun gravityFor(align: String): Int = when (align.lowercase()) {
        "center" -> Gravity.CENTER
        "center_horizontal" -> Gravity.CENTER_HORIZONTAL
        "center_vertical" -> Gravity.CENTER_VERTICAL
        "left", "start" -> Gravity.START
        "right", "end" -> Gravity.END
        "top" -> Gravity.TOP
        "bottom" -> Gravity.BOTTOM
        "fill" -> Gravity.FILL
        else -> Gravity.START
    }

    private fun resolveDimension(value: Int): Int = when (value) {
        -1 -> ViewGroup.LayoutParams.MATCH_PARENT
        -2 -> ViewGroup.LayoutParams.WRAP_CONTENT
        else -> dp(value)
    }

    private fun invokeCallback(callback: V8Function, runtime: V8, fill: (V8Array) -> Unit) {
        try {
            val params = V8Array(runtime)
            fill(params)
            callback.call(runtime, params)
            params.release()
        } catch (e: Exception) {
            // The callback may have been released after script teardown; ignore
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Float = value * context.resources.displayMetrics.density
}
