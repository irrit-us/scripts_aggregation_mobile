// Example 12: Stock Trends
// Loads daily closing prices for a stock symbol and renders them as a
// line chart. Green when the window ends up, red when it ends down.
// Configure STOCK_API_URL with a "{symbol}" placeholder in Settings
// (optionally STOCK_API_KEY sent as a Bearer token).

let title = new Label("Stock Trends");
title.setTextSize(24);
UI.addView(title);

let hint = new Label("Configure STOCK_API_URL with a {symbol} placeholder in Settings.");
hint.setTextSize(12);
hint.setTextColor("#888888");
UI.addView(hint);

let symbolInput = new TextField("Stock symbol");
symbolInput.setText("AAPL");
UI.addView(symbolInput);

let loadBtn = new Button("Load");
loadBtn.setBackgroundColor("#007AFF");
loadBtn.setTextColor("#FFFFFF");
loadBtn.setOnTap(function() {
    let symbol = symbolInput.getValue();
    if (symbol && symbol.trim() !== "") {
        loadTrends(symbol.trim());
    } else {
        showToast("Please enter a symbol");
    }
});
UI.addView(loadBtn);

let chart = new Chart("line");
chart.setWidth(-1);
chart.setHeight(220);
chart.setMargin(0, 8, 0, 8);
chart.setColor("#34C759");
UI.addView(chart);

let statusLabel = new Label("Enter a symbol and tap Load.");
statusLabel.setTextSize(14);
UI.addView(statusLabel);

let statsLabel = new Label("");
statsLabel.setTextSize(13);
statsLabel.setTextColor("#888888");
UI.addView(statsLabel);

function showSetupHint() {
    statusLabel.setText("Missing STOCK_API_URL");
    statsLabel.setText("Add STOCK_API_URL containing \"{symbol}\" in Settings, then try again.");
    console.error("Missing or invalid STOCK_API_URL configuration");
}

function parseCloses(info) {
    if (Array.isArray(info)) {
        if (info.length > 0 && typeof info[0] === "number") {
            return info;
        }
        let fromObjects = [];
        for (let index = 0; index < info.length; index++) {
            let entry = info[index];
            if (entry && typeof entry.close === "number") {
                fromObjects.push(entry.close);
            }
        }
        if (fromObjects.length === info.length && fromObjects.length > 0) {
            return fromObjects;
        }
        return null;
    }
    if (info && Array.isArray(info.closes)) {
        let closes = info.closes;
        for (let index = 0; index < closes.length; index++) {
            if (typeof closes[index] !== "number") {
                return null;
            }
        }
        return closes.length > 0 ? closes : null;
    }
    return null;
}

function renderTrends(closes) {
    let labels = [];
    for (let index = 0; index < closes.length; index++) {
        labels.push("" + (index + 1));
    }
    chart.setLabels(labels);
    chart.setData(closes);

    let first = closes[0];
    let last = closes[closes.length - 1];
    if (last >= first) {
        chart.setColor("#34C759");
    } else {
        chart.setColor("#FF3B30");
    }

    let changeText = "n/a";
    if (first !== 0) {
        let changePct = ((last - first) / first) * 100;
        changeText = changePct.toFixed(2) + "%";
    }
    statusLabel.setText("Loaded " + closes.length + " closes");
    statsLabel.setText("Last close: " + last + " | Change: " + changeText);
    console.log("Stock trends rendered, last close " + last);
}

function loadTrends(symbol) {
    let apiUrl = Config.get("STOCK_API_URL");
    if (!apiUrl || apiUrl.indexOf("{symbol}") === -1) {
        showSetupHint();
        return;
    }
    statusLabel.setText("Loading " + symbol + "...");
    statsLabel.setText("");

    let url = apiUrl.replace("{symbol}", encodeURIComponent(symbol));
    let apiKey = Config.get("STOCK_API_KEY");
    let callback = function(data, error) {
        if (error) {
            statusLabel.setText("Error: " + error);
            console.error("Stock request failed: " + error);
            return;
        }
        let info;
        try {
            info = JSON.parse(data);
        } catch (e) {
            statusLabel.setText("Error parsing response");
            console.error("Parse error: " + e);
            return;
        }
        let closes = parseCloses(info);
        if (closes === null) {
            statusLabel.setText("Unexpected response: no numeric closes found");
            console.error("Response had no closes array of numbers");
            return;
        }
        renderTrends(closes);
    };

    if (apiKey) {
        let headers = { "Authorization": "Bearer " + apiKey };
        Network.get(url, headers, callback);
    } else {
        Network.get(url, callback);
    }
}

console.log("Stock trends example loaded");
