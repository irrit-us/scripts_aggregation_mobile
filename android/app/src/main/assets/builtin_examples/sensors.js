// Example 5: Device Sensors
// Accelerometer and gyroscope readouts in compact axis rows, with inline
// glyph controls (">" start, "X" stop) and a vibrate test row.
// Permissions: ACCELEROMETER, GYROSCOPE

UI.setTitle("Sensors");

let baseSize = 16;
let glyphSize = baseSize * 1.5;

// Builds one compact axis row: bold axis name on the left, value filling
// the row, plus an optional right-aligned action glyph.
function makeAxisRow(axis, action, onTap) {
    let row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity("center_vertical");

    let name = new Label(axis);
    name.setTextSize(baseSize);
    name.setBold(true);
    name.setPadding(8, 0, 8, 0);
    row.addView(name);

    let value = new Label("0.00");
    value.setTextSize(baseSize);
    row.addView(value);
    value.setWeight(1);

    if (action) {
        let btn = new Label(action);
        btn.setTextSize(glyphSize);
        btn.setBold(true);
        btn.setPadding(16, 0, 16, 0);
        btn.setOnTap(onTap);
        row.addView(btn);
    }
    return { row: row, value: value };
}

// Accelerometer section: title row with inline ">" start and "X" stop glyphs
let accelTitleRow = new Layout("horizontal");
accelTitleRow.setWidth(-1);
accelTitleRow.setGravity("center_vertical");

let accelTitle = new Label("Accelerometer");
accelTitle.setTextSize(baseSize + 2);
accelTitle.setBold(true);
accelTitleRow.addView(accelTitle);
accelTitle.setWeight(1);

let accelX = makeAxisRow("X", null, null);
let accelY = makeAxisRow("Y", null, null);
let accelZ = makeAxisRow("Z", null, null);

let accelStart = new Label(">");
accelStart.setTextSize(glyphSize);
accelStart.setBold(true);
accelStart.setPadding(16, 0, 16, 0);
accelStart.setOnTap(function() {
    Sensor.getAccelerometer(function(data) {
        accelX.value.setText(data.x.toFixed(2));
        accelY.value.setText(data.y.toFixed(2));
        accelZ.value.setText(data.z.toFixed(2));
    });
    console.log("Accelerometer started");
    showToast("Sensor started");
});
accelTitleRow.addView(accelStart);

let accelStop = new Label("X");
accelStop.setTextSize(glyphSize);
accelStop.setBold(true);
accelStop.setPadding(16, 0, 16, 0);
accelStop.setOnTap(function() {
    Sensor.stop();
    console.log("Sensors stopped");
    showToast("Sensor stopped");
});
accelTitleRow.addView(accelStop);

UI.addView(accelTitleRow);
UI.addView(accelX.row);
UI.addView(accelY.row);
UI.addView(accelZ.row);

// Gyroscope section: same compact layout with its own start glyph
let gyroTitleRow = new Layout("horizontal");
gyroTitleRow.setWidth(-1);
gyroTitleRow.setGravity("center_vertical");

let gyroTitle = new Label("Gyroscope");
gyroTitle.setTextSize(baseSize + 2);
gyroTitle.setBold(true);
gyroTitleRow.addView(gyroTitle);
gyroTitle.setWeight(1);

let gyroX = makeAxisRow("X", null, null);
let gyroY = makeAxisRow("Y", null, null);
let gyroZ = makeAxisRow("Z", null, null);

let gyroStart = new Label(">");
gyroStart.setTextSize(glyphSize);
gyroStart.setBold(true);
gyroStart.setPadding(16, 0, 16, 0);
gyroStart.setOnTap(function() {
    Sensor.getGyroscope(function(data) {
        gyroX.value.setText(data.x.toFixed(2));
        gyroY.value.setText(data.y.toFixed(2));
        gyroZ.value.setText(data.z.toFixed(2));
    });
    console.log("Gyroscope started");
    showToast("Gyroscope started");
});
gyroTitleRow.addView(gyroStart);

UI.addView(gyroTitleRow);
UI.addView(gyroX.row);
UI.addView(gyroY.row);
UI.addView(gyroZ.row);

// Vibrate test row: description fills the row, "~" glyph triggers 200 ms buzz
let vibrateRow = new Layout("horizontal");
vibrateRow.setWidth(-1);
vibrateRow.setGravity("center_vertical");

let vibrateLabel = new Label("Vibrate (200 ms)");
vibrateLabel.setTextSize(baseSize);
vibrateRow.addView(vibrateLabel);
vibrateLabel.setWeight(1);

let vibrateBtn = new Label("~");
vibrateBtn.setTextSize(glyphSize);
vibrateBtn.setBold(true);
vibrateBtn.setPadding(16, 0, 16, 0);
vibrateBtn.setOnTap(function() {
    Device.vibrate(200);
    console.log("Vibrate triggered (200 ms)");
});
vibrateRow.addView(vibrateBtn);

UI.addView(vibrateRow);
