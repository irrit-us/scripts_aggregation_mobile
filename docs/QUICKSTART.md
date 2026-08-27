# Quick Start Guide

Get SAM (scripts_aggregation_mobile, formerly ScriptHost) running in 5 minutes.

## Prerequisites

- Android Studio (latest version)
- JDK 11+
- Android device or emulator (API 24+)

## Installation

### 1. Clone & Open

```bash
git clone https://github.com/scripthost/scripthost.git
cd scripthost
```

Open `android/` directory in Android Studio.

### 2. Sync & Build

Android Studio will automatically sync Gradle dependencies. Wait for completion.

### 3. Run

Click the Run button (▶) or press `Shift+F10`.

## First Script

### Create a Script

1. Launch the SAM app — the left drawer (script list) opens automatically
2. Tap the "+" icon below the script list in the drawer
3. Select "Create New Script"
4. Fill in details:
   - Name: "My First Script"
   - Version: "1.0.0"
   - Author: "Your Name"
   - Description: "My first script"
5. Enter code:

```javascript
let button = new Button("Hello World");
button.setBackgroundColor("#007AFF");
button.setTextColor("#FFFFFF");

button.setOnTap(function() {
    showAlert("Success", "Your first script works!");
});

UI.addView(button);
```

6. Tap "Save Script"

### Run the Script

1. Find your script in the drawer list (swipe in from the left edge or tap the ☰ icon in the slim top bar to reopen it)
2. Tap on it — it runs right in the content area
3. Grant permissions if requested
4. Tap the button to see the alert!

While a script runs, the top bar shows ✕ next to the script's name. To stop a
running script, tap "Stop Script" (or that ✕ at the top-left) and the
content area returns to the empty state.

## Try Examples

SAM includes 18 example scripts covering every exposed API:

1. **Hello World** - Basic button and alert
2. **Counter** - State management demo
3. **Todo List** - Full CRUD application
4. **Network Request** - API integration
5. **Sensors** - Accelerometer and gyroscope demo
6. **Storage** - File I/O operations
7. **Agent Conversation** - Wrapped agent chat via an OpenAI-compatible API
8. **Server Monitor** - Polls and displays server health status
9. **UI Controls** - Widget showcase
10. **Monitor Port Chart** - Live chart of port health
11. **Daily Fitness** - Daily recurring notification reminder
12. **Stock Trends** - Chart with fetched data
13. **Remote tmux Console** - SSH-based terminal
14. **Sub-Screens** - Multi-page navigation
15. **Markdown Demo** - Markdown rendering
16. **Scheduled Notifications** - One-shot and daily scheduled alerts
17. **Device Info** - Read-only system/device information
18. **Metronome** - Audio metronome demo

To try an example, tap "+" below the drawer script list and choose
"Built-in Examples" — pick one from the list and it installs instantly
(no external file needed). Alternatively use "Import from File" to install
any `.js` file from storage, then run it from the drawer list.

## Common Tasks

### Create a Button

```javascript
let button = new Button("Click Me");
button.setBackgroundColor("#4CAF50");
button.setOnTap(function() {
    console.log("Button clicked!");
});
UI.addView(button);
```

### Make Network Request

```javascript
Network.get("https://api.github.com/users/octocat", function(data, error) {
    if (error) {
        showToast("Error: " + error);
    } else {
        let user = JSON.parse(data);
        showAlert("User", user.name);
    }
});
```

### Save Data

```javascript
Storage.writeFile("mydata.txt", "Hello World");
let content = Storage.readFile("mydata.txt");
console.log(content); // "Hello World"
```

### Configure API Keys

Add keys in the app: open **Settings** (the gear icon below the drawer script list), tap **Add Key**, and
enter a name and value (for example `OPENAI_API_KEY`). Scripts read them via
the `Config` bridge:

```javascript
let apiKey = Config.get("OPENAI_API_KEY");
if (!apiKey) {
    showToast("No API key configured");
} else {
    Network.post("https://api.example.com/v1/chat",
        { "Authorization": "Bearer " + apiKey, "Content-Type": "application/json" },
        JSON.stringify({ prompt: "Hello" }),
        function(data, error) {
            showAlert("Response", error ? error : data);
        });
}
```

### Create a List

```javascript
let list = new ListView();
list.setItems(["Apple", "Banana", "Orange"]);
list.setOnItemTap(function(index) {
    showToast("Selected: " + index);
});
UI.addView(list);
```

## Next Steps

### Learn More

- **API Reference**: See `docs/API.md` for complete API documentation
- **Examples**: Explore `scripts/examples/` for more examples
- **Security**: Read `docs/SECURITY.md` for security best practices

### Build Your Own

1. Start with a simple UI
2. Add interactivity with events
3. Integrate network or storage
4. Test thoroughly
5. Share with others!

### Get Help

- **Documentation**: Check `docs/` directory
- **Issues**: Report bugs on GitHub
- **Community**: Join discussions

## Troubleshooting

### App Won't Build

```bash
cd android
./gradlew clean
./gradlew --refresh-dependencies
./gradlew assembleDebug
```

### Script Won't Run

- Check console output for errors
- Verify permissions are granted
- Ensure syntax is correct
- Try a simple example first

### Permission Denied

- Grant permissions when prompted
- Check script declares required permissions
- Restart app if permissions seem stuck

## Tips

1. **Start Simple**: Begin with basic UI, add complexity gradually
2. **Use Console**: `console.log()` is your friend for debugging
3. **Check Examples**: Learn from working examples
4. **Read Docs**: API documentation has all the details
5. **Test Often**: Run your script frequently during development

## Resources

- **API Docs**: `docs/API.md`
- **Examples**: `scripts/examples/`
- **Build Guide**: `BUILD.md`
- **Contributing**: `CONTRIBUTING.md`
- **Security**: `docs/SECURITY.md`

## Support

Need help? 

- Check documentation first
- Search existing issues
- Ask in discussions
- Contact maintainers

Happy scripting!
