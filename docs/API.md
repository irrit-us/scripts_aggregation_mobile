# ScriptHost API Reference

Complete API documentation for writing scripts in ScriptHost.

## Table of Contents

1. [UI Components](#ui-components)
2. [Network API](#network-api)
3. [Config API](#config-api)
4. [Storage API](#storage-api)
5. [Sensor API](#sensor-api)
6. [Device API](#device-api)
7. [Global Functions](#global-functions)

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

## UI Namespace

The global `UI` object manages the root container.

### UI.addView(view)

Add a view to the main container. If the view is already attached to another
container it is reparented automatically.

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

> **Security note**: `CONFIG` is a non-dangerous permission that is auto-granted
> to any installed script declaring it. Only install scripts you trust.

---

## Storage API

All storage operations use the app's private storage directory.

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
- `NOTIFICATIONS` - Show notifications
- `READ_CONTACTS` - Read contacts (dangerous)
- `WRITE_CONTACTS` - Modify contacts (dangerous)

Dangerous permissions require user approval at runtime.

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
