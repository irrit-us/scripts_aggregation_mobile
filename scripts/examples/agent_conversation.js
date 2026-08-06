// Example 7: Wrapped Agent Conversation
// Sends a user message to an OpenAI-compatible chat completions endpoint.
// The API key and optional base URL are read from Settings via Config.get().

let title = new Label("Agent Chat");
title.setTextSize(24);
UI.addView(title);

let hint = new Label("Configure OPENAI_API_KEY (and optionally OPENAI_API_BASE) in Settings.");
hint.setTextSize(12);
hint.setTextColor("#888888");
UI.addView(hint);

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

let statusLabel = new Label("Ready");
statusLabel.setTextSize(13);
statusLabel.setTextColor("#888888");
UI.addView(statusLabel);

let responseLabel = new Label("Agent response will appear here.");
responseLabel.setTextSize(14);
UI.addView(responseLabel);

function sendMessage(userMessage) {
    let apiKey = Config.get("OPENAI_API_KEY");
    if (!apiKey) {
        statusLabel.setText("Missing OPENAI_API_KEY");
        responseLabel.setText("Add OPENAI_API_KEY in Settings, then try again.");
        console.error("Missing OPENAI_API_KEY configuration");
        return;
    }

    let baseUrl = Config.get("OPENAI_API_BASE");
    if (!baseUrl) {
        baseUrl = "https://api.openai.com/v1/chat/completions";
    }

    statusLabel.setText("Waiting for agent...");

    let headers = {
        "Authorization": "Bearer " + apiKey,
        "Content-Type": "application/json"
    };

    let body = JSON.stringify({
        model: "gpt-4o-mini",
        messages: [
            { role: "system", content: "You are a concise, helpful assistant." },
            { role: "user", content: userMessage }
        ],
        max_tokens: 300
    });

    Network.post(baseUrl, headers, body, function(data, error) {
        if (error) {
            statusLabel.setText("Request failed");
            responseLabel.setText("Error: " + error);
            console.error("Agent request failed: " + error);
            return;
        }
        try {
            let result = JSON.parse(data);
            let reply = result.choices[0].message.content;
            responseLabel.setText(reply);
            statusLabel.setText("Done");
            console.log("Agent replied: " + reply.substring(0, 80));
        } catch (e) {
            statusLabel.setText("Parse error");
            responseLabel.setText("Could not parse the agent response.");
            console.error("Agent response parse error: " + e);
        }
    });
}
