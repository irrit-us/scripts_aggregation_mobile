// Example 14: Sub-Screens (Master/Detail)
// Demonstrates UI.pushPage()/UI.popPage(): tapping an item pushes a detail
// page; the Back button (or the device back button) pops it again.
// Permissions: None

let items = [
    { name: "Inbox", text: "Your inbox holds incoming messages that are waiting to be read." },
    { name: "Calendar", text: "The calendar shows your upcoming events and reminders." },
    { name: "Notes", text: "Notes keep your quick ideas and to-dos in one place." }
];

let title = new Label("Sub-Screens Demo");
title.setTextSize(24);
UI.addView(title);

let depthLabel = new Label("Page depth: " + UI.pageDepth());
depthLabel.setTextSize(12);
depthLabel.setTextColor("#888888");
UI.addView(depthLabel);

function updateDepth() {
    depthLabel.setText("Page depth: " + UI.pageDepth());
}

function openDetail(item) {
    UI.pushPage();
    updateDepth();

    let header = new Label(item.name);
    header.setTextSize(22);
    UI.addView(header);

    let body = new Label(item.text);
    body.setTextSize(15);
    body.setTextColor("#555555");
    UI.addView(body);

    let backBtn = new Button("Back");
    backBtn.setOnTap(function() {
        let popped = UI.popPage();
        if (!popped) {
            showToast("Already at the root page");
        }
        updateDepth();
    });
    UI.addView(backBtn);
}

for (let i = 0; i < items.length; i++) {
    let item = items[i];
    let itemBtn = new Button(item.name);
    itemBtn.setOnTap(function() {
        openDetail(item);
    });
    UI.addView(itemBtn);
}

console.log("Sub-screens demo ready, depth = " + UI.pageDepth());
