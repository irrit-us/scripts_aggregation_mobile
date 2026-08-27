// Example 9: Configurable UI Controls
// Demonstrates the expanded native UI interface: layout containers, form
// controls, common styling configuration, and dialog helpers. All properties
// are applied statically; the interface intentionally has no animation support.
// Permissions: None

// Root background
UI.setBackgroundColor("#ECEFF1");

// ---------------------------------------------------------------
// Layout containers
// ---------------------------------------------------------------
let formLayout = new Layout("vertical");
formLayout.setPadding(8, 8, 8, 8);
formLayout.setBackgroundColor("#FFFFFF");
formLayout.setCornerRadius(8);
UI.addView(formLayout);

let buttonRow = new Layout("horizontal");
buttonRow.setGravity("center");
buttonRow.setPadding(4, 4, 4, 4);
formLayout.addView(buttonRow);

// ---------------------------------------------------------------
// Text configuration
// ---------------------------------------------------------------
let title = new Label("UI Controls Demo");
title.setTextSize(22);
title.setBold(true);
title.setTextColor("#263238");
title.setTextAlign("center");
title.setMargin(0, 4, 0, 12);
formLayout.addView(title);

let hintLabel = new Label("Typed text is echoed below.");
hintLabel.setTextSize(14);
hintLabel.setItalic(true);
hintLabel.setTextColor("#546E7A");
formLayout.addView(hintLabel);

// ---------------------------------------------------------------
// TextField input types and limits
// ---------------------------------------------------------------
let nameField = new TextField("Enter your name");
nameField.setHintTextColor("#90A4AE");
nameField.setInputType("text");
nameField.setMaxLength(40);
nameField.setWidth(-1);
formLayout.addView(nameField);

let numberField = new TextField("Enter a number");
numberField.setInputType("number");
numberField.setMaxLength(6);
numberField.setWidth(-1);
formLayout.addView(numberField);

let fieldEcho = new Label("Value: ");
fieldEcho.setTextSize(14);
formLayout.addView(fieldEcho);

nameField.setOnChange(function(value) {
    fieldEcho.setText("Value: " + value);
    console.log("TextField changed: " + value);
});

// ---------------------------------------------------------------
// Spinner
// ---------------------------------------------------------------
let themeLabel = new Label("Select a color:");
themeLabel.setTextSize(14);
formLayout.addView(themeLabel);

let themeSpinner = new Spinner();
themeSpinner.setItems(["Red", "Green", "Blue"]);
themeSpinner.setSelection(0);
themeSpinner.setWidth(-1);
formLayout.addView(themeSpinner);

themeSpinner.setOnChange(function(index, label) {
    console.log("Color selected: " + label + " (" + index + ")");
});

// ---------------------------------------------------------------
// CheckBox and Switch
// ---------------------------------------------------------------
let notifyCheck = new CheckBox("Option A");
notifyCheck.setChecked(true);
formLayout.addView(notifyCheck);

notifyCheck.setOnChange(function(checked) {
    console.log("Option A checked: " + checked);
});

let syncSwitch = new Switch("Option B");
syncSwitch.setChecked(false);
syncSwitch.setTextColor("#37474F");
formLayout.addView(syncSwitch);

syncSwitch.setOnChange(function(checked) {
    console.log("Option B switched on: " + checked);
});

// ---------------------------------------------------------------
// Slider and ProgressBar
// ---------------------------------------------------------------
let volumeLabel = new Label("Volume: 50");
volumeLabel.setTextSize(14);
formLayout.addView(volumeLabel);

let volumeSlider = new Slider();
volumeSlider.setMax(100);
volumeSlider.setValue(50);
volumeSlider.setWidth(-1);
formLayout.addView(volumeSlider);

let progressBar = new ProgressBar();
progressBar.setMax(100);
progressBar.setProgress(50);
progressBar.setWidth(-1);
formLayout.addView(progressBar);

volumeSlider.setOnChange(function(value) {
    volumeLabel.setText("Volume: " + value);
    progressBar.setProgress(value);
});

// ---------------------------------------------------------------
// ImageView with base64 data
// ---------------------------------------------------------------
let dotImage = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
let imageView = new ImageView();
imageView.setImageBase64(dotImage);
imageView.setScaleType("fit_center");
imageView.setWidth(64);
imageView.setHeight(64);
formLayout.addView(imageView);

// ---------------------------------------------------------------
// Common configuration methods
// ---------------------------------------------------------------
let hintHidden = false;
let toggleButton = new Button("Toggle hint");
toggleButton.setBackgroundColor("#2196F3");
toggleButton.setTextColor("#FFFFFF");
toggleButton.setCornerRadius(6);
buttonRow.addView(toggleButton);

toggleButton.setOnTap(function() {
    hintHidden = !hintHidden;
    hintLabel.setVisible(!hintHidden);
    console.log("Hint label visible: " + (!hintHidden));
});

let applyButton = new Button("Apply");
applyButton.setBackgroundColor("#4CAF50");
applyButton.setTextColor("#FFFFFF");
applyButton.setCornerRadius(6);
buttonRow.addView(applyButton);

applyButton.setOnTap(function() {
    let selected = themeSpinner.getSelection();
    let volume = volumeSlider.getValue();
    let notified = notifyCheck.getChecked();
    let synced = syncSwitch.getChecked();
    showConfirm("Submit", "Submit the current values?", function(confirmed) {
        if (confirmed) {
            console.log("Applied color " + selected + ", volume " + volume);
            showToast("Values submitted", "long");
        } else {
            console.log("Submission discarded");
        }
    });
});

let pickButton = new Button("Pick option");
pickButton.setBackgroundColor("#FF9800");
pickButton.setTextColor("#FFFFFF");
pickButton.setCornerRadius(6);
buttonRow.addView(pickButton);

pickButton.setOnTap(function() {
    showListPicker("Choose action", ["Save", "Export", "Reset"], function(index, label) {
        console.log("Picked: " + label + " (" + index + ")");
        showPrompt("Rename", "New label for " + label + ":", function(value, cancelled) {
            if (!cancelled && value.length > 0) {
                console.log("Renamed to: " + value);
                showToast("Renamed to " + value);
            }
        });
    });
});

// ---------------------------------------------------------------
// ScrollView with nested layout
// ---------------------------------------------------------------
let scrollContainer = new ScrollView();
scrollContainer.setFillViewport(true);
scrollContainer.setWidth(-1);
scrollContainer.setHeight(160);

let tallLayout = new Layout("vertical");
let infoLine = new Label("Scrollable content area");
infoLine.setTextSize(14);
tallLayout.addView(infoLine);

for (let row = 1; row <= 8; row++) {
    let rowLabel = new Label("Row " + row);
    rowLabel.setTextSize(13);
    rowLabel.setTextColor("#455A64");
    tallLayout.addView(rowLabel);
}

scrollContainer.addView(tallLayout);
UI.addView(scrollContainer);

console.log("Configurable UI controls example loaded");
