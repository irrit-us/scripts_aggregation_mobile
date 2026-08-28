// Example 10: Monitor Port Chart
// Polls a monitor endpoint on a configurable interval and renders a rolling
// line chart of the last 20 numeric values. Green line while below the
// threshold, red otherwise; red immediately when the check fails (DOWN).
// The script declares its configurable fields via Config.schema(); after
// it has run once, they appear in Settings under the script's section.
// A compact target/interval row sits above the chart: the URL field fills
// the row, a small interval field sets the poll period in seconds, and
// inline glyph buttons start and stop polling.
// Permissions: INTERNET, CONFIG
UI.setTitle("Port Monitor");

Config.schema(JSON.stringify([
    { key: "MONITOR_URL", label: "Monitor URL", type: "text" },
    { key: "MONITOR_API_KEY", label: "API Key", type: "password" },
    { key: "MONITOR_THRESHOLD", label: "Threshold", type: "number" }
]));

let baseSize = 14;

let hint = new Label("Target defaults to MONITOR_URL from Settings; threshold via MONITOR_THRESHOLD.");
hint.setTextSize(12);
hint.setTextColor("#888888");
UI.addView(hint);

// Compact controls row: target field filling the row, interval field,
// then inline start/stop glyphs at about 1.5x the row text size
let controlsRow = new Layout("horizontal");
controlsRow.setGravity("center_vertical");
controlsRow.setWidth(-1);
UI.addView(controlsRow);

let targetInput = new TextField("Target URL...");
targetInput.setTextSize(baseSize);
let savedUrl = Config.get("MONITOR_URL");
if (savedUrl) {
    targetInput.setValue(savedUrl);
}
controlsRow.addView(targetInput);
targetInput.setWeight(1);

let intervalInput = new TextField("5");
intervalInput.setTextSize(baseSize);
controlsRow.addView(intervalInput);

let startBtn = new Label("▶");
startBtn.setTextSize(baseSize * 1.5);
startBtn.setBold(true);
startBtn.setPadding(16, 0, 8, 0);
startBtn.setOnTap(function() {
    startMonitoring();
});
controlsRow.addView(startBtn);

let stopBtn = new Label("■");
stopBtn.setTextSize(baseSize * 1.5);
stopBtn.setBold(true);
stopBtn.setPadding(8, 0, 16, 0);
stopBtn.setOnTap(function() {
    stopMonitoring();
});
controlsRow.addView(stopBtn);

let chart = new Chart("line");
chart.setWidth(-1);
chart.setHeight(220);
chart.setMargin(0, 8, 0, 8);
chart.setColor("#34C759");
UI.addView(chart);

// Status row: state label fills the row, detail note right-aligned
let statusRow = new Layout("horizontal");
statusRow.setGravity("center_vertical");
statusRow.setWidth(-1);
UI.addView(statusRow);

let statusLabel = new Label("Stopped");
statusLabel.setTextSize(16);
statusLabel.setBold(true);
statusRow.addView(statusLabel);
statusLabel.setWeight(1);

let detailLabel = new Label("Press play to begin polling.");
detailLabel.setTextSize(12);
detailLabel.setTextColor("#888888");
statusRow.addView(detailLabel);

let monitorTimer = null;
let requestInFlight = false;
let values = [];
let maxWindow = 20;

function getIntervalMs() {
    let secs = parseInt(intervalInput.getValue());
    if (isNaN(secs) || secs < 1 || secs > 3600) {
        return 5000;
    }
    return secs * 1000;
}

function startMonitoring() {
    if (monitorTimer !== null) {
        showToast("Already monitoring");
        return;
    }
    let url = targetInput.getValue();
    if (!url || url.trim() === "") {
        url = Config.get("MONITOR_URL");
    }
    if (!url) {
        statusLabel.setText("Missing target URL");
        detailLabel.setText("Enter a URL above or set MONITOR_URL in Settings.");
        console.error("Missing MONITOR_URL configuration");
        return;
    }
    let intervalMs = getIntervalMs();
    statusLabel.setText("Monitoring...");
    pollOnce(url);
    monitorTimer = setInterval(function() {
        pollOnce(url);
    }, intervalMs);
    console.log("Port chart monitoring started for " + url + " every " + intervalMs + " ms");
}

function stopMonitoring() {
    if (monitorTimer !== null) {
        clearInterval(monitorTimer);
        monitorTimer = null;
    }
    requestInFlight = false;
    statusLabel.setText("Stopped");
    detailLabel.setText("Press play to resume polling.");
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
    } catch (err) {
        markDown("Invalid JSON response");
        console.error("Parse error: " + err);
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
