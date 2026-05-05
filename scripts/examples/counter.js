// Example 2: Counter App
// A counter with increment and decrement buttons

let count = 0;

// Title
let title = new Label("Counter App");
title.setTextSize(24);
UI.addView(title);

// Counter display
let counterLabel = new Label("Count: 0");
counterLabel.setTextSize(32);
counterLabel.setTextColor("#000000");
UI.addView(counterLabel);

// Increment button
let incrementBtn = new Button("Increment");
incrementBtn.setBackgroundColor("#4CAF50");
incrementBtn.setTextColor("#FFFFFF");
incrementBtn.setOnTap(function() {
    count++;
    counterLabel.setText("Count: " + count);
    console.log("Count incremented to: " + count);
});
UI.addView(incrementBtn);

// Decrement button
let decrementBtn = new Button("Decrement");
decrementBtn.setBackgroundColor("#F44336");
decrementBtn.setTextColor("#FFFFFF");
decrementBtn.setOnTap(function() {
    count--;
    counterLabel.setText("Count: " + count);
    console.log("Count decremented to: " + count);
});
UI.addView(decrementBtn);

// Reset button
let resetBtn = new Button("Reset");
resetBtn.setBackgroundColor("#9E9E9E");
resetBtn.setTextColor("#FFFFFF");
resetBtn.setOnTap(function() {
    count = 0;
    counterLabel.setText("Count: 0");
    console.log("Counter reset");
});
UI.addView(resetBtn);
