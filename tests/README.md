# Testing Guide

Testing strategy for ScriptHost. Unit tests live in the Android module where
Gradle actually compiles and runs them (`android/app/src/test/`).

## Test Structure

```
android/app/src/test/java/com/scripthost/
├── engine/
│   └── ScriptManagerTest.kt       # Script management tests
└── security/
    └── SignatureVerifierTest.kt   # Signing/verification tests
```

## Running Tests

### Local Unit Tests

```bash
cd android
./gradlew testDebugUnitTest
# or the full suite:
./gradlew test
```

Reports are written to `android/app/build/reports/tests/`.

### Instrumented Tests

```bash
cd android
./gradlew connectedAndroidTest
```

Requires a connected device or emulator.

## Test Categories

### 1. Unit Tests

Test individual components in isolation (no Android device needed):

- **ScriptManager**: Installation, validation, uninstallation, update, search,
  export/import round-trip, metadata persistence across instances
- **SignatureVerifier**: Sign/verify round-trip, tamper detection, key mismatch
  rejection, hash stability

### 2. Integration Tests (device/instrumented)

Covered manually or via instrumented tests on a device:

- **Script Execution**: End-to-end script running (J2V8 requires native libs)
- **UI Bridge**: Component creation and events
- **System Bridge**: Network, storage, sensors
- **Permission Flow**: Request and enforcement

## Adding Tests

1. Add the test class under `android/app/src/test/java/com/scripthost/`
2. Use `TemporaryFolder` instead of Android `Context` where possible; the
   domain classes accept injected storage directories for this reason
3. Run `./gradlew testDebugUnitTest` and iterate

## On-Device API Coverage (`tests/api_coverage.js`)

`api_coverage.js` is a script (not a JVM test) that exercises the full
script-facing API on a real device and prints machine-readable markers:

```
APITEST|PASS|<name>
APITEST|FAIL|<name>|<reason>
APITEST|DONE
```

How to run it:

1. Import `tests/api_coverage.js` into SAM like any other script
   (drawer -> add script -> pick the file).
2. Run it and grant every permission when prompted (storage, notifications,
   sensors, internet) so the permission-gated checks can pass.
3. Read the markers in the on-screen console, or in logcat (tag
   `ScriptConsole`) when debug mode is enabled in Settings.
4. The run is complete when `APITEST|DONE` appears. Timers and network
   checks are asynchronous, so their markers arrive a few hundred
   milliseconds after the synchronous ones.

The Network checks assume a local HTTP server at `http://127.0.0.1:8080/`
(any server answering GET and POST — e.g. `python -m http.server 8080` via
Termux on the device, or `adb reverse tcp:8080 tcp:8080` to a host machine).
Without one, only the three `Network.*` markers report FAIL.

Coverage: all 14 view types with their specific methods, common view
methods, text methods (including `setStrikeThrough`), the `UI` namespace
(`pushPage`/`popPage`/`pageDepth`/`removeView`), Storage CRUD + `listFiles`,
all four timer functions, `Network.get/post` with and without headers,
sensors (register + stop), all read-only `Device` getters, `Config`,
Markdown, console levels, `showToast`, `Notify`, and `Scheduler`.

## Best Practices

1. **Isolate Tests**: Each test should be independent
2. **Fast Tests**: Keep unit tests fast (< 1s each)
3. **Deterministic**: Avoid time-dependent assertions
4. **Test Edge Cases**: Blank input, missing signatures, unknown ids
5. **Test Errors**: Verify failure results, not just happy paths
