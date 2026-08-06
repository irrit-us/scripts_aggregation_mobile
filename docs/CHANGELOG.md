# Changelog

All notable changes to ScriptHost will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Missing Gradle wrapper; `./gradlew` now works as documented
- Build configuration for AGP 8.2: enabled `android.useAndroidX` and removed
  legacy repository declarations that conflicted with the settings plugin
  management (previously blocked all builds)
- J2V8 API mismatches: `executeVoidFunction` replaced with the actual
  `V8Function.call` API, and heap-statistics monitoring removed (not exposed
  by J2V8 6.2.1)
- Embedded platform public key was not a valid RSA key; replaced with a real
  generated RSA-2048 key
- Launcher icons referenced by the manifest but never shipped (build failure)
- Missing `VIBRATE` permission while `Device.vibrate()` requires it
- Signature verification was silently skipped for unsigned scripts when
  verification was enabled; it is now enforced
- Timer IDs always returned `0` and `clearTimeout`/`clearInterval` were missing
- UI bridge touched views from background threads; view creation and mutation
  now happen on the main thread
- V8 callback/object access is marshaled to the main thread; J2V8 enforces
  thread affinity and previously threw on foreign-thread access
- Sensor callbacks fired on background threads; events are now posted to the
  main thread, and `unregister()` stops sensor listeners
- Nullable runtime references could crash bridges; guarded with `?: return`
  and `?: null` fallbacks
- `Network.get`/`Network.post` treated only HTTP 200 as success; non-2xx
  responses are now surfaced as errors
- Stale claims about a 50MB memory limit removed (heap statistics are not
  exposed by J2V8 6.2.1; only the 30-second execution timeout is enforced)

### Changed
- `ScriptManager` now takes an injectable storage directory (plus a `Context`
  convenience constructor), making it testable on the JVM
- `SignatureVerifier` accepts an injectable public key so sign/verify
  round-trips are consistent
- Unit tests moved into `android/app/src/test/` where Gradle actually runs
  them, replacing placeholder assertions with real behavioral tests
- `run_all_tests.py` no longer hard-codes paths from another machine
- `JavaScriptEngine.execute()` now runs on the main thread to satisfy J2V8
  runtime thread affinity
- All Development Roadmap phases (1-7) in README.md completed and ticked
- Documentation synced with the new features (README, docs/API.md,
  docs/EXAMPLES.md, docs/SECURITY.md, docs/QUICKSTART.md, docs/STATUS.md,
  docs/PROJECT_SUMMARY.md)
- Documentation reorganized under `docs/` per standard conventions;
  `README.md` and `LICENSE` remain at the repository root

### Added
- Initial project structure
- JavaScript engine integration with J2V8
- Native UI bridge layer with components (Button, Label, TextField, ListView, Switch, Slider)
- System bridge for network, storage, and sensors
- Permission management system
- Script signature verification
- Script manager for installation and updates
- Example scripts (Hello World, Counter, Todo List, Network Request, Sensors, Storage)
- Comprehensive API documentation
- Security documentation
- Testing framework with unit and integration tests
- Android app with MainActivity, ScriptEditorActivity, and ScriptRuntimeActivity
- Configuration interface: Settings screen to add, edit, and delete API keys
  and settings
- `ConfigStore` (JSON-backed key/value store in app-private storage)
- `Config` bridge for scripts: `Config.get(key)` and `Config.keys()`, gated
  behind the `CONFIG` permission
- Header overloads for `Network.get(url, headers, callback)` and
  `Network.post(url, headers, body, callback)` for authenticated API calls
- Example scripts: `agent_conversation.js` (wrapped agent conversation) and
  `server_monitor.js` (server health monitoring)
- `ConfigStoreTest` unit tests (8 tests)

### Security
- Sandbox isolation for script execution
- Resource limits (30s execution timeout)
- Permission system with dangerous permission flags
- RSA-2048 signature verification
- SHA-256 hash verification for local scripts
- `CONFIG` permission (auto-granted, read-only access to configured API keys
  and settings)
- `docs/SECURITY.md` documents the Config storage model, API-key handling,
  and updated permission/API whitelist

## [1.0.0] - TBD

### Planned
- First stable release
- iOS implementation
- Script marketplace
- Additional UI components
- Performance optimizations
- Internationalization

## Version History

### Version Numbering

- **Major version** (X.0.0): Breaking changes, major features
- **Minor version** (0.X.0): New features, backward compatible
- **Patch version** (0.0.X): Bug fixes, security patches

### Release Schedule

- **Major releases**: Annually
- **Minor releases**: Quarterly
- **Patch releases**: As needed for critical bugs/security

### Support Policy

- **Current major version**: Full support
- **Previous major version**: Security updates for 1 year
- **Older versions**: No support

## Migration Guides

### Upgrading from 0.x to 1.0

TBD

## Contributors

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for a list of contributors.

## Links

- [GitHub Repository](https://github.com/scripthost/scripthost)
- [Documentation](https://scripthost.dev/docs)
- [Issue Tracker](https://github.com/scripthost/scripthost/issues)
- [Release Notes](https://github.com/scripthost/scripthost/releases)
