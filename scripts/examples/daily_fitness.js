// Example: Daily Fitness Reminder
// Shows a fitness tip for today and schedules a daily 8:00 notification
// with that tip via Scheduler; can also preview the notification instantly.

let tips = [
    "Sunday: Stretch for 10 minutes before breakfast.",
    "Monday: Take a brisk 20-minute walk today.",
    "Tuesday: Do 3 sets of 10 push-ups during the day.",
    "Wednesday: Drink water instead of sugary drinks.",
    "Thursday: Take the stairs instead of the elevator.",
    "Friday: Hold a plank for 60 seconds, twice.",
    "Saturday: Go outside for a 30-minute activity."
];

let todayIndex = new Date().getDay();
let todayTip = tips[todayIndex];

let title = new Label("Daily Fitness Reminder");
title.setTextSize(24);
UI.addView(title);

let tipLabel = new Label(todayTip);
tipLabel.setTextSize(16);
UI.addView(tipLabel);

let statusLabel = new Label("Reminder not configured.");
statusLabel.setTextSize(13);
statusLabel.setTextColor("#888888");
UI.addView(statusLabel);

let enableBtn = new Button("Enable daily 8:00 reminder");
enableBtn.setBackgroundColor("#34C759");
enableBtn.setTextColor("#FFFFFF");
enableBtn.setOnTap(function() {
    try {
        let scheduled = Scheduler.scheduleDaily("fitness", 8, 0, "Daily Fitness", todayTip);
        if (scheduled) {
            statusLabel.setText("Daily 8:00 reminder enabled.");
            showToast("Reminder scheduled for 8:00");
        } else {
            statusLabel.setText("Could not schedule reminder.");
            showToast("Scheduling failed");
        }
        console.log("scheduleDaily returned: " + scheduled);
    } catch (e) {
        statusLabel.setText("Scheduler error");
        console.error("Scheduler.scheduleDaily failed: " + e);
    }
});
UI.addView(enableBtn);

let disableBtn = new Button("Disable reminder");
disableBtn.setBackgroundColor("#FF3B30");
disableBtn.setTextColor("#FFFFFF");
disableBtn.setOnTap(function() {
    try {
        let cancelled = Scheduler.cancel("fitness");
        if (cancelled) {
            statusLabel.setText("Reminder disabled.");
            showToast("Reminder cancelled");
        } else {
            statusLabel.setText("No reminder to cancel.");
        }
        console.log("Scheduler.cancel returned: " + cancelled);
    } catch (e) {
        statusLabel.setText("Scheduler error");
        console.error("Scheduler.cancel failed: " + e);
    }
});
UI.addView(disableBtn);

let previewBtn = new Button("Preview notification");
previewBtn.setOnTap(function() {
    try {
        Notify.post("Daily Fitness", todayTip);
        showToast("Notification posted");
        console.log("Preview notification posted");
    } catch (e) {
        statusLabel.setText("Notify error");
        console.error("Notify.post failed: " + e);
    }
});
UI.addView(previewBtn);

console.log("Daily fitness reminder example loaded");
