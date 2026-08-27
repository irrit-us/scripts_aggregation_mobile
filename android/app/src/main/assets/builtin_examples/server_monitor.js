// Example 8: Server Monitoring
// Polls a server health endpoint and shows live UP/DOWN status.
// The script declares its configurable fields via Config.schema(); after
// it has run once, they appear in Settings under the script's section:
//   MONITOR_URL           - health endpoint URL (required)
//   MONITOR_API_KEY       - optional Bearer token (password field)
//   MONITOR_INTERVAL_SEC  - poll interval in seconds (default 5)
// Permissions: INTERNET, CONFIG

Config.schema(JSON.stringify([
    { key: "MONITOR_URL", label: "Monitor URL", type: "text" },
    { key: "MONITOR_API_KEY", label: "API Key", type: "password" },
    { key: "MONITOR_INTERVAL_SEC", label: "Interval (seconds)", type: "number",
      default: "5" }
]));

let title = new Label("Server Monitor");
title.setTextSize(24);
UI.addView(title);

let hint = new Label("Configure MONITOR_URL and optional MONITOR_API_KEY in Settings.");
hint.setTextSize(12);
hint.setTextColor("#888888");
UI.addView(hint);

let startBtn = new Button("Start Monitoring");
startBtn.setBackgroundColor("#007AFF");
startBtn.setTextColor("#FFFFFF");
startBtn.setOnTap(function() {
    startMonitoring();
});
UI.addView(startBtn);

let stopBtn = new Button("Stop Monitoring");
stopBtn.setOnTap(function() {
    stopMonitoring();
});
UI.addView(stopBtn);

let statusLabel = new Label("Stopped");
statusLabel.setTextSize(16);
UI.addView(statusLabel);

let detailLabel = new Label("Press Start to begin polling every 5 seconds.");
detailLabel.setTextSize(13);
detailLabel.setTextColor("#888888");
UI.addView(detailLabel);

let monitorTimer = null;
let pollIntervalMs = 5000;
let requestInFlight = false;

function readIntervalMs() {
    let seconds = parseInt(Config.get("MONITOR_INTERVAL_SEC") || "5", 10);
    if (isNaN(seconds) || seconds < 1) {
        seconds = 5;
    }
    return seconds * 1000;
}

function startMonitoring() {
    if (monitorTimer !== null) {
        showToast("Already monitoring");
        return;
    }
    let url = Config.get("MONITOR_URL");
    if (!url) {
        statusLabel.setText("Missing MONITOR_URL");
        detailLabel.setText("Add MONITOR_URL in Settings, then try again.");
        console.error("Missing MONITOR_URL configuration");
        return;
    }
    pollIntervalMs = readIntervalMs();
    statusLabel.setText("Monitoring...");
    pollOnce(url);
    monitorTimer = setInterval(function() {
        pollOnce(url);
    }, pollIntervalMs);
    console.log("Server monitoring started for " + url);
}

function stopMonitoring() {
    if (monitorTimer !== null) {
        clearInterval(monitorTimer);
        monitorTimer = null;
    }
    requestInFlight = false;
    statusLabel.setText("Stopped");
    detailLabel.setText("Press Start to resume polling.");
    console.log("Server monitoring stopped");
}

function pollOnce(url) {
    if (requestInFlight) {
        return;
    }
    requestInFlight = true;

    let apiKey = Config.get("MONITOR_API_KEY");
    if (apiKey) {
        let headers = { "Authorization": "Bearer " + apiKey };
        Network.get(url, headers, handleResponse);
    } else {
        Network.get(url, handleResponse);
    }
}

function handleResponse(data, error) {
    requestInFlight = false;
    let now = new Date().toLocaleTimeString();
    if (error) {
        statusLabel.setText("DOWN at " + now);
        detailLabel.setText("Error: " + error);
        console.error("Monitor check failed: " + error);
        return;
    }
    statusLabel.setText("UP at " + now);
    try {
        let info = JSON.parse(data);
        detailLabel.setText("HTTP 200 - " + (info.status || "ok"));
    } catch (e) {
        detailLabel.setText("HTTP 200 - " + data.substring(0, 60));
    }
    console.log("Monitor check OK at " + now);
}
