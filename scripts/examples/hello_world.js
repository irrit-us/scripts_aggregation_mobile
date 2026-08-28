// Example 1: Hello World Button
// A simple script that creates a button and shows an alert when clicked.
// Restyled to the compact layout: a single primary-action Button centered
// in a full-width horizontal row instead of filling the whole screen.
// Permissions: None
UI.setTitle("Hello World");

// Compact centered row: gravity keeps the button vertically centered,
// width -1 makes the row fill the screen horizontally
let row = new Layout("horizontal");
row.setWidth(-1);
row.setGravity("center_vertical");
UI.addView(row);

// Single primary action, so a real Button is clearer than a glyph Label
let button = new Button("Click Me!");
button.setBackgroundColor("#007AFF");
button.setTextColor("#FFFFFF");
button.setOnTap(function() {
    showAlert("Hello", "Hello from ScriptHost!");
    console.log("Button was clicked");
});
row.addView(button);
button.setWeight(1);
