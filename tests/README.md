# Testing Guide

Comprehensive testing strategy for ScriptHost.

## Test Structure

```
tests/
├── ScriptManagerTest.kt       # Script management tests
├── SignatureVerifierTest.kt   # Security tests
├── ScriptExecutionTest.kt     # Integration tests
└── README.md                  # This file
```

## Running Tests

### Unit Tests

```bash
cd android
./gradlew test
```

### Instrumented Tests

```bash
cd android
./gradlew connectedAndroidTest
```

### Test Coverage

```bash
cd android
./gradlew jacocoTestReport
```

## Test Categories

### 1. Unit Tests

Test individual components in isolation:

- **ScriptManager**: Installation, uninstallation, search
- **SignatureVerifier**: Signing, verification, hashing
- **PermissionManager**: Permission checks, grants, revocations
- **JavaScriptEngine**: Script execution, sandboxing

### 2. Integration Tests

Test component interactions:

- **Script Execution**: End-to-end script running
- **UI Bridge**: Component creation and events
- **System Bridge**: Network, storage, sensors
- **Permission Flow**: Request and enforcement

### 3. Security Tests

Test security mechanisms:

- **Sandbox Isolation**: Verify scripts cannot escape
- **Permission Enforcement**: Verify unauthorized access blocked
- **Resource Limits**: Verify timeouts and memory limits
- **Signature Verification**: Verify tampered scripts rejected

### 4. Performance Tests

Test performance characteristics:

- **Execution Speed**: Measure script execution time
- **Memory Usage**: Monitor heap allocation
- **Startup Time**: Measure engine initialization
- **Concurrent Scripts**: Test multiple scripts running

## Test Scenarios

### Basic Functionality

```kotlin
@Test
fun testBasicScriptExecution() {
    val script = createTestScript("console.log('test');")
    val result = runBlocking { engine.execute(script) }
    assertTrue(result is ExecutionResult.Success)
}
```

### Permission Checks

```kotlin
@Test
fun testPermissionDenied() {
    val script = createTestScript("""
        Network.get("https://example.com", function(data, error) {
            console.log(data);
        });
    """)
    // Don't grant INTERNET permission
    val result = runBlocking { engine.execute(script) }
    // Should fail or callback with error
}
```

### Resource Limits

```kotlin
@Test
fun testExecutionTimeout() {
    val script = createTestScript("""
        while(true) { }
    """)
    val result = runBlocking { engine.execute(script) }
    assertTrue(result is ExecutionResult.Error)
    assertTrue(result.message.contains("timeout"))
}
```

### UI Components

```kotlin
@Test
fun testButtonCreation() {
    val script = createTestScript("""
        var button = new Button("Test");
        UI.addView(button);
    """)
    val result = runBlocking { engine.execute(script) }
    assertTrue(result is ExecutionResult.Success)
    // Verify button was added to UI
}
```

### Network Operations

```kotlin
@Test
fun testNetworkRequest() {
    val script = createTestScript("""
        Network.get("https://httpbin.org/get", function(data, error) {
            if (!error) {
                console.log("Success");
            }
        });
    """)
    grantPermission(Permission.INTERNET)
    val result = runBlocking { engine.execute(script) }
    // Wait for async callback
    delay(2000)
    // Verify network request succeeded
}
```

### Storage Operations

```kotlin
@Test
fun testFileStorage() {
    val script = createTestScript("""
        Storage.writeFile("test.txt", "content");
        var data = Storage.readFile("test.txt");
        console.log(data);
    """)
    grantPermission(Permission.WRITE_STORAGE)
    grantPermission(Permission.READ_STORAGE)
    val result = runBlocking { engine.execute(script) }
    assertTrue(result is ExecutionResult.Success)
}
```

## Mocking

### Mock Context

```kotlin
@Mock
lateinit var context: Context

@Before
fun setup() {
    MockitoAnnotations.openMocks(this)
    `when`(context.filesDir).thenReturn(File("/tmp/test"))
}
```

### Mock Permissions

```kotlin
class MockPermissionManager : PermissionManager {
    private val granted = mutableSetOf<Permission>()

    fun grant(permission: Permission) {
        granted.add(permission)
    }

    override fun hasPermission(permission: Permission): Boolean {
        return granted.contains(permission)
    }
}
```

### Mock Network

```kotlin
class MockNetworkBridge : ScriptBridge {
    var lastRequest: String? = null

    override fun register(runtime: V8) {
        // Register mock network methods
    }

    fun simulateResponse(data: String) {
        // Trigger callback with mock data
    }
}
```

## Test Data

### Sample Scripts

```kotlin
object TestScripts {
    val HELLO_WORLD = """
        console.log("Hello World");
    """.trimIndent()

    val BUTTON_CLICK = """
        var button = new Button("Click");
        button.setOnTap(function() {
            console.log("Clicked");
        });
        UI.addView(button);
    """.trimIndent()

    val NETWORK_REQUEST = """
        Network.get("https://api.example.com/data", function(data, error) {
            if (error) {
                console.error(error);
            } else {
                console.log(data);
            }
        });
    """.trimIndent()
}
```

### Sample Permissions

```kotlin
object TestPermissions {
    val BASIC = listOf(Permission.INTERNET)
    val DANGEROUS = listOf(Permission.CAMERA, Permission.LOCATION_FINE)
    val ALL = Permission.values().toList()
}
```

## Continuous Integration

### GitHub Actions

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run tests
        run: ./gradlew test
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

## Code Coverage

Target coverage levels:

- **Overall**: 80%+
- **Core Engine**: 90%+
- **Security**: 95%+
- **UI Bridge**: 70%+

## Manual Testing

### Test Checklist

- [ ] Install script from file
- [ ] Run script with UI components
- [ ] Test permission requests
- [ ] Test network operations
- [ ] Test storage operations
- [ ] Test sensor access
- [ ] Test script timeout
- [ ] Test memory limits
- [ ] Test signature verification
- [ ] Test script updates
- [ ] Test script uninstallation
- [ ] Test error handling
- [ ] Test multiple scripts
- [ ] Test script categories
- [ ] Test search functionality

### Device Testing

Test on:

- Android 7.0 (API 24) - Minimum supported
- Android 10.0 (API 29) - Common version
- Android 13.0 (API 33) - Latest stable
- Android 14.0 (API 34) - Current target

Test on:

- Low-end device (2GB RAM)
- Mid-range device (4GB RAM)
- High-end device (8GB+ RAM)

## Debugging Tests

### Enable Verbose Logging

```kotlin
@Before
fun setup() {
    Log.setLevel(Log.VERBOSE)
}
```

### Capture Console Output

```kotlin
val consoleOutput = mutableListOf<String>()

engine.setConsoleLogger { message ->
    consoleOutput.add(message)
}
```

### Breakpoint Debugging

Use Android Studio debugger:

1. Set breakpoint in test
2. Run test in debug mode
3. Step through execution
4. Inspect variables

## Performance Benchmarks

### Execution Speed

```kotlin
@Test
fun benchmarkExecutionSpeed() {
    val script = createTestScript("var x = 0; for(var i = 0; i < 1000; i++) x += i;")
    val startTime = System.currentTimeMillis()
    runBlocking { engine.execute(script) }
    val duration = System.currentTimeMillis() - startTime
    assertTrue(duration < 100) // Should complete in < 100ms
}
```

### Memory Usage

```kotlin
@Test
fun benchmarkMemoryUsage() {
    val runtime = Runtime.getRuntime()
    val before = runtime.totalMemory() - runtime.freeMemory()

    val script = createTestScript("var arr = new Array(10000);")
    runBlocking { engine.execute(script) }

    val after = runtime.totalMemory() - runtime.freeMemory()
    val used = after - before

    assertTrue(used < 10 * 1024 * 1024) // Should use < 10MB
}
```

## Best Practices

1. **Isolate Tests**: Each test should be independent
2. **Clean Up**: Release resources after tests
3. **Use Mocks**: Mock external dependencies
4. **Test Edge Cases**: Test boundary conditions
5. **Test Errors**: Verify error handling
6. **Document Tests**: Add comments explaining test purpose
7. **Fast Tests**: Keep unit tests fast (< 1s each)
8. **Deterministic**: Tests should always produce same result

## Troubleshooting

### Test Failures

1. Check test logs for error messages
2. Verify test environment setup
3. Check for race conditions in async tests
4. Verify mocks are configured correctly
5. Run tests individually to isolate issues

### Flaky Tests

1. Add delays for async operations
2. Use proper synchronization
3. Avoid time-dependent assertions
4. Mock external dependencies
5. Increase timeouts if needed

## Resources

- [JUnit Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)
- [Android Testing Guide](https://developer.android.com/training/testing)
- [Kotlin Coroutines Testing](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
