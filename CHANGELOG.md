# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of OSM Camera Mapper

## [3.4.0] - 2026-08-24

### Added
- Évitement dynamique des caméras publiques avec marge de distance configurable (itinéraires)
- Nouvelles icônes caméra (pin + dot) pour la carte

### Fixed
- Crash lors du rendu de la liste des caméras (remplacement LazyColumn imbriquée par Column/forEach)

### Changed
- Refactor du moteur de routage (RoutingRepository, CameraClusterUtils) et mise à jour du Gradle wrapper

## [1.0.0] - TBD

### Added
- Interactive map with osmdroid showing OSM tiles
- Display existing surveillance cameras from Overpass API
- OAuth 1.0a authentication with OpenStreetMap
- Add new cameras with quick mode (basic info only)
- Add new cameras with detailed mode (all optional tags)
- Support for all standard camera tags (type, mount, direction, etc.)
- User location tracking with GPS
- Multilingual support (English, French, Spanish, German)
- Material 3 design with Jetpack Compose
- Offline camera caching in Room database
- Secure token storage with EncryptedSharedPreferences
- Settings screen
- About screen with links to documentation

### Technical
- MVVM architecture with Clean Architecture principles
- Dagger Hilt for dependency injection
- Kotlin Coroutines for async operations
- Retrofit for API calls
- Room for local database
- DataStore for preferences

### Security
- OAuth 1.0a implementation
- Encrypted storage for tokens
- HTTPS-only communication
- No tracking or analytics

[Unreleased]: https://github.com/muarf/BalanceTaCam/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/muarf/BalanceTaCam/releases/tag/v1.0.0


