# Repository Guidelines

## Project Structure & Module Organization
This repository is an Android app built with Kotlin + Jetpack Compose and a single Gradle module (`:app`).
- `app/src/main/kotlin/com/wahyuakbarwibowo/aminmartkasir/`: core app code.
- `app/src/main/kotlin/.../data/`: Room entities, DAOs, repositories.
- `app/src/main/kotlin/.../ui/`: navigation, screens, viewmodels, reusable Compose components.
- `app/src/main/res/`: Android resources (colors, strings, drawables, mipmaps).
- `db_schema/`: SQL reference files for master/transaction/report structures.
- `docs/`: installation and product documentation.

## Build, Test, and Development Commands
Use the Makefile for daily workflows:
- `make build`: assemble debug APK (`app/build/outputs/apk/debug/app-debug.apk`).
- `make debug`: build + install debug APK.
- `make dev`: clean, build, install, and launch on connected device.
- `make test`: run local unit tests (`testDebugUnitTest`).
- `make lint`: run Android lint for debug variant.
- `make check`: run tests and lint together.
- `make release` / `make release-signed`: unsigned/signed release APK.

Direct Gradle equivalents are available, e.g. `./gradlew assembleDebug`, `./gradlew lintDebug`.

## Coding Style & Naming Conventions
- Follow Kotlin idioms: 4-space indentation, clear null-safety handling, small focused functions.
- Class/file names use `PascalCase` (`ProductRepository.kt`, `DashboardScreen.kt`).
- Functions/variables use `camelCase`; constants use `UPPER_SNAKE_CASE`.
- Compose UI types should be suffixed consistently (`...Screen`, `...Dialog`, `...ViewModel`).
- Keep package organization aligned to feature/domain (`data/local/dao`, `ui/screens`, `ui/viewmodel`).

## Testing Guidelines
Frameworks configured: JUnit4 (`testImplementation`) and AndroidX UI test stack (`androidTestImplementation`).
- Put unit tests in `app/src/test/...` and instrumentation/UI tests in `app/src/androidTest/...`.
- Name test files as `<Subject>Test.kt` and test methods with behavior-driven names (e.g., `fun insertProduct_updatesStock()`).
- Run `make test` before opening PRs; run `make check` for full local validation.

## Commit & Pull Request Guidelines
Recent history favors Conventional Commit-style prefixes: `feat:`, `fix:`, `chore:`, `docs:`.
- Keep commits focused and imperative (example: `fix: handle optional productId in route`).
- PRs should include: concise summary, scope of affected modules, test/lint results, and screenshots/video for UI changes.
- Link related issues/tasks and mention migration/config impacts (e.g., `signing.properties`, DB schema updates).

## Security & Configuration Tips
- Never commit production signing secrets; keep sensitive values in `signing.properties` locally.
- Validate release signing paths from project root (as configured in `app/build.gradle`).
- Use `make release` for unsigned artifacts when sharing build outputs safely.
