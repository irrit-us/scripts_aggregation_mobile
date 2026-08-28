// Example 16: Scheduled Notifications
// Demonstrates the notification APIs: immediate posts with Notify.post,
// one-shot timed triggers with Scheduler.scheduleIn / Scheduler.scheduleAt,
// daily recurring reminders with Scheduler.scheduleDaily, and
// Scheduler.cancel. WorkManager timing is inexact (minute-scale).
// Compact rows: each action is one horizontal row with an input field
// filling the row and an inline glyph button on the right.
// Permissions: NOTIFICATIONS

UI.setTitle("Scheduled Notifications");

let TEXT_SIZE = 16;
let GLYPH_SIZE = TEXT_SIZE * 1.5;

// Parses an integer input value, falling back when it is not a number
function toInt(value, fallback) {
    let parsed = parseInt(value);
    return isNaN(parsed) ? fallback : parsed;
}

// Parses "HH:MM" into hour and minute, clamped to a valid wall clock time
function parseTime(value) {
    let parts = ("" + value).split(":");
    let hour = toInt(parts[0], 8);
    let minute = toInt(parts.length > 1 ? parts[1] : "30", 30);
    if (hour < 0 || hour > 23) hour = 8;
    if (minute < 0 || minute > 59) minute = 30;
    return [hour, minute];
}

// Builds one compact row: a small bold caption, an input field weighted to
// fill the row, and a glyph button on the right that runs the action
function makeRow(caption, hint, value, glyph, onTap) {
    let label = new Label(caption);
    label.setTextSize(TEXT_SIZE);
    label.setBold(true);
    UI.addView(label);

    let row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity("center_vertical");
    UI.addView(row);

    let input = new TextField(hint);
    input.setTextSize(TEXT_SIZE);
    input.setValue(value);
    row.addView(input);
    input.setWeight(1);

    let btn = new Label(glyph);
    btn.setTextSize(GLYPH_SIZE);
    btn.setBold(true);
    btn.setPadding(16, 0, 16, 0);
    btn.setOnTap(function() {
        onTap(input.getValue());
    });
    row.addView(btn);
}

// Immediate notification: a real system notification in the status bar and
// notification drawer, not an in-app toast or dialog
makeRow("Notify now", "Message text...", "This came from Notify.post", "+", function(value) {
    let text = ("" + value).trim();
    if (text === "") text = "This came from Notify.post";
    Notify.post("SAM system notification", text);
    console.log("Notify.post: " + text);
});

// One-shot timed trigger: fires about N seconds after the tap
makeRow("One-shot in N seconds", "Delay in seconds...", "60", "+", function(value) {
    let seconds = toInt(value, 60);
    if (seconds < 1) seconds = 1;
    let id = Scheduler.scheduleIn("demo-oneshot", seconds * 1000, "One-shot",
        "Fired about " + seconds + " seconds after the tap.");
    console.log("one-shot scheduled in " + seconds + "s: " + id);
});

// One-shot at an absolute time: N minutes from now as epoch milliseconds
makeRow("One-shot at +N minutes", "Minutes from now...", "2", "+", function(value) {
    let minutes = toInt(value, 2);
    if (minutes < 1) minutes = 1;
    let atTime = Date.now() + minutes * 60000;
    Scheduler.scheduleAt("demo-at", atTime, "Absolute",
        "Fired at a fixed point in time.");
    console.log("one-shot scheduled at +" + minutes + "min: " + atTime);
});

// Daily recurring reminder at a local wall clock time like 8:30
makeRow("Daily reminder at HH:MM", "Time like 8:30...", "8:30", "+", function(value) {
    let hm = parseTime(value);
    Scheduler.scheduleDaily("demo-daily", hm[0], hm[1], "Morning",
        "This reminder repeats every day.");
    console.log("daily reminder scheduled at " + hm[0] + ":" + hm[1]);
});

// Cancel a schedule by id; works for one-shot and daily schedules alike
makeRow("Cancel a schedule by id", "Schedule id...", "demo-oneshot", "X", function(value) {
    let id = ("" + value).trim();
    if (id === "") id = "demo-oneshot";
    Scheduler.cancel(id);
    console.log("schedule cancelled: " + id);
});
