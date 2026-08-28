// Example 2: Counter App
// A counter with increment and decrement controls: one compact row with
// "-" and "+" glyph buttons around the count, and a small reset button.
// Permissions: None

UI.setTitle("Counter");

let count = 0;

// Compact counter row: "-" glyph, count filling the row, "+" glyph
let counterRow = new Layout("horizontal");
counterRow.setWidth(-1);
counterRow.setGravity("center_vertical");
UI.addView(counterRow);

// Compact "-" glyph in the theme's default text color (dark: white,
// light: black) instead of a full button; ~1.5x the text height
let decrementBtn = new Label("-");
decrementBtn.setTextSize(24 * 1.5);
decrementBtn.setBold(true);
decrementBtn.setPadding(24, 0, 24, 0);
decrementBtn.setOnTap(function() {
    count--;
    countLabel.setText("" + count);
    console.log("Count decremented to: " + count);
});
counterRow.addView(decrementBtn);

// Count display filling the row between the two glyphs
let countLabel = new Label("0");
countLabel.setTextSize(24);
countLabel.setBold(true);
countLabel.setGravity("center");
counterRow.addView(countLabel);
countLabel.setWeight(1);

// Compact "+" glyph on the right
let incrementBtn = new Label("+");
incrementBtn.setTextSize(24 * 1.5);
incrementBtn.setBold(true);
incrementBtn.setPadding(24, 0, 24, 0);
incrementBtn.setOnTap(function() {
    count++;
    countLabel.setText("" + count);
    console.log("Count incremented to: " + count);
});
counterRow.addView(incrementBtn);

// Single primary action: a full-width reset button is clearer here
let resetBtn = new Button("Reset");
resetBtn.setOnTap(function() {
    count = 0;
    countLabel.setText("0");
    console.log("Counter reset");
});
UI.addView(resetBtn);
