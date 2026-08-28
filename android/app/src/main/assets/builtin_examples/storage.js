// Example 6: Storage Demo
// Compact key/value rows with inline glyph actions: save "+", read "O",
// delete "X", plus a refreshable file list with per-row delete glyphs.
// Permissions: READ_STORAGE, WRITE_STORAGE

UI.setTitle("Storage");

var glyphSize = 24; // glyph labels roughly 1.5x the 16pt input text size

// Row 1: key input filling the row, glyph actions on the right
var keyRow = new Layout("horizontal");
keyRow.setGravity("center_vertical");
keyRow.setWidth(-1);
UI.addView(keyRow);

var keyInput = new TextField("Enter key");
keyInput.setTextSize(16);
keyRow.addView(keyInput);
keyInput.setWeight(1);

// Save glyph: writes the value to "<key>.txt"
var saveBtn = new Label("+");
saveBtn.setTextSize(glyphSize);
saveBtn.setBold(true);
saveBtn.setPadding(12, 0, 12, 0);
saveBtn.setOnTap(function() {
    var key = keyInput.getValue();
    var value = valueInput.getValue();

    if (key && value) {
        var success = Storage.writeFile(key + ".txt", value);
        if (success) {
            showToast("Saved successfully");
            console.log("Saved: " + key);
            refreshFiles();
        } else {
            showToast("Save failed");
        }
    }
});
keyRow.addView(saveBtn);

// Read glyph: loads "<key>.txt" back into the value input
var loadBtn = new Label("O");
loadBtn.setTextSize(glyphSize);
loadBtn.setBold(true);
loadBtn.setPadding(12, 0, 12, 0);
loadBtn.setOnTap(function() {
    var key = keyInput.getValue();

    if (key) {
        var value = Storage.readFile(key + ".txt");
        if (value) {
            valueInput.setValue(value);
            showToast("Loaded successfully");
            console.log("Loaded: " + key);
        } else {
            showToast("File not found");
        }
    }
});
keyRow.addView(loadBtn);

// Delete glyph: removes "<key>.txt"
var deleteBtn = new Label("X");
deleteBtn.setTextSize(glyphSize);
deleteBtn.setBold(true);
deleteBtn.setPadding(12, 0, 12, 0);
deleteBtn.setOnTap(function() {
    var key = keyInput.getValue();

    if (key) {
        var success = Storage.deleteFile(key + ".txt");
        if (success) {
            showToast("Deleted successfully");
            console.log("Deleted: " + key);
            refreshFiles();
        } else {
            showToast("Delete failed");
        }
    }
});
keyRow.addView(deleteBtn);

// Row 2: value input filling the row
var valueRow = new Layout("horizontal");
valueRow.setGravity("center_vertical");
valueRow.setWidth(-1);
UI.addView(valueRow);

var valueInput = new TextField("Enter value");
valueInput.setTextSize(16);
valueRow.addView(valueInput);
valueInput.setWeight(1);

// File list header: title filling the row, refresh glyph on the right
var listHeader = new Layout("horizontal");
listHeader.setGravity("center_vertical");
listHeader.setWidth(-1);
UI.addView(listHeader);

var listLabel = new Label("Files:");
listLabel.setTextSize(14);
listLabel.setBold(true);
listHeader.addView(listLabel);
listLabel.setWeight(1);

var refreshBtn = new Label("~");
refreshBtn.setTextSize(glyphSize);
refreshBtn.setBold(true);
refreshBtn.setPadding(12, 0, 12, 0);
refreshBtn.setOnTap(function() {
    refreshFiles();
    console.log("File list refreshed");
});
listHeader.addView(refreshBtn);

// One row per file: name fills the row, "X" delete glyph on the right
var listLayout = new Layout("vertical");
listLayout.setWidth(-1);
UI.addView(listLayout);

function refreshFiles() {
    listLayout.removeAllViews();

    var files = Storage.listFiles(".");
    if (!files || files.length === 0) {
        listLabel.setText("Files: (none)");
        return;
    }
    listLabel.setText("Files: " + files.length);

    for (var i = 0; i < files.length; i++) {
        addFileRow(files[i]);
    }
    console.log("Files: " + files.join(", "));
}

function addFileRow(name) {
    var row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity("center_vertical");

    // Tapping the name loads the file into the inputs
    var nameLabel = new Label(name);
    nameLabel.setTextSize(14);
    nameLabel.setOnTap(function() {
        keyInput.setValue(name.replace(/\.txt$/, ""));
        var value = Storage.readFile(name);
        if (value) {
            valueInput.setValue(value);
        }
        console.log("Loaded: " + name);
    });
    row.addView(nameLabel);
    nameLabel.setWeight(1);

    var rowDelete = new Label("X");
    rowDelete.setTextSize(18);
    rowDelete.setBold(true);
    rowDelete.setPadding(12, 0, 12, 0);
    rowDelete.setOnTap(function() {
        var success = Storage.deleteFile(name);
        if (success) {
            showToast("Deleted successfully");
            console.log("Deleted: " + name);
            refreshFiles();
        } else {
            showToast("Delete failed");
        }
    });
    row.addView(rowDelete);

    listLayout.addView(row);
}

refreshFiles();
