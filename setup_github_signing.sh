#!/bin/bash
# GitHub App Signing Setup - Automated Script

cd "$(dirname "$0")"

echo "🔐 GitHub App Signing Setup"
echo "============================"
echo ""

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) not found!"
    echo "Install it with: brew install gh"
    echo "Then run: gh auth login"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ Not authenticated with GitHub"
    echo "Run: gh auth login"
    exit 1
fi

# Check if keystore exists
if [ ! -f "krishield-release-key.jks" ]; then
    echo "📝 Keystore not found. Generating new keystore..."
    echo ""
    echo "Please enter the following information:"
    echo "(Recommended: use 'krishield123' as password for simplicity)"
    echo ""
    
    keytool -genkey -v -keystore krishield-release-key.jks \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -alias krishield
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to generate keystore"
        exit 1
    fi
    echo ""
    echo "✅ Keystore generated successfully!"
else
    echo "✅ Keystore already exists: krishield-release-key.jks"
fi

# Get keystore password
echo ""
read -sp "Enter keystore password (default: krishield123): " KEYSTORE_PASS
echo ""
if [ -z "$KEYSTORE_PASS" ]; then
    KEYSTORE_PASS="krishield123"
fi

# Get Gemini API key
echo ""
read -p "Enter your Gemini API Key (or press Enter to skip): " API_KEY
if [ -z "$API_KEY" ]; then
    echo "⚠️  Skipping GEMINI_API_KEY (you can add it later)"
fi

# Encode keystore to base64
echo ""
echo "🔄 Encoding keystore to base64..."
base64 -i krishield-release-key.jks -o keystore.b64

if [ $? -ne 0 ]; then
    echo "❌ Failed to encode keystore"
    exit 1
fi

# Set GitHub secrets
echo "📤 Setting GitHub secrets..."
echo ""

gh secret set KEYSTORE_FILE < keystore.b64
gh secret set KEYSTORE_PASSWORD -b "$KEYSTORE_PASS"
gh secret set KEY_ALIAS -b "krishield"
gh secret set KEY_PASSWORD -b "$KEYSTORE_PASS"

if [ ! -z "$API_KEY" ]; then
    gh secret set GEMINI_API_KEY -b "$API_KEY"
fi

# Cleanup
rm keystore.b64

# Verify
echo ""
echo "✅ Setup complete! GitHub secrets added:"
echo ""
gh secret list

echo ""
echo "🎉 All done!"
echo ""
echo "Next steps:"
echo "  1. Push a commit to trigger GitHub Actions"
echo "  2. Check: https://github.com/TheBizarreAbhishek/krishield/actions"
echo "  3. Download signed APK from Releases"
echo ""
echo "Your keystore is saved locally as: krishield-release-key.jks"
echo "⚠️  BACKUP THIS FILE - You cannot recover it if lost!"
echo ""
