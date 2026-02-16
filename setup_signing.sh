#!/bin/bash
# Quick App Signing Setup Script

echo "🔐 KriShield App Signing Setup"
echo "================================"
echo ""

# Check if keystore exists
if [ -f "krishield-release-key.jks" ]; then
    echo "✅ Keystore already exists: krishield-release-key.jks"
else
    echo "📝 Creating new keystore..."
    echo ""
    echo "Please enter the following information:"
    echo "(You can use 'krishield123' as password for simplicity)"
    echo ""
    
    keytool -genkey -v -keystore krishield-release-key.jks \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -alias krishield
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Keystore created successfully!"
    else
        echo "❌ Failed to create keystore"
        exit 1
    fi
fi

echo ""
echo "📄 Setting up local.properties..."

# Get SDK path
SDK_PATH="/Users/abhishek/Library/Android/sdk"
if [ ! -d "$SDK_PATH" ]; then
    SDK_PATH="$ANDROID_HOME"
fi

# Prompt for API key
echo ""
read -p "Enter your Gemini API Key (or press Enter to skip): " API_KEY
if [ -z "$API_KEY" ]; then
    API_KEY="your_gemini_api_key_here"
fi

# Prompt for keystore password
echo ""
read -sp "Enter keystore password (default: krishield123): " KEYSTORE_PASS
echo ""
if [ -z "$KEYSTORE_PASS" ]; then
    KEYSTORE_PASS="krishield123"
fi

# Create local.properties
cat > local.properties << EOL
sdk.dir=$SDK_PATH
GEMINI_API_KEY=$API_KEY
KEYSTORE_FILE=krishield-release-key.jks
KEYSTORE_PASSWORD=$KEYSTORE_PASS
KEY_ALIAS=krishield
KEY_PASSWORD=$KEYSTORE_PASS
EOL

echo "✅ local.properties created!"
echo ""
echo "🎉 Setup Complete!"
echo ""
echo "Next steps:"
echo "  1. Build debug APK:   ./gradlew assembleDebug"
echo "  2. Build release APK: ./gradlew assembleRelease"
echo ""
echo "APK locations:"
echo "  Debug:   app/build/outputs/apk/debug/app-debug.apk"
echo "  Release: app/build/outputs/apk/release/app-release.apk"
echo ""
