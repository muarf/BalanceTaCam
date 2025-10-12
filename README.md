# BalanceTaCam 📷🗺️

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)

Application Android open source pour contribuer les emplacements des caméras de surveillance sur OpenStreetMap.

## 🎯 Features

- **🗺️ Interactive Map**: View existing surveillance cameras on an OSM-based map
- **📍 Add Cameras**: Contribute new camera locations with detailed information
- **🔐 OSM Authentication**: Secure OAuth 1.0a login with OpenStreetMap
- **⚡ Quick & Detailed Modes**: Choose between rapid contribution or comprehensive tagging
- **🌍 Multilingual**: Supports English, French, Spanish, and German
- **📱 Modern UI**: Material 3 design with Jetpack Compose
- **🔒 Privacy-Focused**: No tracking, no analytics, 100% open source

## 📸 Screenshots

*Coming soon*

## 🚀 Download

### From GitHub Releases

Download the latest APK from the [Releases](https://github.com/muarf/BalanceTaCam/releases) page.

### Build from Source

```bash
git clone https://github.com/muarf/BalanceTaCam.git
cd BalanceTaCam
./gradlew assembleDebug
```

The APK will be available at `app/build/outputs/apk/debug/app-debug.apk`

## 📋 Requirements

- **Android 7.0 (Nougat)** or higher
- **GPS** for location services
- **Internet connection** for map tiles and OSM API
- **OpenStreetMap account** to contribute data

## 🏗️ Technical Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Clean Architecture
- **UI**: Jetpack Compose with Material 3
- **Map Library**: osmdroid
- **Networking**: Retrofit + OkHttp
- **Dependency Injection**: Dagger Hilt
- **Database**: Room
- **Authentication**: ScribeJava (OAuth 1.0a)
- **Location**: Google Play Services Location

## 📝 OSM Tags Supported

### Mandatory Tags
- `man_made=surveillance`
- `surveillance:type=camera`

### Optional Tags (Detailed Mode)
- **Camera Type**: `camera:type` (fixed, dome, ptz, panoramic)
- **Mount Type**: `camera:mount` (pole, wall, ceiling, street_lamp)
- **Direction**: `camera:direction` (0-360 degrees)
- **Surveillance Type**: `surveillance` (public, outdoor, indoor)
- **Operator**: `operator` (free text)
- **Operator Type**: `operator:type` (public, private, commercial)
- **Zone**: `surveillance:zone` (town, parking, traffic, building)
- **Description**: `description` (additional details)
- **Level**: `level` (floor number)
- **Height**: `height` (mounting height)

## 🔧 Setup for Development

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Configuration

1. **Clone the repository**
```bash
git clone https://github.com/muarf/BalanceTaCam.git
cd BalanceTaCam
```

2. **Configure OAuth credentials**

You need to register your app on OSM to get OAuth credentials:

- Go to [OpenStreetMap Settings](https://www.openstreetmap.org/user/your-username/oauth_clients)
- Create a new OAuth 1.0a application
- Set callback URL to: `osmcamera://oauth`
- Copy the Consumer Key and Consumer Secret
- Update in `OAuthService.kt`:

```kotlin
private const val CONSUMER_KEY = "your_consumer_key"
private const val CONSUMER_SECRET = "your_consumer_secret"
```

3. **Build and run**
```bash
./gradlew assembleDebug
./gradlew installDebug
```

Or open the project in Android Studio and click Run ▶️

## 📖 Documentation

- [API Documentation](docs/API.md) - Details about OSM API integration
- [Tags Guide](docs/TAGS.md) - Complete guide to surveillance camera tags
- [Contributing Guide](CONTRIBUTING.md) - How to contribute to this project

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) first.

### Ways to Contribute

- 🐛 Report bugs and issues
- 💡 Suggest new features
- 🌍 Add translations
- 📝 Improve documentation
- 💻 Submit pull requests

## 🌐 Resources

- [OpenStreetMap Wiki - Surveillance](https://wiki.openstreetmap.org/wiki/Tag:man_made%3Dsurveillance)
- [OSM API v0.6](https://wiki.openstreetmap.org/wiki/API_v0.6)
- [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API)
- [OSM Forum (French)](https://forum.openstreetmap.fr/)

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

This ensures:
- ✅ Code source always accessible
- ✅ Modifications must be shared under the same license
- ✅ Compatible with OpenStreetMap's spirit
- ✅ Protection against proprietary appropriation

## 👥 Authors & Contributors

- **Your Name** - *Initial work*

See also the list of [contributors](https://github.com/muarf/BalanceTaCam/contributors) who participated in this project.

## 🙏 Acknowledgments

- OpenStreetMap community for the amazing open mapping platform
- osmdroid developers for the Android map library
- All contributors to open source libraries used in this project

## 📬 Contact

- GitHub Issues: [Report a bug](https://github.com/muarf/BalanceTaCam/issues)
- OSM Forum: [Discuss on OSM](https://forum.openstreetmap.fr/)

## ⚠️ Disclaimer

This application is designed for mapping existing surveillance cameras in public spaces for informational purposes. Always respect local laws and privacy regulations when contributing data to OpenStreetMap.

---

Made with ❤️ for the OpenStreetMap community


