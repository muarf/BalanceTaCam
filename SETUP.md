# 🚀 Setup Guide for BalanceTaCam

This guide will help you get the application up and running.

## ✅ What's Been Created

This project includes a **complete Android application** with:

### 📱 Application Features
- ✅ Interactive map with osmdroid
- ✅ OAuth 1.0a authentication with OpenStreetMap
- ✅ Display existing cameras from Overpass API
- ✅ Add new cameras (quick & detailed modes)
- ✅ User location tracking
- ✅ Multilingual support (EN, FR, ES, DE)
- ✅ Material 3 UI with Jetpack Compose

### 🏗️ Technical Implementation
- ✅ MVVM + Clean Architecture
- ✅ Dagger Hilt dependency injection
- ✅ Room database for caching
- ✅ Retrofit for API calls
- ✅ Secure OAuth token storage
- ✅ Complete test structure

### 📚 Documentation
- ✅ Comprehensive README
- ✅ API documentation
- ✅ OSM tags guide
- ✅ Contributing guidelines
- ✅ GitHub Actions CI/CD

## 🔧 Next Steps

### 1. Open in Android Studio

```bash
cd /home/maun/osm-android
# Open this directory in Android Studio
```

### 2. Configure OAuth Credentials

**IMPORTANT**: You need to register your app with OpenStreetMap to get OAuth credentials.

#### Steps:
1. Go to https://www.openstreetmap.org
2. Login to your account (or create one)
3. Go to Settings → OAuth 2 applications → Register new application
   - For OAuth 1.0a: https://www.openstreetmap.org/user/{username}/oauth_clients/new
4. Fill in:
   - **Name**: BalanceTaCam (or your choice)
   - **Callback URL**: `osmcamera://oauth`
   - **Permissions**: Check "Modify the map"
5. Click "Register"
6. Copy the **Consumer Key** and **Consumer Secret**

#### Update the code:
Open `app/src/main/java/com/osmcamera/mapper/data/auth/OAuthService.kt`

Replace lines 19-20:
```kotlin
private const val CONSUMER_KEY = "your_consumer_key_here"
private const val CONSUMER_SECRET = "your_consumer_secret_here"
```

### 3. Sync Gradle

In Android Studio:
- Click "Sync Project with Gradle Files" button
- Wait for dependencies to download

### 4. Build the Project

```bash
./gradlew assembleDebug
```

Or in Android Studio: **Build → Make Project**

### 5. Run on Device/Emulator

- Connect an Android device (API 24+) or start an emulator
- Click **Run ▶️** button in Android Studio
- Or: `./gradlew installDebug`

## 📦 Project Structure

```
osm-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/osmcamera/mapper/
│   │   │   │   ├── data/              # Data layer
│   │   │   │   │   ├── api/           # API services
│   │   │   │   │   ├── auth/          # OAuth authentication
│   │   │   │   │   ├── local/         # Room database
│   │   │   │   │   ├── location/      # GPS services
│   │   │   │   │   ├── model/         # Data models
│   │   │   │   │   └── repository/    # Repositories
│   │   │   │   ├── di/                # Dependency injection
│   │   │   │   ├── presentation/      # UI layer
│   │   │   │   │   ├── screens/       # Compose screens
│   │   │   │   │   ├── viewmodel/     # ViewModels
│   │   │   │   │   ├── navigation/    # Navigation
│   │   │   │   │   └── theme/         # Material theme
│   │   │   │   └── OSMCameraApp.kt    # Application class
│   │   │   ├── res/                    # Resources
│   │   │   │   ├── values/             # Strings, colors
│   │   │   │   ├── values-fr/          # French strings
│   │   │   │   ├── values-es/          # Spanish strings
│   │   │   │   └── values-de/          # German strings
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Tests
│   └── build.gradle.kts
├── docs/                               # Documentation
│   ├── API.md
│   └── TAGS.md
├── .github/workflows/                  # CI/CD
│   ├── android-build.yml
│   └── pr-checks.yml
├── README.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── LICENSE
└── build.gradle.kts
```

## 🐛 Troubleshooting

### "Unresolved reference: BuildConfig"
- Build the project once: `./gradlew assembleDebug`
- BuildConfig is auto-generated

### "OAuth not working"
- Check consumer key/secret are correct
- Verify callback URL is `osmcamera://oauth`
- Test on real device (emulators may have issues with browser)

### "Map not loading"
- Check internet connection
- Verify permissions granted
- Check osmdroid tile cache permissions

### "Location not working"
- Enable GPS on device
- Grant location permissions
- Test on real device (emulator GPS may be unreliable)

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Check Code Style
```bash
./gradlew ktlintCheck
```

## 📱 Building Release APK

### Without signing (for testing)
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

### With signing (for distribution)

1. Create a keystore:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

2. Create `keystore.properties` in project root:
```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=my-key-alias
storeFile=../my-release-key.jks
```

3. Build signed APK:
```bash
./gradlew assembleRelease
```

## 🚢 Deployment

### GitHub Release
1. Tag version: `git tag -a v1.0.0 -m "Version 1.0.0"`
2. Push tag: `git push origin v1.0.0`
3. GitHub Actions will automatically build and create release

### F-Droid
Follow [F-Droid inclusion guide](https://f-droid.org/docs/Inclusion_How-To/)

### Google Play
Follow [Google Play publishing guide](https://support.google.com/googleplay/android-developer/answer/9859152)

## 📖 Additional Resources

- [Android Developer Guide](https://developer.android.com/guide)
- [Jetpack Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)
- [OSM API Documentation](https://wiki.openstreetmap.org/wiki/API_v0.6)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

## 🤝 Need Help?

- Check [CONTRIBUTING.md](CONTRIBUTING.md)
- Open an issue on GitHub
- Join OSM community forums

## ⚠️ Important Notes

### OAuth Configuration
**You MUST configure OAuth credentials** before the app can authenticate with OSM. Without this, you cannot add cameras.

### API Servers
- **Development**: Use `https://master.apis.dev.openstreetmap.org/` for testing
- **Production**: Use `https://api.openstreetmap.org/` for real contributions

**Always test on dev server first!**

### Rate Limits
Be respectful of OSM and Overpass API rate limits:
- Don't make excessive API calls
- Cache data locally when possible
- Implement exponential backoff on errors

## 🎉 You're Ready!

The application is complete and ready to use. Just configure OAuth credentials and you're good to go!

For detailed usage instructions, see [README.md](README.md).

Happy mapping! 🗺️📷


