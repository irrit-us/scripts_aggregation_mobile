// Example 15: Markdown Demo
// Demonstrates the Markdown view: headings, emphasis, code, lists,
// blockquotes, links, and horizontal rules rendered by the built-in
// lightweight parser (no external dependencies).

// Root background
UI.setBackgroundColor("#FAFAFA");

let doc = new Markdown(
    "# SAM Markdown\n" +
    "Lightweight rendering, **no dependencies**.\n" +
    "\n" +
    "## Features\n" +
    "- **Bold**, *italic*, ~~strikethrough~~\n" +
    "- `inline code` spans\n" +
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
    "Plain math like 2 * 3 stays literal."
);
UI.addView(doc);

// Replace the content on demand
let toggle = new Button("Show code sample");
toggle.setOnTap(function () {
    doc.setMarkdown("## Code\n```\nfun hello() = println(\"hi\")\n```");
});
UI.addView(toggle);

console.log("Markdown demo rendered");
