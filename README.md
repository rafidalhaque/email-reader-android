# email-reader-android

Android app (Kotlin + Material 3 UI) to open and parse `.eml` email files.

## Platform support
- **Minimum Android:** 8.0 (API 26)
- **Target/Maximum Android:** 15 (API 35)
- **Architectures:** all supported Android ABIs (app is pure Kotlin with no ABI-specific native libraries)

## Features
- Pick `.eml` files from storage using system file picker
- Parse common headers (`Subject`, `From`, `To`, `Date`)
- Parse plain text message body
- Parse multipart emails and prefer `text/plain` sections
- Decode common transfer encodings (`base64`, `quoted-printable`)

## Build and run
1. Install Android Studio Hedgehog+ or Android SDK/Build Tools for API 35.
2. Run:
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```
3. Install the debug APK from `app/build/outputs/apk/debug/`.
