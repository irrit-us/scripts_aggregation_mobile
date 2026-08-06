# ScriptHost - Mobile Script Aggregation Platform

A powerful mobile script host environment that enables users to write and install scripts to control native UI and system functionality.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Application Host                      │
├─────────────────────────────────────────────────────────┤
│  Native UI Layer (Android/iOS)                          │
│  ├─ Button, Label, ListView, TextField, etc.            │
│  └─ Layout Management (Flexbox-like)                    │
├─────────────────────────────────────────────────────────┤
│  Bridge Layer                                            │
│  ├─ UI Bridge (Component Creation & Events)             │
│  ├─ System Bridge (Network, Storage, Sensors)           │
│  └─ Permission Manager                                   │
├─────────────────────────────────────────────────────────┤
│  Script Runtime Engine                                   │
│  ├─ JavaScript Engine (JavaScriptCore/J2V8)             │
│  ├─ Sandbox Environment                                  │
│  └─ Runtime Monitor (Memory, CPU, Timeout)              │
├─────────────────────────────────────────────────────────┤
│  Script Management                                       │
│  ├─ Installation & Updates                              │
│  ├─ Signature Verification                              │
│  └─ Local Repository                                     │
└─────────────────────────────────────────────────────────┘
```

## Features

- **Multi-language Script Support**: JavaScript (primary), with extensible architecture for Lua/Python
- **Native UI Components**: Full access to native UI widgets with event handling
- **Secure Sandbox**: Isolated script execution with permission management
- **Script Management**: Install, update, and manage scripts locally
- **Cross-platform**: Shared core logic with platform-specific UI implementations
- **Developer Tools**: Debugging support, logging, and comprehensive API documentation

## Project Structure

```
scripts_aggregation_mobile/
├── android/                 # Android native implementation
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/scripthost/
│   │       │   ├── bridge/      # Native bridge implementations
│   │       │   ├── engine/      # Script engine integration
│   │       │   ├── security/    # Permission & sandbox
│   │       │   └── ui/          # UI components
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── ios/                     # iOS native implementation
│   ├── ScriptHost/
│   │   ├── Bridge/          # Native bridge implementations
│   │   ├── Engine/          # Script engine integration
│   │   ├── Security/        # Permission & sandbox
│   │   └── UI/              # UI components
│   └── ScriptHost.xcodeproj
├── shared/                  # Kotlin Multiplatform shared code
│   └── src/
│       ├── commonMain/
│       │   └── kotlin/com/scripthost/
│       │       ├── core/        # Core abstractions
│       │       ├── models/      # Data models
│       │       └── utils/       # Utilities
│       ├── androidMain/
│       └── iosMain/
├── scripts/                 # Example scripts
│   └── examples/
├── docs/                    # Documentation
│   ├── API.md
│   ├── SECURITY.md
│   └── EXAMPLES.md
└── tests/                   # Test suites
```

## Technology Stack

### Android
- **Language**: Kotlin
- **Script Engine**: J2V8 (V8 JavaScript engine)
- **UI**: Native Android Views
- **Build**: Gradle

### iOS
- **Language**: Swift
- **Script Engine**: JavaScriptCore (built-in)
- **UI**: UIKit
- **Build**: Xcode

### Shared
- **Framework**: Kotlin Multiplatform
- **Core Logic**: Script management, security models, data structures

## Security Model

1. **Sandbox Isolation**: Scripts run in isolated contexts with limited API access
2. **Permission System**: Explicit permission declarations and runtime checks
3. **Signature Verification**: All scripts must be signed and verified
4. **Runtime Monitoring**: CPU, memory, and execution time limits
5. **Platform Compliance**: iOS-compliant (no dynamic code download from servers)

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Xcode 13 or later
- JDK 11+
- Kotlin 1.9+

### Build Instructions

#### Android
```bash
cd android
./gradlew assembleDebug
```

#### iOS
```bash
cd ios
xcodebuild -scheme ScriptHost -configuration Debug
```

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

## Development Roadmap

- [x] Phase 1: Architecture design and project setup
- [ ] Phase 2: Core script engine integration
- [ ] Phase 3: Native UI bridge layer
- [ ] Phase 4: Security and permission system
- [ ] Phase 5: Script management module
- [ ] Phase 6: Testing and debugging tools
- [ ] Phase 7: Documentation and examples

## License

MIT License - See LICENSE file for details

## Contributing

Contributions welcome! Please read CONTRIBUTING.md for guidelines.
