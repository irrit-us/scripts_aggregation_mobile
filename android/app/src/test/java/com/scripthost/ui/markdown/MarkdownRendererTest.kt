package com.scripthost.ui.markdown

import android.graphics.Typeface
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [MarkdownRenderer]. Robolectric provides the android.text
 * implementation; no V8 runtime is involved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class MarkdownRendererTest {

    private fun render(markdown: String): Spanned =
        MarkdownRenderer.render(markdown) as Spanned

    private inline fun <reified T> spans(text: Spanned): Array<T> =
        text.getSpans(0, text.length, T::class.java)

    @Test
    fun headings_areBoldAndScaled() {
        val text = render("# One\n## Two\n### Three\n#### Four")
        val sizes = spans<RelativeSizeSpan>(text).map { it.sizeChange }
        assertThat(sizes).containsExactly(1.6f, 1.4f, 1.2f, 1.1f)
        assertThat(spans<StyleSpan>(text).count { it.style == Typeface.BOLD }).isEqualTo(4)
        assertThat(text.toString()).contains("One")
        assertThat(text.toString()).contains("Four")
    }

    @Test
    fun boldItalicStrike_applyStyleSpans() {
        val text = render("**bold** *italic* _also_ ~~gone~~")
        assertThat(spans<StyleSpan>(text).any { it.style == Typeface.BOLD }).isTrue()
        assertThat(spans<StyleSpan>(text).count { it.style == Typeface.ITALIC }).isEqualTo(2)
        assertThat(spans<StrikethroughSpan>(text)).isNotEmpty()
        // Markers are consumed, not rendered
        assertThat(text.toString()).isEqualTo("bold italic also gone")
    }

    @Test
    fun inlineCode_isMonospaceWithBackground() {
        val text = render("use `foo()` here")
        val mono = spans<TypefaceSpan>(text)
        assertThat(mono.any { it.family == "monospace" }).isTrue()
        assertThat(spans<BackgroundColorSpan>(text)).isNotEmpty()
        assertThat(text.toString()).isEqualTo("use foo() here")
    }

    @Test
    fun codeBlock_isMonospaceWithBackgroundAndContentPreserved() {
        val text = render("```\nval a = 1\n**not bold**\n```")
        assertThat(text.toString()).contains("val a = 1")
        // Block content is not inline-parsed
        assertThat(text.toString()).contains("**not bold**")
        assertThat(spans<StyleSpan>(text).count { it.style == Typeface.BOLD }).isEqualTo(0)
        assertThat(spans<TypefaceSpan>(text).any { it.family == "monospace" }).isTrue()
        assertThat(spans<BackgroundColorSpan>(text)).isNotEmpty()
    }

    @Test
    fun unorderedList_rendersBulletsWithMargin() {
        val text = render("- one\n- two\n  - nested")
        assertThat(text.toString()).contains("• one")
        assertThat(text.toString()).contains("• two")
        assertThat(text.toString()).contains("◦ nested")
        assertThat(spans<LeadingMarginSpan>(text)).isNotEmpty()
    }

    @Test
    fun orderedList_keepsNumbers() {
        val text = render("1. first\n2. second")
        assertThat(text.toString()).contains("1. first")
        assertThat(text.toString()).contains("2. second")
        assertThat(spans<LeadingMarginSpan>(text)).isNotEmpty()
    }

    @Test
    fun blockquote_hasQuoteSpan() {
        val text = render("> quoted text")
        assertThat(text.toString()).contains("quoted text")
        assertThat(spans<QuoteSpan>(text)).isNotEmpty()
    }

    @Test
    fun link_hasUrlSpanWithTarget() {
        val text = render("see [the docs](https://example.com/docs) now")
        val urls = spans<URLSpan>(text)
        assertThat(urls.map { it.url }).containsExactly("https://example.com/docs")
        val start = text.getSpanStart(urls[0])
        val end = text.getSpanEnd(urls[0])
        assertThat(text.subSequence(start, end).toString()).isEqualTo("the docs")
    }

    @Test
    fun horizontalRule_rendersDivider() {
        val text = render("above\n---\nbelow")
        assertThat(text.toString()).contains("above")
        assertThat(text.toString()).contains("below")
        assertThat(text.toString()).doesNotContain("---")
    }

    @Test
    fun paragraphs_separatedByBlankLine() {
        val text = render("first paragraph\n\nsecond paragraph")
        assertThat(text.toString()).isEqualTo("first paragraph\n\nsecond paragraph")
    }

    @Test
    fun nested_boldContainingInlineCode_doesNotCrash() {
        val text = render("**bold `code`**")
        assertThat(text.toString()).isEqualTo("bold code")
        assertThat(spans<StyleSpan>(text).any { it.style == Typeface.BOLD }).isTrue()
        assertThat(spans<TypefaceSpan>(text).any { it.family == "monospace" }).isTrue()
    }

    @Test
    fun nested_boldItalic_combinesStyles() {
        val text = render("**bold and _italic_**")
        assertThat(text.toString()).isEqualTo("bold and italic")
        assertThat(spans<StyleSpan>(text).any { it.style == Typeface.BOLD }).isTrue()
        assertThat(spans<StyleSpan>(text).any { it.style == Typeface.ITALIC }).isTrue()
    }

    @Test
    fun nested_ambiguousAsteriskRuns_doNotCrash() {
        // "***" runs are inherently ambiguous for a lightweight parser; the
        // requirement is crash-safety and readable output, not perfect fidelity
        val text = render("**bold *and italic***")
        assertThat(text.toString()).contains("bold and italic")
    }

    @Test
    fun malformed_unclosedMarkers_renderLiterally() {
        assertThat(render("**unclosed").toString()).isEqualTo("**unclosed")
        assertThat(render("stray ` backtick").toString()).isEqualTo("stray ` backtick")
        assertThat(render("~~unclosed").toString()).isEqualTo("~~unclosed")
        assertThat(render("[text](no-close").toString()).isEqualTo("[text](no-close")
    }

    @Test
    fun malformed_emptyAndBlankInput_doNotThrow() {
        assertThat(render("").toString()).isEqualTo("")
        assertThat(render("\n\n").toString()).isEqualTo("")
        assertThat(render("```").toString()).isEqualTo("")
        assertThat(render("```\nunclosed block").toString()).contains("unclosed block")
    }

    @Test
    fun crlf_isNormalized() {
        val text = render("# Title\r\n\r\nbody")
        assertThat(text.toString()).contains("Title")
        assertThat(text.toString()).doesNotContain("\r")
    }
}
