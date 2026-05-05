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
- **Memory Limit**: Maximum 50 MB heap size
- **CPU Monitoring**: Automatic termination of runaway scripts
- **Network Timeout**: 10 seconds for HTTP requests

### API Whitelist

Scripts can only access explicitly exposed APIs:

- UI components (Button, Label, etc.)
- Network (HTTP GET/POST only)
- Storage (app's private directory only)
- Sensors (with permission)
- Device info (non-sensitive only)

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

```kotlin
// Example: Network access check
if (!permissionManager.hasPermission(Permission.INTERNET)) {
    throw SecurityException("Permission denied: INTERNET")
}
```

All sensitive APIs perform permission checks before execution.

## Script Verification

### Digital Signatures

Scripts can be signed with RSA-2048 signatures:

1. **Signing**: Developer signs script with private key
2. **Distribution**: Script + signature distributed together
3. **Verification**: App verifies signature with public key
4. **Rejection**: Invalid signatures are rejected

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
| Resource exhaustion | CPU/memory limits, timeouts |
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
- **Defense**: 30-second execution timeout, CPU monitoring
- **Result**: Script automatically terminated

**Scenario 3: Memory Bomb**
- **Attack**: Script allocates excessive memory
- **Defense**: 50 MB heap limit, memory monitoring
- **Result**: Script terminated when limit exceeded

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

### iOS Compliance

For iOS version (future):

- **No Dynamic Code Download**: Scripts bundled with app or user-imported
- **App Store Guidelines**: Complies with 2.5.2 (software requirements)
- **Sandbox**: iOS app sandbox enforced
- **Entitlements**: Only necessary entitlements requested

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

### Future Improvements

- [ ] Process isolation per script
- [ ] Content Security Policy implementation
- [ ] Automated malware scanning
- [ ] Reputation system for script authors
- [ ] Sandboxed rendering for untrusted content
- [ ] Hardware-backed key storage

## Compliance

### Standards

- OWASP Mobile Top 10 compliance
- CWE/SANS Top 25 mitigation
- Android Security Best Practices
- GDPR data protection principles

### Certifications

- Planned: SOC 2 Type II
- Planned: ISO 27001

## References

- [Android Security](https://source.android.com/security)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [V8 Security](https://v8.dev/docs/security)
- [App Sandbox Design](https://developer.android.com/topic/security/app-sandbox)
