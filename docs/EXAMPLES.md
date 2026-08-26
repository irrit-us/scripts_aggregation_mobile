# Script Examples

This directory contains example scripts demonstrating ScriptHost capabilities.

## Available Examples

### 1. Hello World (`hello_world.js`)

**Description**: Simple button that shows an alert when clicked

**Concepts Demonstrated**:
- Creating a button
- Setting button properties (color, text)
- Handling click events
- Showing alerts

**Permissions Required**: None

**Usage**:
```bash
Import hello_world.js into ScriptHost and run it
```

---

### 2. Counter App (`counter.js`)

**Description**: A counter with increment, decrement, and reset buttons

**Concepts Demonstrated**:
- Multiple UI components
- State management
- Button event handling
- Dynamic text updates

**Permissions Required**: None

**Usage**:
```bash
Import counter.js into ScriptHost and run it
```

---

### 3. Todo List (`todo_list.js`)

**Description**: A functional todo list application

**Concepts Demonstrated**:
- Text input
- ListView component
- Array manipulation
- Adding/removing items
- Toast notifications

**Permissions Required**: None

**Usage**:
```bash
Import todo_list.js into ScriptHost and run it
```

---

### 4. Network Request (`network_request.js`)

**Description**: Fetches GitHub user information from API

**Concepts Demonstrated**:
- HTTP GET requests
- Async callbacks
- JSON parsing
- Error handling
- Network API usage

**Permissions Required**: `INTERNET`

**Usage**:
```bash
Import network_request.js into ScriptHost
Grant INTERNET permission when prompted
Enter a GitHub username and tap "Fetch User"
```

---

### 5. Device Sensors (`sensors.js`)

**Description**: Demonstrates accelerometer and vibration

**Concepts Demonstrated**:
- Sensor API usage
- Real-time data updates
- Starting/stopping sensors
- Device vibration

**Permissions Required**: `ACCELEROMETER`, `VIBRATE`

**Usage**:
```bash
Import sensors.js into ScriptHost
Grant sensor permissions when prompted
Tap "Start Sensor" to see accelerometer data
Move your device to see values change
```

---

### 6. Storage Demo (`storage.js`)

**Description**: File storage operations (save, load, delete)

**Concepts Demonstrated**:
- File I/O operations
- Storage API usage
- Key-value storage pattern
- Error handling

**Permissions Required**: `READ_STORAGE`, `WRITE_STORAGE`

**Usage**:
```bash
Import storage.js into ScriptHost
Enter a key and value
Tap "Save" to store data
Tap "Load" to retrieve data
Tap "Delete" to remove data
```

---

### 7. Agent Conversation (`agent_conversation.js`)

**Description**: Wrapped agent chat against an OpenAI-compatible chat completions
endpoint. Reads the API key from the configuration interface and sends custom
authenticated API calls.

**Concepts Demonstrated**:
- Configuration interface (`Config.get`)
- Custom API calls with `Authorization` headers (`Network.post` with headers)
- JSON request/response handling
- Async callbacks and error handling
- Loading states and UI updates

**Permissions Required**: `CONFIG`, `INTERNET`

**Usage**:
```bash
1. Open Settings and add OPENAI_API_KEY (optionally OPENAI_API_BASE)
2. Import agent_conversation.js into ScriptHost
3. Enter a message and tap "Send"
4. The agent's reply is rendered in the response label
```

---

### 8. Server Monitor (`server_monitor.js`)

**Description**: Polls a server health endpoint and shows live UP/DOWN status.
Uses `setInterval` for polling and optional bearer-token authentication.

**Concepts Demonstrated**:
- Configuration interface (`Config.get`)
- Polling with `setInterval` / `clearInterval`
- Custom API calls with optional auth headers (`Network.get` with headers)
- Request de-duplication (skips overlapping polls)
- Status rendering and error handling

**Permissions Required**: `CONFIG`, `INTERNET`

**Usage**:
```bash
1. Open Settings and add MONITOR_URL (optionally MONITOR_API_KEY)
2. Import server_monitor.js into ScriptHost
3. Tap "Start Monitoring" to begin polling every 5 seconds
4. Tap "Stop Monitoring" to pause
```

---

### 9. Configurable UI Controls (`ui_controls.js`)

**Description**: Showcases the expanded native UI interface: layout
containers, form controls, common styling configuration, and dialog helpers.
All properties are applied statically; the interface intentionally has no
animation support.

**Concepts Demonstrated**:
- `Layout` containers (vertical and horizontal) with gravity and nesting
- `Spinner`, `CheckBox`, `Switch`, `Slider`, and `ProgressBar` form controls
- `TextField` input types, hints, and length limits
- Common configuration methods (padding, margin, corner radius, visibility)
- `ImageView` with base64 image data and scale types
- `ScrollView` with a nested layout
- Dialog helpers (`showConfirm`, `showPrompt`, `showListPicker`) and two-arg
  `showToast(message, "long")`
- Root container background via `UI.setBackgroundColor`

**Permissions Required**: None

**Usage**:
```bash
Import ui_controls.js into ScriptHost and run it
```

---

### 10. Monitor Port Chart (`monitor_port_chart.js`)

**Description**: Polls a monitor endpoint every 5 seconds and renders a
rolling line chart of the last 20 numeric samples. The line is green while
the latest sample is below the configured threshold and red otherwise (and
immediately red when a check fails).

**Concepts Demonstrated**:
- `Chart` widget (`new Chart("line")`, `setData`, `setLabels`, `setColor`)
- Configuration interface (`Config.get`)
- Polling with `setInterval` / `clearInterval`
- Custom API calls with optional auth headers (`Network.get` with headers)
- Threshold-based color changes and error handling

**Permissions Required**: `CONFIG`, `INTERNET`

**Usage**:
```bash
1. Open Settings and add MONITOR_URL (optionally MONITOR_API_KEY and
   MONITOR_THRESHOLD, default 500)
2. Import monitor_port_chart.js into ScriptHost
3. Tap "Start Monitoring" to begin polling every 5 seconds
4. Tap "Stop Monitoring" to pause
```

---

### 11. Daily Fitness Reminder (`daily_fitness.js`)

**Description**: Shows a fitness tip for today and schedules a daily 8:00
notification with that tip. A preview button posts the notification
immediately.

**Concepts Demonstrated**:
- `Scheduler.scheduleDaily` / `Scheduler.cancel` for WorkManager-backed
  daily notifications
- `Notify.post` for immediate notifications
- `NOTIFICATIONS` permission (runtime dialog on Android 13+)
- Error handling with try-catch around bridge calls

**Permissions Required**: `NOTIFICATIONS`

**Usage**:
```bash
1. Import daily_fitness.js into ScriptHost
2. Grant the notification permission when prompted
3. Tap "Enable daily 8:00 reminder" to schedule
4. Tap "Preview notification" to post the tip immediately
5. Tap "Disable reminder" to cancel the schedule
```

---

### 12. Stock Trends (`stock_trends.js`)

**Description**: Loads daily closing prices for a stock symbol and renders
them as a line chart - green when the window ends up, red when it ends down.

**Concepts Demonstrated**:
- `Chart` widget with dynamic data and colors
- Configuration interface (`Config.get`) with a `{symbol}` URL placeholder
- Custom API calls with optional bearer token (`Network.get` with headers)
- Flexible JSON response parsing (arrays of numbers, objects, or `closes`)
- Percent-change computation and error handling

**Permissions Required**: `CONFIG`, `INTERNET`

**Usage**:
```bash
1. Open Settings and add STOCK_API_URL containing a "{symbol}" placeholder
   (optionally STOCK_API_KEY sent as a Bearer token)
2. Import stock_trends.js into ScriptHost
3. Enter a symbol (e.g. AAPL) and tap "Load"
```

---

### 13. Remote tmux Console (`tmux_remote.js`)

**Description**: Connects to a host over SSH and runs tmux commands -
`tmux ls`, `tmux capture-pane -p`, or any custom command - showing the
output in a scrollable console.

**Concepts Demonstrated**:
- `SSH.connect` / `SSH.exec` / `SSH.disconnect` session workflow
- `SSH` permission gating
- Configuration interface (`Config.get`) for connection settings
- Scrollable console output with `ScrollView` + `Label`
- Callback error handling for connect and exec

**Permissions Required**: `CONFIG`, `SSH`

**Usage**:
```bash
1. Open Settings and add TMUX_HOST, TMUX_PORT, TMUX_USER and TMUX_PASSWORD
2. Import tmux_remote.js into ScriptHost
3. Tap "Connect", then "List sessions", "Capture pane", or enter a custom
   command and tap "Run"
4. Tap "Disconnect" when done
```

---

### 14. Sub-Screens (`sub_screens.js`)

**Description**: Master/detail demo using the page stack. Tapping an item
pushes a detail page; a Back button (or the device back button) pops it
again. A depth indicator shows the current stack depth.

**Concepts Demonstrated**:
- Sub-screen navigation with `UI.pushPage()` / `UI.popPage()`
- Tracking the page stack with `UI.pageDepth()`
- View destruction on pop (detail-page handles become invalid)
- Device back button popping a page before closing the script

**Permissions Required**: None

**Usage**:
```bash
Import sub_screens.js into ScriptHost and run it
Tap an item to push its detail page
Tap "Back" (or the device back button) to pop it
```

---

## Creating Your Own Scripts

### Basic Template

```javascript
// Script metadata (for packaging)
// {
//   "name": "My Script",
//   "version": "1.0.0",
//   "author": "Your Name",
//   "description": "What your script does",
//   "permissions": ["INTERNET"],
//   "category": "UTILITY"
// }

// Your script code here
let title = new Label("My Script");
title.setTextSize(24);
UI.addView(title);

let button = new Button("Click Me");
button.setOnTap(function() {
    showToast("Hello from my script!");
});
UI.addView(button);
```

### Script Structure

1. **Initialization**: Create UI components
2. **Event Handlers**: Define callbacks for user interactions
3. **Business Logic**: Implement your script's functionality
4. **Cleanup**: Release resources when done (if needed)

### Tips

- **Start Simple**: Begin with basic UI, add features incrementally
- **Test Frequently**: Run your script often during development
- **Handle Errors**: Always check for null/undefined values
- **Use Console**: Log messages for debugging
- **Follow Conventions**: Use clear variable names and comments

### Common Patterns

**Pattern 1: Form Input**
```javascript
let input = new TextField("Enter text");
let button = new Button("Submit");
button.setOnTap(function() {
    let value = input.getValue();
    if (value) {
        // Process value
        showToast("Submitted: " + value);
    }
});
UI.addView(input);
UI.addView(button);
```

**Pattern 2: List Display**
```javascript
let items = ["Item 1", "Item 2", "Item 3"];
let list = new ListView();
list.setItems(items);
list.setOnItemTap(function(index) {
    showAlert("Selected", items[index]);
});
UI.addView(list);
```

**Pattern 3: Network Fetch**
```javascript
function fetchData(url) {
    Network.get(url, function(data, error) {
        if (error) {
            showToast("Error: " + error);
        } else {
            // Process data
            let json = JSON.parse(data);
            // Update UI with json
        }
    });
}
```

**Pattern 4: Persistent Storage**
```javascript
function saveData(key, value) {
    let success = Storage.writeFile(key + ".json", JSON.stringify(value));
    return success;
}

function loadData(key) {
    let content = Storage.readFile(key + ".json");
    return content ? JSON.parse(content) : null;
}
```

## Advanced Examples

### Multi-Screen Navigation

```javascript
let currentScreen = "home";

function showHomeScreen() {
    UI.clearViews();
    let title = new Label("Home Screen");
    UI.addView(title);
    
    let navButton = new Button("Go to Settings");
    navButton.setOnTap(function() {
        showSettingsScreen();
    });
    UI.addView(navButton);
}

function showSettingsScreen() {
    UI.clearViews();
    let title = new Label("Settings Screen");
    UI.addView(title);
    
    let backButton = new Button("Back");
    backButton.setOnTap(function() {
        showHomeScreen();
    });
    UI.addView(backButton);
}

showHomeScreen();
```

### Data Binding

```javascript
function createDataBoundLabel(initialValue) {
    let value = initialValue;
    let label = new Label(value);
    
    return {
        setValue: function(newValue) {
            value = newValue;
            label.setText(value);
        },
        getValue: function() {
            return value;
        },
        getView: function() {
            return label;
        }
    };
}

let counter = createDataBoundLabel("Count: 0");
UI.addView(counter.getView());

let button = new Button("Increment");
button.setOnTap(function() {
    let current = parseInt(counter.getValue().split(": ")[1]);
    counter.setValue("Count: " + (current + 1));
});
UI.addView(button);
```

## Debugging Tips

1. **Use console.log()**: Log values to track execution
2. **Check Permissions**: Ensure required permissions are granted
3. **Test Network**: Verify URLs are accessible
4. **Validate Input**: Check for empty or invalid values
5. **Handle Errors**: Use try-catch for error-prone operations
6. **Incremental Development**: Test each feature before adding more

## Resources

- [API Reference](API.md) - Complete API documentation
- [Security Guide](SECURITY.md) - Security best practices
- [GitHub Repository](https://github.com/scripthost/scripthost) - Source code and issues

## Contributing Examples

Have a great example script? Contribute it!

1. Create your script following the template
2. Test thoroughly
3. Add documentation
4. Submit a pull request

## License

All example scripts are released under MIT License and can be freely used and modified.
