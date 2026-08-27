// Example 3: Todo List
// A simple todo list with add, complete, and clear functionality.
// Each task is a CheckBox: checking it grays the text out and strikes
// it through; unchecking restores the default style.
// Permissions: None

let todos = [];

// Title
// Input field
let input = new TextField("Enter a task...");
UI.addView(input);

// Add button
let addBtn = new Button("Add Task");
addBtn.setBackgroundColor("#2196F3");
addBtn.setTextColor("#FFFFFF");
addBtn.setOnTap(function() {
    let task = input.getValue();
    if (task && task.trim() !== "") {
        addTodo(task.trim());
        input.setValue("");
        showToast("Task added!");
    }
});
UI.addView(addBtn);

// Todo list: one CheckBox row per task
let listLayout = new Layout("vertical");
listLayout.setWidth(-1);
UI.addView(listLayout);

function addTodo(text) {
    let item = { text: text, done: false };
    let check = new CheckBox(text);
    check.setOnChange(function(checked) {
        item.done = checked;
        check.setStrikeThrough(checked);
        check.setTextColor(checked ? "#888888" : "#212121");
        console.log((checked ? "Done: " : "Reopened: ") + item.text);
    });
    item.checkBox = check;
    listLayout.addView(check);
    todos.push(item);
    console.log("Todo list updated. Total tasks: " + todos.length);
}

// Clear button
let clearBtn = new Button("Clear All");
clearBtn.setBackgroundColor("#F44336");
clearBtn.setTextColor("#FFFFFF");
clearBtn.setOnTap(function() {
    for (let i = 0; i < todos.length; i++) {
        listLayout.removeView(todos[i].checkBox);
    }
    todos = [];
    showToast("All tasks cleared");
});
UI.addView(clearBtn);
