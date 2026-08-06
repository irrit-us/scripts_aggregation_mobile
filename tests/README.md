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

## Best Practices

1. **Isolate Tests**: Each test should be independent
2. **Fast Tests**: Keep unit tests fast (< 1s each)
3. **Deterministic**: Avoid time-dependent assertions
4. **Test Edge Cases**: Blank input, missing signatures, unknown ids
5. **Test Errors**: Verify failure results, not just happy paths
