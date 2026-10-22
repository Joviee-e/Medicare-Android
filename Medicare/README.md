# MediCare+ Android App

This Android application is the patient-facing front end for the MediCare+ health platform. It provides a polished mobile workflow for medication tracking, safety awareness, AI support, and local health information access.

## What the app does

The app lets users:

- manage medication schedules and reminders
- add, edit, and remove prescriptions
- log compliance actions such as taken, snoozed, and skipped
- view AI-powered health guidance in a conversational interface
- review notifications with contextual prioritization
- explore nearby pharmacies and local care options
- configure accessibility preferences and profile details

## App highlights

### Dashboard and home experience
- circular completion summary for medication adherence
- upcoming medication cards and schedule visualization
- direct navigation into reminder or alert flows

### Medicine management
- filter by time-of-day or medication state
- add medicines with dosage, frequency, and date settings
- native date/time dialogs for scheduling
- edit/delete actions with confirmation flows

### AI assistant
- support chat interface with suggestion chips
- conversation history and auto-scrolling behavior
- contextual payloads sent to the backend AI service
- fallback handling for unavailable or unsupported actions

### Pharmacy and location features
- map-style UI with pharmacy points and distance metadata
- search and listing flows for pharmacy options
- built-in action buttons for navigation and call workflows

### Profile and accessibility
- contrast mode, haptics, and voice settings
- user profile details and sign-out confirmation flow
- consistent user experience across onboarding and dashboard screens

## Technology stack

- Kotlin
- Android SDK / Jetpack
- Material Design Components
- Retrofit + OkHttp for API communication
- Gson for JSON parsing
- Geoapify location data integration
- Custom UI components and drawable resources

## Project structure

```text
Medicare/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── androidTest/
│   │   ├── main/
│   │   └── test/
│   └── proguard-rules.pro
├── gradle/
├── icons/
├── UI/
│   ├── home page.png
│   ├── medicine list.png
│   ├── near pharmacy.png
│   ├── profile.png
│   ├── reminder alarm.png
│   └── ai health agent.png
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

## Setup and build

### Prerequisites

- Android Studio
- Java / JDK 11+
- Android SDK configured for the project
- Local backend running if you want live API integration

### Build a debug APK

```bash
cd Medicare
./gradlew assembleDebug
```

### Compile sources

```bash
cd Medicare
./gradlew compileDebugSources
```

### Environment configuration

The app reads a Geoapify key at build time from a local `.env` file or the backend `.env` file.

```env
GEOAPIFY_API_KEY=your_geoapify_key
```

## Backend integration

This app is designed to communicate with the Flask backend in `Medicare-Backend/` via REST APIs. The API contract includes:

- auth flows
- profile management
- medication CRUD
- notifications
- AI chat
- medicine safety analysis
- ML adherence predictor

## UI references

The design assets under `Medicare/UI/` capture the app's main screens, including:

- Home dashboard
- Medicine list
- AI assistant
- Nearby pharmacy view
- Profile settings
- Reminder alarm

## Notes

This app is a polished mobile experience and integration layer for a healthcare product. For the full project context, architecture, and API details, see the repository-level README and backend documentation.
