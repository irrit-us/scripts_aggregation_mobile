// Example 4: Network Request
// Fetches GitHub user info: a compact input row (URL/username field filling
// the row plus an inline ">" go glyph), with the result area shown below.
// Permissions: INTERNET

UI.setTitle("Network Request");

// Compact input row: field fills the row, ">" go glyph on the right
let inputRow = new Layout("horizontal");
inputRow.setGravity("center_vertical");
inputRow.setWidth(-1);
UI.addView(inputRow);

let usernameInput = new TextField("Enter GitHub username");
usernameInput.setTextSize(16);
inputRow.addView(usernameInput);
usernameInput.setWeight(1);

// Compact ">" glyph instead of a full-width button; ~1.5x the text height
let goBtn = new Label(">");
goBtn.setTextSize(16 * 1.5);
goBtn.setBold(true);
goBtn.setPadding(16, 0, 16, 0);
goBtn.setOnTap(function() {
    let username = usernameInput.getValue();
    if (username && username.trim() !== "") {
        console.log("Fetching user: " + username.trim());
        fetchUserInfo(username.trim());
    } else {
        showToast("Please enter a username");
    }
});
inputRow.addView(goBtn);

// Result area below the input row
let resultLabel = new Label("");
resultLabel.setTextSize(14);
UI.addView(resultLabel);

function fetchUserInfo(username) {
    resultLabel.setText("Loading...");

    let url = "https://api.github.com/users/" + username;

    Network.get(url, function(data, error) {
        if (error) {
            resultLabel.setText("Error: " + error);
            console.error("Network error: " + error);
        } else {
            try {
                let user = JSON.parse(data);
                let info = "Name: " + (user.name || "N/A") + "\n" +
                          "Bio: " + (user.bio || "N/A") + "\n" +
                          "Public Repos: " + user.public_repos + "\n" +
                          "Followers: " + user.followers;
                resultLabel.setText(info);
                console.log("User info fetched successfully");
            } catch (e) {
                resultLabel.setText("Error parsing response");
                console.error("Parse error: " + e);
            }
        }
    });
}
