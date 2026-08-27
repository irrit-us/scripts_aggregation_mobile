// Example 13: Remote tmux Console
// Connects to a host over SSH and runs tmux commands, showing the output
// in a scrollable console. The script declares its configurable fields via
// Config.schema(); after it has run once, they appear in Settings under
// the script's section.
// Permissions: SSH, CONFIG

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

let hint = new Label("Configure TMUX_HOST, TMUX_PORT, TMUX_USER and TMUX_PASSWORD in Settings.");
hint.setTextSize(12);
hint.setTextColor("#888888");
UI.addView(hint);

let statusLabel = new Label("Disconnected");
statusLabel.setTextSize(14);
UI.addView(statusLabel);

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

let disconnectBtn = new Button("Disconnect");
disconnectBtn.setBackgroundColor("#FF3B30");
disconnectBtn.setTextColor("#FFFFFF");
disconnectBtn.setOnTap(function() {
    try {
        SSH.disconnect();
    } catch (e) {
        console.error("SSH disconnect failed: " + e);
    }
    connected = false;
    statusLabel.setText("Disconnected");
    console.log("SSH disconnected");
});
UI.addView(disconnectBtn);

let listBtn = new Button("List sessions");
listBtn.setOnTap(function() {
    runCommand("tmux ls");
});
UI.addView(listBtn);

let captureBtn = new Button("Capture pane");
captureBtn.setOnTap(function() {
    runCommand("tmux capture-pane -p");
});
UI.addView(captureBtn);

let commandInput = new TextField("Custom command");
UI.addView(commandInput);

let runBtn = new Button("Run");
runBtn.setBackgroundColor("#34C759");
runBtn.setTextColor("#FFFFFF");
runBtn.setOnTap(function() {
    let command = commandInput.getValue();
    if (!command || command.trim() === "") {
        showToast("Please enter a command");
        return;
    }
    runCommand(command.trim());
});
UI.addView(runBtn);

let scrollContainer = new ScrollView();
scrollContainer.setFillViewport(true);
scrollContainer.setWidth(-1);
scrollContainer.setHeight(240);

let outputLabel = new Label("");
outputLabel.setTextSize(12);
outputLabel.setTextColor("#37474F");
scrollContainer.addView(outputLabel);
UI.addView(scrollContainer);

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
