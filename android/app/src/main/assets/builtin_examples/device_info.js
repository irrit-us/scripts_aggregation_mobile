// Example 17: Device Info
// Demonstrates the read-only Device APIs: system version, architecture,
// time/timezone, device name, memory and storage usage.
// Presented as compact aligned rows: bold label on the left, gray value
// filling the rest of the row, plus an inline refresh glyph in the header.
// Permissions: CONFIG

UI.setTitle("Device Info");

var ROW_TEXT_SIZE = 15;

let info = Device.getInfo();
let sys = Device.getSystemInfo();

// Header row: title fills the row, inline refresh glyph sits on the right
let headerRow = new Layout("horizontal");
headerRow.setGravity("center_vertical");
headerRow.setWidth(-1);
UI.addView(headerRow);

let headerLabel = new Label("Device Info");
headerLabel.setTextSize(17);
headerLabel.setBold(true);
headerRow.addView(headerLabel);
headerLabel.setWeight(1);

// One aligned info row: label on the left, value weighted to fill the row
function addInfoRow(name, valueText) {
    let row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity("center_vertical");

    let nameLabel = new Label(name);
    nameLabel.setTextSize(ROW_TEXT_SIZE);
    nameLabel.setBold(true);
    nameLabel.setPadding(0, 0, 12, 0);
    row.addView(nameLabel);

    let valueLabel = new Label(valueText);
    valueLabel.setTextSize(ROW_TEXT_SIZE);
    valueLabel.setTextColor("#888888");
    row.addView(valueLabel);
    valueLabel.setWeight(1);

    UI.addView(row);
    return valueLabel;
}

// Static rows: values never change, so no references are kept
addInfoRow("Device", info.manufacturer + " " + info.model);
addInfoRow("Name", Device.getDeviceName());
addInfoRow("Android", sys.androidVersion + " (SDK " + sys.sdkVersion + ")");
addInfoRow("ABI", sys.abi + " | supported: " + sys.supportedAbis.join(", "));

// Dynamic rows keep their value Label references so refresh can update them
function timeText() {
    return new Date(Device.getTime()).toLocaleString();
}
function memoryText() {
    let mem = Device.getMemoryInfo();
    return mem.availableMB + " MB free / " + mem.totalMB + " MB total" +
        (mem.lowMemory ? " (low!)" : "");
}
function storageText() {
    let store = Device.getStorageInfo();
    return store.usedMB + " MB used / " + store.totalMB + " MB total" +
        " (" + store.freeMB + " MB free)";
}

let timeValue = addInfoRow("Time", timeText());
addInfoRow("Timezone", Device.getTimeZone());
let memoryValue = addInfoRow("Memory", memoryText());
let storageValue = addInfoRow("Storage", storageText());

let keysValue = addInfoRow("Config keys", Config.keys().join(", "));
keysValue.setTextSize(14);

// Compact refresh glyph in the theme's default text color instead of a
// full-width button; roughly 1.5x the row text height
let refreshBtn = new Label("↻");
refreshBtn.setTextSize(ROW_TEXT_SIZE * 1.5);
refreshBtn.setBold(true);
refreshBtn.setPadding(16, 0, 16, 0);
refreshBtn.setOnTap(function() {
    timeValue.setText(timeText());
    memoryValue.setText(memoryText());
    storageValue.setText(storageText());
    showToast("Refreshed");
    console.log("Device info refreshed");
});
headerRow.addView(refreshBtn);
