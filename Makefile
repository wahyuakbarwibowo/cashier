# Aminmart Cashier - Makefile
# Android Kotlin Project Build & Development Commands

.PHONY: help clean build debug release install run devices sync deps test lint

# Default target
help:
	@echo "Aminmart Cashier - Available Commands"
	@echo "======================================"
	@echo ""
	@echo "Development:"
	@echo "  make sync       - Sync Gradle dependencies"
	@echo "  make deps       - Download all dependencies"
	@echo "  make clean      - Clean build artifacts"
	@echo "  make build      - Build debug APK"
	@echo "  make debug      - Build and install debug APK"
	@echo "  make run        - Run app on connected device"
	@echo "  make dev        - Full dev cycle: clean + build + install + run"
	@echo ""
	@echo "Release:"
	@echo "  make release    - Build release APK (unsigned)"
	@echo "  make bundle     - Build Android App Bundle (.aab)"
	@echo ""
	@echo "Device Management:"
	@echo "  make devices    - List connected Android devices"
	@echo "  make logs       - View app logs (logcat)"
	@echo "  make logs-clear - Clear logcat logs"
	@echo ""
	@echo "Testing & Quality:"
	@echo "  make test       - Run unit tests"
	@echo "  make lint       - Run lint checks"
	@echo "  make check      - Run tests + lint"
	@echo ""
	@echo "Database:"
	@echo "  make db-pull    - Pull database from device"
	@echo "  make db-clear   - Clear app data on device"
	@echo ""
	@echo "Uninstall:"
	@echo "  make uninstall  - Uninstall app from device"

# Gradle wrapper
GRADLE = ./gradlew
ADB = adb
PACKAGE = com.wahyuakbarwibowo.aminmartkasir
APK_DEBUG = app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE = app/build/outputs/apk/release/app-release-unsigned.apk
AAB_RELEASE = app/build/outputs/bundle/release/app-release.aab

# Sync Gradle
sync:
	$(GRADLE) --refresh-dependencies

# Download dependencies
deps:
	$(GRADLE) dependencies

# Clean build
clean:
	$(GRADLE) clean

# Build debug APK
build:
	$(GRADLE) assembleDebug
	@echo ""
	@echo "✓ Debug APK ready: $(APK_DEBUG)"

# Build and install debug APK
debug: build install

# Build release APK (unsigned)
release:
	$(GRADLE) assembleRelease
	@echo ""
	@echo "✓ Release APK ready: $(APK_RELEASE)"
	@echo "⚠ Note: APK is unsigned. Sign it for distribution."

# Build Android App Bundle
bundle:
	$(GRADLE) bundleRelease
	@echo ""
	@echo "✓ AAB ready: $(AAB_RELEASE)"

# Install APK on device
install:
	$(ADB) install -r $(APK_DEBUG)
	@echo ""
	@echo "✓ App installed"

# Run app on device
run:
	$(ADB) shell am start -n $(PACKAGE)/.MainActivity
	@echo ""
	@echo "✓ App launched"

# Full dev cycle: clean + build + install + run
dev: clean build install run

# List connected devices
devices:
	$(ADB) devices
	@echo ""
	@echo "Tip: If no device shown, enable USB Debugging on your Android device"

# View app logs
logs:
	$(ADB) logcat -s "AminmartKasir"

# Clear logs
logs-clear:
	$(ADB) logcat -c
	@echo "✓ Logcat cleared"

# Run unit tests
test:
	$(GRADLE) testDebugUnitTest

# Run lint checks
lint:
	$(GRADLE) lintDebug

# Run tests + lint
check: test lint

# Pull database from device
db-pull:
	@echo "Pulling database from device..."
	$(ADB) pull /data/data/$(PACKAGE)/databases/aminmart_kasir.db ./aminmart_kasir.db
	@echo ""
	@echo "✓ Database saved to: ./aminmart_kasir.db"
	@echo "Open with: DB Browser for SQLite or Android Studio Database Inspector"

# Clear app data
db-clear:
	$(ADB) shell pm clear $(PACKAGE)
	@echo ""
	@echo "✓ App data cleared"

# Uninstall app
uninstall:
	$(ADB) uninstall $(PACKAGE)
	@echo ""
	@echo "✓ App uninstalled"

# Quick build for development (no clean)
quick:
	$(GRADLE) assembleDebug && $(ADB) install -r $(APK_DEBUG) && $(ADB) shell am start -n $(PACKAGE)/.MainActivity

# Rebuild from scratch
rebuild: clean dev

# Install on all connected devices
install-all:
	$(ADB) devices | grep '\t' | awk '{print $$1}' | while read device; do \
		$(ADB) -s $$device install -r $(APK_DEBUG); \
		echo "✓ Installed on $$device"; \
	done

# Screenshot from device
screenshot:
	$(ADB) shell screencap -p /sdcard/screenshot.png
	$(ADB) pull /sdcard/screenshot.png ./screenshot_$(shell date +%Y%m%d_%H%M%S).png
	@echo "✓ Screenshot saved"

# Screen record (Ctrl+C to stop)
record:
	$(ADB) shell screenrecord /sdcard/demo.mp4
	$(ADB) pull /sdcard/demo.mp4 ./recording_$(shell date +%Y%m%d_%H%M%S).mp4
	@echo "✓ Recording saved"
