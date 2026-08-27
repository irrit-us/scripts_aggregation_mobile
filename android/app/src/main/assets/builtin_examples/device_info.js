// Example 17: Device Info
// Demonstrates the read-only Device APIs: system version, architecture,
// time/timezone, device name, memory and storage usage
// Permissions: CONFIG

let title = new Label("Device Info");
title.setTextSize(24);
UI.addView(title);

let info = Device.getInfo();
let sys = Device.getSystemInfo();
let mem = Device.getMemoryInfo();
let store = Device.getStorageInfo();

let lines = [
    "Device: " + info.manufacturer + " " + info.model,
    "Name: " + Device.getDeviceName(),
    "Android: " + sys.androidVersion + " (SDK " + sys.sdkVersion + ")",
    "ABI: " + sys.abi + " | supported: " + sys.supportedAbis.join(", "),
    "Time: " + new Date(Device.getTime()).toLocaleString(),
    "Timezone: " + Device.getTimeZone(),
    "Memory: " + mem.availableMB + " MB free / " + mem.totalMB + " MB total" +
        (mem.lowMemory ? " (low!)" : ""),
    "Storage: " + store.usedMB + " MB used / " + store.totalMB + " MB total" +
        (" (" + store.freeMB + " MB free)")
];

for (let i = 0; i < lines.length; i++) {
    let row = new Label(lines[i]);
    row.setTextSize(15);
    UI.addView(row);
}

let keysLabel = new Label("Config keys: " + Config.keys().join(", "));
keysLabel.setTextSize(14);
UI.addView(keysLabel);

let refreshBtn = new Button("Refresh");
refreshBtn.setBackgroundColor("#007AFF");
refreshBtn.setTextColor("#FFFFFF");
refreshBtn.setOnTap(function() {
    let m = Device.getMemoryInfo();
    lines[6] = "Memory: " + m.availableMB + " MB free / " + m.totalMB + " MB total" +
        (m.lowMemory ? " (low!)" : "");
    showToast("Memory: " + m.availableMB + " MB free");
    console.log("Refreshed: " + m.availableMB + " MB free");
});
UI.addView(refreshBtn);
