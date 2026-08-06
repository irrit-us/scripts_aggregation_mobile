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

### Changed
- `ScriptManager` now takes an injectable storage directory (plus a `Context`
  convenience constructor), making it testable on the JVM
- `SignatureVerifier` accepts an injectable public key so sign/verify
  round-trips are consistent
- Unit tests moved into `android/app/src/test/` where Gradle actually runs
  them, replacing placeholder assertions with real behavioral tests
- `run_all_tests.py` no longer hard-codes paths from another machine

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

### Security
- Sandbox isolation for script execution
- Resource limits (30s timeout, 50MB memory)
- Permission system with dangerous permission flags
- RSA-2048 signature verification
- SHA-256 hash verification for local scripts

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
