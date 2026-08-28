// Example 14: Sub-Screens (Master/Detail)
// Demonstrates UI.pushPage()/UI.popPage(): compact master list rows with a
// chevron glyph open a detail page on tap; the "<" glyph in the detail page
// (or the device back button) pops it again.
// Permissions: None

UI.setTitle("Sub-Screens");

let textSize = 16;

let items = [
    { name: "Inbox", text: "Your inbox holds incoming messages that are waiting to be read." },
    { name: "Calendar", text: "The calendar shows your upcoming events and reminders." },
    { name: "Notes", text: "Notes keep your quick ideas and to-dos in one place." }
];

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

    // Top row: "<" back glyph on the left, detail title filling the row
    let topRow = new Layout("horizontal");
    topRow.setWidth(-1);
    topRow.setGravity("center_vertical");
    UI.addView(topRow);

    let backGlyph = new Label("<");
    backGlyph.setTextSize(textSize * 1.5);
    backGlyph.setBold(true);
    backGlyph.setPadding(16, 0, 16, 0);
    backGlyph.setOnTap(function() {
        let popped = UI.popPage();
        if (!popped) {
            showToast("Already at the root page");
        }
        updateDepth();
    });
    topRow.addView(backGlyph);

    let header = new Label(item.name);
    header.setTextSize(22);
    header.setBold(true);
    topRow.addView(header);
    header.setWeight(1);

    let body = new Label(item.text);
    body.setTextSize(15);
    body.setTextColor("#555555");
    UI.addView(body);
}

// One compact row per item: name (fills) + ">" glyph on the right
let listLayout = new Layout("vertical");
listLayout.setWidth(-1);
UI.addView(listLayout);

for (let i = 0; i < items.length; i++) {
    let item = items[i];

    let row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity("center_vertical");

    let nameLabel = new Label(item.name);
    nameLabel.setTextSize(textSize);
    nameLabel.setPadding(4, 12, 4, 12);
    nameLabel.setOnTap(function() {
        console.log("Opening detail: " + item.name);
        openDetail(item);
    });
    row.addView(nameLabel);
    nameLabel.setWeight(1);

    let chevron = new Label(">");
    chevron.setTextSize(textSize * 1.5);
    chevron.setBold(true);
    chevron.setPadding(16, 0, 16, 0);
    chevron.setOnTap(function() {
        console.log("Opening detail: " + item.name);
        openDetail(item);
    });
    row.addView(chevron);

    listLayout.addView(row);
}

console.log("Sub-screens demo ready, depth = " + UI.pageDepth());
