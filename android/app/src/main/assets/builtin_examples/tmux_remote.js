// Example 13: Remote tmux Console
// Connects to a host over SSH and runs tmux commands, showing the output
// in a scrollable console. The script declares its configurable fields via
// Config.schema(); after it has run once, they appear in Settings under
// the script's section.
// Permissions: SSH, CONFIG
UI.setTitle("tmux Console");

Config.schema(JSON.stringify([
    { key: "TMUX_HOST", label: "Host", type: "text" },
    { key: "TMUX_PORT", label: "Port", type: "number" },
    { key: "TMUX_USER", label: "Username", type: "text" },
    { key: "TMUX_PASSWORD", label: "Password", type: "password" }
]));

let host = Config.get("TMUX_HOST");
let port = Config.get("TMUX_PORT");
let user = Config.get("TMUX_USER");
let password = Config.get("TMUX_PASSWORD");
let connected = false;
let baseSize = 14;

let hint = new Label("Configure TMUX_HOST, TMUX_PORT, TMUX_USER and TMUX_PASSWORD in Settings.");
hint.setTextSize(12);
hint.setTextColor("#888888");
UI.addView(hint);

// Status row: status text fills the row, "X" disconnect glyph on the right
let statusRow = new Layout("horizontal");
statusRow.setGravity("center_vertical");
statusRow.setWidth(-1);
UI.addView(statusRow);

let statusLabel = new Label("Disconnected");
statusLabel.setTextSize(baseSize);
statusRow.addView(statusLabel);
statusLabel.setWeight(1);

let disconnectGlyph = new Label("X");
disconnectGlyph.setTextSize(baseSize * 1.5);
disconnectGlyph.setBold(true);
disconnectGlyph.setPadding(16, 0, 8, 0);
disconnectGlyph.setOnTap(function() {
    try {
        SSH.disconnect();
    } catch (e) {
        console.error("SSH disconnect failed: " + e);
    }
    connected = false;
    statusLabel.setText("Disconnected");
    console.log("SSH disconnected");
});
statusRow.addView(disconnectGlyph);

// Connect stays a single full-width primary-action Button
let connectBtn = new Button("Connect");
connectBtn.setBackgroundColor("#007AFF");
connectBtn.setTextColor("#FFFFFF");
connectBtn.setOnTap(function() {
    if (!host || !port || !user || !password) {
        statusLabel.setText("Missing config: set TMUX_HOST, TMUX_PORT, TMUX_USER and TMUX_PASSWORD in Settings.");
        console.error("Cannot connect: missing TMUX_* configuration keys");
        return;
    }
    statusLabel.setText("Connecting to " + host + ":" + port + "...");
    SSH.connect(host, Number(port), user, password, function(error) {
        if (error) {
            connected = false;
            statusLabel.setText("Connect failed: " + error);
            console.error("SSH connect failed: " + error);
            return;
        }
        connected = true;
        statusLabel.setText("Connected to " + host);
        console.log("SSH connected to " + host);
    });
});
UI.addView(connectBtn);

// Quick-action row: "ls" and "cap" glyph Labels run common tmux commands
let quickRow = new Layout("horizontal");
quickRow.setGravity("center_vertical");
quickRow.setWidth(-1);
UI.addView(quickRow);

let quickLabel = new Label("Quick:");
quickLabel.setTextSize(baseSize);
quickRow.addView(quickLabel);
quickLabel.setWeight(1);

let listGlyph = new Label("ls");
listGlyph.setTextSize(baseSize * 1.5);
listGlyph.setBold(true);
listGlyph.setPadding(16, 0, 16, 0);
listGlyph.setOnTap(function() {
    console.log("Quick action: list tmux sessions");
    runCommand("tmux ls");
});
quickRow.addView(listGlyph);

let captureGlyph = new Label("cap");
captureGlyph.setTextSize(baseSize * 1.5);
captureGlyph.setBold(true);
captureGlyph.setPadding(16, 0, 8, 0);
captureGlyph.setOnTap(function() {
    console.log("Quick action: capture tmux pane");
    runCommand("tmux capture-pane -p");
});
quickRow.addView(captureGlyph);

// Scrollable console output area
let scrollContainer = new ScrollView();
scrollContainer.setFillViewport(true);
scrollContainer.setWidth(-1);
scrollContainer.setHeight(240);

let outputLabel = new Label("");
outputLabel.setTextSize(12);
outputLabel.setTextColor("#37474F");
scrollContainer.addView(outputLabel);
UI.addView(scrollContainer);

// Compact command row below the output: input fills the row, ">" send glyph
let commandRow = new Layout("horizontal");
commandRow.setGravity("center_vertical");
commandRow.setWidth(-1);
UI.addView(commandRow);

let commandInput = new TextField("Custom command");
commandInput.setTextSize(baseSize);
commandRow.addView(commandInput);
commandInput.setWeight(1);

let sendGlyph = new Label(">");
sendGlyph.setTextSize(baseSize * 1.5);
sendGlyph.setBold(true);
sendGlyph.setPadding(16, 0, 8, 0);
sendGlyph.setOnTap(function() {
    let command = commandInput.getValue();
    if (!command || command.trim() === "") {
        showToast("Please enter a command");
        return;
    }
    console.log("Sending command: " + command.trim());
    runCommand(command.trim());
    commandInput.setValue("");
});
commandRow.addView(sendGlyph);

function appendOutput(text) {
    let previous = outputLabel.getText ? outputLabel.getText() : "";
    let block = "----------------------------------------\n" + text;
    outputLabel.setText(previous + block + "\n");
}

function runCommand(command) {
    if (!connected) {
        showToast("Not connected");
        return;
    }
    statusLabel.setText("Running: " + command);
    SSH.exec(command, function(output, error) {
        if (error) {
            statusLabel.setText("Command failed: " + error);
            console.error("SSH exec failed: " + error);
            return;
        }
        statusLabel.setText("Connected to " + host);
        appendOutput("$ " + command + "\n" + output);
        console.log("SSH exec done: " + command);
    });
}

if (!host || !port || !user || !password) {
    statusLabel.setText("Missing config: set TMUX_HOST, TMUX_PORT, TMUX_USER and TMUX_PASSWORD in Settings.");
}

console.log("Remote tmux console example loaded");
