// Example 6: Storage Demo
// Demonstrates file storage capabilities
// Permissions: READ_STORAGE, WRITE_STORAGE

let title = new Label("Storage Demo");
title.setTextSize(24);
UI.addView(title);

let keyInput = new TextField("Enter key");
UI.addView(keyInput);

let valueInput = new TextField("Enter value");
UI.addView(valueInput);

let saveBtn = new Button("Save");
saveBtn.setBackgroundColor("#4CAF50");
saveBtn.setTextColor("#FFFFFF");
saveBtn.setOnTap(function() {
    let key = keyInput.getValue();
    let value = valueInput.getValue();

    if (key && value) {
        let success = Storage.writeFile(key + ".txt", value);
        if (success) {
            showToast("Saved successfully");
            console.log("Saved: " + key);
        } else {
            showToast("Save failed");
        }
    }
});
UI.addView(saveBtn);

let loadBtn = new Button("Load");
loadBtn.setBackgroundColor("#2196F3");
loadBtn.setTextColor("#FFFFFF");
loadBtn.setOnTap(function() {
    let key = keyInput.getValue();

    if (key) {
        let value = Storage.readFile(key + ".txt");
        if (value) {
            valueInput.setValue(value);
            showToast("Loaded successfully");
            console.log("Loaded: " + key);
        } else {
            showToast("File not found");
        }
    }
});
UI.addView(loadBtn);

let deleteBtn = new Button("Delete");
deleteBtn.setBackgroundColor("#F44336");
deleteBtn.setTextColor("#FFFFFF");
deleteBtn.setOnTap(function() {
    let key = keyInput.getValue();

    if (key) {
        let success = Storage.deleteFile(key + ".txt");
        if (success) {
            showToast("Deleted successfully");
            console.log("Deleted: " + key);
        } else {
            showToast("Delete failed");
        }
    }
});
UI.addView(deleteBtn);

let listLabel = new Label("Files: (none)");
listLabel.setTextSize(14);
UI.addView(listLabel);

let listBtn = new Button("List Files");
listBtn.setBackgroundColor("#607D8B");
listBtn.setTextColor("#FFFFFF");
listBtn.setOnTap(function() {
    let files = Storage.listFiles(".");
    if (files && files.length > 0) {
        listLabel.setText("Files: " + files.join(", "));
        console.log("Files: " + files.join(", "));
    } else {
        listLabel.setText("Files: (none)");
    }
});
UI.addView(listBtn);
