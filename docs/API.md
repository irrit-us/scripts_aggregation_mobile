# ScriptHost API Reference

Complete API documentation for writing scripts in ScriptHost.

## Table of Contents

1. [UI Components](#ui-components)
2. [Network API](#network-api)
3. [Storage API](#storage-api)
4. [Sensor API](#sensor-api)
5. [Device API](#device-api)
6. [Global Functions](#global-functions)

---

## UI Components

### Button

Creates a clickable button.

```javascript
let button = new Button(text)
```

**Methods:**
- `setText(text: string)` - Set button text
- `setBackgroundColor(color: string)` - Set background color (hex format: "#RRGGBB")
- `setTextColor(color: string)` - Set text color
- `setOnTap(callback: function)` - Set click handler

**Example:**
```javascript
let btn = new Button("Click Me");
btn.setBackgroundColor("#007AFF");
btn.setTextColor("#FFFFFF");
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
- `setTextColor(color: string)` - Set text color
- `setTextSize(size: number)` - Set text size in sp

**Example:**
```javascript
let label = new Label("Hello World");
label.setTextSize(24);
label.setTextColor("#000000");
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
- `setOnChange(callback: function)` - Set change handler

**Example:**
```javascript
let input = new TextField("Enter name");
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

### Switch

Toggle switch.

```javascript
let switch = new Switch(text)
```

**Methods:**
- `setOnChange(callback: function)` - Set change handler (receives boolean)

**Example:**
```javascript
let toggle = new Switch("Enable notifications");
toggle.setOnChange(function(isChecked) {
    console.log("Switch: " + isChecked);
});
UI.addView(toggle);
```

---

### Slider

Horizontal slider (0-100).

```javascript
let slider = new Slider()
```

**Methods:**
- `setValue(value: number)` - Set slider value (0-100)
- `setOnChange(callback: function)` - Set change handler (receives value)

**Example:**
```javascript
let slider = new Slider();
slider.setValue(50);
slider.setOnChange(function(value) {
    console.log("Slider value: " + value);
});
UI.addView(slider);
```

---

### ImageView

Displays an image.

```javascript
let imageView = new ImageView()
```

**Example:**
```javascript
let image = new ImageView();
UI.addView(image);
```

---

### ScrollView

Scrollable container.

```javascript
let scrollView = new ScrollView()
```

**Example:**
```javascript
let scroll = new ScrollView();
UI.addView(scroll);
```

---

## UI Namespace

### UI.addView(view)

Add a view to the main container.

```javascript
UI.addView(button);
```

### UI.removeView(viewId)

Remove a view from the container.

```javascript
UI.removeView(viewId);
```

### UI.clearViews()

Remove all views from the container.

```javascript
UI.clearViews();
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

### showToast(message)

Show toast notification.

```javascript
showToast("Hello World");
```

---

### setTimeout(callback, delay)

Execute callback after delay.

**Parameters:**
- `callback` (function) - Function to execute
- `delay` (number) - Delay in milliseconds

```javascript
setTimeout(function() {
    console.log("Delayed execution");
}, 1000);
```

---

### setInterval(callback, interval)

Execute callback repeatedly at interval.

**Parameters:**
- `callback` (function) - Function to execute
- `interval` (number) - Interval in milliseconds

```javascript
setInterval(function() {
    console.log("Repeated execution");
}, 1000);
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
- Maximum memory: 50 MB
- Network timeout: 10 seconds
- File operations limited to app's private storage

---

## Support

For issues and questions:
- GitHub: https://github.com/scripthost/scripthost
- Documentation: https://scripthost.dev/docs
