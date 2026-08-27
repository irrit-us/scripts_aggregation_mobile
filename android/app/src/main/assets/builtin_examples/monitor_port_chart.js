// Example 10: Monitor Port Chart
// Polls a monitor endpoint every 5 seconds and renders a rolling line chart
// of the last 20 numeric values. Green line while below the threshold,
// red otherwise; red immediately when the check fails (DOWN).
// The script declares its configurable fields via Config.schema(); after
// it has run once, they appear in Settings under the script's section.
// Permissions: INTERNET, CONFIG

Config.schema(JSON.stringify([
    { key: "MONITOR_URL", label: "Monitor URL", type: "text" },
    { key: "MONITOR_API_KEY", label: "API Key", type: "password" },
    { key: "MONITOR_THRESHOLD", label: "Threshold", type: "number" }
]));

let hint = new Label("Configure MONITOR_URL and optional MONITOR_API_KEY / MONITOR_THRESHOLD in Settings.");
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

let chart = new Chart("line");
chart.setWidth(-1);
chart.setHeight(220);
chart.setMargin(0, 8, 0, 8);
chart.setColor("#34C759");
UI.addView(chart);

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
let values = [];
let maxWindow = 20;

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
    statusLabel.setText("Monitoring...");
    pollOnce(url);
    monitorTimer = setInterval(function() {
        pollOnce(url);
    }, pollIntervalMs);
    console.log("Port chart monitoring started for " + url);
}

function stopMonitoring() {
    if (monitorTimer !== null) {
        clearInterval(monitorTimer);
        monitorTimer = null;
    }
    requestInFlight = false;
    statusLabel.setText("Stopped");
    detailLabel.setText("Press Start to resume polling.");
    console.log("Port chart monitoring stopped");
}

function getThreshold() {
    let raw = Config.get("MONITOR_THRESHOLD");
    let threshold = Number(raw);
    if (!raw || isNaN(threshold)) {
        return 500;
    }
    return threshold;
}

function extractValue(info) {
    if (typeof info.latency_ms === "number") {
        return info.latency_ms;
    }
    if (typeof info.response_time === "number") {
        return info.response_time;
    }
    if (typeof info.value === "number") {
        return info.value;
    }
    return null;
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

function markDown(message) {
    chart.setColor("#FF3B30");
    statusLabel.setText("DOWN");
    detailLabel.setText(message);
    chart.setData(values);
}

function handleResponse(data, error) {
    requestInFlight = false;
    if (error) {
        markDown("Error: " + error);
        console.error("Monitor check failed: " + error);
        return;
    }
    let info;
    try {
        info = JSON.parse(data);
    } catch (e) {
        markDown("Invalid JSON response");
        console.error("Parse error: " + e);
        return;
    }
    let sample = extractValue(info);
    if (sample === null) {
        markDown("No numeric field in response");
        console.error("Response missing latency_ms / response_time / value");
        return;
    }
    values.push(sample);
    if (values.length > maxWindow) {
        values.shift();
    }
    let labels = [];
    for (let index = 0; index < values.length; index++) {
        labels.push("" + (index + 1));
    }
    chart.setLabels(labels);
    chart.setData(values);
    let threshold = getThreshold();
    if (sample < threshold) {
        chart.setColor("#34C759");
        statusLabel.setText("UP - last: " + sample + " ms");
    } else {
        chart.setColor("#FF3B30");
        statusLabel.setText("UP - last: " + sample + " ms (over " + threshold + " ms)");
    }
    detailLabel.setText("Window: " + values.length + " samples, threshold " + threshold + " ms");
    console.log("Monitor sample: " + sample + " ms");
}
