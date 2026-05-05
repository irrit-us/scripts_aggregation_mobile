// Example 4: Network Request
// Fetches data from an API and displays it

let title = new Label("GitHub User Info");
title.setTextSize(24);
UI.addView(title);

let usernameInput = new TextField("Enter GitHub username");
UI.addView(usernameInput);

let fetchBtn = new Button("Fetch User");
fetchBtn.setBackgroundColor("#24292e");
fetchBtn.setTextColor("#FFFFFF");
fetchBtn.setOnTap(function() {
    let username = usernameInput.getValue();
    if (username && username.trim() !== "") {
        fetchUserInfo(username.trim());
    } else {
        showToast("Please enter a username");
    }
});
UI.addView(fetchBtn);

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
