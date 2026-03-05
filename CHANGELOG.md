# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Unit test setup with Mockito and Kotlin Coroutines Test
- `signing.properties.example` for release signing configuration guidance
- ProGuard rules for Room, Gson, and Kotlinx Coroutines

### Changed
- **Theme**: Updated color scheme to match Aminmart pink logo colors
  - Primary: `#E91E8B` (bright pink from cashier machine & logo text)
  - Secondary: `#800020` (dark maroon from silhouette & outlines)
- Moved `MainActivity.kt` and `MainApplication.kt` to consistent Kotlin folder structure
- Enabled R8 full mode with minification and resource shrinking for release builds
- Updated `colors.xml` with dark mode support in `values-night/colors.xml`

### Fixed
- Color theme inconsistency between XML and Compose themes

---

## [1.0.0] - 2026-03-05

### Added
- Retail POS with barcode scanner
- PPOB & Digital Services (Pulsa, PLN, E-Wallet, BPJS, etc.)
- Profit & Loss reporting
- Debt & Receivables management
- Expense tracking
- Low stock alerts
- Package price calculation
- Thermal printer support (58mm Bluetooth)
- Transaction history with date filtering
- Backup & Restore database
- Edit digital transactions
- Backdate transactions
- Dynamic digital categories
- Enhanced UX with keyboard avoidance
- Indonesian date format throughout the app

### Changed
- Kotlin 2.3.10
- Jetpack Compose with Material 3
- Room Database 2.8.4
- Navigation Compose 2.8.5

---

## Version History

| Version | Release Date | Notes |
|---------|--------------|-------|
| 1.0.0   | 2026-03-05   | Initial release with full POS & PPOB features |

---

## Contributing

When contributing to this project, please:
1. Update the CHANGELOG.md with your changes under the [Unreleased] section
2. Follow the existing code style and conventions
3. Add tests for new features
4. Update documentation as needed

---

**Developed with ❤️ by Wahyu Akbar Wibowo**
