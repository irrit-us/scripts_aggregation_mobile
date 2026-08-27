package com.scripthost.ui.markdown

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan

/**
 * Lightweight Markdown renderer producing a [Spannable] for a TextView.
 *
 * Deliberately small and dependency-free. Supported block syntax: headings
 * `#`..`####`, fenced ``` code blocks, unordered lists (`-`/`*`, one nesting
 * level via leading spaces), ordered lists (`1.`), blockquotes (`>`),
 * horizontal rules (`---`/`***`/`___`) and plain paragraphs. Supported inline
 * syntax: `**bold**`, `*italic*` / `_italic_`, `~~strikethrough~~`,
 * `` `inline code` `` and `[text](url)` links.
 *
 * The parser never throws: malformed input (unclosed markers, stray
 * backticks, empty strings) is rendered literally.
 *
 * No V8 or Context dependency, so it is unit-testable on the JVM via
 * Robolectric. [density] scales indentation; pass
 * `resources.displayMetrics.density` from the UI layer.
 */
object MarkdownRenderer {

    private enum class Inline { BOLD, ITALIC, STRIKE }

    private const val CODE_BACKGROUND = 0x14000000
    private const val QUOTE_BAR_COLOR = 0xFF9E9E9E.toInt()
    private const val RULE_COLOR = 0xFF9E9E9E.toInt()
    private const val LINK_COLOR = 0xFF1976D2.toInt()

    private val HEADING_SCALE = floatArrayOf(1.6f, 1.4f, 1.2f, 1.1f)

    /**
     * Render [markdown] into a styled [CharSequence]. [density] converts dp
     * indents to pixels; defaults to 1 for tests.
     */
    fun render(markdown: String, density: Float = 1f): CharSequence {
        val out = SpannableStringBuilder()
        if (markdown.isEmpty()) return out

        val indent = (16 * density).toInt().coerceAtLeast(1)
        val lines = markdown.replace("\r\n", "\n").split("\n")
        var i = 0
        var inCodeBlock = false
        val codeLines = StringBuilder()

        fun flushCodeBlock() {
            if (codeLines.isEmpty()) return
            val text = codeLines.toString().removeSuffix("\n")
            val start = out.length
            out.append(text)
            out.setSpan(TypefaceSpan("monospace"), start, out.length)
            out.setSpan(BackgroundColorSpan(CODE_BACKGROUND), start, out.length)
            out.setSpan(LeadingMarginSpan.Standard(indent, indent), start, out.length)
            out.append("\n")
            codeLines.clear()
        }

        fun appendBlock(block: CharSequence) {
            if (out.isNotEmpty() && !out.endsWith("\n")) out.append("\n")
            out.append(block)
            out.append("\n")
        }

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            when {
                trimmed.startsWith("```") -> {
                    if (inCodeBlock) {
                        flushCodeBlock()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                }
                inCodeBlock -> codeLines.append(line).append("\n")
                trimmed.isEmpty() -> {
                    // Paragraph separator: collapse consecutive blank lines
                    if (out.isNotEmpty() && !out.endsWith("\n\n")) out.append("\n")
                }
                isHorizontalRule(trimmed) -> {
                    val start = out.length
                    appendBlock(SpannableStringBuilder("———————————————————————"))
                    out.setSpan(ForegroundColorSpan(RULE_COLOR), start, out.length - 1)
                }
                else -> appendStyledLine(out, line, indent)
            }
            i++
        }
        if (inCodeBlock) flushCodeBlock() // unclosed fence: render literally as code

        while (out.isNotEmpty() && out[out.length - 1] == '\n') {
            out.delete(out.length - 1, out.length)
        }
        return out
    }

    // ------------------------------------------------------------------
    // Block-level
    // ------------------------------------------------------------------

    private fun isHorizontalRule(trimmed: String): Boolean {
        if (trimmed.length < 3) return false
        val marker = trimmed[0]
        if (marker != '-' && marker != '*' && marker != '_') return false
        return trimmed.all { it == marker }
    }

    private fun appendStyledLine(out: SpannableStringBuilder, line: String, indent: Int) {
        val trimmedStart = line.trimStart()
        val leadingSpaces = line.length - trimmedStart.length

        // Heading
        val headingMatch = Regex("^(#{1,4})\\s+(.*)$").find(trimmedStart)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val start = out.length
            appendInline(out, headingMatch.groupValues[2], emptySet())
            out.setSpan(StyleSpan(Typeface.BOLD), start, out.length)
            out.setSpan(RelativeSizeSpan(HEADING_SCALE[level - 1]), start, out.length)
            out.append("\n")
            return
        }

        // Blockquote
        if (trimmedStart.startsWith(">")) {
            val content = trimmedStart.removePrefix(">").removePrefix(" ")
            val start = out.length
            appendInline(out, content, emptySet())
            out.setSpan(QuoteSpan(QUOTE_BAR_COLOR), start, out.length)
            out.setSpan(LeadingMarginSpan.Standard(indent, indent), start, out.length)
            out.append("\n")
            return
        }

        // Unordered list
        val bulletMatch = Regex("^[-*]\\s+(.*)$").find(trimmedStart)
        if (bulletMatch != null) {
            val nested = leadingSpaces >= 2
            val start = out.length
            out.append(if (nested) "◦ " else "• ")
            appendInline(out, bulletMatch.groupValues[1], emptySet())
            val margin = indent * (if (nested) 2 else 1)
            out.setSpan(LeadingMarginSpan.Standard(margin, margin), start, out.length)
            out.append("\n")
            return
        }

        // Ordered list
        val orderedMatch = Regex("^(\\d+)\\.\\s+(.*)$").find(trimmedStart)
        if (orderedMatch != null) {
            val nested = leadingSpaces >= 2
            val start = out.length
            out.append(orderedMatch.groupValues[1]).append(". ")
            appendInline(out, orderedMatch.groupValues[2], emptySet())
            val margin = indent * (if (nested) 2 else 1)
            out.setSpan(LeadingMarginSpan.Standard(margin, margin), start, out.length)
            out.append("\n")
            return
        }

        // Plain paragraph line
        appendInline(out, trimmedStart, emptySet())
        out.append("\n")
    }

    // ------------------------------------------------------------------
    // Inline-level
    // ------------------------------------------------------------------

    /**
     * Append [text] to [out], parsing inline markers. [styles] are the styles
     * inherited from enclosing markers. Unclosed markers render literally.
     */
    private fun appendInline(out: SpannableStringBuilder, text: String, styles: Set<Inline>) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '`' -> {
                    val close = text.indexOf('`', i + 1)
                    if (close < 0) {
                        appendPlain(out, text, i, i + 1, styles)
                        i++
                    } else {
                        val start = out.length
                        out.append(text, i + 1, close)
                        out.setSpan(TypefaceSpan("monospace"), start, out.length)
                        out.setSpan(BackgroundColorSpan(CODE_BACKGROUND), start, out.length)
                        applyStyles(out, start, out.length, styles)
                        i = close + 1
                    }
                }
                text.startsWith("**", i) -> {
                    var close = text.indexOf("**", i + 2)
                    if (close > i + 2 && text.getOrNull(close + 2) == '*') {
                        // A run of 3+ asterisks: the extra one closes an inner
                        // marker (e.g. "**bold *italic***"), so include it
                        close += 1
                    }
                    if (close < 0 || close == i + 2) {
                        appendPlain(out, text, i, i + 2, styles)
                        i += 2
                    } else {
                        appendInline(out, text.substring(i + 2, close), styles + Inline.BOLD)
                        i = close + 2
                    }
                }
                text.startsWith("~~", i) -> {
                    val close = text.indexOf("~~", i + 2)
                    if (close < 0 || close == i + 2) {
                        appendPlain(out, text, i, i + 2, styles)
                        i += 2
                    } else {
                        appendInline(out, text.substring(i + 2, close), styles + Inline.STRIKE)
                        i = close + 2
                    }
                }
                c == '*' || c == '_' -> {
                    val close = text.indexOf(c, i + 1)
                    if (close < 0 || close == i + 1) {
                        appendPlain(out, text, i, i + 1, styles)
                        i++
                    } else {
                        appendInline(out, text.substring(i + 1, close), styles + Inline.ITALIC)
                        i = close + 1
                    }
                }
                c == '[' -> {
                    val parsed = tryParseLink(text, i)
                    if (parsed == null) {
                        appendPlain(out, text, i, i + 1, styles)
                        i++
                    } else {
                        val (label, url, end) = parsed
                        val start = out.length
                        appendInline(out, label, styles)
                        if (out.length > start) {
                            out.setSpan(URLSpan(url), start, out.length)
                            out.setSpan(ForegroundColorSpan(LINK_COLOR), start, out.length)
                        }
                        i = end
                    }
                }
                else -> {
                    // Plain run up to the next potential marker
                    var j = i + 1
                    while (j < text.length && text[j] != '`' && text[j] != '*'
                        && text[j] != '_' && text[j] != '~' && text[j] != '[') {
                        j++
                    }
                    appendPlain(out, text, i, j, styles)
                    i = j
                }
            }
        }
    }

    /** Parse `[label](url)` starting at index [from]; returns label, url and end index, or null. */
    private fun tryParseLink(text: String, from: Int): Triple<String, String, Int>? {
        val labelEnd = text.indexOf("](", from + 1)
        if (labelEnd < 0) return null
        val urlEnd = text.indexOf(')', labelEnd + 2)
        if (urlEnd < 0) return null
        val label = text.substring(from + 1, labelEnd)
        val url = text.substring(labelEnd + 2, urlEnd)
        if (url.isEmpty()) return null
        return Triple(label, url, urlEnd + 1)
    }

    private fun appendPlain(
        out: SpannableStringBuilder,
        text: String,
        from: Int,
        to: Int,
        styles: Set<Inline>
    ) {
        val start = out.length
        out.append(text, from, to)
        applyStyles(out, start, out.length, styles)
    }

    private fun applyStyles(out: SpannableStringBuilder, start: Int, end: Int, styles: Set<Inline>) {
        if (end <= start) return
        for (style in styles) {
            when (style) {
                Inline.BOLD -> out.setSpan(StyleSpan(Typeface.BOLD), start, end)
                Inline.ITALIC -> out.setSpan(StyleSpan(Typeface.ITALIC), start, end)
                Inline.STRIKE -> out.setSpan(StrikethroughSpan(), start, end)
            }
        }
    }

    private fun SpannableStringBuilder.setSpan(span: Any, start: Int, end: Int) {
        if (end > start) {
            setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
