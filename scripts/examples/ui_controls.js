// Example 9: Configurable UI Controls
// Kitchen-sink demo of the native UI widgets, restyled as compact labeled
// rows: text fields and spinners fill their row, actions are small inline
// glyph labels, and only the primary "Apply" action stays a full button.
// Permissions: None
UI.setTitle("UI Controls");

let TEXT = 14;

// Root background
UI.setBackgroundColor("#ECEFF1");

// Main card container
let formLayout = new Layout("vertical");
formLayout.setPadding(8, 8, 8, 8);
formLayout.setBackgroundColor("#FFFFFF");
formLayout.setCornerRadius(8);
UI.addView(formLayout);

// Helper: compact horizontal row, vertically centered, full width
function makeRow(parent) {
    let row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity("center_vertical");
    parent.addView(row);
    return row;
}

// Hint line: small italic note, toggleable via the "?" glyph below
let hintLabel = new Label("Typed text is echoed below.");
hintLabel.setTextSize(TEXT);
hintLabel.setItalic(true);
hintLabel.setTextColor("#546E7A");
formLayout.addView(hintLabel);

// Name row: input field fills the row, inline echo label on the right
let nameRow = makeRow(formLayout);

let nameField = new TextField("Enter your name");
nameField.setHintTextColor("#90A4AE");
nameField.setInputType("text");
nameField.setMaxLength(40);
nameField.setTextSize(TEXT);
nameRow.addView(nameField);
nameField.setWeight(1);

let fieldEcho = new Label("");
fieldEcho.setTextSize(TEXT);
fieldEcho.setTextColor("#546E7A");
fieldEcho.setPadding(8, 0, 0, 0);
nameRow.addView(fieldEcho);

nameField.setOnChange(function(value) {
    fieldEcho.setText(value);
    console.log("TextField changed: " + value);
});

// Number row: limited numeric input filling the row
let numberRow = makeRow(formLayout);

let numberField = new TextField("Enter a number");
numberField.setInputType("number");
numberField.setMaxLength(6);
numberField.setTextSize(TEXT);
numberRow.addView(numberField);
numberField.setWeight(1);

// Spinner row: leading label, spinner fills the rest of the row
let spinnerRow = makeRow(formLayout);

let themeLabel = new Label("Color:");
themeLabel.setTextSize(TEXT);
themeLabel.setPadding(0, 0, 8, 0);
spinnerRow.addView(themeLabel);

let themeSpinner = new Spinner();
themeSpinner.setItems(["Red", "Green", "Blue"]);
themeSpinner.setSelection(0);
spinnerRow.addView(themeSpinner);
themeSpinner.setWeight(1);

themeSpinner.setOnChange(function(index, label) {
    console.log("Color selected: " + label + " (" + index + ")");
});

// Toggles row: CheckBox and Switch side by side in one compact row
let toggleRow = makeRow(formLayout);

let notifyCheck = new CheckBox("Option A");
notifyCheck.setChecked(true);
notifyCheck.setTextSize(TEXT);
toggleRow.addView(notifyCheck);
notifyCheck.setWeight(1);

notifyCheck.setOnChange(function(checked) {
    console.log("Option A checked: " + checked);
});

let syncSwitch = new Switch("Option B");
syncSwitch.setChecked(false);
syncSwitch.setTextColor("#37474F");
syncSwitch.setTextSize(TEXT);
toggleRow.addView(syncSwitch);

syncSwitch.setOnChange(function(checked) {
    console.log("Option B switched on: " + checked);
});

// Volume row: value label, slider fills the row
let volumeRow = makeRow(formLayout);

let volumeLabel = new Label("Volume: 50");
volumeLabel.setTextSize(TEXT);
volumeLabel.setPadding(0, 0, 8, 0);
volumeRow.addView(volumeLabel);

let volumeSlider = new Slider();
volumeSlider.setMax(100);
volumeSlider.setValue(50);
volumeRow.addView(volumeSlider);
volumeSlider.setWeight(1);

// ProgressBar mirrors the slider, full width beneath the row
let progressBar = new ProgressBar();
progressBar.setMax(100);
progressBar.setProgress(50);
progressBar.setWidth(-1);
formLayout.addView(progressBar);

volumeSlider.setOnChange(function(value) {
    volumeLabel.setText("Volume: " + value);
    progressBar.setProgress(value);
});

// Image row: small base64 image plus a weighted caption
let imageRow = makeRow(formLayout);

let dotImage = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
let imageView = new ImageView();
imageView.setImageBase64(dotImage);
imageView.setScaleType("fit_center");
imageView.setWidth(48);
imageView.setHeight(48);
imageRow.addView(imageView);

let imageCaption = new Label("ImageView from base64 data");
imageCaption.setTextSize(TEXT);
imageCaption.setTextColor("#455A64");
imageCaption.setPadding(12, 0, 0, 0);
imageRow.addView(imageCaption);
imageCaption.setWeight(1);

// Actions row: glyph labels for secondary actions, primary "Apply" button
let hintHidden = false;
let actionsRow = makeRow(formLayout);

let hintToggle = new Label("?");
hintToggle.setTextSize(TEXT * 1.5);
hintToggle.setBold(true);
hintToggle.setPadding(16, 0, 16, 0);
hintToggle.setOnTap(function() {
    hintHidden = !hintHidden;
    hintLabel.setVisible(!hintHidden);
    console.log("Hint label visible: " + (!hintHidden));
});
actionsRow.addView(hintToggle);

let pickAction = new Label("...");
pickAction.setTextSize(TEXT * 1.5);
pickAction.setBold(true);
pickAction.setPadding(16, 0, 16, 0);
pickAction.setOnTap(function() {
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
actionsRow.addView(pickAction);

let applyButton = new Button("Apply");
applyButton.setBackgroundColor("#4CAF50");
applyButton.setTextColor("#FFFFFF");
applyButton.setCornerRadius(6);
actionsRow.addView(applyButton);
applyButton.setWeight(1);

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

// ScrollView: list rows with weighted content and a ">" glyph on the right
let scrollContainer = new ScrollView();
scrollContainer.setFillViewport(true);
scrollContainer.setWidth(-1);
scrollContainer.setHeight(160);

let tallLayout = new Layout("vertical");
let infoLine = new Label("Scrollable content area");
infoLine.setTextSize(TEXT);
infoLine.setItalic(true);
infoLine.setTextColor("#546E7A");
tallLayout.addView(infoLine);

for (let rowNum = 1; rowNum <= 8; rowNum++) {
    let listRow = new Layout("horizontal");
    listRow.setWidth(-1);
    listRow.setGravity("center_vertical");

    let rowLabel = new Label("Row " + rowNum);
    rowLabel.setTextSize(13);
    rowLabel.setTextColor("#455A64");
    rowLabel.setPadding(4, 0, 4, 0);
    listRow.addView(rowLabel);
    rowLabel.setWeight(1);

    let rowArrow = new Label(">");
    rowArrow.setTextSize(13 * 1.5);
    rowArrow.setBold(true);
    rowArrow.setPadding(12, 0, 12, 0);
    rowArrow.setOnTap(function() {
        console.log("Scroll row tapped: " + rowNum);
        showToast("Row " + rowNum + " tapped");
    });
    listRow.addView(rowArrow);

    tallLayout.addView(listRow);
}

scrollContainer.addView(tallLayout);
UI.addView(scrollContainer);

console.log("Configurable UI controls example loaded");
