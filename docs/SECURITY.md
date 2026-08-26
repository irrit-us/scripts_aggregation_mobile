# Security Model

ScriptHost implements a comprehensive security model to protect users while enabling powerful scripting capabilities.

## Architecture

```
┌─────────────────────────────────────────┐
│         User Scripts (Untrusted)        │
├─────────────────────────────────────────┤
│         Sandbox Environment             │
│  - Isolated V8 Context                  │
│  - Resource Limits                      │
│  - API Whitelist                        │
├─────────────────────────────────────────┤
│         Permission Manager              │
│  - Runtime Permission Checks            │
│  - User Consent                         │
├─────────────────────────────────────────┤
│         Bridge Layer                    │
│  - Controlled API Access                │
│  - Input Validation                     │
├─────────────────────────────────────────┤
│         Native System (Trusted)         │
└─────────────────────────────────────────┘
```

## Sandbox Isolation

### JavaScript Engine Isolation

Scripts run in isolated V8 contexts with:

- **Separate Global Scope**: Each script has its own global object
- **No Native Module Access**: Cannot require() native modules
- **Limited Built-ins**: Only safe JavaScript built-ins available
- **No File System Access**: Cannot access fs, os, or other Node.js modules

### Resource Limits

Scripts are constrained by:

- **Execution Time**: Maximum 30 seconds per execution
- **Network Timeout**: 10 seconds for HTTP requests

### API Whitelist

Scripts can only access explicitly exposed APIs:

- UI components (Button, Label, Chart, etc.)
- Network (HTTP GET/POST, with optional custom headers)
- Config (configured API keys and settings, with permission)
- Storage (app's private directory only)
- Sensors (with permission)
- Device info (non-sensitive only)
- Notifications (Notify and Scheduler, with permission)
- SSH (remote command execution, with permission)

## Permission System

### Permission Types

**Non-Dangerous Permissions** (auto-granted):
- INTERNET
- NETWORK_STATE
- READ_STORAGE (app private)
- WRITE_STORAGE (app private)
- ACCELEROMETER
- GYROSCOPE
- VIBRATE
- NOTIFICATIONS
- CONFIG (read configured API keys and settings)
- SSH (connect to remote hosts via SSH)

> On Android 13 (API 33) and above, `NOTIFICATIONS` maps to the
> `POST_NOTIFICATIONS` runtime permission, so the system shows a consent
> dialog the first time a script uses `Notify` or `Scheduler` even though the
> permission is non-dangerous.

**Dangerous Permissions** (require user consent):
- LOCATION_FINE
- LOCATION_COARSE
- CAMERA
- RECORD_AUDIO
- READ_CONTACTS
- WRITE_CONTACTS

### Permission Flow

1. **Declaration**: Script declares required permissions in metadata
2. **Installation**: User reviews permissions before installing
3. **Runtime Check**: Permission checked before each sensitive operation
4. **User Consent**: System prompts user for dangerous permissions
5. **Revocation**: User can revoke permissions at any time

### Permission Enforcement

Enforcement is a double gate: a sensitive operation only proceeds when the
script **declared** the permission in its metadata **and** the system-level
check passes. Every bridge (System, Config, Notification, SSH) is constructed
with the running script's ID and verifies both conditions through
`PermissionManager.hasScriptPermission(scriptId, permission)` before executing:

```kotlin
// Example: Network access check inside a bridge
if (!permissionManager.hasScriptPermission(scriptId, Permission.INTERNET)) {
    throw SecurityException("Permission denied: INTERNET for script $scriptId")
}
```

For permissions backed by an Android runtime permission, the system-level half
of the gate asks the OS as well — for example `NOTIFICATIONS` maps to
`POST_NOTIFICATIONS` on Android 13 (API 33) and above, so the OS consent dialog
must also have been accepted. All sensitive APIs perform these checks before
execution.

### Bridge-Specific Notes

- **SSH**: every `SSH.*` call checks `Permission.SSH` before touching the
  network. SSH credentials are supplied by the script at call time (for
  example via `Config`) and are never persisted by the bridge; the session
  lives only in memory while the script runs.
- **Scheduled notifications**: `Scheduler.scheduleDaily` registers a native
  WorkManager worker (`DailyNotificationWorker`) that posts the stored
  title/message at the requested time. No script code executes in the
  background, so a scheduled notification cannot be used to bypass the
  sandbox or execution limits.

## Configuration Storage & API Keys

Scripts read configured API keys and settings through the `Config` bridge
(`Config.get(key)`, `Config.keys()`), gated behind the `CONFIG` permission.

### Storage Model

- **Location**: `app_config.json` in the app's private files directory
- **Format**: JSON object mapping string keys to encrypted values
  (AES-256-GCM ciphertext, Base64-encoded; see below)
- **Access**: Read-only from scripts; managed from the Settings screen
- **Scope**: Values are per-app, shared by all scripts

### Security Considerations

- Values are encrypted at rest with AES-256-GCM (`AesGcmValueCipher`). The
  encryption key is generated and held by the AndroidKeyStore
  (`KeystoreKeyProvider`), so it never leaves the device's hardware-backed
  keystore. Each value is encrypted with a fresh random 12-byte IV and stored
  as `Base64(IV || ciphertext || GCM tag)`; the GCM tag also detects
  tampering. Values written by older app versions as plaintext are migrated
  transparently: they are read as-is once and re-saved encrypted on the next
  save.
- `CONFIG` is auto-granted (non-dangerous), so the effective control is
  trust at install time: only install scripts whose permission requests you
  have reviewed, and treat a script that requests `CONFIG` as able to read
  all configured keys.
- Values never leave the device unless a script explicitly sends them, for
  example as an `Authorization` header or request body.
- The Settings screen masks values when listing them and never logs them;
  scripts that fail the `CONFIG` permission check get a `SecurityException`.

## Script Verification

### Digital Signatures

Scripts can be signed with RSA-2048 signatures:

1. **Signing**: Developer signs script with private key
2. **Distribution**: Script + signature distributed together
3. **Verification**: App verifies signature with public key
4. **Rejection**: Invalid signatures are rejected

When signature verification is enabled, a valid signature is mandatory:
unsigned scripts are rejected rather than silently accepted. Locally authored
scripts (created or edited in the app) are installed with verification
explicitly disabled.

The app embeds the platform public key used for verification
(SHA-256 fingerprint of the X.509 SPKI encoding:
`05eb3708f86a5919d294403da3135fa361d83842b391f9b90881ba484745f6d6`).
The matching private key is held by the distribution channel, stored outside
the repository, and is never embedded in the app.

### Hash Verification

For local scripts without signatures:

1. **Hash Computation**: SHA-256 hash of script content
2. **Storage**: Hash stored with script metadata
3. **Verification**: Hash checked on each load
4. **Tampering Detection**: Modified scripts detected and rejected

### Signature Format

```json
{
  "name": "MyScript",
  "version": "1.0.0",
  "author": "developer@example.com",
  "sourceCode": "...",
  "signature": "BASE64_ENCODED_RSA_SIGNATURE",
  "permissions": ["INTERNET"]
}
```

## Threat Model

### Threats Addressed

| Threat | Mitigation |
|--------|-----------|
| Malicious code execution | Sandbox isolation, API whitelist |
| Privilege escalation | Permission system, runtime checks |
| Data exfiltration | Network permission, storage isolation |
| Resource exhaustion | Execution timeout |
| Code tampering | Signature verification, hash checks |
| Permission abuse | User consent, dangerous permission flags |
| Script injection | Input validation, CSP-like restrictions |

### Attack Scenarios

**Scenario 1: Malicious Network Access**
- **Attack**: Script tries to send user data to attacker server
- **Defense**: INTERNET permission required, user sees permission request
- **Result**: User can deny permission or uninstall script

**Scenario 2: Infinite Loop**
- **Attack**: Script runs infinite loop to freeze device
- **Defense**: 30-second execution timeout; synchronous execution is
  interruptible between timer callbacks and network responses
- **Result**: Script terminated when the execution timeout is reached

**Scenario 3: Memory Bomb**
- **Attack**: Script allocates excessive memory
- **Defense**: 30-second execution timeout; heap monitoring is not available in
  the bundled J2V8 runtime, so memory pressure is bounded by the timeout
- **Result**: Script terminated when the execution timeout is reached

**Scenario 4: File System Access**
- **Attack**: Script tries to read sensitive files
- **Defense**: Storage API limited to app's private directory
- **Result**: Cannot access files outside sandbox

**Scenario 5: Code Injection**
- **Attack**: Attacker modifies script after installation
- **Defense**: Signature/hash verification on load
- **Result**: Modified script rejected

## Platform Compliance

### Android Security

- **App Sandbox**: Scripts run within app's Linux user ID
- **SELinux**: Enforced by Android OS
- **Permission Model**: Follows Android 6.0+ runtime permissions
- **Network Security**: HTTPS enforced for script downloads
- **Storage Isolation**: Cannot access other apps' data

## Best Practices for Script Authors

### Security Guidelines

1. **Minimize Permissions**: Only request necessary permissions
2. **Validate Input**: Always validate user input
3. **Secure Network**: Use HTTPS for all network requests
4. **Error Handling**: Handle errors gracefully, don't expose internals
5. **Data Privacy**: Don't collect unnecessary user data
6. **Code Review**: Have scripts reviewed before distribution
7. **Version Control**: Use semantic versioning, document changes

### Code Examples

**Bad Practice:**
```javascript
// Don't: Expose sensitive data
let password = input.getValue();
Network.post("http://example.com/log", password, ...);
```

**Good Practice:**
```javascript
// Do: Validate and sanitize input
let username = input.getValue().trim();
if (username.length > 0 && username.length < 50) {
    // Process username
}
```

## Security Auditing

### Audit Checklist

- [ ] All permissions justified and documented
- [ ] Input validation on all user inputs
- [ ] Error messages don't leak sensitive info
- [ ] Network requests use HTTPS
- [ ] No hardcoded credentials
- [ ] Resource usage reasonable
- [ ] Code signed with valid signature
- [ ] Tested in sandbox environment

### Reporting Vulnerabilities

If you discover a security vulnerability:

1. **Do Not** publicly disclose the vulnerability
2. Email security@scripthost.dev with details
3. Include proof-of-concept if possible
4. Allow 90 days for fix before disclosure

## Updates and Patches

### Security Updates

- Critical vulnerabilities patched within 7 days
- High-severity issues patched within 30 days
- Regular security audits conducted quarterly
- Dependency updates monitored continuously

### Version Policy

- Security patches: Patch version (1.0.x)
- Security features: Minor version (1.x.0)
- Breaking security changes: Major version (x.0.0)

## Limitations

### Known Limitations

1. **Side-Channel Attacks**: Timing attacks not fully mitigated
2. **Resource Exhaustion**: Sophisticated attacks may evade limits
3. **Social Engineering**: Cannot prevent user from granting permissions
4. **Zero-Day Exploits**: V8 engine vulnerabilities possible

## Compliance

### Standards

- OWASP Mobile Top 10 compliance
- CWE/SANS Top 25 mitigation
- Android Security Best Practices
- GDPR data protection principles

## References

- [Android Security](https://source.android.com/security)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [V8 Security](https://v8.dev/docs/security)
- [App Sandbox Design](https://developer.android.com/topic/security/app-sandbox)
