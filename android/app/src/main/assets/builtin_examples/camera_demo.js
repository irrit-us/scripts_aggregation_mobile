// Example 19: Camera Demo
// Capture a photo with the Camera API: Camera.isAvailable checks hardware,
// Camera.takePhoto calls back with base64, error - base64 is a JPEG base64
// string with no prefix on success, error is a string on cancel or failure.
// The photo is shown in an ImageView below via imageView.setImageBase64.
// Permissions: CAMERA

UI.setTitle("Camera Demo");

// Status row: label filling the row, compact "CAM" capture glyph on the right
let statusRow = new Layout("horizontal");
statusRow.setGravity("center_vertical");
statusRow.setWidth(-1);
UI.addView(statusRow);

let statusLabel = new Label("Ready to capture");
statusLabel.setTextSize(16);
statusRow.addView(statusLabel);
statusLabel.setWeight(1);

// Compact capture glyph instead of a full-width button, ~1.5x the text size
let captureBtn = new Label("CAM");
captureBtn.setTextSize(24);
captureBtn.setBold(true);
captureBtn.setPadding(16, 0, 16, 0);
statusRow.addView(captureBtn);

// ImageView below: shows the captured photo, fixed height around 320
let imageView = new ImageView();
imageView.setScaleType("fit_center");
imageView.setWidth(-1);
imageView.setHeight(320);
UI.addView(imageView);

let busy = false;

if (!Camera.isAvailable()) {
    statusLabel.setText("No camera available on this device");
    captureBtn.setTextColor("#888888");
    console.log("Camera not available");
} else {
    captureBtn.setOnTap(function() {
        if (busy) {
            showToast("Capture already in progress");
            return;
        }
        busy = true;
        statusLabel.setText("Capturing...");
        console.log("Capture started");
        Camera.takePhoto(function(base64, error) {
            busy = false;
            if (error) {
                statusLabel.setText("Capture failed: " + error);
                showToast("Camera: " + error, "long");
                console.log("Capture error: " + error);
            } else {
                imageView.setImageBase64(base64);
                statusLabel.setText("Photo captured");
                showToast("Photo captured");
                console.log("Photo captured, base64 length: " + base64.length);
            }
        });
    });
}
