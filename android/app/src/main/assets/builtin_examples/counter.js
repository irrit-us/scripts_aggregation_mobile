// Example 2: Counter App
// A counter with increment and decrement buttons
// Permissions: None
UI.setTitle("Counter");


let count = 0;

// Title
// Counter display
let counterLabel = new Label("Count: 0");
counterLabel.setTextSize(32);
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
