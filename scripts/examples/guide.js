// Guide - the built-in onboarding script.
// Auto-installed and run on first launch; can also be reinstalled any time
// from "+" -> "Built-in Examples". Renders this guide with the Markdown view.
// Permissions: CONFIG (none strictly required)
UI.setTitle("Guide");


let md = new Markdown(
    "# Welcome to SAM\n" +
    "*script aggregation mobile* - run JavaScript mini-apps on your phone.\n" +
    "\n" +
    "## Get around\n" +
    "- **Swipe right** or tap **the top-left button** to open the script drawer\n" +
    "- Tap a script in the drawer to run it on the right - it replaces this page\n" +
    "- Inside a script's pushed pages the top-left button becomes **X**: tap it (or fling right) to pop back\n" +
    "- To leave a running script, open the drawer - the script keeps running in the background\n" +
    "\n" +
    "## Add scripts\n" +
    "1. Tap **+** at the bottom of the drawer\n" +
    "2. **Built-in Examples** - 18 ready-made scripts, one tap to install\n" +
    "3. **Import from File** - pick any `.js` file; it is copied into the app\n" +
    "4. **Create New Script** - write your own in the editor\n" +
    "\n" +
    "## Settings (gear icon)\n" +
    "- **Debug mode** - mirror script console output to Logcat, and show the console + Stop Script panel\n" +
    "- **Appearance** - follow system / light / dark\n" +
    "- **Agent** - API base URL and key for the Agent Conversation script\n" +
    "- Review and revoke per-script permissions\n" +
    "\n" +
    "## What scripts can do\n" +
    "- UI: buttons, lists, sliders, charts, **markdown** (like this page)\n" +
    "- Network requests, local storage, sensors, vibration\n" +
    "- Notifications: immediate and scheduled reminders\n" +
    "- Device info: time, timezone, memory, storage, Android version, ABI\n" +
    "- Multi-page flows with `UI.pushPage()` / `UI.popPage()`\n" +
    "\n" +
    "## Try next\n" +
    "- **todo_list** - checkboxes, strikethrough, scrolling\n" +
    "- **metronome** - timers and vibration\n" +
    "- **device_info** - read-only system info\n" +
    "- **sub_screens** - multi-page navigation\n" +
    "\n" +
    "> Tip: long-press a script in the drawer for edit / export / delete.\n" +
    "\n" +
    "Happy scripting!"
);
UI.addView(md);
