// Example 7: Agent Conversation
// Chat with an OpenAI-compatible chat completions endpoint, shown as a
// scrollable message list (user messages tinted blue, agent messages gray,
// system/error messages red). After each exchange a status line shows the
// model, the HTTP outcome, and the elapsed time in milliseconds.
//
// Configuration is read from Settings via Config:
//   OPENAI_API_KEY  - required; the chat shows a hint when it is missing
//   AGENT_API_URL   - optional base URL (default https://api.openai.com/v1)
// A dedicated Agent section is planned for Settings; until then add these
// keys by hand.
// Permissions: INTERNET, CONFIG.

let DEFAULT_BASE_URL = "https://api.openai.com/v1";
let MODEL = "gpt-4o-mini";
let COLOR_USER = "#D1E7FF";
let COLOR_AGENT = "#F1F1F1";
let COLOR_SYSTEM = "#FFE0E0";
let history = [];

let title = new Label("Agent Chat");
title.setTextSize(24);
UI.addView(title);

// Static setup hint: only shown when no key is configured yet
if (!Config.get("OPENAI_API_KEY")) {
    let hint = new Label("Set OPENAI_API_KEY (and optionally AGENT_API_URL) in Settings.");
    hint.setTextSize(12);
    hint.setTextColor("#888888");
    UI.addView(hint);
}

// Scrollable chat area; messages are appended as colored labels
let chatScroll = new ScrollView();
chatScroll.setHeight(320);
let chatLayout = new Layout("vertical");
chatScroll.addView(chatLayout);
UI.addView(chatScroll);

let statusLabel = new Label("Ready");
statusLabel.setTextSize(12);
statusLabel.setTextColor("#888888");
UI.addView(statusLabel);

let messageInput = new TextField("Ask the agent something...");
UI.addView(messageInput);

let sendBtn = new Button("Send");
sendBtn.setBackgroundColor("#10a37f");
sendBtn.setTextColor("#FFFFFF");
sendBtn.setOnTap(function() {
    let userMessage = messageInput.getValue();
    if (userMessage && userMessage.trim() !== "") {
        sendMessage(userMessage.trim());
    } else {
        showToast("Please enter a message");
    }
});
UI.addView(sendBtn);

function addMessage(text, bgColor) {
    let bubble = new Label(text);
    bubble.setTextSize(14);
    bubble.setBackgroundColor(bgColor);
    bubble.setCornerRadius(8);
    bubble.setPadding(12, 8, 12, 8);
    bubble.setMargin(0, 4, 0, 4);
    chatLayout.addView(bubble);
}

function sendMessage(userMessage) {
    let apiKey = Config.get("OPENAI_API_KEY");
    if (!apiKey) {
        addMessage("OPENAI_API_KEY is not set. Add it in Settings, then try again.", COLOR_SYSTEM);
        statusLabel.setText("Missing OPENAI_API_KEY");
        console.error("Missing OPENAI_API_KEY configuration");
        return;
    }

    let baseUrl = Config.get("AGENT_API_URL") || DEFAULT_BASE_URL;

    addMessage(userMessage, COLOR_USER);
    messageInput.setValue("");
    statusLabel.setText("Waiting for agent...");
    let startedAt = Date.now();
    history.push({ role: "user", content: userMessage });

    let headers = {
        "Authorization": "Bearer " + apiKey,
        "Content-Type": "application/json"
    };

    let body = JSON.stringify({
        model: MODEL,
        messages: [{ role: "system", content: "You are a concise, helpful assistant." }].concat(history),
        max_tokens: 300
    });

    Network.post(baseUrl + "/chat/completions", headers, body, function(data, error) {
        let elapsed = Date.now() - startedAt;
        if (error) {
            addMessage("Network error: " + error, COLOR_SYSTEM);
            statusLabel.setText(MODEL + " | " + error + " | " + elapsed + " ms");
            console.error("Agent request failed: " + error);
            return;
        }
        try {
            let result = JSON.parse(data);
            let reply = result.choices[0].message.content;
            history.push({ role: "assistant", content: reply });
            addMessage(reply, COLOR_AGENT);
            statusLabel.setText(MODEL + " | HTTP 2xx | " + elapsed + " ms");
            console.log("Agent replied in " + elapsed + " ms");
        } catch (e) {
            addMessage("Could not parse the agent response.", COLOR_SYSTEM);
            statusLabel.setText(MODEL + " | parse error | " + elapsed + " ms");
            console.error("Agent response parse error: " + e);
        }
    });
}
