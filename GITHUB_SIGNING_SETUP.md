# GitHub App Signing Setup Guide

## Complete Setup in 3 Steps

### Step 1: Generate Keystore Locally

```bash
cd /Volumes/LinuxFS/Android_Development/krishield

# Generate keystore (one-time)
keytool -genkey -v -keystore krishield-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias krishield

# Enter password: krishield123 (or your choice)
# Fill in your details when prompted
```

**Save these values:**
- Keystore password: `krishield123`
- Key alias: `krishield`
- Key password: `krishield123`

---

### Step 2: Encode Keystore to Base64

```bash
# Convert keystore to base64
base64 -i krishield-release-key.jks -o keystore.b64

# Copy the base64 string
cat keystore.b64 | pbcopy
```

The base64 string is now in your clipboard!

---

### Step 3: Add to GitHub Secrets

#### Option A: Using GitHub CLI (Easiest)
```bash
# Set secrets using gh CLI
gh secret set KEYSTORE_FILE < keystore.b64
gh secret set KEYSTORE_PASSWORD -b "krishield123"
gh secret set KEY_ALIAS -b "krishield"
gh secret set KEY_PASSWORD -b "krishield123"
gh secret set GEMINI_API_KEY -b "your_gemini_api_key_here"

# Verify secrets were added
gh secret list
```

#### Option B: Using GitHub Web UI
1. Go to: https://github.com/TheBizarreAbhishek/krishield/settings/secrets/actions
2. Click **"New repository secret"**
3. Add these secrets:

| Secret Name | Value |
|------------|-------|
| `KEYSTORE_FILE` | Paste the base64 string from clipboard |
| `KEYSTORE_PASSWORD` | `krishield123` |
| `KEY_ALIAS` | `krishield` |
| `KEY_PASSWORD` | `krishield123` |
| `GEMINI_API_KEY` | Your Gemini API key |

---

## Verification

After adding secrets, push any commit to trigger GitHub Actions:

```bash
git commit --allow-empty -m "test: Trigger build with signing"
git push
```

Check the Actions tab: https://github.com/TheBizarreAbhishek/krishield/actions

You should see:
- ✅ Build with Gradle
- ✅ Build Release APK (signed)
- ✅ Upload Release APK
- ✅ Create GitHub Release

---

## Quick Setup Script

Run this automated script:

```bash
#!/bin/bash
cd /Volumes/LinuxFS/Android_Development/krishield

echo "🔐 GitHub App Signing Setup"
echo "==========================="

# Check if keystore exists
if [ ! -f "krishield-release-key.jks" ]; then
    echo "📝 Generating keystore..."
    keytool -genkey -v -keystore krishield-release-key.jks \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -alias krishield
else
    echo "✅ Keystore already exists"
fi

# Encode to base64
echo "🔄 Encoding keystore to base64..."
base64 -i krishield-release-key.jks -o keystore.b64

# Set GitHub secrets
echo "📤 Setting GitHub secrets..."
gh secret set KEYSTORE_FILE < keystore.b64
gh secret set KEYSTORE_PASSWORD -b "krishield123"
gh secret set KEY_ALIAS -b "krishield"
gh secret set KEY_PASSWORD -b "krishield123"

# Verify
echo ""
echo "✅ Setup complete! Secrets added:"
gh secret list

# Cleanup
rm keystore.b64

echo ""
echo "🎉 All done! Push a commit to test signing."
```

Save as `setup_github_signing.sh` and run:
```bash
chmod +x setup_github_signing.sh
./setup_github_signing.sh
```

---

## Troubleshooting

### Error: gh not found
**Fix:** Install GitHub CLI:
```bash
brew install gh
gh auth login
```

### Error: Secret already exists
**Fix:** Delete and re-add:
```bash
gh secret delete KEYSTORE_FILE
gh secret set KEYSTORE_FILE < keystore.b64
```

### Build fails with "Keystore not found"
**Fix:** Make sure `KEYSTORE_FILE` secret contains the base64 string, not the file path

### APK not signed
**Fix:** Check GitHub Actions logs for signing errors:
```bash
gh run view --log-failed
```

---

## Security Notes

1. ✅ **Never commit keystore to Git** - Already in `.gitignore`
2. ✅ **Never commit passwords** - Use GitHub Secrets
3. ✅ **Backup your keystore** - Store `krishield-release-key.jks` safely
4. ✅ **Use strong passwords** - For production apps
5. ✅ **Rotate secrets** - If compromised

---

## Current Workflow

Your `.github/workflows/build-release.yml` already:
- ✅ Reads secrets from GitHub
- ✅ Decodes base64 keystore
- ✅ Signs release APK
- ✅ Uploads to GitHub Releases

Just add the secrets and you're done! 🚀
