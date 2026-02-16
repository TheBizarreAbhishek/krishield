# KriShield - Viva Questions & Answers

## 1. Project Overview Questions

### Q: What is KriShield and what problem does it solve?
**A:** KriShield is an AI-powered Android application designed to help Indian farmers with crop management. It solves multiple problems:
- **Crop Disease Detection**: Using Gemini AI to identify diseases from crop images
- **Market Price Information**: Real-time market prices from NHB (National Horticulture Board)
- **Weather Forecasting**: 7-day weather forecast using Open-Meteo API
- **AI Chat Assistant**: Farming advice and guidance through Gemini AI
- **Price Advisory**: AI-powered price trend analysis for selling decisions

### Q: Who is the target audience?
**A:** Indian farmers, especially those who need:
- Quick disease identification without expert consultation
- Real-time market price information to get fair prices
- Weather updates for planning irrigation and harvesting
- Farming advice in simple language

---

## 2. Technology Stack Questions

### Q: Why did you choose Java for Android development instead of Kotlin?
**A:** 
1. **Wider Compatibility**: Java works on older Android versions (API 21+)
2. **Stability**: Java is mature and well-documented for Android
3. **Learning Curve**: Easier for team members familiar with Java
4. **Library Support**: All Android libraries have excellent Java support
5. **Industry Standard**: Many production apps still use Java

### Q: What is the minimum Android version supported?
**A:** API Level 21 (Android 5.0 Lollipop) - covers 99%+ of devices in India

### Q: Why Gemini AI instead of other AI models?
**A:**
1. **Multimodal**: Can process both text and images (for crop disease detection)
2. **Free Tier**: Generous free quota for development and testing
3. **Latest Model**: Using gemini-2.0-flash-exp for fast responses
4. **Google Integration**: Easy integration with Android ecosystem
5. **Grounding/Search**: Can search the web for real-time market data

---

## 3. Architecture & Design Questions

### Q: Explain the app architecture
**A:** We use **MVVM-inspired architecture**:
- **Activities**: UI layer (MainActivity, WeatherActivity, ChatActivity, etc.)
- **Services**: Business logic (GeminiService, OpenMeteoService)
- **Models**: Data structures (WeatherModels, ChatMessage)
- **Adapters**: RecyclerView adapters for lists (ChatAdapter)
- **Layouts**: XML-based Material Design 3 UI

### Q: Why Material Design 3?
**A:**
1. **Modern UI**: Latest design guidelines from Google
2. **Accessibility**: Built-in accessibility features
3. **Consistency**: Familiar UI patterns for users
4. **Components**: Pre-built components (MaterialCardView, MaterialButton)
5. **Theming**: Easy color and theme customization

### Q: How do you handle API keys securely?
**A:**
1. **BuildConfig**: API keys stored in `local.properties` (not in Git)
2. **User Settings**: Users can input their own Gemini API key in Settings
3. **SharedPreferences**: Encrypted storage for user's API key
4. **GitHub Secrets**: CI/CD uses encrypted secrets
5. **Fallback**: App uses user's key if set, otherwise BuildConfig key

---

## 4. Feature-Specific Questions

### Q: How does the Weather Forecast feature work?
**A:**
1. **GPS Detection**: Uses `FusedLocationProviderClient` to get user location
2. **Open-Meteo API**: Free weather API, no key required
3. **Retrofit**: HTTP client for API calls
4. **Gson**: JSON parsing for weather data
5. **7-Day Forecast**: Shows daily high/low temps and conditions
6. **Weather Codes**: Converts numeric codes to emojis (☀️ ⛅ 🌧️)

### Q: Why Open-Meteo instead of OpenWeatherMap?
**A:**
1. **No API Key**: Completely free, no registration needed
2. **No Rate Limits**: Unlimited requests
3. **Accurate Data**: Uses official weather models (NOAA, DWD)
4. **Open Source**: Transparent and reliable
5. **Indian Coverage**: Good coverage for Indian locations

### Q: How does the Market Price feature work?
**A:**
1. **NHB WebView**: Loads National Horticulture Board's official website
2. **Real-time Data**: Shows live market prices from mandis
3. **No Scraping**: Uses official website directly (legal and reliable)
4. **JavaScript Enabled**: For interactive price tables

### Q: Explain the AI Chat feature
**A:**
1. **Gemini 2.0 Flash**: Fast, multimodal AI model
2. **System Instruction**: Pre-configured as farming expert for India
3. **Image Support**: Can analyze crop images for disease detection
4. **Context-Aware**: Remembers conversation history
5. **Streaming**: Real-time response display

### Q: How does the Settings feature solve rate limit issues?
**A:**
1. **Custom API Key**: Users can input their own Gemini API key
2. **SharedPreferences**: Saves key locally on device
3. **Test Functionality**: Validates key before saving
4. **Fallback**: Uses BuildConfig key if user hasn't set one
5. **Privacy**: Key stored only on user's device

---

## 5. Technical Implementation Questions

### Q: How do you handle network requests?
**A:**
- **Retrofit**: Type-safe HTTP client for REST APIs
- **OkHttp**: Underlying HTTP client with connection pooling
- **Gson**: JSON serialization/deserialization
- **Callbacks**: Async execution with `ExecutorService`
- **Error Handling**: Try-catch blocks with user-friendly messages

### Q: Explain the RecyclerView implementation in Chat
**A:**
```java
1. ChatAdapter extends RecyclerView.Adapter
2. Two view types: USER and AI messages
3. ViewHolder pattern for performance
4. DiffUtil for efficient updates
5. Smooth scrolling to latest message
```

### Q: How do you request and handle permissions?
**A:**
```java
// Location permission for Weather
if (ContextCompat.checkSelfPermission(...) != GRANTED) {
    ActivityCompat.requestPermissions(...);
}

@Override
public void onRequestPermissionsResult(...) {
    if (granted) {
        fetchLocation();
    } else {
        showError("Permission denied");
    }
}
```

### Q: What is the CI/CD pipeline?
**A:**
1. **GitHub Actions**: Automated build on every push
2. **Gradle Build**: Compiles and packages APK
3. **JDK 17**: Java Development Kit version
4. **Signed APK**: Uses keystore for release signing
5. **GitHub Releases**: Automatic APK upload
6. **Artifact Storage**: Build artifacts saved for 90 days

---

## 6. Database & Storage Questions

### Q: Do you use any database?
**A:** Currently using **SharedPreferences** for:
- User's custom API key
- App settings and preferences
- Simple key-value storage

**Future**: Plan to add Room Database for:
- Chat history persistence
- Offline crop disease database
- Saved market price favorites

### Q: Why SharedPreferences instead of Room?
**A:**
- **Simplicity**: Only storing API key and settings
- **Performance**: Fast for small data
- **No Overhead**: No need for complex database setup
- **Sufficient**: Current use case doesn't need relational data

---

## 7. Testing & Debugging Questions

### Q: How do you test the app?
**A:**
1. **Manual Testing**: On physical Android devices
2. **GitHub Actions**: Automated build verification
3. **Lint Checks**: Android Lint for code quality
4. **API Testing**: Test endpoints with Postman
5. **User Testing**: Feedback from farmers

### Q: How do you handle errors?
**A:**
```java
try {
    // API call
} catch (Exception e) {
    Log.e(TAG, "Error", e);
    runOnUiThread(() -> {
        Toast.makeText(this, "Error: " + e.getMessage(), 
            Toast.LENGTH_LONG).show();
    });
}
```

---

## 8. Deployment Questions

### Q: How do you build and release the APK?
**A:**
1. **Local Build**: `./gradlew assembleRelease`
2. **Signing**: Uses keystore (krishield-release-key.jks)
3. **GitHub Actions**: Automated build on push
4. **Release APK**: Uploaded to GitHub Releases
5. **Version Control**: Git tags for releases

### Q: What is the APK signing process?
**A:**
```gradle
signingConfigs {
    release {
        storeFile file('krishield-release-key.jks')
        storePassword KEYSTORE_PASSWORD
        keyAlias KEY_ALIAS
        keyPassword KEY_PASSWORD
    }
}
```

---

## 9. Future Enhancements Questions

### Q: What features are planned for future?
**A:**
1. **Offline Mode**: Cached data for no-internet scenarios
2. **Multi-language**: Hindi, Tamil, Telugu, etc.
3. **Voice Input**: Speech-to-text for farmers
4. **SMS Integration**: Weather/price alerts via SMS
5. **Government Schemes**: Database of farming schemes
6. **Soil Testing**: Integration with soil testing labs
7. **Crop Calendar**: Planting and harvesting reminders

---

## 10. Challenges & Solutions

### Q: What challenges did you face?
**A:**
1. **Rate Limits**: Solved with user-configurable API keys
2. **Build Errors**: Fixed Java version compatibility issues
3. **XML Syntax**: Debugged malformed layouts
4. **API Integration**: Handled async callbacks properly
5. **GitHub Actions**: Configured proper secrets and paths

### Q: How did you optimize performance?
**A:**
1. **RecyclerView**: Efficient list rendering
2. **ViewHolder Pattern**: Reuses views
3. **Async Operations**: Network calls on background threads
4. **Image Compression**: Before sending to Gemini
5. **Caching**: Retrofit caches API responses

---

## Quick Fire Round

**Q: What is Retrofit?**
A: Type-safe HTTP client for Android

**Q: What is Gson?**
A: JSON parser library by Google

**Q: What is Material Design?**
A: Google's design system for Android apps

**Q: What is an Intent?**
A: Message object to start activities or services

**Q: What is a ViewHolder?**
A: Pattern to cache view references in RecyclerView

**Q: What is Gradle?**
A: Build automation tool for Android

**Q: What is an APK?**
A: Android Package Kit - installable app file

**Q: What is ProGuard?**
A: Code obfuscation and optimization tool

**Q: What is AndroidManifest.xml?**
A: Configuration file declaring app components and permissions

**Q: What is BuildConfig?**
A: Auto-generated class containing build-time constants

---

## Pro Tips for Viva

1. **Be Confident**: You built this, you know it best
2. **Explain Simply**: Use analogies for complex concepts
3. **Show Code**: Have code snippets ready to show
4. **Demo Ready**: Have APK installed and working
5. **Know Limitations**: Be honest about what's not implemented
6. **Future Vision**: Show you're thinking ahead
7. **User Focus**: Always bring it back to helping farmers

**Good Luck! 🚀**
