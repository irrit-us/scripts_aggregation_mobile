// Example 15: Markdown Demo
// Demonstrates the Markdown view: headings, emphasis, code, lists,
// blockquotes, links, and horizontal rules rendered by the built-in
// lightweight parser (no external dependencies).
// Permissions: None

UI.setTitle("Markdown Demo");

// Root background
UI.setBackgroundColor("#FAFAFA");

// Marker helpers: bold markers and code ticks are assembled at runtime
// so this source file stays free of raw emphasis and fence characters.
let STAR = String.fromCharCode(42);
let BOLD = STAR + STAR;
let TICK = String.fromCharCode(96);
let FENCE = TICK + TICK + TICK;

// Showcase document covering the parser's main features
let showcase =
    "# SAM Markdown\n" +
    "Lightweight rendering, " + BOLD + "no dependencies" + BOLD + ".\n" +
    "\n" +
    "## Features\n" +
    "- " + BOLD + "Bold" + BOLD + ", *italic*, ~~strikethrough~~\n" +
    "- " + TICK + "inline code" + TICK + " spans\n" +
    "  - one nesting level\n" +
    "\n" +
    "1. First\n" +
    "2. Second\n" +
    "\n" +
    "> Blockquotes get a leading bar.\n" +
    "\n" +
    "---\n" +
    "See [the docs](https://example.com/docs) for more.\n" +
    "\n" +
    "Plain math like 2 * 3 stays literal.";

let codeSample =
    "## Code\n" +
    FENCE + "\n" +
    "fun hello() = println(\"hi\")\n" +
    FENCE;

// Compact control row: section title filling the row, "<>" toggle glyph
// on the right (about 1.5x the label text size)
let controlRow = new Layout("horizontal");
controlRow.setGravity("center_vertical");
controlRow.setWidth(-1);
UI.addView(controlRow);

let hint = new Label("Showcase document");
hint.setTextSize(16);
controlRow.addView(hint);
hint.setWeight(1);

let showingCode = false;

let toggleGlyph = new Label("<>");
toggleGlyph.setTextSize(24);
toggleGlyph.setBold(true);
toggleGlyph.setPadding(16, 0, 16, 0);
toggleGlyph.setOnTap(function() {
    showingCode = !showingCode;
    doc.setMarkdown(showingCode ? codeSample : showcase);
    hint.setText(showingCode ? "Code sample" : "Showcase document");
    console.log("Markdown switched to: " + (showingCode ? "code sample" : "showcase"));
});
controlRow.addView(toggleGlyph);

// Markdown view renders below the control row
let doc = new Markdown(showcase);
UI.addView(doc);

console.log("Markdown demo rendered");
