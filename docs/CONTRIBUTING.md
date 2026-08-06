# Contributing to ScriptHost

Thank you for your interest in contributing to ScriptHost! This document provides guidelines for contributing to the project.

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Respect differing viewpoints and experiences

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in Issues
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots if applicable
   - Device/OS information
   - Script code that triggers the bug

### Suggesting Features

1. Check if the feature has been suggested
2. Create a new issue with:
   - Clear description of the feature
   - Use cases and benefits
   - Possible implementation approach
   - Any potential drawbacks

### Contributing Code

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**
   - Follow the coding style
   - Add tests for new functionality
   - Update documentation
   - Ensure all tests pass

4. **Commit your changes**
   ```bash
   git commit -m "Add feature: description"
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request**
   - Describe your changes
   - Reference related issues
   - Include screenshots for UI changes

## Development Setup

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11+
- Kotlin 1.9+
- Git

### Setup Steps

1. Clone the repository
   ```bash
   git clone https://github.com/scripthost/scripthost.git
   cd scripthost
   ```

2. Open in Android Studio
   ```bash
   studio android/
   ```

3. Sync Gradle dependencies

4. Run the app
   ```bash
   ./gradlew installDebug
   ```

## Coding Standards

### Kotlin Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable names

### Code Organization

```kotlin
// 1. Package declaration
package com.scripthost.feature

// 2. Imports (grouped and sorted)
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.scripthost.models.Script

// 3. Class documentation
/**
 * Description of the class
 */
class MyClass {
    // 4. Companion object
    companion object {
        const val TAG = "MyClass"
    }

    // 5. Properties
    private val property: String = ""

    // 6. Init blocks
    init {
        // Initialization
    }

    // 7. Public methods
    fun publicMethod() {
        // Implementation
    }

    // 8. Private methods
    private fun privateMethod() {
        // Implementation
    }
}
```

### Documentation

- Add KDoc comments for public APIs
- Explain complex logic with inline comments
- Update README.md for significant changes
- Add examples for new features

### Testing

- Write unit tests for new functionality
- Maintain test coverage above 80%
- Test edge cases and error conditions
- Use descriptive test names

```kotlin
@Test
fun `should return error when script has invalid syntax`() {
    // Test implementation
}
```

## Pull Request Guidelines

### PR Title

Use conventional commit format:

- `feat: Add new feature`
- `fix: Fix bug in script execution`
- `docs: Update API documentation`
- `test: Add tests for permission manager`
- `refactor: Improve code structure`
- `perf: Optimize script loading`
- `chore: Update dependencies`

### PR Description

Include:

- **What**: What changes were made
- **Why**: Why these changes were necessary
- **How**: How the changes were implemented
- **Testing**: How the changes were tested
- **Screenshots**: For UI changes

### Review Process

1. Automated checks must pass (build, tests, lint)
2. At least one maintainer approval required
3. Address review feedback
4. Squash commits before merging

## Project Structure

```
scripts_aggregation_mobile/
├── android/              # Android app
│   ├── app/
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/com/scripthost/
│   │       │   │   ├── bridge/      # Native bridges
│   │       │   │   ├── engine/      # Script engine
│   │       │   │   ├── security/    # Security
│   │       │   │   ├── ui/          # UI activities
│   │       │   │   └── models/      # Data models
│   │       │   └── res/             # Resources
│   │       └── test/                # Unit tests
│   └── build.gradle.kts
├── scripts/             # Example scripts
├── docs/                # Documentation
└── tests/               # Test suites
```

## Areas for Contribution

### High Priority

- [ ] iOS implementation
- [ ] Script marketplace backend
- [ ] Additional UI components
- [ ] More example scripts
- [ ] Performance optimizations

### Medium Priority

- [ ] Lua script support
- [ ] Python script support
- [ ] Script debugging tools
- [ ] IDE plugins
- [ ] Internationalization

### Low Priority

- [ ] Theme customization
- [ ] Script templates
- [ ] Community features
- [ ] Analytics dashboard

## Getting Help

- **Documentation**: Check docs/ directory
- **Issues**: Search existing issues
- **Discussions**: Use GitHub Discussions
- **Chat**: Join our Discord server

## Recognition

Contributors will be:

- Listed in CONTRIBUTORS.md
- Mentioned in release notes
- Credited in the app's About section

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Questions?

Feel free to ask questions by:

- Opening an issue
- Starting a discussion
- Contacting maintainers

Thank you for contributing to ScriptHost!
