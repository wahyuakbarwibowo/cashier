# 🛒 Aminmart Cashier - Android Kotlin Project

## 🌟 Project Overview
Aminmart Cashier is a modern Point of Sales (POS) Android application designed for retail stores, minimarkets, and PPOB (Payment Point Online Bank) agents. It has been completely refactored to **Native Android using Kotlin and Jetpack Compose**.

### Tech Stack
- **Language**: Kotlin 2.3.10
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles
- **Database**: Room (SQLite) with StateFlow/Coroutines for reactive data streams
- **Navigation**: Navigation Compose
- **Hardware Integration**: Android Bluetooth API for 58mm Thermal Printers

## 🚀 Building and Running

The project includes a `Makefile` to simplify common development commands. You can also use the standard Gradle wrapper.

### Quick Start (Makefile)
- **Build & Install (Debug)**: `make dev` (Cleans, builds, installs, and runs the app on a connected device/emulator)
- **Build Debug APK**: `make build`
- **Run Lint & Tests**: `make check` (Runs both `test` and `lint`)
- **View App Logs**: `make logs` (Tail logs specifically for the app process)
- **Clear App Data**: `make db-clear`
- **Pull Database**: `make db-pull` (Pulls the local Room DB for inspection)

### Gradle Equivalent Commands
- **Build Debug**: `./gradlew assembleDebug`
- **Install Debug**: `./gradlew installDebug`
- **Run Unit Tests**: `./gradlew testDebugUnitTest`
- **Build Signed Release**: `./gradlew assembleRelease` (Requires `signing.properties` configured)

## 🏗️ Development Conventions

### Architecture Flow
The application strictly follows a unidirectional data flow and MVVM pattern:
`Composable (UI) <-> ViewModel (StateFlow) <-> Repository <-> Room DAO (Local Database)`

### Coding Guidelines
- **UI Components**: Use Jetpack Compose exclusively. Avoid XML layouts except for base app configuration and splash screens.
- **State Management**: Use `StateFlow` and `MutableStateFlow` in ViewModels to expose UI state to the Composables.
- **Asynchronous Operations**: Utilize Kotlin Coroutines (`viewModelScope` in ViewModels, `runTest` for testing).
- **Styling**: Adhere to the established "Pink Theme" matching the Aminmart brand (`#E91E8B` primary) defined in `Theme.kt` and `Color.kt`. Ensure Dark Mode compatibility.
- **Database**: All local persistence is handled via Room. New entities should be added to `data/local/entity`, DAOs to `data/local/dao`, and exposed via `data/repository`.
- **Testing**: Write JUnit4 unit tests for Repositories and ViewModels using Mockito for mocking dependencies. Place tests in `app/src/test/kotlin/`.

### Adding New Features Workflow
1. **Data Layer**: Create a Room `@Entity` and its `@Dao`. Add the DAO to `AppDatabase.kt`.
2. **Repository**: Create a Repository class that depends on the DAO to expose data flows and operations.
3. **ViewModel**: Create a ViewModel that consumes the Repository and holds UI state.
4. **UI Layer**: Create a new `@Composable` screen in `ui/screens` and configure its navigation route in `AppNavigation.kt`.
