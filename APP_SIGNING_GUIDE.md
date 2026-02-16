# App Signing Setup Guide

## Quick Setup (5 minutes)

### Step 1: Generate Keystore (One-time)
```bash
cd /Volumes/LinuxFS/Android_Development/krishield
keytool -genkey -v -keystore krishield-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias krishield
```

**Fill in the prompts:**
- Password: `krishield123` (or your choice)
- Name: Your name
- Organization: Your college/company
- City, State, Country: Your location
- Confirm: `yes`

### Step 2: Create local.properties
```bash
# Create/edit local.properties file
cat > local.properties << EOF
sdk.dir=/Users/abhishek/Library/Android/sdk
GEMINI_API_KEY=your_gemini_api_key_here
KEYSTORE_FILE=krishield-release-key.jks
KEYSTORE_PASSWORD=krishield123
KEY_ALIAS=krishield
KEY_PASSWORD=krishield123
EOF
```

### Step 3: Build Signed APK
```bash
# Debug build (for testing)
./gradlew assembleDebug

# Release build (signed APK)
./gradlew assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release.apk`

---

## Already Have Keystore?

If you already have `krishield-release-key.jks`, just update `local.properties`:

```properties
sdk.dir=/Users/abhishek/Library/Android/sdk
GEMINI_API_KEY=your_api_key
KEYSTORE_FILE=krishield-release-key.jks
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=krishield
KEY_PASSWORD=your_password
```

---

## Quick Commands

### Build Debug APK (No signing needed)
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK (Signed)
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Install on Device
```bash
# Debug
adb install app/build/outputs/apk/debug/app-debug.apk

# Release
adb install app/build/outputs/apk/release/app-release.apk
```

---

## Troubleshooting

### Error: SDK location not found
**Fix:** Add to `local.properties`:
```properties
sdk.dir=/Users/abhishek/Library/Android/sdk
```

### Error: Keystore not found
**Fix:** Make sure `krishield-release-key.jks` is in project root:
```bash
ls -la krishield-release-key.jks
```

### Error: Wrong password
**Fix:** Update password in `local.properties`:
```properties
KEYSTORE_PASSWORD=correct_password
KEY_PASSWORD=correct_password
```

---

## Important Notes

1. **Never commit keystore to Git** - Already in `.gitignore`
2. **Never commit local.properties** - Already in `.gitignore`
3. **Backup your keystore** - You can't recover it if lost!
4. **Use debug builds for testing** - Faster and no signing needed

---

## Current Setup

Your `build.gradle` is already configured to:
- ✅ Read signing config from `local.properties`
- ✅ Use debug signing for debug builds (automatic)
- ✅ Use release signing for release builds (needs keystore)
- ✅ Automatically sign APKs when building release

Just create the keystore and `local.properties` file, then you're done! 🚀
