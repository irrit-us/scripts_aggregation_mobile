// Example 7: Agent Conversation
// Chat with an OpenAI-compatible chat completions endpoint, shown as a
// scrollable message list (user messages tinted blue, agent messages gray,
// system/error messages red). After each exchange a status line shows the
// model, the HTTP outcome, and the elapsed time in milliseconds.
//
// The script declares its configurable fields via Config.schema(); after
// it has run once, they appear in Settings under the script's section:
//   OPENAI_API_KEY  - required API key (password field)
//   AGENT_API_URL   - optional base URL (default https://api.openai.com/v1)
//   AGENT_MODEL     - model select (default gpt-4o-mini)
// Permissions: INTERNET, CONFIG.
UI.setTitle("Agent Chat");

Config.schema(JSON.stringify([
    { key: "OPENAI_API_KEY", label: "API Key", type: "password" },
    { key: "AGENT_API_URL", label: "API Base URL", type: "text",
      default: "https://api.openai.com/v1" },
    { key: "AGENT_MODEL", label: "Model", type: "select",
      options: ["gpt-4o-mini", "gpt-4o"], default: "gpt-4o-mini" }
]));

let DEFAULT_BASE_URL = "https://api.openai.com/v1";
let DEFAULT_MODEL = "gpt-4o-mini";
let COLOR_USER = "#D1E7FF";
let COLOR_AGENT = "#F1F1F1";
let COLOR_SYSTEM = "#FFE0E0";
let history = [];
let chatRows = [];

// Static setup hint: only shown when no key is configured yet
if (!Config.get("OPENAI_API_KEY")) {
    let hint = new Label("Set OPENAI_API_KEY (and optionally AGENT_API_URL) in Settings.");
    hint.setTextSize(12);
    hint.setTextColor("#888888");
    UI.addView(hint);
}

// Scrollable chat area; messages are appended as bubble rows
let chatScroll = new ScrollView();
chatScroll.setHeight(320);
let chatLayout = new Layout("vertical");
chatLayout.setWidth(-1);
chatScroll.addView(chatLayout);
UI.addView(chatScroll);

// Status row: status text fills the row, "X" clear glyph on the right
let statusRow = new Layout("horizontal");
statusRow.setWidth(-1);
statusRow.setGravity("center_vertical");
UI.addView(statusRow);

let statusLabel = new Label("Ready");
statusLabel.setTextSize(12);
statusLabel.setTextColor("#888888");
statusRow.addView(statusLabel);
statusLabel.setWeight(1);

let clearBtn = new Label("X");
clearBtn.setTextSize(18);
clearBtn.setBold(true);
clearBtn.setPadding(16, 0, 8, 0);
clearBtn.setOnTap(function() {
    history = [];
    for (let idx = 0; idx < chatRows.length; idx++) {
        chatLayout.removeView(chatRows[idx]);
    }
    chatRows = [];
    statusLabel.setText("Cleared");
    console.log("Chat history cleared");
});
statusRow.addView(clearBtn);

// Compact input row: message field filling the row, ">" send glyph on the right
let inputRow = new Layout("horizontal");
inputRow.setWidth(-1);
inputRow.setGravity("center_vertical");
UI.addView(inputRow);

let messageInput = new TextField("Ask the agent something...");
messageInput.setTextSize(14);
inputRow.addView(messageInput);
messageInput.setWeight(1);

// Compact ">" send glyph tinted with the accent color, ~1.5x the text height
let sendBtn = new Label(">");
sendBtn.setTextSize(21);
sendBtn.setBold(true);
sendBtn.setTextColor("#10a37f");
sendBtn.setPadding(16, 0, 16, 0);
sendBtn.setOnTap(function() {
    let userMessage = messageInput.getValue();
    if (userMessage && userMessage.trim() !== "") {
        sendMessage(userMessage.trim());
    } else {
        showToast("Please enter a message");
    }
});
inputRow.addView(sendBtn);

function addMessage(text, bgColor, alignRight) {
    // Each message lives in a full-width row so the bubble hugs its side:
    // agent/system on the left, the user's own messages on the right.
    let row = new Layout("horizontal");
    row.setWidth(-1);
    row.setGravity(alignRight ? "right" : "left");

    // A small side glyph marks who is speaking: agent vs user
    let badge = new Label(alignRight ? "You" : "AI");
    badge.setTextSize(11);
    badge.setTextColor("#888888");
    badge.setPadding(4, 0, 4, 0);

    let bubble = new Label(text);
    bubble.setTextSize(14);
    bubble.setBackgroundColor(bgColor);
    bubble.setCornerRadius(8);
    bubble.setPadding(12, 8, 12, 8);
    bubble.setMargin(0, 4, 0, 4);
    bubble.setWeight(1);

    if (alignRight) {
        row.addView(bubble);
        row.addView(badge);
    } else {
        row.addView(badge);
        row.addView(bubble);
    }
    chatLayout.addView(row);
    chatRows.push(row);
}

function sendMessage(userMessage) {
    let apiKey = Config.get("OPENAI_API_KEY");
    if (!apiKey) {
        addMessage("OPENAI_API_KEY is not set. Add it in Settings, then try again.", COLOR_SYSTEM, false);
        statusLabel.setText("Missing OPENAI_API_KEY");
        console.error("Missing OPENAI_API_KEY configuration");
        return;
    }

    let baseUrl = Config.get("AGENT_API_URL") || DEFAULT_BASE_URL;
    let model = Config.get("AGENT_MODEL") || DEFAULT_MODEL;

    addMessage(userMessage, COLOR_USER, true);
    messageInput.setValue("");
    statusLabel.setText("Waiting for agent...");
    let startedAt = Date.now();
    history.push({ role: "user", content: userMessage });
    console.log("Message sent to " + model + ": " + userMessage);

    let headers = {
        "Authorization": "Bearer " + apiKey,
        "Content-Type": "application/json"
    };

    let body = JSON.stringify({
        model: model,
        messages: [{ role: "system", content: "You are a concise, helpful assistant." }].concat(history),
        max_tokens: 300
    });

    Network.post(baseUrl + "/chat/completions", headers, body, function(data, error) {
        let elapsed = Date.now() - startedAt;
        if (error) {
            addMessage("Network error: " + error, COLOR_SYSTEM, false);
            statusLabel.setText(model + " | " + error + " | " + elapsed + " ms");
            console.error("Agent request failed: " + error);
            return;
        }
        try {
            let result = JSON.parse(data);
            let reply = result.choices[0].message.content;
            history.push({ role: "assistant", content: reply });
            addMessage(reply, COLOR_AGENT, false);
            statusLabel.setText(model + " | HTTP 2xx | " + elapsed + " ms");
            console.log("Agent replied in " + elapsed + " ms");
        } catch (err) {
            addMessage("Could not parse the agent response.", COLOR_SYSTEM, false);
            statusLabel.setText(model + " | parse error | " + elapsed + " ms");
            console.error("Agent response parse error: " + err);
        }
    });
}
