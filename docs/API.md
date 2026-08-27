# ScriptHost API Reference

Complete API documentation for writing scripts in ScriptHost.

## Table of Contents

1. [UI Components](#ui-components)
2. [Network API](#network-api)
3. [Config API](#config-api)
4. [Storage API](#storage-api)
5. [Sensor API](#sensor-api)
6. [Device API](#device-api)
7. [Notify API](#notify-api)
8. [Scheduler API](#scheduler-api)
9. [SSH API](#ssh-api)
10. [Global Functions](#global-functions)

---

## UI Components

All components are created as objects and attached to the screen with
`UI.addView(view)`. Components can be nested inside `Layout` and `ScrollView`
containers. Every property is applied statically; the interface intentionally
provides no animation support.

### Common Configuration Methods

Every component exposes the following methods:

- `setVisible(visible: boolean)` - Show or hide the view (`GONE` when hidden)
- `setEnabled(enabled: boolean)` - Enable or disable interaction
- `setPadding(left, top, right, bottom: number)` - Padding in dp
- `setMargin(left, top, right, bottom: number)` - Margin in dp
- `setWidth(px: number)` - Width in dp; `-1` matches the parent, `-2` wraps content
- `setHeight(px: number)` - Height in dp; `-1` matches the parent, `-2` wraps content
- `setAlpha(alpha: number)` - Opacity from `0.0` to `1.0`
- `setBackgroundColor(color: string)` - Background color (hex: `"#RRGGBB"`)
- `setCornerRadius(radiusDp: number)` - Rounded background corners in dp
- `getViewId()` - Native view id (used with `UI.removeView`)

### Common Text Configuration Methods

Text-capable components (Button, Label, Switch, CheckBox, TextField) also
expose:

- `setTextSize(size: number)` - Text size in sp
- `setTextColor(color: string)` - Text color (hex: `"#RRGGBB"`)
- `setBold(bold: boolean)` - Toggle bold typeface
- `setItalic(italic: boolean)` - Toggle italic typeface
- `setTextAlign(align: string)` - Alignment: `left`, `right`, `center`,
  `top`, `bottom`, `fill`, `start`, `end`
- `setAllCaps(allCaps: boolean)` - Force uppercase text
- `setStrikeThrough(enabled: boolean)` - Toggle strikethrough text

### Button

Creates a clickable button.

```javascript
let button = new Button(text)
```

**Methods:**
- `setText(text: string)` - Set button text
- `setOnTap(callback: function)` - Set click handler

**Example:**
```javascript
let btn = new Button("Click Me");
btn.setBackgroundColor("#007AFF");
btn.setTextColor("#FFFFFF");
btn.setCornerRadius(8);
btn.setOnTap(function() {
    console.log("Button clicked");
});
UI.addView(btn);
```

---

### Label

Displays text.

```javascript
let label = new Label(text)
```

**Methods:**
- `setText(text: string)` - Set label text

**Example:**
```javascript
let label = new Label("Hello World");
label.setTextSize(24);
label.setTextColor("#000000");
label.setBold(true);
UI.addView(label);
```

---

### TextField

Text input field.

```javascript
let textField = new TextField(hint)
```

**Methods:**
- `getValue()` - Get current text value
- `setValue(text: string)` - Set text value
- `setOnChange(callback: function)` - Set change handler (receives text)
- `setHint(hint: string)` - Set placeholder text
- `setHintTextColor(color: string)` - Set placeholder color
- `setInputType(type: string)` - Input mode: `text`, `number`, `phone`,
  `email`, `password`, `multiline`
- `setMaxLength(maxLength: number)` - Limit the number of characters

**Example:**
```javascript
let input = new TextField("Enter name");
input.setInputType("text");
input.setMaxLength(30);
input.setOnChange(function(text) {
    console.log("Input changed: " + text);
});
UI.addView(input);
```

---

### ListView

Scrollable list of items.

```javascript
let listView = new ListView()
```

**Methods:**
- `setItems(items: array)` - Set list items (array of strings)
- `setOnItemTap(callback: function)` - Set item click handler (receives index)
- `setSelection(index: number)` - Scroll to and select an item

**Example:**
```javascript
let list = new ListView();
list.setItems(["Item 1", "Item 2", "Item 3"]);
list.setOnItemTap(function(index) {
    console.log("Tapped item: " + index);
});
UI.addView(list);
```

---

### ImageView

Displays an image.

```javascript
let imageView = new ImageView()
```

**Methods:**
- `setImageBase64(data: string)` - Set image from base64-encoded bytes
- `setScaleType(type: string)` - Scaling: `center`, `center_crop`,
  `center_inside`, `fit_center`, `fit_start`, `fit_end`, `fit_xy`

**Example:**
```javascript
let image = new ImageView();
image.setImageBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
image.setScaleType("fit_center");
UI.addView(image);
```

---

### Switch

Toggle switch.

```javascript
let switchView = new Switch(text)
```

**Methods:**
- `setText(text: string)` - Set label text
- `setChecked(checked: boolean)` - Set switch state
- `getChecked()` - Get switch state
- `setOnChange(callback: function)` - Set change handler (receives boolean)

**Example:**
```javascript
let toggle = new Switch("Enable notifications");
toggle.setChecked(true);
toggle.setOnChange(function(isChecked) {
    console.log("Switch: " + isChecked);
});
UI.addView(toggle);
```

---

### Slider

Horizontal slider.

```javascript
let slider = new Slider()
```

**Methods:**
- `setValue(value: number)` - Set slider value
- `getValue()` - Get current value
- `setMax(max: number)` - Set maximum value
- `setMin(min: number)` - Set minimum value (Android 8.0+)
- `setOnChange(callback: function)` - Set change handler (receives value)

**Example:**
```javascript
let slider = new Slider();
slider.setMax(100);
slider.setValue(50);
slider.setOnChange(function(value) {
    console.log("Slider value: " + value);
});
UI.addView(slider);
```

---

### ScrollView

Scrollable container that holds a single content view. Wrap multiple children
in a `Layout` and add the layout to the ScrollView.

```javascript
let scrollView = new ScrollView()
```

**Methods:**
- `addView(view)` - Set the content view (replaces any existing content)
- `removeView(view)` - Remove the content view
- `setFillViewport(fillViewport: boolean)` - Expand content to fill the viewport

**Example:**
```javascript
let scroll = new ScrollView();
let content = new Layout("vertical");
content.addView(new Label("Line 1"));
scroll.addView(content);
UI.addView(scroll);
```

---

### CheckBox

Checkable option with a text label.

```javascript
let checkBox = new CheckBox(text)
```

**Methods:**
- `setText(text: string)` - Set label text
- `setChecked(checked: boolean)` - Set checked state
- `getChecked()` - Get checked state
- `setOnChange(callback: function)` - Set change handler (receives boolean)

**Example:**
```javascript
let check = new CheckBox("Enable notifications");
check.setChecked(true);
check.setOnChange(function(isChecked) {
    console.log("Checked: " + isChecked);
});
UI.addView(check);
```

---

### Spinner

Dropdown selector.

```javascript
let spinner = new Spinner()
```

**Methods:**
- `setItems(items: array)` - Set options (array of strings)
- `setSelection(index: number)` - Select an option
- `getSelection()` - Get selected index
- `setOnChange(callback: function)` - Set change handler
  (receives `(index, label)`)

**Example:**
```javascript
let spinner = new Spinner();
spinner.setItems(["Light", "Dark", "System"]);
spinner.setSelection(0);
spinner.setOnChange(function(index, label) {
    console.log("Selected: " + label);
});
UI.addView(spinner);
```

---

### ProgressBar

Horizontal progress indicator.

```javascript
let progressBar = new ProgressBar()
```

**Methods:**
- `setMax(max: number)` - Set maximum value
- `setProgress(value: number)` - Set current value
- `getProgress()` - Get current value
- `setIndeterminate(indeterminate: boolean)` - Toggle indeterminate mode

**Example:**
```javascript
let progress = new ProgressBar();
progress.setMax(100);
progress.setProgress(40);
UI.addView(progress);
```

---

### Layout

Linear container for arranging child views.

```javascript
let layout = new Layout(orientation)
```

`orientation` is `"vertical"` (default) or `"horizontal"`.

**Methods:**
- `addView(view)` - Add a child view (reparents it if already attached)
- `removeView(view)` - Remove a child view
- `setOrientation(orientation: string)` - Set `"vertical"` or `"horizontal"`
- `setGravity(gravity: string)` - Child alignment: `center`,
  `center_horizontal`, `center_vertical`, `left`, `right`, `top`, `bottom`,
  `start`, `end`, `fill`

**Example:**
```javascript
let row = new Layout("horizontal");
row.setGravity("center");
row.addView(new Button("A"));
row.addView(new Button("B"));
UI.addView(row);
```

---

### Chart

Lightweight chart view for numeric series.

```javascript
let chart = new Chart(type)
```

`type` is `"line"` or `"bar"`.

**Methods:**
- `setData(values: array)` - Set data points (array of numbers)
- `setLabels(labels: array)` - Set category labels (array of strings)
- `setColor(color: string)` - Series color (hex: `"#RRGGBB"`)

**Permissions Required:** None

**Example:**
```javascript
let chart = new Chart("line");
chart.setLabels(["Mon", "Tue", "Wed"]);
chart.setData([3, 7, 5]);
chart.setColor("#34C759");
UI.addView(chart);
```

---

### Markdown

Lightweight Markdown view rendered by a small built-in parser (no external dependencies). Links are clickable.

```javascript
let md = new Markdown(markdown)
```

`markdown` is the initial Markdown text.

**Supported syntax:** headings `#`..`####`, `**bold**`, `*italic*` / `_italic_`, `~~strikethrough~~`, `` `inline code` ``, fenced ``` code blocks ```, unordered lists (`-`/`*`, one nesting level), ordered lists (`1.`), `> blockquote`, `[text](url)` links, horizontal rules (`---`), and paragraphs.

**Methods:**
- `setMarkdown(text: string)` - Replace the rendered content

**Permissions Required:** None

**Example:**
```javascript
let md = new Markdown("# Report\n**Status:** OK\n- [details](https://example.com)");
UI.addView(md);
md.setMarkdown("Updated *content*");
```

---

## UI Namespace

The global `UI` object manages the root container and the script's page stack.

### Page Model (Sub-Screens)

Each script owns a stack of pages. The stack always starts with a single root
page (depth 1), and `UI.pushPage()` pushes additional pages on top of it to
build sub-screens such as master/detail flows:

- `UI.addView()` always targets the **top** page of the stack; only the top
  page is visible in the runtime activity.
- Popping a page destroys every view created on it — their handles become
  invalid and must not be reused.
- The device back button pops the top page first; the script only closes when
  back is pressed at the root page (depth 1).
- Single-page scripts are unaffected: if `UI.pushPage()` is never called,
  `UI.addView()` targets the root page exactly as before.

### UI.addView(view)

Add a view to the top page of the page stack. If the view is already attached
to another container it is reparented automatically.

```javascript
UI.addView(button);
```

### UI.removeView(viewId)

Remove a view from the container by its id (from `getViewId()`).

```javascript
UI.removeView(view.getViewId());
```

### UI.clearViews()

Remove all views from the container.

```javascript
UI.clearViews();
```

### UI.setBackgroundColor(color)

Set the background color of the root container.

```javascript
UI.setBackgroundColor("#ECEFF1");
```

### UI.pushPage()

Push a new empty page onto the script's page stack. The new page becomes the
target of subsequent `UI.addView()` calls and is the only page visible in the
runtime activity.

**Returns:** The new stack depth (number)

```javascript
let depth = UI.pushPage();
UI.addView(new Label("Detail page, depth " + depth));
```

### UI.popPage()

Pop the top page off the script's page stack. All views created on the popped
page are destroyed and their handles become invalid.

**Returns:** `true` when a page was popped; `false` at the root page
(depth 1), in which case nothing changes

```javascript
let popped = UI.popPage();
if (!popped) {
    showToast("Already at the root page");
}
```

### UI.pageDepth()

Get the current page stack depth.

**Returns:** Stack depth (number); `1` is the root page

```javascript
console.log("Depth: " + UI.pageDepth());
```

**Example (master/detail):**
```javascript
let list = new ListView();
list.setItems(["Inbox", "Calendar", "Settings"]);
list.setOnItemTap(function(index) {
    UI.pushPage();
    let back = new Button("Back");
    back.setOnTap(function() { UI.popPage(); });
    UI.addView(back);
});
UI.addView(list);
```

See `scripts/examples/sub_screens.js` for a complete demo.

---

## Network API

### Network.get(url, callback)

Perform HTTP GET request.

**Parameters:**
- `url` (string) - URL to fetch
- `callback` (function) - Callback function (data, error)

**Permissions Required:** `INTERNET`

**Example:**
```javascript
Network.get("https://api.example.com/data", function(data, error) {
    if (error) {
        console.error("Error: " + error);
    } else {
        console.log("Response: " + data);
    }
});
```

---

### Network.get(url, headers, callback)

Perform HTTP GET request with custom headers (e.g. `Authorization`).

**Parameters:**
- `url` (string) - URL to fetch
- `headers` (object) - Object of header name to value, e.g. `{ "Authorization": "Bearer sk-..." }`
- `callback` (function) - Callback function (data, error)

**Permissions Required:** `INTERNET`

**Example:**
```javascript
let headers = { "Authorization": "Bearer " + Config.get("OPENAI_API_KEY") };
Network.get("https://api.example.com/me", headers, function(data, error) {
    if (error) {
        console.error("Error: " + error);
    } else {
        console.log("Response: " + data);
    }
});
```

---

### Network.post(url, body, callback)

Perform HTTP POST request.

**Parameters:**
- `url` (string) - URL to post to
- `body` (string) - Request body (JSON string)
- `callback` (function) - Callback function (data, error)

**Permissions Required:** `INTERNET`

**Example:**
```javascript
let payload = JSON.stringify({ name: "John", age: 30 });
Network.post("https://api.example.com/users", payload, function(data, error) {
    if (error) {
        console.error("Error: " + error);
    } else {
        console.log("Response: " + data);
    }
});
```

---

### Network.post(url, headers, body, callback)

Perform HTTP POST request with custom headers (e.g. `Authorization`).

**Parameters:**
- `url` (string) - URL to post to
- `headers` (object) - Object of header name to value, e.g. `{ "Authorization": "Bearer sk-..." }`
- `body` (string) - Request body (JSON string)
- `callback` (function) - Callback function (data, error)

**Permissions Required:** `INTERNET`

**Example:**
```javascript
let headers = {
    "Authorization": "Bearer " + Config.get("OPENAI_API_KEY"),
    "Content-Type": "application/json"
};
let payload = JSON.stringify({ query: "hello" });
Network.post("https://api.example.com/chat", headers, payload, function(data, error) {
    if (error) {
        console.error("Error: " + error);
    } else {
        console.log("Response: " + data);
    }
});
```

---

## Config API

Reads API keys and other settings configured by the user in the Settings screen.

### Config.get(key)

Get a configured value by key.

**Parameters:**
- `key` (string) - Configuration key, e.g. `"OPENAI_API_KEY"`

**Returns:** String value, or `null`/`undefined` when not configured

**Permissions Required:** `CONFIG`

**Example:**
```javascript
let apiKey = Config.get("OPENAI_API_KEY");
if (apiKey) {
    console.log("API key is configured");
} else {
    console.log("API key missing - configure it in Settings");
}
```

---

### Config.keys()

List all configured keys. Values are not exposed.

**Returns:** Array of key strings

**Permissions Required:** `CONFIG`

**Example:**
```javascript
let keys = Config.keys();
for (let i = 0; i < keys.length; i++) {
    console.log("Configured: " + keys[i]);
}
```

---

### Config.schema(jsonString)

Declare the script's own configurable fields. After the script has run once
and declared its schema, the fields appear as a dedicated section at the
bottom of Settings under the script's title. Values the user enters are read
back with the existing `Config.get(key)` (global key namespace, unchanged).

The argument is a JSON STRING (use `JSON.stringify`) holding an array of
field objects:

- `key` (string, required) - Config key read back via `Config.get`
- `label` (string, optional) - Display label; defaults to `key`
- `type` (string, required) - `"text"`, `"password"`, `"number"`,
  `"boolean"`, `"multiline"` (multi-line text input), or `"select"`
  (select requires a non-empty `options` array)
- `options` (array of strings, select only)
- `default` (string/number/boolean, optional)

Fields with unknown types, missing keys, duplicate keys, or selects without
options are skipped with a logged warning; valid fields are kept.
Re-declaring replaces the previous schema; uninstalling the script drops it.

**Returns:** Boolean success status (false without permission or on malformed JSON)

**Permissions Required:** `CONFIG`

**Example:**
```javascript
Config.schema(JSON.stringify([
    { key: "OPENAI_API_KEY", label: "API Key", type: "password" },
    { key: "AGENT_API_URL", label: "API Base URL", type: "text",
      default: "https://api.openai.com/v1" },
    { key: "AGENT_MODEL", label: "Model", type: "select",
      options: ["gpt-4o-mini", "gpt-4o"], default: "gpt-4o-mini" }
]));
```

> **Security note**: `CONFIG` is a non-dangerous permission that is auto-granted
> to any installed script declaring it. Only install scripts you trust.

### Keys Used by the Bundled Examples

- `MONITOR_URL` / `MONITOR_API_KEY` / `MONITOR_THRESHOLD` - monitor endpoint
  URL, optional bearer token, and optional numeric alert threshold
  (`server_monitor.js`, `monitor_port_chart.js`)
- `STOCK_API_URL` / `STOCK_API_KEY` - quote API URL containing a `{symbol}`
  placeholder, plus an optional bearer token (`stock_trends.js`)
- `TMUX_HOST` / `TMUX_PORT` / `TMUX_USER` / `TMUX_PASSWORD` - SSH connection
  settings for the remote tmux console (`tmux_remote.js`)
- `AGENT_API_URL` / `OPENAI_API_KEY` - OpenAI-compatible chat endpoint base
  URL (optional; default `https://api.openai.com/v1`) and required API key
  (`agent_conversation.js`); both can be set from the dedicated Agent section
  in Settings

---

## Storage API

All storage operations use the app's private storage directory. Filenames are
confined to that directory via canonical-path checking: `../` traversal
sequences and absolute paths outside the app's private directory are rejected
(the call fails closed with `null`/`false`, the same as a permission denial).

### Storage.readFile(filename)

Read file contents.

**Returns:** String content or null if file doesn't exist

**Permissions Required:** `READ_STORAGE`

**Example:**
```javascript
let content = Storage.readFile("data.txt");
if (content) {
    console.log("File content: " + content);
}
```

---

### Storage.writeFile(filename, content)

Write content to file.

**Returns:** Boolean success status

**Permissions Required:** `WRITE_STORAGE`

**Example:**
```javascript
let success = Storage.writeFile("data.txt", "Hello World");
if (success) {
    console.log("File saved");
}
```

---

### Storage.deleteFile(filename)

Delete a file.

**Returns:** Boolean success status

**Permissions Required:** `WRITE_STORAGE`

**Example:**
```javascript
let success = Storage.deleteFile("data.txt");
if (success) {
    console.log("File deleted");
}
```

---

### Storage.listFiles(directory)

List files in directory.

**Returns:** Array of filenames

**Permissions Required:** `READ_STORAGE`

**Example:**
```javascript
let files = Storage.listFiles("");
console.log("Files: " + files.join(", "));
```

---

## Sensor API

### Sensor.getAccelerometer(callback)

Get accelerometer data.

**Parameters:**
- `callback` (function) - Called with sensor data { x, y, z }

**Permissions Required:** `ACCELEROMETER`

**Example:**
```javascript
Sensor.getAccelerometer(function(data) {
    console.log("X: " + data.x + ", Y: " + data.y + ", Z: " + data.z);
});
```

---

### Sensor.getGyroscope(callback)

Get gyroscope data.

**Parameters:**
- `callback` (function) - Called with sensor data { x, y, z }

**Permissions Required:** `GYROSCOPE`

**Example:**
```javascript
Sensor.getGyroscope(function(data) {
    console.log("X: " + data.x + ", Y: " + data.y + ", Z: " + data.z);
});
```

---

### Sensor.stop()

Stop sensor listening.

**Example:**
```javascript
Sensor.stop();
```

---

## Device API

### Device.vibrate(durationMs)

Vibrate the device.

**Parameters:**
- `durationMs` (number) - Vibration duration in milliseconds

**Permissions Required:** `VIBRATE`

**Example:**
```javascript
Device.vibrate(200); // Vibrate for 200ms
```

---

### Device.getInfo()

Get device information.

**Returns:** Object with device info

**Example:**
```javascript
let info = Device.getInfo();
console.log("Manufacturer: " + info.manufacturer);
console.log("Model: " + info.model);
console.log("Android Version: " + info.androidVersion);
console.log("SDK Version: " + info.sdkVersion);
```

---

### Device.getTime()

Current time as epoch milliseconds.

**Returns:** Number (milliseconds since the Unix epoch)

**Example:**
```javascript
console.log("Now: " + Device.getTime());
```

---

### Device.getTimeZone()

Current time zone ID.

**Returns:** String (e.g. `"Asia/Shanghai"`)

**Example:**
```javascript
console.log("Time zone: " + Device.getTimeZone());
```

---

### Device.getDeviceName()

User-visible device name (Settings device name, falling back to the model).

**Returns:** String

**Example:**
```javascript
console.log("Device: " + Device.getDeviceName());
```

---

### Device.getMemoryInfo()

Memory information.

**Returns:** Object `{ totalMB, availableMB, lowMemory }` (sizes in MiB)

**Example:**
```javascript
let mem = Device.getMemoryInfo();
console.log("Free: " + mem.availableMB + " / " + mem.totalMB + " MB");
```

---

### Device.getStorageInfo()

Internal data-storage information.

**Returns:** Object `{ totalMB, freeMB, usedMB }` (sizes in MiB)

**Example:**
```javascript
let storage = Device.getStorageInfo();
console.log("Used: " + storage.usedMB + " / " + storage.totalMB + " MB");
```

---

### Device.getSystemInfo()

System version and architecture information. Manufacturer and model are in
`Device.getInfo()`.

**Returns:** Object `{ androidVersion, sdkVersion, abi, supportedAbis }`
(`androidVersion` is the release string, `abi` the primary ABI,
`supportedAbis` an array of all supported ABIs)

**Example:**
```javascript
let sys = Device.getSystemInfo();
console.log("Android " + sys.androidVersion + " (SDK " + sys.sdkVersion + ")");
console.log("ABI: " + sys.abi + " of " + sys.supportedAbis.join(", "));
```

---

## Notify API

Posts immediate local notifications.

### Notify.post(title, message)

Post a local notification right away.

**Parameters:**
- `title` (string) - Notification title
- `message` (string) - Notification body text

**Returns:** Boolean success status

**Permissions Required:** `NOTIFICATIONS`

> On Android 13 (API 33) and above, posting notifications requires the
> `POST_NOTIFICATIONS` runtime permission. The system shows a permission
> dialog the first time a script uses `Notify` or `Scheduler`.

**Example:**
```javascript
let posted = Notify.post("Daily Fitness", "Take a brisk 20-minute walk today.");
if (posted) {
    console.log("Notification posted");
}
```

---

## Scheduler API

Schedules local notifications, one-shot or recurring. Scheduling is backed by
WorkManager, so notifications fire even while the script is not running; no
script code executes in the background. WorkManager timing is inexact
(battery-friendly): treat all delays as minute-scale precision, not an exact
alarm clock.

### Scheduler.scheduleIn(id, delayMs, title, message)

Schedule a one-shot notification `delayMs` milliseconds from now. Calling
again with the same `id` replaces the existing schedule.

**Parameters:**
- `id` (string) - Unique identifier for the schedule
- `delayMs` (number) - Delay in milliseconds (>= 0)
- `title` (string) - Notification title
- `message` (string) - Notification body text

**Returns:** Boolean success status (false when permission is missing or the delay is invalid)

**Permissions Required:** `NOTIFICATIONS` (same Android 13+ runtime dialog as
`Notify`)

**Example:**
```javascript
Scheduler.scheduleIn("standup", 15 * 60000, "Standup", "Starts in 15 minutes");
```

---

### Scheduler.scheduleAt(id, epochMs, title, message)

Schedule a one-shot notification at an absolute time. Times in the past fire
as soon as possible.

**Parameters:**
- `id` (string) - Unique identifier for the schedule
- `epochMs` (number) - Absolute time in milliseconds since the Unix epoch
- `title` (string) - Notification title
- `message` (string) - Notification body text

**Returns:** Boolean success status

**Permissions Required:** `NOTIFICATIONS`

**Example:**
```javascript
Scheduler.scheduleAt("deploy", Date.now() + 3600000, "Deploy",
    "Deployment window opens in one hour");
```

---

### Scheduler.scheduleDaily(id, hour, minute, title, message)

Schedule a daily notification at the given local time. Calling again with the
same `id` replaces the existing schedule.

**Parameters:**
- `id` (string) - Unique identifier for the schedule
- `hour` (number) - Hour of day, 0-23
- `minute` (number) - Minute of hour, 0-59
- `title` (string) - Notification title
- `message` (string) - Notification body text

**Returns:** Boolean success status

**Permissions Required:** `NOTIFICATIONS` (same Android 13+ runtime dialog as
`Notify`)

**Example:**
```javascript
let scheduled = Scheduler.scheduleDaily("fitness", 8, 0, "Daily Fitness",
    "Stretch for 10 minutes before breakfast.");
if (scheduled) {
    showToast("Reminder scheduled for 8:00");
}
```

---

### Scheduler.cancel(id)

Cancel a previously scheduled notification — daily or one-shot.

**Parameters:**
- `id` (string) - Identifier passed to `Scheduler.scheduleDaily`,
  `Scheduler.scheduleIn`, or `Scheduler.scheduleAt`

**Returns:** Boolean success status

**Permissions Required:** `NOTIFICATIONS`

**Example:**
```javascript
let cancelled = Scheduler.cancel("fitness");
if (cancelled) {
    showToast("Reminder cancelled");
}
```

---

## SSH API

Runs commands on a remote host over an interactive SSH session (JSch). One
session is kept per running script; credentials are supplied by the script
(for example via `Config`) and are not persisted by the bridge.

### SSH.connect(host, port, username, password, callback)

Open an SSH session to a remote host.

**Parameters:**
- `host` (string) - Hostname or IP address
- `port` (number) - SSH port (usually 22)
- `username` (string) - Login user
- `password` (string) - Login password
- `callback` (function) - Callback function (error)

**Permissions Required:** `SSH`

**Example:**
```javascript
SSH.connect("192.168.1.10", 22, "pi", "secret", function(error) {
    if (error) {
        console.error("Connect failed: " + error);
    } else {
        console.log("Connected");
    }
});
```

---

### SSH.exec(command, callback)

Execute a command on the connected host.

**Parameters:**
- `command` (string) - Shell command to run
- `callback` (function) - Callback function (output, error)

**Permissions Required:** `SSH`

**Example:**
```javascript
SSH.exec("tmux ls", function(output, error) {
    if (error) {
        console.error("Command failed: " + error);
    } else {
        console.log(output);
    }
});
```

---

### SSH.disconnect()

Close the current SSH session.

**Example:**
```javascript
SSH.disconnect();
```

---

## Global Functions

### console.log(message)

Log message to console.

```javascript
console.log("Hello World");
```

---

### console.warn(message)

Log warning to console.

```javascript
console.warn("This is a warning");
```

---

### console.error(message)

Log error to console.

```javascript
console.error("This is an error");
```

---

### showAlert(title, message)

Show alert dialog.

```javascript
showAlert("Success", "Operation completed");
```

---

### showConfirm(title, message, callback)

Show a confirmation dialog. The callback receives `(confirmed: boolean)`.

```javascript
showConfirm("Delete", "Delete this item?", function(confirmed) {
    if (confirmed) {
        console.log("Confirmed");
    }
});
```

---

### showPrompt(title, message, callback)

Show a text input dialog. The callback receives
`(value: string, cancelled: boolean)`.

```javascript
showPrompt("Rename", "New name:", function(value, cancelled) {
    if (!cancelled) {
        console.log("New name: " + value);
    }
});
```

---

### showListPicker(title, items, callback)

Show a list selection dialog. The callback receives
`(index: number, label: string)`.

```javascript
showListPicker("Choose action", ["Save", "Export"], function(index, label) {
    console.log("Picked: " + label);
});
```

---

### showToast(message)

Show a toast notification.

```javascript
showToast("Hello World");
```

---

### showToast(message, duration)

Show a toast notification with an explicit duration. Pass `"long"` for a
longer display time (any other value uses the default short duration).

```javascript
showToast("Saved", "long");
```

---

### setTimeout(callback, delay)

Execute callback after delay. Returns a timer ID that can be passed to
`clearTimeout`.

**Parameters:**
- `callback` (function) - Function to execute
- `delay` (number) - Delay in milliseconds

```javascript
var timer = setTimeout(function() {
    console.log("Delayed execution");
}, 1000);
```

---

### clearTimeout(timerId)

Cancel a timer previously created with `setTimeout`.

**Parameters:**
- `timerId` (number) - Timer ID returned by `setTimeout`

```javascript
var timer = setTimeout(function() {
    console.log("This will not run");
}, 1000);
clearTimeout(timer);
```

---

### setInterval(callback, interval)

Execute callback repeatedly at interval. Returns a timer ID that can be passed
to `clearInterval`.

**Parameters:**
- `callback` (function) - Function to execute
- `interval` (number) - Interval in milliseconds

```javascript
var timer = setInterval(function() {
    console.log("Repeated execution");
}, 1000);
```

---

### clearInterval(timerId)

Cancel a timer previously created with `setInterval`.

**Parameters:**
- `timerId` (number) - Timer ID returned by `setInterval`

```javascript
var timer = setInterval(function() {
    console.log("Repeated execution");
}, 1000);
clearInterval(timer);
```

---

## Permissions

Scripts must declare required permissions in their metadata:

- `INTERNET` - Network access
- `NETWORK_STATE` - Check network state
- `READ_STORAGE` - Read files
- `WRITE_STORAGE` - Write files
- `LOCATION_FINE` - Precise location (dangerous)
- `LOCATION_COARSE` - Approximate location (dangerous)
- `CAMERA` - Camera access (dangerous)
- `RECORD_AUDIO` - Microphone access (dangerous)
- `ACCELEROMETER` - Accelerometer sensor
- `GYROSCOPE` - Gyroscope sensor
- `VIBRATE` - Vibrate device
- `NOTIFICATIONS` - Show notifications (maps to the `POST_NOTIFICATIONS`
  runtime permission on Android 13 / API 33 and above)
- `SSH` - Connect to remote hosts via SSH
- `READ_CONTACTS` - Read contacts (dangerous)
- `WRITE_CONTACTS` - Modify contacts (dangerous)

Dangerous permissions require user approval at runtime.

> **Enforcement note**: Declaring a permission is only half of the gate. Every
> bridge (Network, Config, Storage, Notify, Scheduler, SSH, etc.) is bound to
> the running script's ID and calls
> `PermissionManager.hasScriptPermission(scriptId, permission)` before each
> sensitive operation, so the permission must both be declared by the script
> and pass the system-level check. Undeclared access is denied: the call
> fails closed (returns `null`/`false`, reports an error to the callback, or
> no-ops, depending on the API).

---

## Best Practices

1. **Error Handling**: Always check for errors in callbacks
2. **Resource Cleanup**: Stop sensors when done
3. **Memory Management**: Clear views when no longer needed
4. **Async Operations**: Use callbacks for network and sensor operations
5. **User Feedback**: Provide visual feedback for long operations
6. **Permission Requests**: Only request necessary permissions
7. **Testing**: Test scripts thoroughly before distribution

---

## Limitations

- Maximum execution time: 30 seconds
- Network timeout: 10 seconds
- File operations limited to app's private storage

---

## Support

For issues and questions:
- GitHub: https://github.com/scripthost/scripthost
- Documentation: https://scripthost.dev/docs
