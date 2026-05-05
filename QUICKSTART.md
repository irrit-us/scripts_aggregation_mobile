# Quick Start Guide

Get ScriptHost running in 5 minutes.

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

1. Launch ScriptHost app
2. Tap "Add Script"
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

1. Find your script in the list
2. Tap on it
3. Select "Run"
4. Grant permissions if requested
5. Tap the button to see the alert!

## Try Examples

ScriptHost includes 6 example scripts:

1. **Hello World** - Basic button and alert
2. **Counter** - State management demo
3. **Todo List** - Full CRUD application
4. **Network Request** - API integration
5. **Sensors** - Accelerometer demo
6. **Storage** - File I/O operations

To try an example:

1. Copy code from `scripts/examples/`
2. Create new script in app
3. Paste code and save
4. Run and explore!

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

Happy scripting! 🚀
