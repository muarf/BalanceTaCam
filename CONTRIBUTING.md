# Contributing to BalanceTaCam

First off, thank you for considering contributing to BalanceTaCam! 🎉

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Translation](#translation)

## 📜 Code of Conduct

This project follows the [OpenStreetMap Code of Conduct](https://wiki.openstreetmap.org/wiki/Good_practice). By participating, you are expected to uphold this code.

## 🤝 How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check existing issues. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce**
- **Expected behavior**
- **Actual behavior**
- **Screenshots** (if applicable)
- **Device information** (Android version, device model)
- **App version**

### Suggesting Features

Feature requests are welcome! Please provide:

- **Clear use case**
- **Expected behavior**
- **Why this feature would be useful**
- **Mockups or examples** (if applicable)

### Code Contributions

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

## 🛠️ Development Setup

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17
- **Android SDK**: Level 34
- **Kotlin**: 1.9.22

### Setup Steps

1. Clone the repository:
```bash
git clone https://github.com/muarf/BalanceTaCam.git
cd BalanceTaCam
```

2. Open in Android Studio

3. Configure OAuth credentials (see README.md)

4. Sync Gradle and build

### Running Tests

```bash
./gradlew test                 # Unit tests
./gradlew connectedAndroidTest # Instrumented tests
```

### Building

```bash
./gradlew assembleDebug       # Debug APK
./gradlew assembleRelease     # Release APK
```

## 📝 Coding Standards

### Kotlin Style

Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

Key points:
- Use 4 spaces for indentation
- Use camelCase for function/variable names
- Use PascalCase for class names
- Keep line length under 120 characters
- Use meaningful variable names

### Architecture

This project follows **MVVM + Clean Architecture**:

```
presentation/
├── screens/        # UI screens (Composables)
├── viewmodel/      # ViewModels
├── navigation/     # Navigation logic
└── theme/          # Compose theme

domain/
└── usecase/        # Business logic (if needed)

data/
├── model/          # Data models
├── api/            # API services
├── repository/     # Repositories
├── local/          # Local database
└── auth/           # Authentication
```

### Compose Guidelines

- Keep composables small and focused
- Extract reusable components
- Use `remember` and `rememberSaveable` appropriately
- Prefer stateless composables
- Use `ViewModel` for state management

### Comments

- Write self-documenting code
- Add KDoc for public APIs
- Explain *why*, not *what*

Example:
```kotlin
/**
 * Creates a camera on OpenStreetMap
 * 
 * @param cameraData Camera information to upload
 * @return Result with node ID on success
 */
suspend fun createCamera(cameraData: CameraFormData): Result<String>
```

## 💬 Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add detailed mode for camera submission
fix: correct OAuth redirect handling
docs: update README with setup instructions
style: format code according to Kotlin conventions
refactor: simplify camera repository logic
test: add unit tests for CameraViewModel
chore: update dependencies
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Maintenance tasks

## 🔄 Pull Request Process

1. **Update documentation** if needed
2. **Add tests** for new features
3. **Ensure all tests pass**
4. **Update CHANGELOG.md**
5. **Request review** from maintainers

### PR Checklist

- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Commented complex code
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests added/updated
- [ ] All tests passing
- [ ] No breaking changes (or documented)

## 🌍 Translation

We welcome translations! The app currently supports:
- English (en)
- French (fr)
- Spanish (es)
- German (de)

### Adding a New Language

1. Create new strings file: `app/src/main/res/values-{lang}/strings.xml`
2. Copy all strings from `values/strings.xml`
3. Translate all values (keep keys unchanged)
4. Update `defaultConfig` in `app/build.gradle.kts`:
```kotlin
resourceConfigurations.addAll(listOf("en", "fr", "es", "de", "your_lang"))
```
5. Submit a PR

### Translation Guidelines

- Keep placeholders intact (`%s`, `%d`)
- Maintain string length (for UI layout)
- Use appropriate formality level
- Test the UI after translation

## 🧪 Testing Guidelines

### Unit Tests

- Test ViewModels logic
- Test data transformations
- Test validation logic
- Use MockK for mocking

### UI Tests

- Test navigation flows
- Test form validation
- Test critical user paths

## 🐛 Debugging Tips

### Enable Debug Logging

In `OkHttpClient` (AppModule.kt):
```kotlin
level = HttpLoggingInterceptor.Level.BODY  // See full requests/responses
```

### OSM API Testing

Use the development API for testing:
```kotlin
const val BASE_URL = "https://master.apis.dev.openstreetmap.org/"
```

**Important**: Never test on production OSM! Always use the dev server.

## 📦 Release Process

For maintainers:

1. Update version in `app/build.gradle.kts`
2. Update CHANGELOG.md
3. Create git tag: `git tag -a v1.0.0 -m "Version 1.0.0"`
4. Push tag: `git push origin v1.0.0`
5. GitHub Actions will build and create release

## ❓ Questions?

- Open an issue for general questions
- Check existing issues and discussions
- Join the OSM forum for mapping-related questions

## 🙏 Thank You!

Your contributions make this project better for everyone. Thank you for being part of the open source and OpenStreetMap communities!


