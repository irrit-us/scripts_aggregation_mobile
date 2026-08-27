# SAM - Mobile Script Aggregation Platform

SAM (**s**cripts **a**ggregation **m**obile, formerly ScriptHost) is the Android app in this repository.

A powerful mobile script host environment that enables users to write and install scripts to control native UI and system functionality.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Application Host                      │
├─────────────────────────────────────────────────────────┤
│  Native UI Layer (Android)                              │
│  ├─ Button, Label, ListView, TextField, etc.            │
│  └─ Layout Management (Flexbox-like)                    │
├─────────────────────────────────────────────────────────┤
│  Bridge Layer                                            │
│  ├─ UI Bridge (Component Creation & Events)             │
│  ├─ System Bridge (Network, Storage, Sensors)           │
│  ├─ Config Bridge (API Keys & Settings)                 │
│  └─ Permission Manager                                   │
├─────────────────────────────────────────────────────────┤
│  Script Runtime Engine                                   │
│  ├─ JavaScript Engine (J2V8)                            │
│  ├─ Sandbox Environment                                  │
│  └─ Runtime Monitor (Execution Timeout)                 │
├─────────────────────────────────────────────────────────┤
│  Script Management                                       │
│  ├─ Installation & Updates                              │
│  ├─ Signature Verification                              │
│  └─ Local Repository                                     │
└─────────────────────────────────────────────────────────┘
```

## Features

- **Multi-language Script Support**: JavaScript (primary), with extensible architecture for Lua/Python
- **Native UI Components**: 13 configurable widgets (Button, Label, TextField, ListView, ImageView, Switch, Slider, ScrollView, CheckBox, Spinner, ProgressBar, Layout, Chart) with common styling, layout containers, dialog helpers, and sub-screen navigation via the `UI.pushPage`/`UI.popPage` page stack
- **Secure Sandbox**: Isolated script execution with permission management
- **Script Management**: Install, update, and manage scripts locally
- **Discord-Style Drawer UI**: Left drawer holds the script list; scripts run in the content area beside it under one slim in-app header (☰ + title, no system ActionBar)
- **Configuration Interface**: Manage API keys and settings from the Settings screen, plus app-level options (debug mode, light/dark appearance)
- **Custom API Calls**: HTTP GET/POST with custom headers for authenticated APIs
- **Wrapped Agent Conversations**: Example agent-chat script for OpenAI-compatible endpoints
- **Server Monitoring**: Example script that polls and displays server health status
- **Developer Tools**: Debugging support, logging, and comprehensive API documentation

## Project Structure

```
scripts_aggregation_mobile/
├── android/                 # Android app (single Gradle module)
│   └── app/
│       └── src/
│           ├── main/java/com/scripthost/
│           │   ├── bridge/      # Script-facing bridges (UI, System, Config, Notify, SSH)
│           │   ├── config/      # Encrypted key/value configuration store
│           │   ├── engine/      # J2V8 script engine & script manager
│           │   ├── models/      # Data models
│           │   ├── notify/      # Scheduled-notification worker
│           │   ├── security/    # Permissions & signature verification
│           │   ├── ssh/         # SSH session management
│           │   ├── ui/          # Activities & chart view
│           │   └── util/        # Logging
│           └── test/            # JUnit + Robolectric unit tests
├── scripts/
│   └── examples/            # Example scripts
├── docs/                    # Documentation
│   ├── API.md               # Complete API reference
│   ├── SECURITY.md          # Security model
│   ├── EXAMPLES.md          # Example script patterns
│   ├── BUILD.md             # Build instructions
│   ├── QUICKSTART.md        # 5-minute quick start
│   ├── CHANGELOG.md         # Version history
│   └── CONTRIBUTING.md      # Contribution guidelines
└── tests/                   # Test suites
```

## Technology Stack

- **Language**: Kotlin
- **Script Engine**: J2V8 (V8 JavaScript engine)
- **UI**: Native Android Views
- **Background Scheduling**: WorkManager
- **SSH**: JSch (maintained fork)
- **Testing**: JUnit 4, Robolectric, Truth, mockito-kotlin, WorkManager Test
- **Build**: Gradle

## Security Model

1. **Sandbox Isolation**: Scripts run in isolated contexts with limited API access
2. **Permission System**: Explicit permission declarations and runtime checks
3. **Signature Verification**: All scripts must be signed and verified
4. **Runtime Monitoring**: Execution time limits and network timeouts
5. **Platform Compliance**: No dynamic code download from servers

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11+
- Kotlin 1.9+

### Build Instructions

#### Android
```bash
cd android
./gradlew assembleDebug
```

#### Run Unit Tests (Android)
```bash
cd android
./gradlew testDebugUnitTest
```

This runs both the plain JUnit and the Robolectric suites on the JVM — no
emulator or device required. The first run needs network access so Robolectric
can download its `android-all` jars. Note that J2V8 loads a native `.so`, so
JVM/Robolectric tests cannot construct a V8 runtime; bridges are tested via
direct method calls rather than through `register()`/`unregister()`.

## Script API Example

```javascript
// Create a button
let button = new Button("Click Me");
button.backgroundColor = "#007AFF";
button.textColor = "#FFFFFF";

// Handle tap event
button.onTap = function() {
    console.log("Button tapped!");
    showAlert("Hello", "Button was clicked");
};

// Add to main view
UI.addView(button);

// Create a list
let list = new ListView();
list.items = ["Item 1", "Item 2", "Item 3"];
list.onItemTap = function(index) {
    console.log("Tapped item: " + index);
};

UI.addView(list);
```

## Configuration

API keys and other settings are managed from the **设置** (Settings) screen
(opened from the button at the bottom of the drawer). Entries are stored as key/value pairs in the
app's private storage and are readable by scripts that declare the `CONFIG`
permission:

```javascript
// Read an API key configured in Settings
let apiKey = Config.get("OPENAI_API_KEY");
```

Example uses:

- `OPENAI_API_KEY` / `OPENAI_API_BASE` - wrapped agent conversations
  (`scripts/examples/agent_conversation.js`)
- `MONITOR_URL` / `MONITOR_API_KEY` / `MONITOR_THRESHOLD` - server monitoring
  and rolling port chart (`scripts/examples/server_monitor.js`,
  `scripts/examples/monitor_port_chart.js`)
- `STOCK_API_URL` / `STOCK_API_KEY` - stock trend line charts
  (`scripts/examples/stock_trends.js`)
- `TMUX_HOST` / `TMUX_PORT` / `TMUX_USER` / `TMUX_PASSWORD` - remote tmux
  console over SSH (`scripts/examples/tmux_remote.js`)

Other examples showcasing the newer bridges:

- `scripts/examples/daily_fitness.js` - daily scheduled reminder via
  `Scheduler.scheduleDaily` plus instant previews via `Notify.post`

Custom HTTP requests can attach configured keys as headers:

```javascript
let headers = { "Authorization": "Bearer " + Config.get("OPENAI_API_KEY") };
Network.post(url, headers, JSON.stringify({ query: "hello" }), function(data, error) {
    if (error) { console.error(error); } else { console.log(data); }
});
```

> Note: `CONFIG` is a non-dangerous permission that is auto-granted to any
> installed script that declares it. Only install scripts you trust. See
> `docs/SECURITY.md` for details.

## License

MIT License - See LICENSE file for details

## Contributing

Contributions welcome! Please read [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) for guidelines.
