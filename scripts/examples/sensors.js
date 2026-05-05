// Example 5: Device Sensors
// Demonstrates accelerometer access

let title = new Label("Accelerometer Demo");
title.setTextSize(24);
UI.addView(title);

let xLabel = new Label("X: 0.00");
xLabel.setTextSize(18);
UI.addView(xLabel);

let yLabel = new Label("Y: 0.00");
yLabel.setTextSize(18);
UI.addView(yLabel);

let zLabel = new Label("Z: 0.00");
zLabel.setTextSize(18);
UI.addView(zLabel);

let startBtn = new Button("Start Sensor");
startBtn.setBackgroundColor("#4CAF50");
startBtn.setTextColor("#FFFFFF");
startBtn.setOnTap(function() {
    Sensor.getAccelerometer(function(data) {
        xLabel.setText("X: " + data.x.toFixed(2));
        yLabel.setText("Y: " + data.y.toFixed(2));
        zLabel.setText("Z: " + data.z.toFixed(2));
    });
    showToast("Sensor started");
});
UI.addView(startBtn);

let stopBtn = new Button("Stop Sensor");
stopBtn.setBackgroundColor("#F44336");
stopBtn.setTextColor("#FFFFFF");
stopBtn.setOnTap(function() {
    Sensor.stop();
    showToast("Sensor stopped");
});
UI.addView(stopBtn);

let vibrateBtn = new Button("Vibrate");
vibrateBtn.setBackgroundColor("#FF9800");
vibrateBtn.setTextColor("#FFFFFF");
vibrateBtn.setOnTap(function() {
    Device.vibrate(200);
});
UI.addView(vibrateBtn);
