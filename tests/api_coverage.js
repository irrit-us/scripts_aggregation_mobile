// API Coverage Script (tests/api_coverage.js)
// Exercises the full script API surface and prints machine-readable
// markers to the console:
//   APITEST|PASS|<name>
//   APITEST|FAIL|<name>|<reason>
//   APITEST|DONE
//
// How to run: import this file into SAM as a script, run it, and read the
// on-screen console (or logcat tag "ScriptConsole" when debug mode is on).
// Grant every permission when prompted so permission-gated checks can pass.
//
// The Network checks assume a local HTTP server at http://127.0.0.1:8080/
// (any server that answers GET and POST, e.g. "python -m http.server 8080"
// on the device via Termux, or adb reverse tcp:8080 tcp:8080 to a host).
// They report FAIL with the error when it is unreachable.

var LOCAL_URL = "http://127.0.0.1:8080/";

function pass(name) { console.log("APITEST|PASS|" + name); }
function fail(name, reason) { console.log("APITEST|FAIL|" + name + "|" + reason); }

function check(name, fn) {
    try {
        var result = fn();
        if (result === false) {
            fail(name, "returned false");
        } else {
            pass(name);
        }
    } catch (e) {
        fail(name, "" + e);
    }
}

// ---------------------------------------------------------------
// UI components (all 14 view types) + common + text methods
// ---------------------------------------------------------------

var titleLabel = new Label("API Coverage");
UI.addView(titleLabel);

check("Label", function() {
    titleLabel.setText("API Coverage");
    return true;
});

check("common view methods", function() {
    titleLabel.setVisible(true);
    titleLabel.setEnabled(true);
    titleLabel.setPadding(4, 4, 4, 4);
    titleLabel.setMargin(4, 4, 4, 4);
    titleLabel.setWidth(-2);
    titleLabel.setHeight(-2);
    titleLabel.setAlpha(0.9);
    titleLabel.setAlpha(1.0);
    titleLabel.setBackgroundColor("#EEEEEE");
    titleLabel.setCornerRadius(4);
    return titleLabel.getViewId() === titleLabel.viewId;
});

check("text methods", function() {
    titleLabel.setTextSize(24);
    titleLabel.setTextColor("#333333");
    titleLabel.setBold(true);
    titleLabel.setItalic(true);
    titleLabel.setTextAlign("center");
    titleLabel.setAllCaps(false);
    titleLabel.setStrikeThrough(true);
    titleLabel.setStrikeThrough(false);
    return true;
});

check("Button", function() {
    var button = new Button("Tap");
    button.setText("Tap me");
    button.setOnTap(function() {});
    UI.addView(button);
    return true;
});

check("TextField", function() {
    var field = new TextField("hint");
    field.setHint("type here");
    field.setHintTextColor("#999999");
    field.setInputType("text");
    field.setMaxLength(32);
    field.setOnChange(function(text) {});
    field.setValue("abc");
    UI.addView(field);
    return field.getValue() === "abc";
});

check("ListView", function() {
    var list = new ListView();
    list.setItems(["one", "two"]);
    list.setSelection(0);
    list.setOnItemTap(function(index) {});
    UI.addView(list);
    return true;
});

check("ImageView", function() {
    var image = new ImageView();
    image.setScaleType("fit_center");
    UI.addView(image);
    return true;
});

check("Switch", function() {
    var toggle = new Switch("toggle");
    toggle.setText("toggle");
    toggle.setChecked(true);
    toggle.setOnChange(function(checked) {});
    UI.addView(toggle);
    return toggle.getChecked() === true;
});

check("Slider", function() {
    var slider = new Slider();
    slider.setMin(0);
    slider.setMax(100);
    slider.setValue(50);
    slider.setOnChange(function(value) {});
    UI.addView(slider);
    return slider.getValue() === 50;
});

check("ScrollView", function() {
    var scroll = new ScrollView();
    var inner = new Label("content");
    scroll.addView(inner);
    scroll.setFillViewport(false);
    scroll.removeView(inner);
    scroll.addView(inner);
    UI.addView(scroll);
    return true;
});

check("CheckBox + setStrikeThrough", function() {
    var box = new CheckBox("box");
    box.setText("box");
    box.setChecked(true);
    box.setStrikeThrough(true);
    box.setStrikeThrough(false);
    box.setOnChange(function(checked) {});
    UI.addView(box);
    return box.getChecked() === true;
});

check("Spinner", function() {
    var spinner = new Spinner();
    spinner.setItems(["x", "y"]);
    spinner.setSelection(1);
    spinner.setOnChange(function(index, label) {});
    UI.addView(spinner);
    return spinner.getSelection() === 1;
});

check("ProgressBar", function() {
    var progress = new ProgressBar();
    progress.setMax(100);
    progress.setProgress(40);
    progress.setIndeterminate(false);
    UI.addView(progress);
    return progress.getProgress() === 40;
});

check("Layout", function() {
    var layout = new Layout("vertical");
    var child = new Label("child");
    layout.addView(child);
    layout.setOrientation("horizontal");
    layout.setGravity("center");
    layout.removeView(child);
    layout.addView(child);
    UI.addView(layout);
    return true;
});

check("Chart", function() {
    var chart = new Chart("line");
    chart.setLabels(["a", "b", "c"]);
    chart.setData([1, 2, 3]);
    chart.setColor("#34C759");
    UI.addView(chart);
    return true;
});

check("Markdown", function() {
    var md = new Markdown("# Title\n**bold**");
    md.setMarkdown("updated *text*");
    UI.addView(md);
    return true;
});

check("UI namespace", function() {
    var marker = new Label("page-two");
    UI.addView(marker);
    UI.removeView(marker.getViewId());
    if (UI.pageDepth() !== 1) return false;
    if (UI.pushPage() !== 2) return false;
    UI.addView(new Label("on page two"));
    if (UI.pageDepth() !== 2) return false;
    if (!UI.popPage()) return false;
    return UI.pageDepth() === 1;
});

// ---------------------------------------------------------------
// Storage
// ---------------------------------------------------------------

check("Storage writeFile/readFile", function() {
    if (!Storage.writeFile("api_coverage.txt", "coverage")) return false;
    return Storage.readFile("api_coverage.txt") === "coverage";
});

check("Storage listFiles", function() {
    var files = Storage.listFiles(".");
    return files !== null && files !== undefined;
});

check("Storage deleteFile", function() {
    return Storage.deleteFile("api_coverage.txt") === true;
});

// ---------------------------------------------------------------
// Device (read-only)
// ---------------------------------------------------------------

check("Device.getInfo", function() {
    var info = Device.getInfo();
    return info.model !== undefined && info.sdkVersion > 0;
});

check("Device.getTime", function() {
    return Device.getTime() > 0;
});

check("Device.getTimeZone", function() {
    return Device.getTimeZone().length > 0;
});

check("Device.getDeviceName", function() {
    return Device.getDeviceName().length > 0;
});

check("Device.getMemoryInfo", function() {
    var mem = Device.getMemoryInfo();
    return mem.totalMB > 0 && mem.availableMB >= 0;
});

check("Device.getStorageInfo", function() {
    var storage = Device.getStorageInfo();
    return storage.totalMB >= 0;
});

check("Device.getSystemInfo", function() {
    var sys = Device.getSystemInfo();
    return sys.sdkVersion > 0 && sys.supportedAbis.length > 0;
});

check("Device.vibrate", function() {
    Device.vibrate(30);
    return true;
});

// ---------------------------------------------------------------
// Config
// ---------------------------------------------------------------

check("Config.get (missing key)", function() {
    var value = Config.get("__api_coverage_missing__");
    return value === null || value === undefined;
});

check("Config.keys", function() {
    return Config.keys().length >= 0;
});

// ---------------------------------------------------------------
// Console, toast, notifications, scheduler
// ---------------------------------------------------------------

check("console.warn/console.error", function() {
    console.warn("coverage warn");
    console.error("coverage error");
    return true;
});

check("showToast", function() {
    showToast("coverage");
    showToast("coverage", "long");
    showToast("coverage", 1); // non-string duration must not throw
    return true;
});

check("Notify.post", function() {
    // Silently ignored without the NOTIFICATIONS permission; must not throw
    Notify.post("API Coverage", "immediate notification");
    return typeof Notify.post === "function";
});

check("Scheduler.scheduleIn/cancel", function() {
    if (typeof Scheduler.scheduleIn !== "function") return false;
    if (typeof Scheduler.scheduleAt !== "function") return false;
    if (typeof Scheduler.scheduleDaily !== "function") return false;
    if (!Scheduler.scheduleIn("api_coverage", 60000, "t", "m")) return false;
    return Scheduler.cancel("api_coverage") === true;
});

// ---------------------------------------------------------------
// Sensors (register + stop; absent sensors fail silently by design)
// ---------------------------------------------------------------

check("Sensor accelerometer/gyroscope", function() {
    Sensor.getAccelerometer(function(data) {});
    Sensor.getGyroscope(function(data) {});
    Sensor.stop();
    return true;
});

// ---------------------------------------------------------------
// Timers (async), then Network, then APITEST|DONE
// ---------------------------------------------------------------

var intervalFires = 0;
var coverageInterval = setInterval(function() {
    intervalFires = intervalFires + 1;
    if (intervalFires >= 2) {
        clearInterval(coverageInterval);
        pass("setInterval+clearInterval");
    }
}, 50);

check("clearTimeout", function() {
    var id = setTimeout(function() {
        fail("clearTimeout", "callback fired after clear");
    }, 100);
    clearTimeout(id);
    return true;
});

check("setTimeout registration", function() {
    var id = setTimeout(function() {
        pass("setTimeout fired");
        runNetworkChecks();
    }, 300);
    return id !== 0;
});

function runNetworkChecks() {
    Network.get(LOCAL_URL, function(data, error) {
        if (error) {
            fail("Network.get", error);
        } else {
            pass("Network.get");
        }
        Network.post(LOCAL_URL, "ping", function(postData, postError) {
            if (postError) {
                fail("Network.post", postError);
            } else {
                pass("Network.post");
            }
            Network.get(LOCAL_URL, { "X-Coverage": "1" }, function(headerData, headerError) {
                if (headerError) {
                    fail("Network.get+headers", headerError);
                } else {
                    pass("Network.get+headers");
                }
                console.log("APITEST|DONE");
            });
        });
    });
}
