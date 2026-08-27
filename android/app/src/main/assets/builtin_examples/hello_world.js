// Example 1: Hello World Button
// A simple script that creates a button and shows an alert when clicked
// Permissions: None
UI.setTitle("Hello World");


let button = new Button("Click Me!");
button.setBackgroundColor("#007AFF");
button.setTextColor("#FFFFFF");

button.setOnTap(function() {
    showAlert("Hello", "Hello from ScriptHost!");
    console.log("Button was clicked");
});

UI.addView(button);
