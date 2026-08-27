# Script Examples

This directory contains example scripts demonstrating ScriptHost capabilities.

Note: running scripts already show their name in the app's top bar, so the
examples deliberately do not render their own in-page title labels.

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

**Description**: A compact checklist with an inline "+" add button, per-item
"X" delete buttons, and a font size configurable in Settings

**Concepts Demonstrated**:
- Horizontal rows with `setWeight` (input fills, button stays compact)
- Per-item delete via row removal
- Gray strikethrough on completion with theme-aware color restore
- Script-declared config (`TODO_FONT_SIZE`, number) via `Config.schema`

**Permissions Required**: CONFIG (only for the font-size setting)

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

**Description**: Chat with an OpenAI-compatible chat completions endpoint,
shown as a scrollable message list (user messages tinted blue, agent
messages gray, system/error messages red). After each exchange a status
line shows the model, the HTTP outcome, and the elapsed time.

**Concepts Demonstrated**:
- Script-provided config fields (`Config.schema`): `OPENAI_API_KEY`
  (required), `AGENT_API_URL` (optional base URL), `AGENT_MODEL` (select) —
  after the first run they appear in Settings under the script's section
- Custom API calls with `Authorization` headers (`Network.post` with headers)
- Chat-style UI: ScrollView + colored message labels
- Per-exchange interaction info (model, HTTP status/error, elapsed ms)
- Conversation history sent with each request
- Graceful error display as in-chat system messages

**Permissions Required**: `CONFIG`, `INTERNET`

**Usage**:
```bash
1. Run once, then open Settings and fill the Agent Chat section
   (at minimum OPENAI_API_KEY)
2. Import agent_conversation.js into ScriptHost
3. Enter a message and tap "Send"
4. The agent's reply appears as a new message bubble with a status line
```

---

### 8. Server Monitor (`server_monitor.js`)

**Description**: Polls a server health endpoint and shows live UP/DOWN status.
Uses `setInterval` for polling and optional bearer-token authentication.
Declares its configurable fields via `Config.schema` (URL, API key, poll
interval), so they appear in Settings under the script's section after the
first run.

**Concepts Demonstrated**:
- Script-provided config fields (`Config.schema`) read back with `Config.get`
- Polling with `setInterval` / `clearInterval` at a configurable interval
- Custom API calls with optional auth headers (`Network.get` with headers)
- Request de-duplication (skips overlapping polls)
- Status rendering and error handling

**Permissions Required**: `CONFIG`, `INTERNET`

**Usage**:
```bash
1. Run once, then open Settings and fill the Server Monitor section
   (MONITOR_URL, optionally MONITOR_API_KEY / MONITOR_INTERVAL_SEC)
2. Import server_monitor.js into ScriptHost
3. Tap "Start Monitoring" to begin polling
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

### 11. Daily Fitness Checklist (`daily_fitness.js`)

**Description**: A configurable daily checklist rendered from a markdown
plan. The plan is configured in Settings (`FITNESS_PLAN_MD`, a multiline
field declared via `Config.schema`); a small built-in sample plan is used
until then. Checking an item grays it out and strikes it through, and the
done-state is persisted per day (a new day starts unchecked).

**Concepts Demonstrated**:
- Script-provided config fields (`Config.schema`) with the `multiline` type
- Markdown checklist parsing: `#` lines become section labels, `- [ ]`
  lines become CheckBoxes, other lines become gray notes
- `setStrikeThrough` + text color for completed items
- Per-day state persistence via `Storage` (`fitness_state_<yyyy-mm-dd>.json`)
- `Scheduler.scheduleDaily` / `Scheduler.cancel` and `Notify.post`, with
  the notification body reporting "N of M checklist items done today"

**Permissions Required**: `NOTIFICATIONS`, `READ_STORAGE`, `WRITE_STORAGE`,
`CONFIG`

**Usage**:
```bash
1. Import daily_fitness.js into ScriptHost and run it once
2. Open Settings and paste your plan into the script's FITNESS_PLAN_MD field
3. Check items off during the day; state resets on the next day
4. Optionally enable the daily 8:00 reminder
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

### 15. Markdown Demo (`markdown_demo.js`)

**Description**: Renders Markdown text with the built-in lightweight parser
and replaces the content on demand when a button is tapped.

**Concepts Demonstrated**:
- `new Markdown(text)` and `UI.addView(md)`
- `md.setMarkdown(text)` to replace rendered content
- Headings, emphasis, code, lists, blockquotes, links, horizontal rules

**Permissions Required**: None

**Usage**:
```bash
Import markdown_demo.js into ScriptHost and run it
Tap "Show code sample" to swap in a fenced code block
```

---

### 16. Scheduled Notifications (`scheduled_notify.js`)

**Description**: Posts an immediate notification, schedules one-shot
notifications after a delay and at an absolute time, sets up a daily
recurring reminder, and cancels a schedule.

**Concepts Demonstrated**:
- `Notify.post(title, message)` for immediate notifications
- `Scheduler.scheduleIn(id, delayMs, ...)` one-shot after a delay
- `Scheduler.scheduleAt(id, epochMs, ...)` one-shot at an absolute time
- `Scheduler.scheduleDaily(...)` and `Scheduler.cancel(id)`

**Permissions Required**: NOTIFICATIONS

**Usage**:
```bash
Import scheduled_notify.js into ScriptHost and run it
Grant the notifications permission when prompted
```

---

### 17. Device Info (`device_info.js`)

**Description**: Displays read-only device and system information —
manufacturer/model, device name, Android version, CPU architecture,
time/timezone, and live memory/storage usage with a refresh button.

**Concepts Demonstrated**:
- `Device.getInfo()` and `Device.getSystemInfo()` for version/ABI details
- `Device.getTime()`, `Device.getTimeZone()`, `Device.getDeviceName()`
- `Device.getMemoryInfo()` and `Device.getStorageInfo()` for usage stats
- `Config.keys()` to list configured key names

**Permissions Required**: CONFIG (only for the `Config.keys()` row)

---

### 18. Metronome (`metronome.js`)

**Description**: A BPM metronome. A slider sets the tempo (40-208 BPM) with
a live BPM label; a start/stop button toggles the beat. Each beat flashes
the beat indicator and vibrates; the first beat of every 4 is accented
(different color, longer vibration).

**Concepts Demonstrated**:
- `setInterval`/`clearInterval` for periodic beats (always cleared before
  re-creation, so timers never stack)
- `Slider` with `setMin`/`setMax`/`setValue` and live updates
- `Device.vibrate` with accent patterns
- Visual feedback via background-color flashes

**Permissions Required**: VIBRATE

**Usage**:
```bash
Import metronome.js into ScriptHost and run it
Drag the slider to set the tempo, tap "Start"
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
//   "permissions": ["INTERNET"]
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
