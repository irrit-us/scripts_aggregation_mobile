// Example 3: Todo List
// A compact checklist: add row with an inline "+" button, per-item delete
// "X" buttons, gray strikethrough on completion, and a font size that is
// configurable in Settings (TODO_FONT_SIZE, applies on the next run).
// Permissions: CONFIG (only for the font-size setting)

UI.setTitle("Todo List");

Config.schema(JSON.stringify([
    { key: "TODO_FONT_SIZE", label: "Font size", type: "number", default: 16 }
]));

function fontSize() {
    let v = parseInt(Config.get("TODO_FONT_SIZE"));
    return (isNaN(v) || v < 10 || v > 40) ? 16 : v;
}

let todos = [];

// Compact add row: input field filling the row, "+" button on the right
let addRow = new Layout("horizontal");
addRow.setGravity("center_vertical");
UI.addView(addRow);

let input = new TextField("Enter a task...");
input.setTextSize(fontSize());
addRow.addView(input);
input.setWeight(1);

let addBtn = new Button("+");
addBtn.setTextSize(22);
addBtn.setBold(true);
addBtn.setBackgroundColor("#2196F3");
addBtn.setTextColor("#FFFFFF");
addBtn.setPadding(12, 0, 12, 0);
addBtn.setOnTap(function() {
    let task = input.getValue();
    if (task && task.trim() !== "") {
        addTodo(task.trim());
        input.setValue("");
    }
});
addRow.addView(addBtn);

// One row per task: checkbox (fills) + "X" delete button on the right
let listLayout = new Layout("vertical");
listLayout.setWidth(-1);
UI.addView(listLayout);

function addTodo(text) {
    let item = { text: text, done: false };

    let row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity("center_vertical");

    let check = new CheckBox(text);
    check.setTextSize(fontSize());
    check.setMargin(0, 0, 0, 0);
    check.setPadding(4, 0, 4, 0);
    check.setOnChange(function(checked) {
        item.done = checked;
        check.setStrikeThrough(checked);
        check.setTextColor(checked ? "#888888" : "");
        console.log((checked ? "Done: " : "Reopened: ") + item.text);
    });
    row.addView(check);
    check.setWeight(1);

    let deleteBtn = new Button("X");
    deleteBtn.setTextSize(14);
    deleteBtn.setTextColor("#FF3B30");
    deleteBtn.setBackgroundColor("#00000000");
    deleteBtn.setPadding(8, 0, 8, 0);
    deleteBtn.setOnTap(function() {
        listLayout.removeView(row);
        let at = todos.indexOf(item);
        if (at >= 0) todos.splice(at, 1);
        console.log("Deleted: " + item.text + " (" + todos.length + " left)");
    });
    row.addView(deleteBtn);

    item.row = row;
    todos.push(item);
    listLayout.addView(row);
    console.log("Todo added: " + text + " (" + todos.length + " total)");
}
