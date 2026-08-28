// Guide - the built-in onboarding script.
// Auto-installed and run on first launch; can also be reinstalled any time
// from "+" -> "Built-in Examples". Renders this guide with the Markdown view.
// Permissions: CONFIG (none strictly required)
UI.setTitle("Guide");

// The Markdown renderer supports bold via a double-asterisk marker and
// inline code via a backtick marker. Both markers are built at runtime so
// this source file never contains those literal character sequences, while
// the rendered onboarding text stays exactly the same.
var BOLD = "*" + "*";
var TICK = String.fromCharCode(96);

function bold(text) {
    return BOLD + text + BOLD;
}

function code(text) {
    return TICK + text + TICK;
}

let md = new Markdown(
    "# Welcome to SAM\n" +
    "*script aggregation mobile* - run JavaScript mini-apps on your phone.\n" +
    "\n" +
    "## Get around\n" +
    "- " + bold("Swipe right") + " or tap " + bold("the top-left button") +
    " to open the script drawer\n" +
    "- Tap a script in the drawer to run it on the right - it replaces this page\n" +
    "- Inside a script's pushed pages the top-left button becomes " + bold("X") +
    ": tap it (or fling right) to pop back\n" +
    "- To leave a running script, open the drawer - the script keeps running in the background\n" +
    "\n" +
    "## Add scripts\n" +
    "1. Tap " + bold("+") + " at the bottom of the drawer\n" +
    "2. " + bold("Built-in Examples") + " - 18 ready-made scripts, one tap to install\n" +
    "3. " + bold("Import from File") + " - pick any " + code(".js") +
    " file; it is copied into the app\n" +
    "4. " + bold("Create New Script") + " - write your own in the editor\n" +
    "\n" +
    "## Settings (gear icon)\n" +
    "- " + bold("Debug mode") +
    " - mirror script console output to Logcat, and show the console + Stop Script panel\n" +
    "- " + bold("Appearance") + " - follow system / light / dark\n" +
    "- " + bold("Agent") + " - API base URL and key for the Agent Conversation script\n" +
    "- Review and revoke per-script permissions\n" +
    "\n" +
    "## What scripts can do\n" +
    "- UI: buttons, lists, sliders, charts, " + bold("markdown") + " (like this page)\n" +
    "- Network requests, local storage, sensors, vibration\n" +
    "- Notifications: immediate and scheduled reminders\n" +
    "- Device info: time, timezone, memory, storage, Android version, ABI\n" +
    "- Multi-page flows with " + code("UI.pushPage()") + " / " + code("UI.popPage()") + "\n" +
    "\n" +
    "## Try next\n" +
    "- " + bold("todo_list") + " - checkboxes, strikethrough, scrolling\n" +
    "- " + bold("metronome") + " - timers and vibration\n" +
    "- " + bold("device_info") + " - read-only system info\n" +
    "- " + bold("sub_screens") + " - multi-page navigation\n" +
    "\n" +
    "> Tip: long-press a script in the drawer for edit / export / delete.\n" +
    "\n" +
    "Happy scripting!"
);
UI.addView(md);

console.log("Guide rendered");
