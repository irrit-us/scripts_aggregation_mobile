// Example 5: Device Sensors
// Demonstrates accelerometer and gyroscope access
// Permissions: ACCELEROMETER, GYROSCOPE

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

let gyroTitle = new Label("Gyroscope");
gyroTitle.setTextSize(20);
UI.addView(gyroTitle);

let gyroLabel = new Label("X: 0.00, Y: 0.00, Z: 0.00");
gyroLabel.setTextSize(16);
UI.addView(gyroLabel);

let gyroBtn = new Button("Start Gyroscope");
gyroBtn.setBackgroundColor("#9C27B0");
gyroBtn.setTextColor("#FFFFFF");
gyroBtn.setOnTap(function() {
    Sensor.getGyroscope(function(data) {
        gyroLabel.setText("X: " + data.x.toFixed(2) +
            ", Y: " + data.y.toFixed(2) +
            ", Z: " + data.z.toFixed(2));
    });
    showToast("Gyroscope started");
});
UI.addView(gyroBtn);
