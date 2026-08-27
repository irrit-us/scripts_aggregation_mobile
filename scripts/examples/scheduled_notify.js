// Example 16: Scheduled Notifications
// Demonstrates the notification APIs: immediate posts with Notify.post,
// one-shot timed triggers with Scheduler.scheduleIn / Scheduler.scheduleAt,
// daily recurring reminders with Scheduler.scheduleDaily, and
// Scheduler.cancel. WorkManager timing is inexact (minute-scale).

// Immediate notification
Notify.post("SAM", "Scheduled notifications demo started");

// One-shot: fires about one minute from now
let oneShot = Scheduler.scheduleIn("demo-oneshot", 60000, "One-shot",
    "Fired about a minute after the script ran.");
console.log("one-shot scheduled: " + oneShot);

// One-shot at an absolute time (epoch milliseconds)
let atTime = Date.now() + 2 * 60000;
Scheduler.scheduleAt("demo-at", atTime, "Absolute",
    "Fired at a fixed point in time.");

// Daily recurring reminder at 08:30 local time
Scheduler.scheduleDaily("demo-daily", 8, 30, "Morning",
    "This reminder repeats every day.");

// Cancel the one-shot again (cancel works for daily schedules too)
Scheduler.cancel("demo-oneshot");
