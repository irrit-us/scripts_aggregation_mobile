// Example 3: Todo List
// A simple todo list with add and remove functionality

let todos = [];

// Title
let title = new Label("Todo List");
title.setTextSize(24);
UI.addView(title);

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
        todos.push(task);
        updateList();
        input.setValue("");
        showToast("Task added!");
    }
});
UI.addView(addBtn);

// Todo list
let todoList = new ListView();
todoList.setOnItemTap(function(index) {
    showAlert("Task", todos[index]);
});
UI.addView(todoList);

// Clear button
let clearBtn = new Button("Clear All");
clearBtn.setBackgroundColor("#F44336");
clearBtn.setTextColor("#FFFFFF");
clearBtn.setOnTap(function() {
    todos = [];
    updateList();
    showToast("All tasks cleared");
});
UI.addView(clearBtn);

function updateList() {
    todoList.setItems(todos);
    console.log("Todo list updated. Total tasks: " + todos.length);
}
