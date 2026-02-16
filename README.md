# KriShield - AI-Powered Farming Assistant

KriShield is an Android application designed for Indian farmers to leverage AI technology for smart farming assistance.

## Features

- 🤖 **AI Chat Assistant**: Powered by Google Gemini API with farming-focused responses
- 📸 **Crop Disease Detection**: Upload crop images for AI analysis
- 🎤 **Voice Input**: Chat using voice (coming soon)
- 🌦️ **Weather Forecast**: Get weather updates (coming soon)
- 🏛️ **Government Schemes**: Information about farming schemes (coming soon)
- 📅 **Crop Calendar**: Seasonal crop planning (coming soon)

## Tech Stack

- **Language**: Java
- **UI**: Material Design 3
- **AI**: Google Gemini API
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Building the App

### Prerequisites

- Android Studio (latest version)
- JDK 17
- Android SDK 34

### Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device or emulator

### Release Build

The app uses GitHub Actions for automated builds. To build locally:

```bash
./gradlew assembleRelease
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/krishield/
│   │   ├── activities/     # UI Activities
│   │   ├── adapters/       # RecyclerView Adapters
│   │   ├── models/         # Data Models
│   │   ├── services/       # Gemini AI Service
│   │   └── utils/          # Utility Classes
│   └── res/                # Resources (layouts, drawables, etc.)
```

## License

This project is developed as a college project for agricultural technology advancement.

## Contributors

- Abhishek (Developer)

---

Made with ❤️ for Indian Farmers
