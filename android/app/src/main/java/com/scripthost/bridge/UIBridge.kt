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
import com.eclipsesource.v8.V8Value
import com.scripthost.engine.ScriptBridge
import com.scripthost.ui.chart.ChartScale
import com.scripthost.ui.chart.SimpleChartView
import com.scripthost.ui.markdown.MarkdownRenderer
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
 * All V8 access happens on the engine's V8 thread (J2V8 thread affinity);
 * view mutation is marshaled to the main thread through [onUiThread], and
 * Java→JS callbacks arriving on the main thread are marshaled back through
 * [onEngineThread].
 *
 * The constructor's [rootView] acts as a page HOST: the bridge stacks full-size
 * page containers inside it and always shows exactly one page (the top of the
 * stack). [addView]/[removeView]/[clearViews] target the current page.
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

    /** Theme-default text colors captured at view registration, for blank resets. */
    private val defaultTextColors = ConcurrentHashMap<Int, android.content.res.ColorStateList>()

    /**
     * Retained callback twins. J2V8 auto-releases V8Value parameters when a
     * registered method returns, so callbacks that fire later are kept as
     * twins and released on [unregister] (or right after a one-shot fire).
     */
    private val retainedCallbacks = ConcurrentHashMap.newKeySet<V8Value>()
    private var nextViewId = 1000
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Handler of the engine's V8 thread, captured in [register] (which the
     * engine invokes on that thread). Java→JS callbacks arriving on the main
     * thread (view events, dialogs) are re-marshaled onto it.
     */
    private var engineHandler: Handler? = null

    /** Stack of pages hosted by [rootView]; the bottom entry is the root page. */
    private val pageStack = java.util.ArrayDeque<LinearLayout>()

    /**
     * Optional listener notified with the new depth after [pushPage] /
     * [popPage] change the page stack. Called on the engine thread; hosts
     * that update UI must marshal to the main thread themselves.
     */
    var onPageDepthChanged: ((Int) -> Unit)? = null

    /**
     * Invoked when the script sets its display title via `UI.setTitle(...)`.
     * Called on the engine thread; hosts that update UI must marshal to the
     * main thread themselves.
     */
    var onSetTitle: ((String) -> Unit)? = null

    /** The page script UI currently lands in (top of the stack). */
    private val currentPage: LinearLayout?
        get() = pageStack.peek()

    init {
        val rootPage = createPage()
        onUiThread { rootView.addView(rootPage) }
        pageStack.push(rootPage)
    }

    override fun register(runtime: V8) {
        this.runtime = runtime
        // register() is invoked on the engine's V8 thread
        engineHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

        // Create UI namespace
        val uiObject = V8Object(runtime)
        runtime.add("UI", uiObject)

        // Register UI methods
        uiObject.registerJavaMethod(this, "addView", "addView", arrayOf(V8Object::class.java))
        uiObject.registerJavaMethod(this, "removeView", "removeView", arrayOf(Int::class.java))
        uiObject.registerJavaMethod(this, "clearViews", "clearViews", emptyArray())
        uiObject.registerJavaMethod(this, "pushPage", "pushPage", emptyArray())
        uiObject.registerJavaMethod(this, "popPage", "popPage", emptyArray())
        uiObject.registerJavaMethod(this, "pageDepth", "pageDepth", emptyArray())
        uiObject.registerJavaMethod(this, "setTitle", "setTitle", arrayOf(String::class.java))
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
        runtime.registerJavaMethod(this, "createChart", "Chart", arrayOf(String::class.java))
        runtime.registerJavaMethod(this, "createMarkdown", "Markdown", arrayOf(String::class.java))

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
        retainedCallbacks.forEach { if (!it.isReleased) it.release() }
        retainedCallbacks.clear()
        viewRegistry.clear()
        viewStyles.clear()
        textStates.clear()
        pageStack.clear()
        onPageDepthChanged = null
        onSetTitle = null
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
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setButtonOnTap", "setOnTap",
            arrayOf(V8Object::class.java, V8Function::class.java), true)

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
            arrayOf(V8Object::class.java, String::class.java), true)

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
            arrayOf(V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setTextFieldValue", "setValue",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setTextFieldOnChange", "setOnChange",
            arrayOf(V8Object::class.java, V8Function::class.java), true)
        jsObject.registerJavaMethod(this, "setTextFieldHint", "setHint",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setTextFieldHintColor", "setHintTextColor",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setTextFieldInputType", "setInputType",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setTextFieldMaxLength", "setMaxLength",
            arrayOf(V8Object::class.java, Int::class.java), true)

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
            arrayOf(V8Object::class.java, V8Array::class.java), true)
        jsObject.registerJavaMethod(this, "setListViewOnItemTap", "setOnItemTap",
            arrayOf(V8Object::class.java, V8Function::class.java), true)
        jsObject.registerJavaMethod(this, "setListViewSelection", "setSelection",
            arrayOf(V8Object::class.java, Int::class.java), true)

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
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setImageScaleType", "setScaleType",
            arrayOf(V8Object::class.java, String::class.java), true)

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
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setSwitchChecked", "setChecked",
            arrayOf(V8Object::class.java, Boolean::class.java), true)
        jsObject.registerJavaMethod(this, "getSwitchChecked", "getChecked",
            arrayOf(V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setSwitchOnChange", "setOnChange",
            arrayOf(V8Object::class.java, V8Function::class.java), true)

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
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "getSliderValue", "getValue",
            arrayOf(V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setSliderMax", "setMax",
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "setSliderMin", "setMin",
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "setSliderOnChange", "setOnChange",
            arrayOf(V8Object::class.java, V8Function::class.java), true)

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
            arrayOf(V8Object::class.java, V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "removeChildFromScrollView", "removeView",
            arrayOf(V8Object::class.java, V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setScrollViewFillViewport", "setFillViewport",
            arrayOf(V8Object::class.java, Boolean::class.java), true)

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
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setCheckBoxChecked", "setChecked",
            arrayOf(V8Object::class.java, Boolean::class.java), true)
        jsObject.registerJavaMethod(this, "getCheckBoxChecked", "getChecked",
            arrayOf(V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setCheckBoxOnChange", "setOnChange",
            arrayOf(V8Object::class.java, V8Function::class.java), true)

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
            arrayOf(V8Object::class.java, V8Array::class.java), true)
        jsObject.registerJavaMethod(this, "setSpinnerSelection", "setSelection",
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "getSpinnerSelection", "getSelection",
            arrayOf(V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setSpinnerOnChange", "setOnChange",
            arrayOf(V8Object::class.java, V8Function::class.java), true)

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
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "setProgressBarValue", "setProgress",
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "getProgressBarValue", "getProgress",
            arrayOf(V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setProgressBarIndeterminate", "setIndeterminate",
            arrayOf(V8Object::class.java, Boolean::class.java), true)

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
            arrayOf(V8Object::class.java, V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "removeChildFromLayout", "removeView",
            arrayOf(V8Object::class.java, V8Object::class.java), true)
        jsObject.registerJavaMethod(this, "setLayoutOrientation", "setOrientation",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setLayoutGravity", "setGravity",
            arrayOf(V8Object::class.java, String::class.java), true)

        return jsObject
    }

    /**
     * Create a Chart component. Type is "line" (default) or "bar".
     */
    @Suppress("unused")
    fun createChart(type: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val chart = onUiThread {
            SimpleChartView(context).apply {
                chartType = if (type.equals("bar", true)) {
                    SimpleChartView.ChartType.BAR
                } else {
                    SimpleChartView.ChartType.LINE
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(220)
                ).apply {
                    setMargins(dp(16), dp(16), dp(16), dp(16))
                }
            }
        }

        val viewId = registerView(chart)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "setChartData", "setData",
            arrayOf(V8Object::class.java, V8Array::class.java), true)
        jsObject.registerJavaMethod(this, "setChartLabels", "setLabels",
            arrayOf(V8Object::class.java, V8Array::class.java), true)
        jsObject.registerJavaMethod(this, "setChartColor", "setColor",
            arrayOf(V8Object::class.java, String::class.java), true)

        return jsObject
    }

    /**
     * Create a Markdown component: a TextView rendering [markdown] via the
     * lightweight [MarkdownRenderer]. Links are clickable.
     */
    @Suppress("unused")
    fun createMarkdown(markdown: String): V8Object {
        val runtime = this.runtime ?: throw IllegalStateException("Runtime not initialized")

        val textView = onUiThread {
            TextView(context).apply {
                textSize = 15f
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(16), dp(16), dp(16), dp(16))
                }
                text = MarkdownRenderer.render(markdown, resources.displayMetrics.density)
            }
        }

        val viewId = registerView(textView)
        val jsObject = newJsObject(runtime, viewId)
        jsObject.registerJavaMethod(this, "setMarkdownContent", "setMarkdown",
            arrayOf(V8Object::class.java, String::class.java), true)

        return jsObject
    }

    // ------------------------------------------------------------------
    // Common view configuration methods (registered on every component)
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun setViewVisible(receiver: V8Object, visible: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            viewRegistry[viewId]?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    @Suppress("unused")
    fun setViewEnabled(receiver: V8Object, enabled: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { viewRegistry[viewId]?.isEnabled = enabled }
    }

    @Suppress("unused")
    fun setViewPadding(receiver: V8Object, left: Int, top: Int, right: Int, bottom: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            viewRegistry[viewId]?.setPadding(dp(left), dp(top), dp(right), dp(bottom))
        }
    }

    @Suppress("unused")
    fun setViewMargin(receiver: V8Object, left: Int, top: Int, right: Int, bottom: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(
                dp(left), dp(top), dp(right), dp(bottom)
            )
        }
    }

    @Suppress("unused")
    fun setViewWidth(receiver: V8Object, width: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            view.layoutParams?.let {
                it.width = resolveDimension(width)
                view.requestLayout()
            }
        }
    }

    @Suppress("unused")
    fun setViewHeight(receiver: V8Object, height: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            view.layoutParams?.let {
                it.height = resolveDimension(height)
                view.requestLayout()
            }
        }
    }

    @Suppress("unused")
    fun setViewAlpha(receiver: V8Object, alpha: Double) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { viewRegistry[viewId]?.alpha = alpha.toFloat().coerceIn(0f, 1f) }
    }

    @Suppress("unused")
    fun setViewBackgroundColor(receiver: V8Object, color: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            val style = viewStyles.getOrPut(viewId) { ViewStyle() }
            style.backgroundColor = parseColor(color)
            applyBackground(view, style)
        }
    }

    @Suppress("unused")
    fun setViewCornerRadius(receiver: V8Object, radiusDp: Double) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] ?: return@onUiThread
            val style = viewStyles.getOrPut(viewId) { ViewStyle() }
            style.cornerRadiusDp = radiusDp.toFloat()
            applyBackground(view, style)
        }
    }

    @Suppress("unused")
    fun getViewId(receiver: V8Object): Int = receiver.getInteger("_viewId")

    // ------------------------------------------------------------------
    // Text configuration methods (registered on text-capable components)
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun setTextViewSize(receiver: V8Object, size: Double) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? TextView)?.textSize = size.toFloat() }
    }

    @Suppress("unused")
    fun setTextViewColor(receiver: V8Object, color: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] as? TextView ?: return@onUiThread
            // Blank resets to the theme default captured at registration;
            // scripts use this to undo a custom color (e.g. "done" gray).
            applyTextColor(view, color, defaultTextColors[viewId])
        }
    }

    @Suppress("unused")
    fun setTextViewBold(receiver: V8Object, bold: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] as? TextView ?: return@onUiThread
            textStates.getOrPut(viewId) { TextState() }.bold = bold
            applyTypeface(view, textStates[viewId] ?: TextState())
        }
    }

    @Suppress("unused")
    fun setTextViewItalic(receiver: V8Object, italic: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val view = viewRegistry[viewId] as? TextView ?: return@onUiThread
            textStates.getOrPut(viewId) { TextState() }.italic = italic
            applyTypeface(view, textStates[viewId] ?: TextState())
        }
    }

    @Suppress("unused")
    fun setTextViewAlign(receiver: V8Object, align: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            (viewRegistry[viewId] as? TextView)?.gravity = gravityFor(align)
        }
    }

    @Suppress("unused")
    fun setTextViewAllCaps(receiver: V8Object, allCaps: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? TextView)?.isAllCaps = allCaps }
    }

    @Suppress("unused")
    fun setTextViewStrikeThrough(receiver: V8Object, enabled: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? TextView)?.let { applyStrikeThrough(it, enabled) } }
    }

    // ------------------------------------------------------------------
    // Component-specific setters
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun setButtonText(receiver: V8Object, text: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? Button)?.text = text }
    }

    @Suppress("unused")
    fun setButtonOnTap(receiver: V8Object, callback: V8Function) {
        val viewId = receiver.getInteger("_viewId")
        val runtime = this.runtime ?: return
        val retained = retainCallback(callback)
        onUiThread {
            (viewRegistry[viewId] as? Button)?.setOnClickListener {
                onEngineThread { retained.call(runtime, null) }
            }
        }
    }

    @Suppress("unused")
    fun setLabelText(receiver: V8Object, text: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? TextView)?.text = text }
    }

    @Suppress("unused")
    fun getTextFieldValue(receiver: V8Object): String {
        val viewId = receiver.getInteger("_viewId")
        return onUiThread { (viewRegistry[viewId] as? EditText)?.text?.toString() ?: "" }
    }

    @Suppress("unused")
    fun setTextFieldValue(receiver: V8Object, value: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? EditText)?.setText(value) }
    }

    @Suppress("unused")
    fun setTextFieldOnChange(receiver: V8Object, callback: V8Function) {
        val viewId = receiver.getInteger("_viewId")
        val runtime = this.runtime ?: return
        val retained = retainCallback(callback)
        onUiThread {
            (viewRegistry[viewId] as? EditText)?.addTextChangedListener(
                object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val text = s.toString()
                        onEngineThread {
                            val params = V8Array(runtime).push(text)
                            retained.call(runtime, params)
                            params.release()
                        }
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                }
            )
        }
    }

    @Suppress("unused")
    fun setTextFieldHint(receiver: V8Object, hint: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? EditText)?.hint = hint }
    }

    @Suppress("unused")
    fun setTextFieldHintColor(receiver: V8Object, color: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? EditText)?.setHintTextColor(parseColor(color)) }
    }

    @Suppress("unused")
    fun setTextFieldInputType(receiver: V8Object, type: String) {
        val viewId = receiver.getInteger("_viewId")
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
    fun setTextFieldMaxLength(receiver: V8Object, maxLength: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val field = viewRegistry[viewId] as? EditText ?: return@onUiThread
            field.filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
        }
    }

    @Suppress("unused")
    fun setListViewItems(receiver: V8Object, items: V8Array) {
        val viewId = receiver.getInteger("_viewId")
        val listView = viewRegistry[viewId] as? android.widget.ListView ?: return
        val itemList = mutableListOf<String>()

        for (i in 0 until items.length()) {
            itemList.add(items.getString(i))
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, itemList)
        onUiThread { listView.adapter = adapter }
    }

    @Suppress("unused")
    fun setListViewOnItemTap(receiver: V8Object, callback: V8Function) {
        val viewId = receiver.getInteger("_viewId")
        val runtime = this.runtime ?: return
        val retained = retainCallback(callback)
        onUiThread {
            (viewRegistry[viewId] as? android.widget.ListView)?.setOnItemClickListener { _, _, position, _ ->
                onEngineThread {
                    val params = V8Array(runtime).push(position)
                    retained.call(runtime, params)
                    params.release()
                }
            }
        }
    }

    @Suppress("unused")
    fun setListViewSelection(receiver: V8Object, index: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? android.widget.ListView)?.setSelection(index) }
    }

    @Suppress("unused")
    fun setImageBase64(receiver: V8Object, base64: String) {
        val viewId = receiver.getInteger("_viewId")
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
    fun setImageScaleType(receiver: V8Object, scaleType: String) {
        val viewId = receiver.getInteger("_viewId")
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
    fun setSwitchText(receiver: V8Object, text: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.text = text }
    }

    @Suppress("unused")
    fun setSwitchChecked(receiver: V8Object, checked: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.isChecked = checked }
    }

    @Suppress("unused")
    fun getSwitchChecked(receiver: V8Object): Boolean {
        val viewId = receiver.getInteger("_viewId")
        return onUiThread { (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.isChecked ?: false }
    }

    @Suppress("unused")
    fun setSwitchOnChange(receiver: V8Object, callback: V8Function) {
        val viewId = receiver.getInteger("_viewId")
        val runtime = this.runtime ?: return
        val retained = retainCallback(callback)
        onUiThread {
            (viewRegistry[viewId] as? androidx.appcompat.widget.SwitchCompat)?.setOnCheckedChangeListener { _, isChecked ->
                onEngineThread {
                    val params = V8Array(runtime).push(isChecked)
                    retained.call(runtime, params)
                    params.release()
                }
            }
        }
    }

    @Suppress("unused")
    fun setSliderValue(receiver: V8Object, value: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? SeekBar)?.progress = value }
    }

    @Suppress("unused")
    fun getSliderValue(receiver: V8Object): Int {
        val viewId = receiver.getInteger("_viewId")
        return onUiThread { (viewRegistry[viewId] as? SeekBar)?.progress ?: 0 }
    }

    @Suppress("unused")
    fun setSliderMax(receiver: V8Object, max: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? SeekBar)?.max = max }
    }

    @Suppress("unused")
    fun setSliderMin(receiver: V8Object, min: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val slider = viewRegistry[viewId] as? SeekBar ?: return@onUiThread
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                slider.min = min
            }
        }
    }

    @Suppress("unused")
    fun setSliderOnChange(receiver: V8Object, callback: V8Function) {
        val viewId = receiver.getInteger("_viewId")
        val runtime = this.runtime ?: return
        val retained = retainCallback(callback)
        onUiThread {
            (viewRegistry[viewId] as? SeekBar)?.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        onEngineThread {
                            val params = V8Array(runtime).push(progress)
                            retained.call(runtime, params)
                            params.release()
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                }
            )
        }
    }

    @Suppress("unused")
    fun setCheckBoxText(receiver: V8Object, text: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? CheckBox)?.text = text }
    }

    @Suppress("unused")
    fun setCheckBoxChecked(receiver: V8Object, checked: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? CheckBox)?.isChecked = checked }
    }

    @Suppress("unused")
    fun getCheckBoxChecked(receiver: V8Object): Boolean {
        val viewId = receiver.getInteger("_viewId")
        return onUiThread { (viewRegistry[viewId] as? CheckBox)?.isChecked ?: false }
    }

    @Suppress("unused")
    fun setCheckBoxOnChange(receiver: V8Object, callback: V8Function) {
        val viewId = receiver.getInteger("_viewId")
        val runtime = this.runtime ?: return
        val retained = retainCallback(callback)
        onUiThread {
            (viewRegistry[viewId] as? CheckBox)?.setOnCheckedChangeListener { _, isChecked ->
                onEngineThread {
                    val params = V8Array(runtime).push(isChecked)
                    retained.call(runtime, params)
                    params.release()
                }
            }
        }
    }

    @Suppress("unused")
    fun setSpinnerItems(receiver: V8Object, items: V8Array) {
        val viewId = receiver.getInteger("_viewId")
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
    fun setSpinnerSelection(receiver: V8Object, index: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? Spinner)?.setSelection(index) }
    }

    @Suppress("unused")
    fun getSpinnerSelection(receiver: V8Object): Int {
        val viewId = receiver.getInteger("_viewId")
        return onUiThread { (viewRegistry[viewId] as? Spinner)?.selectedItemPosition ?: 0 }
    }

    @Suppress("unused")
    fun setSpinnerOnChange(receiver: V8Object, callback: V8Function) {
        val viewId = receiver.getInteger("_viewId")
        val runtime = this.runtime ?: return
        val retained = retainCallback(callback)
        onUiThread {
            (viewRegistry[viewId] as? Spinner)?.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val label = parent?.getItemAtPosition(position)?.toString() ?: ""
                        onEngineThread {
                            val params = V8Array(runtime).push(position).push(label)
                            retained.call(runtime, params)
                            params.release()
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    @Suppress("unused")
    fun setProgressBarMax(receiver: V8Object, max: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? ProgressBar)?.max = max }
    }

    @Suppress("unused")
    fun setProgressBarValue(receiver: V8Object, value: Int) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? ProgressBar)?.progress = value }
    }

    @Suppress("unused")
    fun getProgressBarValue(receiver: V8Object): Int {
        val viewId = receiver.getInteger("_viewId")
        return onUiThread { (viewRegistry[viewId] as? ProgressBar)?.progress ?: 0 }
    }

    @Suppress("unused")
    fun setProgressBarIndeterminate(receiver: V8Object, indeterminate: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? ProgressBar)?.isIndeterminate = indeterminate }
    }

    @Suppress("unused")
    fun setChartData(receiver: V8Object, data: V8Array) {
        val viewId = receiver.getInteger("_viewId")
        val values = mutableListOf<Float>()
        for (i in 0 until data.length()) {
            when (data.getType(i)) {
                V8Value.INTEGER -> values.add(data.getInteger(i).toFloat())
                V8Value.DOUBLE -> values.add(data.getDouble(i).toFloat())
            }
        }
        onUiThread { (viewRegistry[viewId] as? SimpleChartView)?.data = values }
    }

    @Suppress("unused")
    fun setChartLabels(receiver: V8Object, labels: V8Array) {
        val viewId = receiver.getInteger("_viewId")
        val labelList = mutableListOf<String>()
        for (i in 0 until labels.length()) {
            labelList.add(labels.getString(i))
        }
        onUiThread { (viewRegistry[viewId] as? SimpleChartView)?.labels = labelList }
    }

    @Suppress("unused")
    fun setChartColor(receiver: V8Object, color: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val chart = viewRegistry[viewId] as? SimpleChartView ?: return@onUiThread
            chart.lineColor = ChartScale.parseColorOr(color, chart.lineColor)
        }
    }

    @Suppress("unused")
    fun setMarkdownContent(receiver: V8Object, markdown: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            val textView = viewRegistry[viewId] as? TextView ?: return@onUiThread
            textView.text = MarkdownRenderer.render(markdown, context.resources.displayMetrics.density)
        }
    }

    // ------------------------------------------------------------------
    // Container methods
    // ------------------------------------------------------------------

    @Suppress("unused")
    fun addChildToLayout(receiver: V8Object, childObject: V8Object) {
        val layoutId = receiver.getInteger("_viewId")
        val childId = childObject.getInteger("_viewId")
        val child = viewRegistry[childId] ?: return
        val layout = viewRegistry[layoutId] as? ViewGroup ?: return
        onUiThread {
            (child.parent as? ViewGroup)?.removeView(child)
            layout.addView(child)
        }
    }

    @Suppress("unused")
    fun removeChildFromLayout(receiver: V8Object, childObject: V8Object) {
        val layoutId = receiver.getInteger("_viewId")
        val childId = childObject.getInteger("_viewId")
        val child = viewRegistry[childId] ?: return
        val layout = viewRegistry[layoutId] as? ViewGroup ?: return
        onUiThread { layout.removeView(child) }
    }

    @Suppress("unused")
    fun setLayoutOrientation(receiver: V8Object, orientation: String) {
        val viewId = receiver.getInteger("_viewId")
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
    fun setLayoutGravity(receiver: V8Object, gravity: String) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread {
            (viewRegistry[viewId] as? LinearLayout)?.gravity = gravityFor(gravity)
        }
    }

    @Suppress("unused")
    fun addChildToScrollView(receiver: V8Object, childObject: V8Object) {
        val scrollViewId = receiver.getInteger("_viewId")
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
    fun removeChildFromScrollView(receiver: V8Object, childObject: V8Object) {
        val scrollViewId = receiver.getInteger("_viewId")
        val childId = childObject.getInteger("_viewId")
        val child = viewRegistry[childId] ?: return
        val scrollView = viewRegistry[scrollViewId] as? ScrollView ?: return
        onUiThread { scrollView.removeView(child) }
    }

    @Suppress("unused")
    fun setScrollViewFillViewport(receiver: V8Object, fillViewport: Boolean) {
        val viewId = receiver.getInteger("_viewId")
        onUiThread { (viewRegistry[viewId] as? ScrollView)?.isFillViewport = fillViewport }
    }

    // ------------------------------------------------------------------
    // UI namespace methods
    // ------------------------------------------------------------------

    /**
     * Add view to the current page. Reparents the view if it is already
     * attached elsewhere.
     */
    @Suppress("unused")
    fun addView(jsObject: V8Object) {
        val viewId = jsObject.getInteger("_viewId")
        val view = viewRegistry[viewId] ?: return

        onUiThread {
            (view.parent as? ViewGroup)?.removeView(view)
            currentPage?.addView(view)
        }
    }

    /**
     * Remove view from the current page.
     */
    @Suppress("unused")
    fun removeView(viewId: Int) {
        val view = viewRegistry[viewId] ?: return

        onUiThread {
            currentPage?.removeView(view)
        }

        viewRegistry.remove(viewId)
    }

    /**
     * Clear all views on the current page.
     */
    @Suppress("unused")
    fun clearViews() {
        onUiThread {
            currentPage?.removeAllViews()
        }
        viewRegistry.clear()
        viewStyles.clear()
        textStates.clear()
    }

    /**
     * Push a new page onto the stack. The host shows exactly one page at a
     * time, so the previous page is detached (kept alive) and the new empty
     * page becomes the current page. Returns the new page depth.
     */
    fun pushPage(): Int {
        val newPage = createPage()
        onUiThread {
            currentPage?.let { rootView.removeView(it) }
            rootView.addView(newPage)
        }
        pageStack.push(newPage)
        onPageDepthChanged?.invoke(pageStack.size)
        return pageStack.size
    }

    /**
     * Pop the current page, re-attaching the page below it to the host.
     * Views that lived on the dropped page are unregistered. Returns false
     * (and does nothing) when already at the root page.
     */
    fun popPage(): Boolean {
        if (pageStack.size <= 1) return false
        val dropped = pageStack.pop()
        val restored = pageStack.peek()
        onUiThread {
            rootView.removeView(dropped)
            if (restored != null && restored.parent !== rootView) {
                rootView.addView(restored)
            }
        }
        val staleIds = viewRegistry.entries
            .filter { isContainedIn(it.value, dropped) }
            .map { it.key }
        for (id in staleIds) {
            viewRegistry.remove(id)
            viewStyles.remove(id)
            textStates.remove(id)
        }
        onPageDepthChanged?.invoke(pageStack.size)
        return true
    }

    /**
     * Current page depth; 1 means only the root page exists.
     */
    fun pageDepth(): Int = pageStack.size

    /**
     * Set the background color of the root container.
     */
    @Suppress("unused")
    fun setRootBackgroundColor(color: String) {
        onUiThread { rootView.setBackgroundColor(parseColor(color)) }
    }

    /**
     * `UI.setTitle(title)` — the script's display title for the top bar.
     * Hosts fall back to a prettified script name when never called.
     */
    @Suppress("unused")
    fun setTitle(title: String) {
        onSetTitle?.invoke(title)
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
        val retained = retainCallback(callback)
        onUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK") { _, _ ->
                    invokeCallback(retained, runtime) { it.push(true) }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    invokeCallback(retained, runtime) { it.push(false) }
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
        val retained = retainCallback(callback)
        onUiThread {
            val input = EditText(context)
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val value = input.text.toString()
                    invokeCallback(retained, runtime) {
                        it.push(value).push(false)
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    invokeCallback(retained, runtime) { it.push("").push(true) }
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
        val retained = retainCallback(callback)
        val labels = (0 until items.length()).map { items.getString(it) }.toTypedArray()
        onUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(labels) { _, which ->
                    invokeCallback(retained, runtime) {
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
        // Second arg is optional; only the string "long" (any case) selects
        // LENGTH_LONG — any other type is tolerated as LENGTH_SHORT.
        val long = parameters.length() > 1 &&
            parameters.getType(1) == V8Value.STRING &&
            parameters.getString(1).equals("long", ignoreCase = true)
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
            arrayOf(V8Object::class.java, Boolean::class.java), true)
        jsObject.registerJavaMethod(this, "setViewEnabled", "setEnabled",
            arrayOf(V8Object::class.java, Boolean::class.java), true)
        jsObject.registerJavaMethod(this, "setViewPadding", "setPadding",
            arrayOf(V8Object::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "setViewMargin", "setMargin",
            arrayOf(V8Object::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "setViewWidth", "setWidth",
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "setViewHeight", "setHeight",
            arrayOf(V8Object::class.java, Int::class.java), true)
        jsObject.registerJavaMethod(this, "setViewAlpha", "setAlpha",
            arrayOf(V8Object::class.java, Double::class.java), true)
        jsObject.registerJavaMethod(this, "setViewBackgroundColor", "setBackgroundColor",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setViewCornerRadius", "setCornerRadius",
            arrayOf(V8Object::class.java, Double::class.java), true)
        jsObject.registerJavaMethod(this, "getViewId", "getViewId", arrayOf(V8Object::class.java), true)
    }

    private fun registerTextMethods(jsObject: V8Object) {
        jsObject.registerJavaMethod(this, "setTextViewSize", "setTextSize",
            arrayOf(V8Object::class.java, Double::class.java), true)
        jsObject.registerJavaMethod(this, "setTextViewColor", "setTextColor",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setTextViewBold", "setBold",
            arrayOf(V8Object::class.java, Boolean::class.java), true)
        jsObject.registerJavaMethod(this, "setTextViewItalic", "setItalic",
            arrayOf(V8Object::class.java, Boolean::class.java), true)
        jsObject.registerJavaMethod(this, "setTextViewAlign", "setTextAlign",
            arrayOf(V8Object::class.java, String::class.java), true)
        jsObject.registerJavaMethod(this, "setTextViewAllCaps", "setAllCaps",
            arrayOf(V8Object::class.java, Boolean::class.java), true)
        jsObject.registerJavaMethod(this, "setTextViewStrikeThrough", "setStrikeThrough",
            arrayOf(V8Object::class.java, Boolean::class.java), true)
    }

    /**
     * Run [block] on the Android main thread, blocking the caller until it
     * completes. This is a no-op when already on the main thread.
     *
     * Callers are usually the engine's V8 thread (JS→Java bridge methods).
     * Block-waiting here is deadlock-free because the main thread never
     * block-waits on the engine thread: script execution is awaited
     * asynchronously and engine teardown never blocks (see JavaScriptEngine).
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

    /**
     * Run [block] on the engine's V8 thread. This is a no-op when already on
     * it. Java→JS callbacks (view events, dialogs) fire on the main thread
     * and MUST be re-marshaled: J2V8 throws on foreign-thread access.
     */
    private fun onEngineThread(block: () -> Unit) {
        val handler = engineHandler
        if (handler == null || Looper.myLooper() == handler.looper) {
            block()
        } else {
            handler.post(block)
        }
    }

    private fun registerView(view: View): Int {
        val viewId = nextViewId++
        viewRegistry[viewId] = view
        if (view is TextView) {
            defaultTextColors[viewId] = view.textColors
        }
        return viewId
    }

    /** Create a full-size vertical page container, on the UI thread. */
    private fun createPage(): LinearLayout {
        return onUiThread {
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    /** True if [view] is [container] itself or nested somewhere inside it. */
    private fun isContainedIn(view: View, container: ViewGroup): Boolean {
        var node: View? = view
        while (node != null) {
            if (node === container) return true
            node = node.parent as? View
        }
        return false
    }

    private fun parseColor(colorString: String): Int = parseColorSafe(colorString)

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

    /** Retain [callback] beyond the current call (J2V8 releases parameters on return). */
    private fun retainCallback(callback: V8Function): V8Function {
        val retained = callback.twin()
        retainedCallbacks.add(retained)
        return retained
    }

    /** Release a retained callback twin once it can no longer fire. */
    private fun releaseCallback(callback: V8Function) {
        if (retainedCallbacks.remove(callback) && !callback.isReleased) {
            callback.release()
        }
    }

    private fun invokeCallback(callback: V8Function, runtime: V8, fill: (V8Array) -> Unit) {
        // Dialog buttons fire on the main thread; V8 work belongs to the
        // engine thread
        onEngineThread {
            try {
                val params = V8Array(runtime)
                fill(params)
                callback.call(runtime, params)
                params.release()
            } catch (e: Exception) {
                // The callback may have been released after script teardown; ignore
            } finally {
                // Dialog callbacks are one-shot: release the retained twin
                releaseCallback(callback)
            }
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Float = value * context.resources.displayMetrics.density

    companion object {
        /**
         * Add or remove [android.graphics.Paint.STRIKE_THRU_TEXT_FLAG] while
         * preserving all other paint flags. V8-free so it is JVM-testable.
         */
        internal fun applyStrikeThrough(textView: TextView, enabled: Boolean) {
            textView.paintFlags = if (enabled) {
                textView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textView.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        /**
         * Apply a text color, or restore [defaultColors] when [color] is
         * blank. V8-free so it is JVM-testable.
         */
        internal fun applyTextColor(
            textView: TextView,
            color: String,
            defaultColors: android.content.res.ColorStateList?
        ) {
            if (color.isBlank()) {
                defaultColors?.let { textView.setTextColor(it) }
            } else {
                textView.setTextColor(parseColorSafe(color))
            }
        }

        internal fun parseColorSafe(colorString: String): Int {
            return try {
                Color.parseColor(colorString)
            } catch (e: Exception) {
                Color.BLACK
            }
        }
    }
}
