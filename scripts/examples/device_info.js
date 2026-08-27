// Example 17: Device Info
// Demonstrates the read-only Device APIs: system version, architecture,
// time/timezone, device name, memory and storage usage
// Permissions: CONFIG
UI.setTitle("Device Info");


let info = Device.getInfo();
let sys = Device.getSystemInfo();

let staticLines = [
    "Device: " + info.manufacturer + " " + info.model,
    "Name: " + Device.getDeviceName(),
    "Android: " + sys.androidVersion + " (SDK " + sys.sdkVersion + ")",
    "ABI: " + sys.abi + " | supported: " + sys.supportedAbis.join(", ")
];

for (let i = 0; i < staticLines.length; i++) {
    let row = new Label(staticLines[i]);
    row.setTextSize(15);
    UI.addView(row);
}

// Dynamic rows keep their Label references so Refresh can update them
function timeText() {
    return "Time: " + new Date(Device.getTime()).toLocaleString();
}
function memoryText() {
    let mem = Device.getMemoryInfo();
    return "Memory: " + mem.availableMB + " MB free / " + mem.totalMB + " MB total" +
        (mem.lowMemory ? " (low!)" : "");
}
function storageText() {
    let store = Device.getStorageInfo();
    return "Storage: " + store.usedMB + " MB used / " + store.totalMB + " MB total" +
        (" (" + store.freeMB + " MB free)");
}

let timeLabel = new Label(timeText());
timeLabel.setTextSize(15);
UI.addView(timeLabel);

let timezoneLabel = new Label("Timezone: " + Device.getTimeZone());
timezoneLabel.setTextSize(15);
UI.addView(timezoneLabel);

let memoryLabel = new Label(memoryText());
memoryLabel.setTextSize(15);
UI.addView(memoryLabel);

let storageLabel = new Label(storageText());
storageLabel.setTextSize(15);
UI.addView(storageLabel);

let keysLabel = new Label("Config keys: " + Config.keys().join(", "));
keysLabel.setTextSize(14);
UI.addView(keysLabel);

let refreshBtn = new Button("Refresh");
refreshBtn.setBackgroundColor("#007AFF");
refreshBtn.setTextColor("#FFFFFF");
refreshBtn.setOnTap(function() {
    timeLabel.setText(timeText());
    memoryLabel.setText(memoryText());
    storageLabel.setText(storageText());
    showToast("Refreshed");
    console.log("Device info refreshed");
});
UI.addView(refreshBtn);
