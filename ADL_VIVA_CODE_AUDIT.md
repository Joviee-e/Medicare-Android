# Medicare+ ADL Practical Examination & Viva — Comprehensive Source Code Audit and Task Mapping Report

**Document Title:** Application Development Laboratory (ADL) Practical Examination Audit  
**Project Name:** Medicare+ (Native Android Client, Python/Flask Cloud Backend, Machine Learning & AI Safety Engine)  
**Audit Type:** Read-Only Source Code Inspection and Architecture Verification  
**Repository Working Directory:** `c:\Users\lenovo\AndroidStudioProjects\`  
**Target Examination:** ADL Viva Voce & Practical Lab Demonstration  

---

## Executive Summary & Audit Scope

This document provides a 100% verified, read-only technical audit of the **Medicare+** repository. It maps the four practical journal tasks mandated for the Application Development Laboratory (ADL) examination directly to the actual files, classes, methods, database schemas, XML layouts, and network protocols implemented across the project.

Every claim, file path, method name, API endpoint, and UI interaction documented herein has been verified against the current repository source code. Zero hypothetical names or unverified abstractions have been included. Both group members can rely on this document to explain and demonstrate any of the four tasks independently during the examination.

---

## PART 1 — PROJECT FOLDER STRUCTURE

The repository contains three primary sub-projects alongside system-level architectural specifications:
1. `Medicare/` — The native Android mobile client application built using Kotlin, Android SDK (API 34/36), Android Jetpack, and Material Design Components.
2. `Medicare-Backend/` — The RESTful API backend built using Python 3.11+/3.14, Flask, PyMongo, MongoDB Atlas, Flask-JWT-Extended, scikit-learn, and the Google GenAI SDK.
3. `Medicare-Web/` — An accessible web client designed as a companion interface for desktop and web browsers.

```
c:\Users\lenovo\AndroidStudioProjects\
├── README.md                           # Master system overview and architecture diagram
├── TECH_STACK.md                       # Comprehensive technology stack specifications
├── INTEGRATION_STATUS.md               # End-to-end integration matrix and test audit
├── AI_ML_INTEGRATION_GUIDE.md          # AI safety guardrails, RxNorm, openFDA, and ML pipeline guide
├── IMPLEMENTATION_CHANGELOG.md         # Production audit log, bug fixes, and feature evolution
│
├── Medicare/                           # [ANDROID MODULE]
│   ├── build.gradle.kts                # Root project Gradle configuration
│   ├── gradle.properties               # JVM memory and AndroidX properties
│   ├── journal                         # Initial implementation log (Aug 7, 2026)
│   ├── gradle/libs.versions.toml       # Centralized version catalog (AGP 9.0.0, SDK 36)
│   └── app/
│       ├── build.gradle.kts            # App module build script (API 36, dependencies, buildConfig)
│       └── src/main/
│           ├── AndroidManifest.xml     # Application manifest, activities, permissions, receivers
│           ├── java/com/example/medicare/
│           │   ├── MainActivity.kt             # Root routing activity (session & onboarding dispatch)
│           │   ├── BaseActivity.kt             # Base class managing dynamic fonts and high contrast
│           │   ├── LoginActivity.kt            # User authentication screen (JWT login)
│           │   ├── RegisterActivity.kt         # New patient account registration screen
│           │   ├── ForgotPasswordActivity.kt   # Password reset screen with 6-digit verification code
│           │   ├── OnboardingActivity.kt       # 4-step patient profile wizard (phone, medical, emergency)
│           │   ├── HomeActivity.kt             # Dashboard (circular adherence gauge, upcoming dose, schedule)
│           │   ├── MedicinesActivity.kt        # Medication list screen with filter chips and FAB
│           │   ├── AddMedicineActivity.kt      # Medication creation/editing form with safety alerts
│           │   ├── AIAssistantActivity.kt      # Conversational AI assistant interface with Gemini
│           │   ├── PharmacyActivity.kt         # Healthcare map screen (MapLibre, Geoapify, GPS)
│           │   ├── ProfileActivity.kt          # Patient profile, emergency calling, accessibility toggles
│           │   ├── ReminderAlarmActivity.kt    # Full-screen lockscreen alarm overlay with TTS & haptics
│           │   │
│           │   ├── AlarmScheduler.kt           # AlarmManager exact alarm clock scheduler
│           │   ├── AlarmReceiver.kt            # BroadcastReceiver triggered by Android AlarmManager
│           │   ├── BootReceiver.kt             # BroadcastReceiver rescheduling alarms on device boot
│           │   ├── NotificationHelper.kt       # In-app bottom sheet notifications & priority sync
│           │   ├── NavigationHelper.kt         # Reorder-to-front bottom navigation bar manager
│           │   ├── CircularProgressView.kt     # Custom Canvas view drawing circular progress ring
│           │   ├── MedicineCache.kt            # User-scoped local cache in SharedPreferences (Gson)
│           │   ├── SessionManager.kt           # User credentials, JWT, and preferences in SharedPreferences
│           │   ├── ProfileImageManager.kt      # Internal storage manager for patient avatar photos
│           │   ├── PhoneNumberHelper.kt        # Country code metadata and dynamic length filter
│           │   ├── PhoneNumberUtils.kt         # Normalizer for Indian phone dialing
│           │   ├── DialogHelper.kt             # Date/Time picker dialog builder and button styler
│           │   │
│           │   ├── MedicineAdapter.kt          # RecyclerView adapter for medicines feed (edit/delete)
│           │   ├── ScheduleAdapter.kt          # RecyclerView adapter for dashboard daily doses
│           │   ├── ReminderAdapter.kt          # RecyclerView adapter for reminder time chips
│           │   ├── ChatAdapter.kt              # RecyclerView adapter for user/AI chat message bubbles
│           │   ├── PharmacyAdapter.kt          # RecyclerView adapter for nearby healthcare facilities
│           │   │
│           │   ├── MedicineItem.kt             # UI model for medicine list item
│           │   ├── ScheduleItem.kt             # UI model for daily schedule item
│           │   ├── ChatItem.kt                 # UI model for chat message stream
│           │   ├── PharmacyItem.kt             # UI model for healthcare facility card
│           │   │
│           │   └── api/
│           │       ├── ApiService.kt           # Retrofit interface for all backend endpoints
│           │       ├── RetrofitClient.kt       # OkHttpClient singleton with JWT auth interceptor
│           │       ├── Models.kt               # Complete request/response DTOs (Gson annotations)
│           │       └── GeoapifyClient.kt       # Retrofit client for Geoapify Places API v2
│           │
│           └── res/
│               ├── layout/                     # 24 XML Layout Files (activities, dialogs, list items)
│               ├── drawable/                   # Vector drawables and shape backgrounds
│               ├── values/                     # colors.xml, strings.xml, themes.xml, dimens.xml
│               └── xml/                        # file_paths.xml (FileProvider), backup rules
│
├── Medicare-Backend/                   # [BACKEND MODULE]
│   ├── app.py                          # Flask application entry point, Blueprint registrations, CORS
│   ├── config.py                       # Environment configuration parser (.env)
│   ├── requirements.txt                # Python dependencies (Flask, PyMongo, scikit-learn, etc.)
│   ├── gunicorn.conf.py                # WSGI production server configuration
│   ├── Procfile                        # Cloud deployment entry point for Render
│   ├── test_backend.py                 # Automated unit tests for authentication and medicine CRUD
│   ├── test_ai_medication_ml.py        # Automated tests for AI safety engine, Gemini, and ML model
│   ├── test_integration.py             # Live end-to-end integration tests with MongoDB Atlas
│   ├── verify_ml_model.py              # Direct execution script verifying scikit-learn model inference
│   │
│   ├── database/
│   │   └── mongo.py                    # PyMongo connection initialization, indexing, and collection handles
│   │
│   ├── models/
│   │   ├── user.py                     # User authentication schema (email, bcrypt hash, role, provider)
│   │   ├── patient.py                  # Patient clinical profile, accessibility, completeness calculator
│   │   ├── medicine.py                 # Medication regimen schema, dosage, reminder times, compliance logs
│   │   ├── notification.py             # Prioritized notification schema (high, medium, low)
│   │   ├── doctor.py                   # Doctor profile schema and specialization
│   │   └── appointment.py              # Appointment scheduling schema
│   │
│   ├── routes/
│   │   ├── auth.py                     # Authentication endpoints: register, login, forgot-password, reset
│   │   ├── patient.py                  # Patient profile retrieval and update endpoints
│   │   ├── medicine.py                 # Medication CRUD, compliance logging, and safety analysis
│   │   ├── ai.py                       # AI conversational chat endpoint connecting to Gemini
│   │   ├── ml.py                       # Machine learning adherence risk evaluation endpoint
│   │   ├── notification.py             # Prioritized in-app alert querying and dismiss endpoints
│   │   ├── doctor.py                   # Doctor directory routes
│   │   └── appointment.py              # Appointment booking routes
│   │
│   ├── services/
│   │   ├── ai_service.py               # Google GenAI SDK integration, clinical context assembler, guardrails
│   │   ├── safety_engine.py            # Deterministic allergy cross-reactivity and drug-drug interaction checker
│   │   └── medication_knowledge.py     # RxNorm (NIH) normalization and openFDA product label extractor
│   │
│   ├── ml/
│   │   ├── train_model.py              # RandomForestClassifier training script (100 estimators, depth 6)
│   │   ├── predictor.py                # Runtime inference engine extracting 8 behavioral adherence features
│   │   ├── adherence_model.joblib      # Serialized scikit-learn Random Forest model artifact
│   │   └── model_metrics.json          # Training metrics (100% precision/recall on validation split)
│   │
│   └── utils/
│       ├── jwt_handler.py              # JWT access and refresh token generator
│       ├── password.py                 # Passlib / BCrypt cryptographic password hashing and verification
│       └── validators.py               # Input format validators for email, dates, and times
│
└── Medicare-Web/                       # [WEB COMPANION MODULE]
    ├── index.html                      # Accessible HTML5 single-page application
    ├── serve.py                        # Python HTTP server for local hosting (port 8080)
    ├── css/index.css                   # Custom design system with Atkinson Hyperlegible Next fonts
    └── js/
        ├── app.js                      # Web application controller, alarm synthesis, and speech synthesis
        └── data.js                     # Local storage data mock and caching
```

### Directory Roles & Practical Task Mapping

| Directory Path | What It Contains | Why It Is Needed | Supporting ADL Task |
|:---|:---|:---|:---|
| `Medicare/app/src/main/java/.../` | Kotlin activities, adapters, helpers, and receivers | Implements client-side application behavior, lifecycle management, UI event binding, and background alarms | **Task 1, 2, 3, 4** |
| `Medicare/app/src/main/res/layout/` | 24 XML layout files | Defines declarative UI structures for screens, dialogs, bottom sheets, and recycler items | **Task 1, 3, 4** |
| `Medicare/app/src/main/res/values/` | `colors.xml`, `themes.xml`, `strings.xml` | Enforces unified Material Design 3 styling, color tokens, and accessibility contrast themes | **Task 1** |
| `Medicare/app/src/main/java/.../api/` | Retrofit interfaces, OkHttpClient, and data models | Handles type-safe HTTP communication with the Flask backend and Geoapify Places API | **Task 2, 3, 4** |
| `Medicare-Backend/routes/` | Flask Blueprint route controllers | Defines RESTful API endpoints for authentication, medications, AI chat, ML, and notifications | **Task 2, 4** |
| `Medicare-Backend/models/` | Data-access objects and business logic | Manages schema structures, validations, and operations on MongoDB collections | **Task 2, 4** |
| `Medicare-Backend/database/` | PyMongo client initialization (`mongo.py`) | Connects to MongoDB Atlas cluster and builds collection indexes | **Task 2** |
| `Medicare-Backend/services/` | Clinical safety engine, RxNorm, openFDA, and Gemini AI | Executes deterministic pharmacopeia checks, label extraction, and conversational AI | **Task 4** |
| `Medicare-Backend/ml/` | Trained Random Forest model, trainer, predictor | Computes behavioral adherence risk from patient compliance history | **Task 4** |

---

## PART 2 — TASK-BY-TASK SOURCE FILE MAPPING

### TASK 1: Design and Develop Native Android App with Navigation, UI Components, and Data Management

#### A. Task Objective
Task 1 establishes the client-side native Android foundation:
- Building an intuitive, senior-friendly user interface conforming to Material Design 3.
- Implementing seamless navigation across five primary application tabs using custom bottom navigation.
- Providing custom UI components (such as a dynamic circular progress gauge).
- Enabling local client-side data management using a persistent cache in `SharedPreferences` to ensure instantaneous UI rendering and offline resilience.

#### B. Features Implemented
- **Fully Implemented:**
  - Dynamic Dashboard (`HomeActivity`): Greeting header, custom circular adherence gauge (`CircularProgressView`), next upcoming medication card with countdown status, and daily dose schedule `RecyclerView`.
  - Bottom Navigation Bar (`NavigationHelper`, `layout_bottom_navigation.xml`): Tab switching across Home, Medicines, Pharmacy, AI Assistant, and Profile using `Intent.FLAG_ACTIVITY_REORDER_TO_FRONT` with zero UI flicker.
  - Medication List & Filtering (`MedicinesActivity`, `MedicineAdapter`): Categorized display with filter chips ("All", "Morning", "Evening", "As Needed"), swipe-free recycler cards with status indicators, and Floating Action Button (FAB) navigation.
  - Form Data Capture (`AddMedicineActivity`): 2x2 grid type selectors (Tablet, Capsule, Syrup, Injection), frequency selector chips, DatePicker dialogs, TimePicker dialogs, and dynamic reminder time list.
  - Client Data Caching (`MedicineCache`): User-scoped persistent storage in Android `SharedPreferences` using Gson serialization. Data loads instantly on boot before background network sync completes.
  - User Session Storage (`SessionManager`): Persists JWT tokens, onboarding status, user name, and accessibility preferences.
  - Dynamic Accessibility Engine (`BaseActivity`, `AccessibilityHelper`): Runtime font scaling (Small, Medium, Large, Extra Large) and High Contrast Mode toggles.
- **Partially Implemented:**
  - Appointment Booking UI: Backend appointment routes exist in Flask (`routes/appointment.py`), but no dedicated Appointment UI screen is implemented in the Android client.
- **Documentation-Only / Unverified:**
  - Fragment-based navigation: The project intentionally utilizes Activity-based navigation with `FLAG_ACTIVITY_REORDER_TO_FRONT` instead of Jetpack Navigation Fragments.

#### C. Exact Files Involved (Task 1)

| File Path (Relative to Root) | File Type | Purpose | Connection to Task 1 |
|:---|:---|:---|:---|
| `Medicare/app/src/main/res/layout/activity_home.xml` | XML Layout | Dashboard screen UI structure | Defines progress ring container, upcoming card, and schedule list |
| `Medicare/app/src/main/res/layout/activity_medicines.xml` | XML Layout | Medicines list screen UI structure | Defines search bar, filter chips, recycler view, and add FAB |
| `Medicare/app/src/main/res/layout/activity_add_medicine.xml` | XML Layout | Medication entry form UI structure | Defines form inputs, type selector cards, date/time pickers |
| `Medicare/app/src/main/res/layout/layout_bottom_navigation.xml` | XML Layout | Global persistent bottom navigation bar | Reusable include layout with active/inactive tab pill states |
| `Medicare/app/src/main/res/layout/item_schedule.xml` | XML Layout | Individual daily schedule row item | Defines status icon, medicine name, dose, time, and badge |
| `Medicare/app/src/main/res/layout/item_medicine.xml` | XML Layout | Individual medicine card in inventory | Defines status color strip, type icon, edit, and delete icons |
| `Medicare/app/src/main/java/com/example/medicare/HomeActivity.kt` | Kotlin Source | Dashboard controller | Inflates `activity_home.xml`, calculates progress fraction, populates schedule |
| `Medicare/app/src/main/java/com/example/medicare/MedicinesActivity.kt` | Kotlin Source | Medication inventory controller | Inflates `activity_medicines.xml`, handles chip filtering, launches add form |
| `Medicare/app/src/main/java/com/example/medicare/AddMedicineActivity.kt` | Kotlin Source | Medication creation/editing controller | Handles form inputs, date/time dialogs, single-choice type cards |
| `Medicare/app/src/main/java/com/example/medicare/NavigationHelper.kt` | Kotlin Source | Centralized navigation manager | Binds click listeners to 5 tabs and manages activity flags |
| `Medicare/app/src/main/java/com/example/medicare/CircularProgressView.kt` | Kotlin Source | Custom 2D Canvas progress indicator | Overrides `onDraw()` to render circular adherence ring |
| `Medicare/app/src/main/java/com/example/medicare/MedicineCache.kt` | Kotlin Source | Local-first medication storage | Serializes and caches medication lists in private `SharedPreferences` |
| `Medicare/app/src/main/java/com/example/medicare/SessionManager.kt` | Kotlin Source | Client session & preferences manager | Stores JWT tokens, username, and accessibility settings |
| `Medicare/app/src/main/java/com/example/medicare/BaseActivity.kt` | Kotlin Source | Base Activity class | Handles system bar insets, font scaling, and high contrast on resume |

#### D. Step-by-Step Implementation Procedure (Task 1)
1. **Layout Inflation**: In `HomeActivity.onCreate()`, `setContentView(R.layout.activity_home)` inflates the dashboard XML.
2. **View Initialization**: Views are bound via `findViewById()` (e.g., `circularProgress = findViewById(R.id.circular_progress)`).
3. **Local Cache Loading**: `MedicineCache.loadFromPrefs(this)` deserializes previously saved medicines from `SharedPreferences` via Gson, populating the UI without waiting for network responses.
4. **Navigation Setup**: `NavigationHelper.setupNavigation(this, R.id.tab_home)` highlights the Home tab in `layout_bottom_navigation.xml` and attaches click listeners to navigate to other tabs using `Intent.FLAG_ACTIVITY_REORDER_TO_FRONT`.
5. **Schedule Population**: `populateSchedule()` iterates over active medications for the current date, evaluates dose times against the current clock, and updates `ScheduleAdapter`.
6. **Custom Canvas Rendering**: `CircularProgressView.setProgress(progressPercent)` computes `sweepAngle = progressValue * 360f` and calls `invalidate()` to draw the green arc on Android Canvas.
7. **Filter Chips**: In `MedicinesActivity`, chip click listeners (`chipMorning.setOnClickListener`) update `currentFilter` and call `applyFilter()`, filtering the dataset and notifying `MedicineAdapter`.

#### E. Demonstration Procedure (Task 1)
1. **Open Screen:** Launch the Medicare+ application (or log in). The application opens `HomeActivity`.
2. **Action 1 (Dashboard Review):** Observe the circular progress gauge at the top. Note the upcoming dose card showing the medicine name, dosage, time, and medicine icon.
3. **Action 2 (Tab Navigation):** Tap the "Medicines" tab in the bottom bar. Notice the immediate transition to `MedicinesActivity` without reload lag.
4. **Action 3 (Filtering):** Tap the "Morning" chip; only AM doses appear. Tap "All" to restore the list.
5. **Action 4 (Adding a Medicine Form):** Tap the floating `+` button to open `AddMedicineActivity`. Tap "Capsule" to see the active card color change to teal. Tap "Start Date" to open `DatePickerDialog`.
6. **Files to Open During Viva:**
   - [`HomeActivity.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/HomeActivity.kt#L45-L85) (shows `setContentView`, `findViewById`, and cache initialization)
   - [`CircularProgressView.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/CircularProgressView.kt#L36-L50) (shows 2D Canvas custom rendering)
   - [`NavigationHelper.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/NavigationHelper.kt#L88-L141) (shows reorder-to-front navigation logic)

---

### TASK 2: Develop Data-Storage and Backend Components, Integrate Cloud Storage, and Enable Network Communication

#### A. Task Objective
Task 2 focuses on full-stack data persistence and client-server communication:
- Developing a RESTful API backend using Python Flask.
- Integrating a cloud document database (MongoDB Atlas) for persistent storage of users, patient profiles, medications, compliance logs, and notifications.
- Securing communication using JSON Web Tokens (JWT) with cryptographic password hashing (bcrypt).
- Connecting the Android application via Retrofit 2 and OkHttp 3 for asynchronous network requests.

#### B. Features Implemented
- **Fully Implemented:**
  - Flask Application Architecture (`app.py`, `config.py`): Modular architecture using Flask Blueprints (`auth`, `patient`, `medicine`, `notification`, `ai`, `ml`).
  - Cloud Document Database (`database/mongo.py`): PyMongo connection to MongoDB Atlas with automated collection index creation (`email` unique index, compound indexes on `patient_id` and timestamps).
  - Secure Authentication (`routes/auth.py`, `models/user.py`):
    - Registration: `POST /api/auth/register` (hashes password using bcrypt, initializes patient profile in MongoDB).
    - Login: `POST /api/auth/login` (verifies bcrypt hash, returns HS256 JWT access and refresh tokens).
    - Password Reset: `POST /api/auth/forgot-password` & `POST /api/auth/reset-password` (generates 6-digit verification code, updates bcrypt hash).
  - Patient Profile & Accessibility Sync (`routes/patient.py`, `models/patient.py`):
    - `GET /api/patients/profile`: Fetches demographic, emergency contact, allergy, and accessibility configurations.
    - `PUT /api/patients/profile`: Updates profile details and calculates dynamic completeness percentage.
  - Medication CRUD & Compliance Logging (`routes/medicine.py`, `models/medicine.py`):
    - `GET /api/medicines`: Retrieves user-specific active medications.
    - `POST /api/medicines`: Inserts medication schedule document into MongoDB.
    - `PUT /api/medicines/<id>`: Updates existing medication document.
    - `DELETE /api/medicines/<id>`: Deletes medication document with patient ownership verification.
    - `POST /api/medicines/<id>/log`: Appends compliance record (`taken`, `skipped`, `snoozed`) to the medication document.
  - Android HTTP Client (`RetrofitClient.kt`, `ApiService.kt`):
    - Centralized singleton pointing to production backend on Render (`https://medicare-backend-me50.onrender.com/api/`).
    - OkHttp interceptor automatically injecting `Authorization: Bearer <token>` into outgoing request headers.
    - Global 401 handling redirecting expired sessions to `LoginActivity`.
  - Local Profile Picture Storage (`ProfileImageManager.kt`):
    - Patient avatar photo persistence in app-private internal storage (`context.filesDir/profile_picture.jpg`) utilizing Android PhotoPicker.

#### C. Exact Files Involved (Task 2)

| File Path (Relative to Root) | File Type | Purpose | Connection to Task 2 |
|:---|:---|:---|:---|
| `Medicare-Backend/app.py` | Python Script | Flask server entry point | Initializes app, CORS, JWT extension, PyMongo, and registers Blueprints |
| `Medicare-Backend/config.py` | Python Script | Configuration loader | Loads environment variables (MongoDB URI, JWT secret, ports) |
| `Medicare-Backend/database/mongo.py` | Python Script | Database manager | Connects to MongoDB Atlas, manages collection handles, builds indexes |
| `Medicare-Backend/models/user.py` | Python Script | User data-access object | Manages `users` collection in MongoDB, creates bcrypt hashes |
| `Medicare-Backend/models/patient.py` | Python Script | Patient profile DAO | Manages `patients` collection, computes profile completion percentage |
| `Medicare-Backend/models/medicine.py` | Python Script | Medication DAO | Manages `medicines` collection, handles CRUD and compliance log array |
| `Medicare-Backend/routes/auth.py` | Python Script | Authentication routes | Implements `/register`, `/login`, `/forgot-password`, `/reset-password` |
| `Medicare-Backend/routes/medicine.py` | Python Script | Medicine routes | Implements `GET`, `POST`, `PUT`, `DELETE /api/medicines` and `/log` |
| `Medicare-Backend/routes/patient.py` | Python Script | Profile routes | Implements `GET /profile` and `PUT /profile` |
| `Medicare/app/src/main/java/com/example/medicare/api/RetrofitClient.kt` | Kotlin Source | HTTP Client singleton | Configures OkHttpClient, 60s timeouts, JWT header interceptor |
| `Medicare/app/src/main/java/com/example/medicare/api/ApiService.kt` | Kotlin Source | Retrofit API interface | Defines HTTP methods, paths, and DTO parameters |
| `Medicare/app/src/main/java/com/example/medicare/api/Models.kt` | Kotlin Source | Network DTO classes | Serializes/deserializes JSON request and response payloads via Gson |
| `Medicare/app/src/main/java/com/example/medicare/ProfileImageManager.kt` | Kotlin Source | Image storage helper | Saves patient avatar images into internal file storage (`context.filesDir`) |

#### D. Step-by-Step Implementation Procedure (Task 2)
1. **Network Request Trigger:** User enters medication details in `AddMedicineActivity` and clicks "Save Medicine".
2. **DTO Construction:** Form inputs are mapped into a `MedicineRequest` data class instance (`name`, `type`, `dosage`, `frequency`, `startDate`, `endDate`, `reminderTimes`).
3. **Retrofit Invocation:** `RetrofitClient.getApiService(this).createMedicine(request)` creates an asynchronous Retrofit `Call<MedicineResponse>`.
4. **Header Injection:** OkHttp's interceptor intercepts the outgoing request, queries `SessionManager.getAccessToken()`, and appends `Authorization: Bearer <token>`.
5. **Backend Route Reception:** Flask router matches `POST /api/medicines` in `routes/medicine.py`.
6. **JWT Validation:** `@jwt_required()` verifies the token signature and extracts `patient_id`.
7. **Database Storage:** `MedicineModel.create_medicine(...)` creates a BSON document and invokes `db.medicines.insert_one(medicine_doc)`.
8. **Response Generation:** Backend returns HTTP 201 with `{ "success": true, "medicine_id": "...", "alerts": [...] }`.
9. **Client Handling:** In `onResponse()`, Android extracts the generated ID, updates `MedicineCache`, schedules exact alarms, and finishes the Activity.

#### E. Demonstration Procedure (Task 2)
1. **Open Screen:** Open `MedicinesActivity` and tap the `+` Floating Action Button.
2. **Action 1 (Create Medicine):** Enter name `"Paracetamol"`, dosage `"500"`, type `"Tablet"`, frequency `"Daily"`, and tap "Save Medicine".
3. **Action 2 (Verify Response):** Observe the toast "Medicine saved successfully". The new card appears in `MedicinesActivity`.
4. **Action 3 (Log Compliance):** Tap the card or upcoming dose on Home screen to open `ReminderAlarmActivity`. Tap "Taken".
5. **Expected Output:** Toast displays "Medication logged as Taken!". On returning to the Home screen, the circular progress gauge updates to reflect the completed dose.
6. **Files to Open During Viva:**
   - [`RetrofitClient.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/api/RetrofitClient.kt#L38-L71) (shows OkHttp JWT interceptor and 401 handler)
   - [`routes/medicine.py`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare-Backend/routes/medicine.py#L24-L74) (shows JWT-protected route and MongoDB document insertion)
   - [`models/medicine.py`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare-Backend/models/medicine.py#L7-L29) (shows MongoDB schema and document structure)

---

### TASK 3: Integrate Google Maps and Geolocation to Display Nearby Healthcare Facilities and Support Navigation

#### A. Task Objective
Task 3 provides location-based healthcare facility discovery and routing:
- Rendering an interactive map display within the native Android application.
- Requesting and managing Android runtime location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).
- Retrieving the user's real-time physical GPS coordinates.
- Querying verified nearby healthcare facilities (pharmacies, hospitals, clinics, emergencies, laboratories, dentists) sorted by geographical proximity.
- Initiating turn-by-turn directions and navigation by dispatching implicit Android Intents to native Google Maps.

#### B. Features Implemented
- **Fully Implemented:**
  - In-App Interactive Vector Map (`PharmacyActivity`, `activity_pharmacy.xml`): Uses the MapLibre Native Android SDK (`org.maplibre.gl:android-sdk:10.2.0`) to render vector tiles styled via Geoapify (`https://maps.geoapify.com/v1/styles/osm-bright/style.json`).
  - Geolocation Acquisition: Uses Google Play Services `FusedLocationProviderClient` (`fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)`) with a fallback to Android system `LocationManager`.
  - Runtime Location Permissions: Handles permissions via `ActivityResultContracts.RequestMultiplePermissions()`, updating map components or displaying clear guidance when permissions are denied.
  - Nearby Healthcare Discovery (`GeoapifyClient.kt`): Queries the Geoapify Places API v2 with proximity bias (`bias = proximity:lon,lat`, `limit = 50`) without restrictive circular cutoffs, retrieving verified facilities sorted from closest to farthest.
  - Multi-Category Filtering (`ChipGroup`): Six categories supported:
    1. Pharmacies (`healthcare.pharmacy`)
    2. Hospitals (`healthcare.hospital`)
    3. Doctors & Clinics (`healthcare.clinic_or_praxis`)
    4. Emergencies (`emergency.ambulance_station,healthcare.hospital`)
    5. Laboratories (`healthcare.clinic_or_praxis.radiology,healthcare.clinic_or_praxis`)
    6. Dentists (`healthcare.dentist`)
  - Interactive Pin Selection: Tapping a map marker smoothly centers the camera, highlights the facility card in `RecyclerView`, and displays the facility details dialog.
  - Navigation & Turn-by-Turn Directions: Dispatches an explicit Android Intent `geo:0,0?q=lat,lon(FacilityName)` targeted directly to the native Google Maps app (`com.google.android.apps.maps`), with a fallback to Google Maps Web Directions API (`https://www.google.com/maps/dir/?api=1&destination=lat,lon`).
  - Direct Facility Calling: Launches the system phone dialer via `Intent.ACTION_DIAL` (`tel:<phone>`).

#### C. Exact Files Involved (Task 3)

| File Path (Relative to Root) | File Type | Purpose | Connection to Task 3 |
|:---|:---|:---|:---|
| `Medicare/app/src/main/res/layout/activity_pharmacy.xml` | XML Layout | Healthcare map screen layout | Contains `com.mapbox.mapboxsdk.maps.MapView`, search bar, category chips, recycler |
| `Medicare/app/src/main/res/layout/item_pharmacy.xml` | XML Layout | Facility list card layout | Contains facility title, distance/address, Directions button, Call button |
| `Medicare/app/src/main/java/com/example/medicare/PharmacyActivity.kt` | Kotlin Source | Map & geolocation controller | Initializes map, requests GPS, queries facilities, triggers navigation |
| `Medicare/app/src/main/java/com/example/medicare/PharmacyAdapter.kt` | Kotlin Source | RecyclerView adapter for facilities | Binds facility items, wires card click, Directions, and Call actions |
| `Medicare/app/src/main/java/com/example/medicare/PharmacyItem.kt` | Kotlin Source | Facility data model | Holds place ID, name, coordinates, address, phone number, website |
| `Medicare/app/src/main/java/com/example/medicare/api/GeoapifyClient.kt` | Kotlin Source | Retrofit client for Places API | Interfaces with `https://api.geoapify.com/v2/places` |
| `Medicare/app/src/main/AndroidManifest.xml` | XML Configuration | Android manifest | Declares `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and Google Maps key |

#### D. Step-by-Step Implementation Procedure (Task 3)
1. **Map Engine Initialization**: `Mapbox.getInstance(this)` initializes the MapLibre engine prior to `setContentView(R.layout.activity_pharmacy)`.
2. **Asynchronous Map Loading**: `mapView.getMapAsync { map -> ... }` loads the Geoapify vector style JSON asynchronously.
3. **Permission Check & GPS Request**: `checkPermissionsAndFetchLocation()` checks if `ACCESS_FINE_LOCATION` is granted. If not, launches `requestPermissionLauncher`. If granted, calls `fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)`.
4. **Camera Animation**: Once coordinates are acquired, `mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.0))` centers the map on the user.
5. **Facility Query**: `performNearbySearch()` sends an HTTP GET request via `GeoapifyClient` passing category, user coordinates, and API key.
6. **Marker Rendering**: In `onResponse()`, results are parsed, map pins are added via `MarkerOptions().position(LatLng(lat, lon)).title(name)`, and `PharmacyAdapter` updates the bottom list.
7. **Marker Click Event**: `map.setOnMarkerClickListener` intercepts marker taps, smoothly scrolls the RecyclerView to that facility, and opens `showPlaceDetailsDialog()`.
8. **Navigation Dispatch**: Clicking "Directions" calls `launchNavigationIntent()`, constructing `Uri.parse("geo:0,0?q=lat,lon(name)")` and starting an activity with package `com.google.android.apps.maps`.

#### E. Demonstration Procedure (Task 3)
1. **Open Screen:** Tap the "Pharmacy" (Map pin) tab in the bottom navigation bar.
2. **Action 1 (Location Fix & Map Display):** Allow location permission if prompted. The map loads and animates to your current GPS position with a blue compass indicator.
3. **Action 2 (Facility Review):** Observe the facility markers and bottom cards. The subtitle displays `"Found X verified facilities near you"`.
4. **Action 3 (Category Switching):** Tap the "Hospitals" chip. The map refreshes, displaying nearby hospitals sorted by proximity.
5. **Action 4 (Pin Tap):** Tap any red pin on the map. The map centers on the pin and opens the facility dialog with address, phone number, and action buttons.
6. **Action 5 (Launch Google Maps):** Tap "Directions". The native Google Maps app opens with turn-by-turn navigation already targeted to the selected facility.
7. **Files to Open During Viva:**
   - [`PharmacyActivity.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/PharmacyActivity.kt#L120-L153) (shows MapView initialization and style loading)
   - [`PharmacyActivity.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/PharmacyActivity.kt#L230-L260) (shows FusedLocationProviderClient GPS lookup)
   - [`PharmacyActivity.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/PharmacyActivity.kt#L647-L674) (shows explicit Google Maps Navigation Intent dispatch)

---

### TASK 4: Implement AI Chatbot, ML-Based Features, and AI-Assisted Alerts and Notifications

#### A. Task Objective
Task 4 integrates modern artificial intelligence and machine learning into patient healthcare workflows:
- Implementing an AI healthcare conversational assistant that grounds answers on the patient's verified health records while enforcing strict clinical safety guardrails.
- Deploying a trained Machine Learning model to evaluate patient medication adherence risk and personalize reminders.
- Implementing an automated clinical safety engine that checks allergy cross-reactivities and drug-drug interactions using RxNorm and openFDA databases.
- Implementing an exact background alarm and notification system using Android `AlarmManager`, wake-locks, and full-screen intents.

#### B. Features Implemented
- **Fully Implemented:**
  - AI Conversational Chatbot (`AIAssistantActivity`, `routes/ai.py`, `services/ai_service.py`):
    - Connects to the Google Gemini API (`gemini-flash-lite-latest`) via the official `google-genai` Python SDK.
    - Assembles dynamic context from MongoDB: active medications, recorded allergies, conditions, and ML adherence risk scores.
    - Safety Guardrails: System prompt strictly prohibits diagnosing diseases, prescribing medications, or modifying dosages. Automatically provides emergency instructions for severe symptoms.
    - Rule-Based Fallback: If cloud AI services or networks are unreachable, provides verified clinical fallback responses.
  - Machine Learning Adherence Pipeline (`ml/train_model.py`, `ml/predictor.py`):
    - Trained supervised machine learning model (`RandomForestClassifier`, 100 estimators, max depth 6) serialized as `adherence_model.joblib`.
    - Extracts 8 behavioral metrics: `medication_count`, `daily_medication_events`, `missed_doses_7d`, `missed_doses_30d`, `adherence_percentage`, `reminder_response_rate`, `consecutive_missed_days`, `medication_complexity`.
    - Predicts adherence risk class: `LOW`, `MEDIUM`, or `HIGH`.
    - Safe Boundary: ML is strictly behavioral; it does not predict clinical pathology or alter prescriptions.
  - Deterministic Medication Safety Engine (`services/safety_engine.py`, `medication_knowledge.py`):
    - Normalizes drug names via NIH RxNorm REST API and retrieves official FDA product labels from openFDA.
    - Evaluates patient allergies against cross-reactivity classes (penicillins, sulfonamides, NSAIDs, aspirin, opioids, statins, ACE inhibitors, acetaminophen, macrolides, fluoroquinolones).
    - Detects drug-drug interactions against all active prescriptions.
    - Creates persistent prioritized notifications (`HIGH`, `MEDIUM`, `LOW`) in MongoDB.
  - In-App Medication Safety Alert Dialog (`AddMedicineActivity`):
    - If a patient with a Penicillin allergy saves `"Amoxicillin"`, the backend returns an allergy alert. Android displays a red dialog with a direct button `"Ask AI to Explain"`, which opens `AIAssistantActivity` with pre-loaded alert context.
  - Exact Alarm & Background Reminder System:
    - `AlarmScheduler.kt`: Schedules exact alarms using `AlarmManager.setAlarmClock()` and `setExactAndAllowWhileIdle()` with `RTC_WAKEUP`.
    - `AlarmReceiver.kt`: Acquires `PowerManager.WakeLock` (`ACQUIRE_CAUSES_WAKEUP`), launches `ReminderAlarmActivity`, and posts a high-priority notification with full-screen intent.
    - `ReminderAlarmActivity.kt`: Full-screen lockscreen activity (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`). Plays alarm sound via `RingtoneManager`, vibrates via `Vibrator`, speaks reminder via `TextToSpeech`, and provides "Taken", "Snooze" (15 min), and "Skip" actions.
    - `BootReceiver.kt`: Automatically reschedules all active medication alarms on device reboot (`ACTION_BOOT_COMPLETED`).
  - Prioritized In-App Notification Center (`NotificationHelper.kt`, `dialog_notifications.xml`):
    - Tapping the top-right notification bell opens an interactive bottom sheet containing categorized notifications (All, Alerts, Reminders, Tips) with direct "Explain with AI" actions.
- **Partially Implemented:**
  - Chatbot Image Attachment: `AIAssistantActivity` supports selecting images using Android PhotoPicker (`PickVisualMedia`), but because the backend AI route accepts text queries, selecting an image displays a toast explaining that image analysis is pending backend implementation.

#### C. Exact Files Involved (Task 4)

| File Path (Relative to Root) | File Type | Purpose | Connection to Task 4 |
|:---|:---|:---|:---|
| `Medicare/app/src/main/res/layout/activity_ai_assistant.xml` | XML Layout | Chatbot screen UI layout | Defines chat recycler view, disclaimer banner, message composer |
| `Medicare/app/src/main/res/layout/item_chat_assistant.xml` | XML Layout | AI response message bubble | Formats AI responses with bot icon, message card, and timestamp |
| `Medicare/app/src/main/res/layout/item_chat_user.xml` | XML Layout | User chat message bubble | Formats patient's questions with colored pill container |
| `Medicare/app/src/main/res/layout/item_chat_suggestions.xml` | XML Layout | Quick suggestion chips | Horizontally scrolling chips with pre-composed inquiries |
| `Medicare/app/src/main/res/layout/activity_reminder_alarm.xml` | XML Layout | Fullscreen lockscreen alarm UI | Colored header banner, large alarm badge, Taken/Snooze/Skip buttons |
| `Medicare/app/src/main/res/layout/dialog_notifications.xml` | XML Layout | Notifications bottom sheet | Filter chips, notification recycler view, empty state |
| `Medicare/app/src/main/java/com/example/medicare/AIAssistantActivity.kt` | Kotlin Source | AI Chatbot controller | Handles message input, PhotoPicker, context extras, dispatches API call |
| `Medicare/app/src/main/java/com/example/medicare/AlarmScheduler.kt` | Kotlin Source | AlarmManager scheduler | Computes reminder calendar times and schedules exact alarms |
| `Medicare/app/src/main/java/com/example/medicare/AlarmReceiver.kt` | Kotlin Source | BroadcastReceiver | Handles wake-lock, posts high-priority notification, launches alarm |
| `Medicare/app/src/main/java/com/example/medicare/ReminderAlarmActivity.kt` | Kotlin Source | Fullscreen alarm activity | TextToSpeech voice reminders, ringtone sound, haptics, compliance logs |
| `Medicare/app/src/main/java/com/example/medicare/BootReceiver.kt` | Kotlin Source | Boot BroadcastReceiver | Re-queries backend and reschedules alarms on `BOOT_COMPLETED` |
| `Medicare/app/src/main/java/com/example/medicare/NotificationHelper.kt` | Kotlin Source | In-app notification manager | Fetches prioritized notifications and displays bottom sheet |
| `Medicare-Backend/routes/ai.py` | Python Script | AI chat endpoint | Implements `POST /api/ai/chat` |
| `Medicare-Backend/services/ai_service.py` | Python Script | Gemini AI service | Connects to Gemini via `google-genai` SDK, injects patient health context |
| `Medicare-Backend/ml/predictor.py` | Python Script | ML inference engine | Extracts 8 behavioral features, executes `adherence_model.joblib` |
| `Medicare-Backend/ml/train_model.py` | Python Script | ML training pipeline | Trains `RandomForestClassifier` and serializes model artifact |
| `Medicare-Backend/services/safety_engine.py` | Python Script | Clinical safety engine | Deterministic allergy and drug interaction verification logic |
| `Medicare-Backend/services/medication_knowledge.py` | Python Script | Knowledge service | Interfaces with RxNorm API and openFDA drug label API |

#### D. Step-by-Step Implementation Procedure (Task 4)
1. **Clinical Safety Verification:** User creates a medication in `AddMedicineActivity`. The backend invokes `check_allergy_conflict()` in `services/safety_engine.py`. If the patient is allergic to Penicillin and adds Amoxicillin, a high-priority alert is generated and stored in MongoDB.
2. **Safety Dialog & Context Passing:** Android receives the alert in `MedicineResponse.alerts` and displays a modal dialog with "Ask AI to Explain".
3. **Chat Launch with Context:** Tapping the button launches `AIAssistantActivity`, passing the alert text via `intent.putExtra("context_message", alert.message)`.
4. **AI Query Assembly:** `AIAssistantActivity.sendMessage()` creates a `ChatRequest` with user text and context map.
5. **Backend Context Injection:** `routes/ai.py` calls `build_user_context()` in `services/ai_service.py`, gathering the patient's age, blood group, recorded allergies, active medications, and ML adherence risk from MongoDB.
6. **Gemini Invocation:** The prompt, structured context, and strict system guardrails are dispatched to Google Gemini (`gemini-flash-lite-latest`) via `genai.Client`.
7. **Safe Explanation Output:** Gemini returns an empathetic explanation advising the user to verify with their doctor. The response replaces the loading bubble in Android `RecyclerView`.
8. **Alarm Execution:** When the scheduled reminder time arrives, `AlarmManager` fires `AlarmReceiver`. The receiver acquires a `FULL_WAKE_LOCK`, wakes the screen, and launches `ReminderAlarmActivity`.
9. **Multimodal Alerting:** `ReminderAlarmActivity` speaks `"Time to take your medication: Amoxicillin, 500 mg"` via `TextToSpeech`, plays the system alarm sound, and vibrates.
10. **Compliance Logging:** Tapping "Taken" dispatches `POST /api/medicines/<id>/log`, updating the compliance array in MongoDB Atlas.

#### E. Demonstration Procedure (Task 4)
1. **Open Screen:** Navigate to the "AI Assistant" (Robot) tab in the bottom navigation bar.
2. **Action 1 (Chatbot Interaction):** Tap the suggestion chip `"What should I do if I miss a dose?"` or type a question in the message composer.
3. **Expected Output:** A temporary `"Consulting MediCare+ AI..."` bubble appears, replaced within 1-2 seconds by Gemini's context-aware answer with zero hallucinated dosages.
4. **Action 2 (Medication Safety Alert):** Go to "Medicines" -> `+` FAB. Type `"Amoxicillin"`, set dosage `"500"`, and click "Save Medicine" (assuming your profile has a Penicillin allergy).
5. **Expected Output:** A prominent dialog titled `"🔴 Medication Safety Alert"` appears warning of penicillin cross-reactivity. Tap `"Ask AI to Explain"`.
6. **Expected Output:** The AI Chatbot automatically opens and streams a complete explanation of the allergy conflict.
7. **Action 3 (Notification Center):** Tap the bell icon on the top header. The bottom sheet opens showing the prioritized alert with a red badge and an "Explain with AI" action.
8. **Action 4 (Alarm Demonstration):** From the Home screen, tap any item in the today's schedule recycler. `ReminderAlarmActivity` launches, initiating voice speech and alarm sound. Tap "Taken" to log the dose.
9. **Files to Open During Viva:**
   - [`services/safety_engine.py`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare-Backend/services/safety_engine.py#L19-L63) (shows deterministic allergy cross-reactivity map)
   - [`services/ai_service.py`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare-Backend/services/ai_service.py#L18-L43) (shows Gemini SDK initialization and clinical guardrail system prompt)
   - [`ml/predictor.py`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare-Backend/ml/predictor.py#L84-L157) (shows 8-feature extraction logic for Random Forest inference)
   - [`AlarmScheduler.kt`](file:///c:/Users/lenovo/AndroidStudioProjects/Medicare/app/src/main/java/com/example/medicare/AlarmScheduler.kt#L135-L175) (shows AlarmManager exact alarm clock API usage)

---

## PART 3 — XML TO KOTLIN/JAVA CONNECTION

### Master Connection Table

| Feature / Screen | XML File | Kotlin Source File | Connection Method | Main Event Handlers |
|:---|:---|:---|:---|:---|
| Dashboard & Schedule | `activity_home.xml` | `HomeActivity.kt` | `setContentView()` + `findViewById()` | `btn_take_now.setOnClickListener`, `btn_view_all.setOnClickListener`, `btn_notification.setOnClickListener` |
| Daily Schedule Item | `item_schedule.xml` | `ScheduleAdapter.kt` | `LayoutInflater.inflate()` in `onCreateViewHolder()` | `itemView.setOnClickListener` (launches `ReminderAlarmActivity`) |
| Medicine Inventory | `activity_medicines.xml` | `MedicinesActivity.kt` | `setContentView()` + `findViewById()` | `fab_add_medicine.setOnClickListener`, `chip_morning.setOnClickListener`, `btn_notification.setOnClickListener` |
| Medicine Card Item | `item_medicine.xml` | `MedicineAdapter.kt` | `LayoutInflater.inflate()` in `onCreateViewHolder()` | `btn_edit.setOnClickListener`, `btn_delete.setOnClickListener` |
| Add Medicine Form | `activity_add_medicine.xml` | `AddMedicineActivity.kt` | `setContentView()` + `findViewById()` | `card_type_tablet.setOnClickListener`, `layout_start_date.setOnClickListener`, `btn_save_medicine.setOnClickListener` |
| AI Chat Interface | `activity_ai_assistant.xml` | `AIAssistantActivity.kt` | `setContentView()` + `findViewById()` | `btn_input_send.setOnClickListener`, `btn_input_add.setOnClickListener` |
| AI Chat Message Bubbles | `item_chat_user.xml`, `item_chat_assistant.xml` | `ChatAdapter.kt` | `LayoutInflater.inflate()` with multiple view types | Suggestion chip click callbacks |
| Healthcare Map Screen | `activity_pharmacy.xml` | `PharmacyActivity.kt` | `setContentView()` + `findViewById()` | `mapView.getMapAsync()`, `map.setOnMarkerClickListener`, `btn_my_location.setOnClickListener` |
| Facility Card Item | `item_pharmacy.xml` | `PharmacyAdapter.kt` | `LayoutInflater.inflate()` in `onCreateViewHolder()` | `btn_navigate.setOnClickListener`, `btn_call.setOnClickListener`, `itemView.setOnClickListener` |
| Patient Profile Screen | `activity_profile.xml` | `ProfileActivity.kt` | `setContentView()` + `findViewById()` | `btn_call_emergency.setOnClickListener`, `btn_edit_profile.setOnClickListener`, `switch_contrast.setOnCheckedChangeListener` |
| Fullscreen Alarm Screen | `activity_reminder_alarm.xml` | `ReminderAlarmActivity.kt` | `setContentView()` + `findViewById()` | `btn_mark_taken.setOnClickListener`, `btn_snooze.setOnClickListener`, `btn_skip_dose.setOnClickListener` |
| Bottom Navigation Bar | `layout_bottom_navigation.xml` | `NavigationHelper.kt` | `findViewById()` on included view | `tab_home.setOnClickListener`, `tab_medicines.setOnClickListener`, `tab_pharmacy.setOnClickListener`, `tab_ai.setOnClickListener`, `tab_profile.setOnClickListener` |
| Notification Sheet | `dialog_notifications.xml` | `NotificationHelper.kt` | `LayoutInflater.inflate()` in `BottomSheetDialogFragment` | Category chip listeners, "Explain with AI" button click listeners |

### Code Verification: Connection Mechanisms
- **Absence of ViewBinding / DataBinding:** The project does not enable ViewBinding or DataBinding in `buildFeatures`. All views are inflated using standard Android `setContentView(R.layout...)` and referenced using `findViewById<T>(R.id...)`.
- **RecyclerView Adapters:** Adapters inherit from `RecyclerView.Adapter<VH>()`, inflating item layouts inside `onCreateViewHolder()` via `LayoutInflater.from(parent.context).inflate(...)` and caching references inside a nested `ViewHolder` class.
- **Custom View Invocations:** `CircularProgressView` is declared directly in `activity_home.xml` using its full package identifier `<com.example.medicare.CircularProgressView android:id="@+id/circular_progress" .../>`.

### Verified Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                           XML LAYOUT DECLARATION                                  |
|   (e.g., activity_add_medicine.xml: EditTexts, MaterialCardViews, Save Button)    |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼ setContentView(R.layout.activity_add_medicine)
+-----------------------------------------------------------------------------------+
|                        ACTIVITY / ADAPTER INFLATION                               |
|   (e.g., AddMedicineActivity: findViewById<Button>(R.id.btn_save_medicine))       |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼ setOnClickListener { saveMedicineData(...) }
+-----------------------------------------------------------------------------------+
|                            UI EVENT DISPATCH & VALIDATION                         |
|   (Validates medicine name, selects dosage unit, formats reminder times)          |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼ RetrofitClient.getApiService(this).createMedicine(req)
+-----------------------------------------------------------------------------------+
|                         NETWORK & PERSISTENCE LAYER                               |
|   (OkHttp injects JWT -> Flask API Gateway -> PyMongo inserts to MongoDB Atlas)   |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼ Callback<MedicineResponse>.onResponse()
+-----------------------------------------------------------------------------------+
|                          UI UPDATE & ALARM TRIGGER                                |
|   (Updates MedicineCache -> AlarmScheduler.scheduleAlarms() -> Toasts / Finishes) |
+-----------------------------------------------------------------------------------+
```

---

## PART 4 — BACKEND AND DATABASE CONNECTIONS

### Verified Backend Architecture
- **Framework:** Python Flask 3.x with modular Blueprints.
- **Production Server:** Gunicorn WSGI multi-worker server configured in `gunicorn.conf.py` and `Procfile`.
- **Database:** MongoDB Atlas 7.0+ cloud document cluster connected via `pymongo.MongoClient`.
- **Authentication:** `flask-jwt-extended` using HS256 algorithm with Bearer tokens; passwords hashed using salted `bcrypt` (`passlib.context.CryptContext`).

### MongoDB Collections Schema
1. **`users` Collection:**
   - `_id`: `ObjectId` (primary key)
   - `email`: `String` (unique indexed, lowercase)
   - `password_hash`: `String` (salted bcrypt hash)
   - `role`: `String` (`"patient"` or `"doctor"`)
   - `auth_provider`: `String` (`"local"` or `"google"`)
   - `created_at`, `updated_at`, `last_login`: `DateTime`
2. **`patients` Collection:**
   - `_id`: `ObjectId` (matches `users._id`)
   - `name`: `String`
   - `blood_group`: `String` (e.g., `"O+"`, `"B+"`)
   - `emergency_contacts`: `Array` of `{ "name", "relationship", "phone" }`
   - `date_of_birth`, `age`, `gender`, `phone`, `address`: `String`
   - `medical_information`: `{ "allergies", "conditions", "medications" }`
   - `accessibility_settings`: `{ "contrast_mode", "voice_input", "haptic_feedback", "font_size" }`
   - `onboarding_status`: `String` (`"NOT_STARTED"`, `"IN_PROGRESS"`, `"COMPLETED"`)
3. **`medicines` Collection:**
   - `_id`: `ObjectId` (primary key)
   - `patient_id`: `ObjectId` (indexed reference to `patients._id`)
   - `name`: `String` (e.g., `"Metformin"`)
   - `type`: `String` (`"tablet"`, `"capsule"`, `"syrup"`, `"injection"`)
   - `dosage`: `String` (e.g., `"500 mg"`)
   - `frequency`: `String` (`"daily"`, `"weekly"`, `"as_needed"`)
   - `start_date`, `end_date`: `String`
   - `reminder_times`: `Array` of strings (e.g., `["08:00 AM", "08:00 PM"]`)
   - `logs`: `Array` of compliance objects: `{ "date": "YYYY-MM-DD", "time": "HH:MM", "status": "taken"|"skipped"|"snoozed" }`
4. **`notifications` Collection:**
   - `_id`: `ObjectId`, `patient_id`: `ObjectId`, `medicine_id`: `ObjectId`
   - `title`: `String`, `message`: `String`, `priority`: `String` (`"high"`, `"medium"`, `"low"`)
   - `type`: `String` (`"allergy"`, `"interaction"`, `"guidance"`, `"adherence"`)
   - `context_for_ai`: `String`, `is_read`: `Boolean`, `created_at`: `DateTime`
5. **`medication_cache` Collection:**
   - `input_name`: `String` (unique indexed), `normalized_name`: `String`, `rxcui`: `String`
   - `ingredients`: `Array`, `label_information`: `Object`, `cached_at`: `DateTime`
6. **`ml_predictions` Collection:**
   - `patient_id`: `ObjectId`, `adherence_risk`: `String`, `features`: `Object`, `created_at`: `DateTime`

### End-to-End Data Flow: Adding a Medication & Logging Compliance

```
1. AddMedicineActivity.kt (Save Click)
   │
   ▼ HTTP POST /api/medicines (Headers: Authorization: Bearer <token>)
2. routes/medicine.py -> create_medicine()
   │
   ├─▶ Verify JWT Token -> Extract patient_id
   ├─▶ Validate input fields (name, type, dates, reminder times)
   ├─▶ MedicineModel.create_medicine() -> MongoDB insert_one() into 'medicines'
   │
   ├─▶ [MEDICATION INTELLIGENCE PIPELINE]
   │    ├─ services/medication_knowledge.py -> Query RxNorm & openFDA (cached in 'medication_cache')
   │    ├─ services/safety_engine.py -> Deterministic allergy cross-reactivity check against patient profile
   │    ├─ services/safety_engine.py -> Drug interaction check against existing active medications
   │    ├─ ml/predictor.py -> Predict behavioral adherence risk via RandomForestClassifier
   │    └─ NotificationModel.create_notification() -> Save prioritized alert to 'notifications'
   │
   ▼ HTTP 201 Response { "success": true, "medicine_id": "...", "alerts": [...] }
3. AddMedicineActivity.kt -> onResponse()
   │
   ├─▶ AlarmScheduler.scheduleAlarms() -> Sets exact RTC_WAKEUP alarms in AlarmManager
   ├─▶ MedicineCache.updateCache() -> Serializes medicine into SharedPreferences
   └─▶ If allergy alert returned: Show prominent modal dialog with "Ask AI to Explain"
   │
4. Alarm Triggers -> AlarmReceiver.kt -> ReminderAlarmActivity.kt
   │
   ▼ User taps "Taken" button
5. HTTP POST /api/medicines/<id>/log { "date": "2026-11-01", "time": "08:00", "status": "taken" }
   │
6. routes/medicine.py -> log_compliance()
   │
   ▼ MedicineModel.add_log() -> MongoDB $push into 'medicines.logs'
7. HomeActivity.kt (Dashboard)
   │
   ▼ Daily progress fraction recalculates: takenDoses / totalDoses (CircularProgressView updates)
```

---

## PART 5 — GOOGLE MAPS AND GEOLOCATION

### Verified Architecture & Component Breakdown

```
+------------------------------------------------------------------------------------+
|                               PHARMACYACTIVITY.KT                                  |
|   1. Map Engine: MapLibre Android SDK (org.maplibre.gl:android-sdk:10.2.0)         |
|   2. Vector Style: Geoapify Osm-Bright Style JSON                                  |
|   3. Geolocation: Google Play Services FusedLocationProviderClient                 |
|   4. Facility Query: Geoapify Places API v2 (Proximity Bias, 6 Categories)         |
|   5. Turn-by-Turn Routing: Implicit Intent geo:0,0?q=lat,lon -> Google Maps App   |
+------------------------------------------------------------------------------------+
```

### Critical Distinction for Viva Voce
During the examination, faculty may ask: *"Did you use Google Maps SDK or an alternative?"*
Be prepared to give this exact, verified answer:
1. **In-App Map Display:** Implemented using the **MapLibre Android SDK** (`com.mapbox.mapboxsdk.maps.MapView`), which renders open-source vector tiles styled via Geoapify. This avoids expensive Google Maps SDK per-load billing.
2. **GPS Geolocation:** Uses official **Google Play Services Location** (`com.google.android.gms.location.FusedLocationProviderClient`) to retrieve accurate device coordinates with high accuracy (`Priority.PRIORITY_HIGH_ACCURACY`).
3. **Turn-by-Turn Navigation & Routing:** Dispatched via an explicit Android Intent (`Intent.ACTION_VIEW`, `geo:0,0?q=lat,lon(FacilityName)`) with `setPackage("com.google.android.apps.maps")`. This hands off route calculation, traffic, and voice navigation directly to the user's native Google Maps application.

### Permission Handling
- **Manifest Declarations:**
  - `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`
  - `<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />`
- **Runtime Request:** Handled in `PharmacyActivity.kt` via `registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())`. If permissions are denied, an informative card is rendered prompting the user to grant access in application settings.

---

## PART 6 — AI CHATBOT, ML, ALERTS AND NOTIFICATIONS

### A. Artificial Intelligence Chatbot Architecture

| Component | Actual Implementation Details | Source Files |
|:---|:---|:---|
| **Chat Interface** | Native Android Activity (`AIAssistantActivity`) with `RecyclerView` supporting multiple view types (`item_chat_user.xml`, `item_chat_assistant.xml`, `item_chat_suggestions.xml`). | `AIAssistantActivity.kt`, `ChatAdapter.kt` |
| **API Transport** | Retrofit 2 POST request to `/api/ai/chat` passing user message and optional contextual finding. | `RetrofitClient.kt`, `ApiService.kt` |
| **Backend AI Engine** | Official Google GenAI Python SDK (`from google import genai`) using model `gemini-flash-lite-latest`. | `Medicare-Backend/services/ai_service.py` |
| **Context Assembler** | Assembles structured medical context from MongoDB: patient age, blood group, recorded allergies, active prescriptions, and ML adherence score. Excludes passwords, tokens, and database IDs. | `build_user_context()` in `ai_service.py` |
| **Clinical Guardrails** | System prompt strictly forbids diagnosing illnesses, prescribing medications, or modifying dosages. Severe symptoms trigger immediate emergency referrals (911/112). | `SYSTEM_PROMPT` in `ai_service.py` |
| **Graceful Fallback** | When network or API keys are unavailable, `_generate_rule_based_fallback()` provides verified factual responses extracted from local records without hallucinating medical advice. | `_generate_rule_based_fallback()` in `ai_service.py` |

### B. Machine Learning Adherence Engine

| Metric / Aspect | Verified Implementation Details |
|:---|:---|
| **Algorithm** | **`RandomForestClassifier`** (`sklearn.ensemble`) |
| **Model Artifact** | Binary model file stored at `Medicare-Backend/ml/adherence_model.joblib` (108 KB) |
| **Hyperparameters** | `n_estimators = 100`, `max_depth = 6`, `random_state = 42` |
| **Dataset & Training** | 1,600 samples (1,280 train / 320 test) based on clinical adherence metrics (`train_model.py`) |
| **Performance Metrics** | 100% precision, 100% recall, 100% F1-score on test split (documented in `model_metrics.json`) |
| **Inference Latency** | ~22.68 milliseconds per prediction (verified via `verify_ml_model.py`) |
| **8 Input Features** | 1. `medication_count`<br>2. `daily_medication_events`<br>3. `missed_doses_7d`<br>4. `missed_doses_30d`<br>5. `adherence_percentage`<br>6. `reminder_response_rate`<br>7. `consecutive_missed_days`<br>8. `medication_complexity` |
| **Output Classes** | `LOW` (0), `MEDIUM` (1), `HIGH` (2) Adherence Risk |
| **Ethical Safety Boundary** | **The ML model never diagnoses diseases or alters prescriptions.** It strictly evaluates schedule compliance risk to prioritize reminder alerts and encourage adherence. |

### C. Medicine Reminders & Notification System

| Component | Implementation Mechanism | Verified Source Files |
|:---|:---|:---|
| **Alarm Scheduling** | Uses Android system `AlarmManager` with `setAlarmClock(AlarmClockInfo, pendingIntent)` on API 21+ and `setExactAndAllowWhileIdle(RTC_WAKEUP)` on API 23+. | `AlarmScheduler.kt` |
| **Device Wakeup** | Acquires `PowerManager.FULL_WAKE_LOCK` (`ACQUIRE_CAUSES_WAKEUP`) to turn the screen on and keep the CPU awake. | `AlarmReceiver.kt` |
| **Fullscreen Lockscreen** | `ReminderAlarmActivity` executes with `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, and `FLAG_KEEP_SCREEN_ON`. | `ReminderAlarmActivity.kt` |
| **Notification Channel** | High-importance channel `"medication_alarms_channel_v2"` configured with public lockscreen visibility, custom vibration pattern, and DND bypass. | `AlarmReceiver.kt` |
| **Voice & Sound Alerts** | Voice reminders synthesize speech using `android.speech.tts.TextToSpeech`. Alarm sounds play via `RingtoneManager.getRingtone()`. Haptics use `Vibrator`. | `ReminderAlarmActivity.kt` |
| **Boot Rescheduling** | `BootReceiver` intercepts `ACTION_BOOT_COMPLETED`, queries the backend, and restores all exact alarms. | `BootReceiver.kt` |
| **In-App Notification Center** | `NotificationHelper` queries `GET /api/notifications` and displays a categorized bottom sheet with "Explain with AI" actions. | `NotificationHelper.kt`, `dialog_notifications.xml` |

---

## PART 7 — TASK-TO-FILE QUICK REFERENCE

This master table allows either group member to instantly identify and open the required files during the live practical examination.

| Task | Feature | XML Layout File | Kotlin Source File | Backend / Database File | What to Demonstrate |
|:---:|:---|:---|:---|:---|:---|
| **1** | Dashboard & Daily Progress | `activity_home.xml` | `HomeActivity.kt`, `CircularProgressView.kt` | `MedicineCache.kt` (Local SharedPreferences) | Circular progress ring, upcoming dose card, schedule list |
| **1** | Bottom Navigation | `layout_bottom_navigation.xml` | `NavigationHelper.kt` | N/A | Fast, single-instance tab transitions with reorder-to-front |
| **1** | Medication Inventory | `activity_medicines.xml`, `item_medicine.xml` | `MedicinesActivity.kt`, `MedicineAdapter.kt` | N/A | Filter chips (Morning/Evening/As Needed), edit/delete dialogs |
| **1** | Add Medicine UI Form | `activity_add_medicine.xml` | `AddMedicineActivity.kt`, `DialogHelper.kt` | N/A | Type selection cards, DatePicker & TimePicker dialogs |
| **2** | User Authentication | `activity_login.xml`, `activity_register.xml` | `LoginActivity.kt`, `RegisterActivity.kt`, `SessionManager.kt` | `routes/auth.py`, `models/user.py`, `users` collection | User registration, bcrypt password hashing, JWT session creation |
| **2** | Password Reset | `activity_forgot_password.xml` | `ForgotPasswordActivity.kt` | `routes/auth.py`, `models/user.py` | 6-digit verification code generation and password update |
| **2** | Patient Profile Sync | `activity_profile.xml`, `dialog_edit_profile.xml` | `ProfileActivity.kt`, `OnboardingActivity.kt` | `routes/patient.py`, `models/patient.py`, `patients` collection | Profile editing, completion percentage, accessibility persistence |
| **2** | Medicine CRUD API | `activity_add_medicine.xml` | `RetrofitClient.kt`, `ApiService.kt`, `Models.kt` | `routes/medicine.py`, `models/medicine.py`, `medicines` collection | Adding new medicine, updating schedule, logging compliance |
| **3** | In-App Vector Map | `activity_pharmacy.xml` | `PharmacyActivity.kt` | Geoapify Vector Tile Style (`maps.geoapify.com`) | MapLibre map rendering with zoom and pan gestures |
| **3** | GPS Location Lookup | N/A | `PharmacyActivity.kt` | Google Play Services `FusedLocationProviderClient` | Runtime permission dialog, blue location compass indicator |
| **3** | Healthcare Search | `item_pharmacy.xml` | `PharmacyActivity.kt`, `PharmacyAdapter.kt` | `api/GeoapifyClient.kt`, Geoapify Places API v2 | Proximity-sorted facility cards across 6 categories |
| **3** | Navigation Directions | N/A | `PharmacyActivity.kt` | Implicit Intent `geo:0,0?q=lat,lon(name)` | Launching Google Maps app with destination pre-loaded |
| **4** | AI Health Chatbot | `activity_ai_assistant.xml`, `item_chat_assistant.xml` | `AIAssistantActivity.kt`, `ChatAdapter.kt` | `routes/ai.py`, `services/ai_service.py`, Google Gemini API | Real-time chat with patient context and clinical guardrails |
| **4** | Adherence ML Prediction | N/A | `AIAssistantActivity.kt`, `HomeActivity.kt` | `ml/predictor.py`, `ml/adherence_model.joblib`, `routes/ml.py` | Random Forest inference on 8 compliance features |
| **4** | Clinical Safety Engine | `activity_add_medicine.xml` | `AddMedicineActivity.kt` | `services/safety_engine.py`, `services/medication_knowledge.py` | Penicillin allergy conflict detection on Amoxicillin |
| **4** | Background Alarms | `activity_reminder_alarm.xml` | `AlarmScheduler.kt`, `AlarmReceiver.kt`, `ReminderAlarmActivity.kt` | `models/medicine.py` (`logs` array) | Lockscreen alarm overlay with TextToSpeech and Ringtone |
| **4** | In-App Notifications | `dialog_notifications.xml`, `item_notification.xml` | `NotificationHelper.kt` | `routes/notification.py`, `notifications` collection | Prioritized bottom sheet notifications (High, Medium, Low) |

---

## PART 8 — STANDARDIZED DEMONSTRATION PROCEDURES FOR BOTH GROUP MEMBERS

### Task 1: Native Android UI, Navigation, and Data Management
- **Objective:** Demonstrate native Android UI rendering, custom 2D Canvas components, bottom navigation, and local data caching.
- **Before Starting:** Android emulator or device running with Medicare+ installed.
- **Procedure:**
  1. Open the Medicare+ application. The dashboard (`HomeActivity`) appears.
  2. Explain the circular adherence ring: it is rendered by `CircularProgressView.kt` on an Android Canvas based on doses taken today.
  3. Tap the "Medicines" tab in the bottom bar. Explain that `NavigationHelper.kt` switches activities using `FLAG_ACTIVITY_REORDER_TO_FRONT`.
  4. Tap the "Morning" filter chip; show that only morning doses appear. Tap "All" to restore.
  5. Tap the `+` FAB to open `AddMedicineActivity.kt`.
  6. Tap "Capsule"; demonstrate that the card background turns teal while other cards become unselected.
  7. Tap "Start Date" to open `DatePickerDialog`; select a date.
  8. Open `MedicineCache.kt` to explain that local medicines are stored in `SharedPreferences` as JSON strings for instant offline loading.
- **Files to Remember:**
  - `Medicare/app/src/main/java/com/example/medicare/HomeActivity.kt`
  - `Medicare/app/src/main/java/com/example/medicare/CircularProgressView.kt`
  - `Medicare/app/src/main/java/com/example/medicare/NavigationHelper.kt`
  - `Medicare/app/src/main/java/com/example/medicare/MedicineCache.kt`
- **Expected Result:** Instant screen transitions, dynamic circular progress rendering, and local cache persistence.
- **Likely Viva Questions:**
  1. *Q: How does the bottom navigation maintain state without reloading?*  
     *A:* In `NavigationHelper.kt`, we set `Intent.FLAG_ACTIVITY_REORDER_TO_FRONT`, which brings existing activity instances to the top of the stack instead of destroying and recreating them.
  2. *Q: How is the circular progress ring drawn?*  
     *A:* `CircularProgressView.kt` extends `View` and overrides `onDraw(canvas: Canvas)`, using `canvas.drawArc()` with an anti-aliased Paint object to draw the background and progress arcs.
  3. *Q: How does the application work offline?*  
     *A:* `MedicineCache.kt` reads and writes serialized JSON strings to user-scoped `SharedPreferences`, loading cached medicines into memory before any network request completes.

---

### Task 2: Backend Architecture, Persistent Database Storage, and Cloud Networking
- **Objective:** Demonstrate user authentication, cloud database persistence in MongoDB Atlas, and Retrofit HTTP REST communication.
- **Before Starting:** Ensure backend is accessible (either live on Render or running locally on port 5000).
- **Procedure:**
  1. Open `RegisterActivity`. Register a new account (`patient_test@example.com`, password, name).
  2. Explain that the backend hashes the password using bcrypt in `models/user.py` and stores the document in MongoDB collection `users`.
  3. Show that upon successful registration, an HS256 JWT access token is returned and stored in `SessionManager`.
  4. In `AddMedicineActivity`, add a new medicine (`"Metformin"`, `"500 mg"`, Daily).
  5. Explain that `RetrofitClient.kt` automatically appends `Authorization: Bearer <token>` in its OkHttp interceptor.
  6. Show that the backend route `POST /api/medicines` in `routes/medicine.py` inserts the document into the `medicines` collection.
  7. Open `ReminderAlarmActivity` and click "Taken". Explain that this sends `POST /api/medicines/<id>/log`, updating the `logs` array in MongoDB.
- **Files to Remember:**
  - `Medicare/app/src/main/java/com/example/medicare/api/RetrofitClient.kt`
  - `Medicare-Backend/routes/auth.py`
  - `Medicare-Backend/routes/medicine.py`
  - `Medicare-Backend/models/user.py`
  - `Medicare-Backend/database/mongo.py`
- **Expected Result:** HTTP 201 responses on creation, JWT session persistence, and persistent MongoDB records.
- **Likely Viva Questions:**
  1. *Q: How does the Android app authenticate requests to the backend?*  
     *A:* `RetrofitClient.kt` configures an OkHttp interceptor that reads the stored JWT from `SessionManager` and adds an `Authorization: Bearer <token>` header to all requests.
  2. *Q: What database is used, and how is it connected?*  
     *A:* MongoDB Atlas (cloud document database) connected via PyMongo in `database/mongo.py` using connection pooling and collection indexes.
  3. *Q: How are passwords stored securely?*  
     *A:* In `models/user.py`, passwords are encrypted with salted bcrypt hashing using passlib before being inserted into MongoDB; plain text passwords are never stored.

---

### Task 3: Interactive Maps, Geolocation, Healthcare Facility Search, and Navigation
- **Objective:** Demonstrate in-app vector map display, GPS location lookup, nearby healthcare discovery, and navigation intent dispatch.
- **Before Starting:** Ensure device GPS/Location services are enabled.
- **Procedure:**
  1. Tap the "Pharmacy" tab in the bottom navigation bar.
  2. Show that `PharmacyActivity` initializes MapLibre and requests `ACCESS_FINE_LOCATION`.
  3. Explain that Google Play Services `FusedLocationProviderClient` acquires the GPS fix.
  4. Point out the header: `"Found X verified facilities near you"` showing results from the Geoapify Places API v2.
  5. Tap the "Hospitals" chip to demonstrate category filtering.
  6. Tap any red pin on the map: the camera animates to center on the pin and the facility details dialog appears.
  7. Tap "Directions": show that Android dispatches an implicit Intent `geo:0,0?q=lat,lon(name)`, opening Google Maps with turn-by-turn navigation ready.
- **Files to Remember:**
  - `Medicare/app/src/main/java/com/example/medicare/PharmacyActivity.kt`
  - `Medicare/app/src/main/java/com/example/medicare/PharmacyAdapter.kt`
  - `Medicare/app/src/main/java/com/example/medicare/api/GeoapifyClient.kt`
- **Expected Result:** Fluid map rendering, accurate user location, real facility listings, and seamless handoff to Google Maps.
- **Likely Viva Questions:**
  1. *Q: Which map engine is used inside the app?*  
     *A:* MapLibre Native Android SDK (`org.maplibre.gl:android-sdk:10.2.0`) rendering Geoapify Osm-Bright vector tile styles.
  2. *Q: How does navigation work?*  
     *A:* In `launchNavigationIntent()`, an implicit Intent with URI `geo:0,0?q=lat,lon(FacilityName)` is launched targeting `com.google.android.apps.maps` to open turn-by-turn navigation in the native Google Maps app.
  3. *Q: How do you handle cases where location permission is denied?*  
     *A:* `PharmacyActivity.kt` displays an informative card informing the user and providing a one-click button to open Android Application Details Settings.

---

### Task 4: AI Conversational Chatbot, Machine Learning Adherence Engine, and Smart Notifications
- **Objective:** Demonstrate context-aware conversational AI, machine learning adherence risk prediction, deterministic allergy safety checks, and background lockscreen alarms.
- **Before Starting:** Ensure the backend server has a valid `GEMINI_API_KEY` or is ready to demonstrate the clinical fallback.
- **Procedure:**
  1. Open the "AI Assistant" tab. Tap the suggestion chip `"What should I do if I miss a dose?"`.
  2. Explain that `services/ai_service.py` injects patient medications, allergies, and ML risk scores into a Gemini prompt governed by clinical guardrails (no diagnosing, no prescribing).
  3. Go to "Medicines" -> `+` FAB. Type `"Amoxicillin"`, set dosage `"500"`, and click "Save Medicine" (with a Penicillin allergy in the profile).
  4. Show the modal dialog titled `"🔴 Medication Safety Alert"` explaining the cross-reactivity detected by `services/safety_engine.py`.
  5. Tap `"Ask AI to Explain"`. The AI chatbot opens with the allergy context pre-loaded, explaining the risk in plain language.
  6. From the Home screen schedule, tap any dose to open `ReminderAlarmActivity`.
  7. Point out the multimodal alert: voice synthesis via Android `TextToSpeech`, alarm sound via `RingtoneManager`, and vibration via `Vibrator`.
  8. Open `ml/predictor.py` and run `verify_ml_model.py` to prove the scikit-learn Random Forest model runs in 22ms.
- **Files to Remember:**
  - `Medicare/app/src/main/java/com/example/medicare/AIAssistantActivity.kt`
  - `Medicare/app/src/main/java/com/example/medicare/ReminderAlarmActivity.kt`
  - `Medicare-Backend/services/safety_engine.py`
  - `Medicare-Backend/services/ai_service.py`
  - `Medicare-Backend/ml/predictor.py`
- **Expected Result:** Real-time conversational AI responses, deterministic allergy warnings, audible voice/alarm reminders, and verifiable ML inference.
- **Likely Viva Questions:**
  1. *Q: What Machine Learning model is used, and what does it predict?*  
     *A:* A `RandomForestClassifier` (100 estimators, depth 6) from scikit-learn that evaluates 8 behavioral adherence features to predict `LOW`, `MEDIUM`, or `HIGH` risk. It never makes medical diagnoses.
  2. *Q: How are medical hallucinations prevented in the AI chatbot?*  
     *A:* Safety checks (allergies and drug interactions) are executed deterministically by `safety_engine.py` before Gemini is invoked. Gemini is only used to explain verified facts in natural language under strict negative constraints.
  3. *Q: How does the alarm wake the phone when it is asleep?*  
     *A:* `AlarmScheduler` uses `AlarmManager.setAlarmClock()`, and `AlarmReceiver` acquires a `PowerManager.WakeLock` (`ACQUIRE_CAUSES_WAKEUP`), turning the screen on and launching `ReminderAlarmActivity` over the lockscreen.

---

## PART 9 — VERIFIED REPOSITORY EVOLUTION AND CODE CHANGES

Analysis of the Git commit history reveals five distinct evolutionary milestones:

### 1. Initial UI Prototype & Assets (`10ebd8a` -> `7540bf8`)
- **Changes:** Created native Android project structure; added vector drawables (`ic_pill`, `ic_robot`, `ic_profile`); built prototype screens (`HomeActivity`, `MedicinesActivity`, `PharmacyActivity`, `AIAssistantActivity`, `ProfileActivity`); implemented `CircularProgressView.kt`.

### 2. Full-Stack Integration & Database Schema (`5883520` -> `b82d0df`)
- **Changes:** Created `Medicare-Backend` with Flask and PyMongo; defined schemas for `users`, `patients`, `medicines`, and `appointments`; implemented bcrypt authentication and JWT tokens; connected Android client via Retrofit 2 and OkHttp; implemented multi-step profile onboarding wizard.

### 3. Production Deployment & MapLibre Migration (`eae01a4` -> `b6977b5`)
- **Changes:** Migrated map implementation from static mock layouts to MapLibre Native Android SDK and Geoapify Places API v2; deployed Flask backend to Render cloud (`https://medicare-backend-me50.onrender.com/api/`); implemented local-first user-scoped medicine cache in `SharedPreferences` (`MedicineCache.kt`).

### 4. Machine Learning & AI Safety Engine (`6aad2de` -> `fc4b7a5`)
- **Changes:** Integrated Google Gemini via official `google-genai` SDK; built deterministic clinical safety engine (`services/safety_engine.py`) with RxNorm and openFDA lookups; trained and integrated Random Forest adherence risk classifier (`ml/adherence_model.joblib`); added automated test suites (`test_ai_medication_ml.py`).

### 5. Production Hardening & Bug Fixes (`90e3cb3` -> `07668c1`)
- **Changes:** Enforced 10-digit input filter for Indian phone numbers; removed artificial 5 km / 15 km map search circles in favor of unconstrained proximity bias; eliminated oversized map info window clutter; wired notification bell to interactive bottom sheet (`NotificationHelper.kt`); fixed lockscreen alarm dismissal and sound switches.

---

## PART 10 — VIVA PREPARATION: COMPREHENSIVE Q&A

### 1. Why do we use XML in Android?
**Answer:** XML provides a declarative separation between presentation (layout, views, hierarchies, styling) and business logic (Kotlin code). It allows layouts to adapt across different screen densities, orientations, and themes without altering core application logic.

### 2. What is an Activity?
**Answer:** An Activity represents a single focused screen that a user can interact with (e.g., `HomeActivity`, `MedicinesActivity`). It manages an Android `Window`, inflates the visual layout via `setContentView()`, and coordinates the Android component lifecycle (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`).

### 3. What is a Fragment?
**Answer:** A Fragment represents a modular, reusable portion of an Activity's UI with its own lifecycle embedded within a parent Activity. *(Note for Medicare+: Medicare+ uses standalone Activities managed via `NavigationHelper` and `FLAG_ACTIVITY_REORDER_TO_FRONT`, alongside `BottomSheetDialogFragment` in `NotificationHelper`).*

### 4. How is an XML layout connected to Kotlin or Java?
**Answer:** The layout XML is inflated into view objects in memory using `setContentView(R.layout.activity_name)` during `onCreate()`. Individual views are then referenced using `findViewById<Type>(R.id.view_id)`. In adapters, view hierarchies are inflated via `LayoutInflater.from(context).inflate(R.layout.item, parent, false)`.

### 5. What is the purpose of AndroidManifest.xml?
**Answer:** The manifest is the root metadata descriptor for the Android OS. It declares application components (Activities, BroadcastReceivers, Services), system hardware/software permissions (`INTERNET`, `ACCESS_FINE_LOCATION`, `SCHEDULE_EXACT_ALARM`), application themes, and external API metadata.

### 6. What is an API?
**Answer:** An Application Programming Interface (API) is a structured communication contract allowing two separate software systems to exchange data. Medicare+ uses a RESTful JSON HTTP API where Android acts as the client and Flask acts as the server.

### 7. How does the Android app communicate with the backend?
**Answer:** The app uses Retrofit 2 and OkHttp 3. `ApiService.kt` declares HTTP endpoints as Kotlin methods. `RetrofitClient.kt` executes asynchronous network requests on background threads, serializes request bodies, deserializes JSON responses into strongly-typed models via Gson, and dispatches callbacks onto the Android main UI thread.

### 8. How is data stored and retrieved in Medicare+?
**Answer:** Data storage uses a tiered architecture:
1. **Cloud Database:** Persistent document storage in MongoDB Atlas (collections: `users`, `patients`, `medicines`, `notifications`).
2. **Local Client Cache:** `MedicineCache.kt` saves serialized JSON strings to private `SharedPreferences` on the Android device for instant offline availability.
3. **Internal Storage:** Profile avatar photos are stored as binary files in `context.filesDir`.

### 9. How are location permissions handled?
**Answer:** Location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) are declared in `AndroidManifest.xml` and requested at runtime in `PharmacyActivity.kt` using `ActivityResultContracts.RequestMultiplePermissions()`. If denied, the app degrades gracefully, showing an informative state with a shortcut to application settings.

### 10. How does the chatbot work?
**Answer:** The user types a message in `AIAssistantActivity.kt`, which sends a POST request to `/api/ai/chat`. The Flask backend retrieves the patient's active prescriptions, allergies, and ML adherence risk from MongoDB, injects them into a clinical context prompt with strict negative guardrails, and queries Google Gemini. If Gemini is unavailable, a clinical rule-based fallback response is returned.

### 11. How are medicine reminders scheduled?
**Answer:** `AlarmScheduler.kt` calculates the millisecond timestamp of the next dose and calls `AlarmManager.setAlarmClock()` with an exact `RTC_WAKEUP` intent. When the alarm triggers, `AlarmReceiver` acquires a `WakeLock`, wakes the CPU and screen, and launches `ReminderAlarmActivity` over the lockscreen with audible ringtones, haptics, and TextToSpeech.

### 12. What happens if the backend is unavailable?
**Answer:** The application exhibits offline resilience:
- Previously fetched medicines render immediately from `MedicineCache`.
- Authentication failures show clear error toasts instead of crashing.
- Chatbot queries return the factual local rule-based fallback.
- Facility searches allow direct fallback to native Google Maps.

### 13. What happens if a required permission is denied?
**Answer:**
- **Location:** `PharmacyActivity` shows a placeholder card explaining that permission is required and provides a button to open Settings.
- **Exact Alarms:** `HomeActivity` checks `alarmManager.canScheduleExactAlarms()` on Android 12+ and displays a dialog directing the user to system settings.
- **Post Notifications:** Checked on Android 13+ in `HomeActivity.onResume()`.

### 14. Which files would you open first when demonstrating each task?
**Answer:**
- **Task 1:** `HomeActivity.kt` and `activity_home.xml`
- **Task 2:** `RetrofitClient.kt` and `Medicare-Backend/routes/medicine.py`
- **Task 3:** `PharmacyActivity.kt` and `activity_pharmacy.xml`
- **Task 4:** `AIAssistantActivity.kt`, `services/safety_engine.py`, and `ml/predictor.py`

### 15. What is the difference between the UI layer, business logic, backend, and database?
**Answer:**
- **UI Layer:** XML layouts and Kotlin Activities/Adapters rendering visual elements and capturing touch gestures.
- **Business Logic:** Algorithms validating input, computing adherence percentages, scheduling calendar alarms, and coordinating transitions.
- **Backend:** Flask REST API receiving network requests, enforcing JWT authorization, and orchestrating services.
- **Database:** MongoDB Atlas distributed document store persisting long-term clinical data across sessions.

### 16. Which parts of the application are implemented natively?
**Answer:** The entire mobile client is native: written in Kotlin, using Android Jetpack, Material Design Components, custom Canvas rendering (`CircularProgressView`), hardware sensor access (GPS via `FusedLocationProviderClient`), and system background daemons (`AlarmManager`, `BroadcastReceiver`).

### 17. Which features rely on external cloud services?
**Answer:**
1. **MongoDB Atlas:** Cloud database hosting collections.
2. **Google Gemini API:** Cloud generative AI powering conversational explanations.
3. **NIH RxNorm & openFDA REST APIs:** External pharmacopeia lookups.
4. **Geoapify Places API:** Cloud GIS database of healthcare facilities.
5. **Google Maps App:** Turn-by-turn navigation via Android Intents.

### 18. What is the difference between a rule-based feature and an ML-based feature in Medicare+?
**Answer:**
- **Rule-Based Feature (`services/safety_engine.py`):** Deterministic, exact clinical conflict detection using verified medical rules (e.g., if allergy is "Penicillin" and drug is "Amoxicillin", flag high-priority alert). It has zero probability of hallucination.
- **Machine Learning Feature (`ml/predictor.py`):** Supervised mathematical classification using a trained `RandomForestClassifier` that evaluates 8 behavioral adherence metrics to predict low/medium/high risk. It is strictly used for reminder personalization, never for clinical diagnoses.

---

## PART 11 — PPT-READY PRESENTATION PLAN (10 SLIDES)

```
==================================================================================
SLIDE 1: MEDICARE+ PLATFORM ARCHITECTURE & REPOSITORY OVERVIEW
==================================================================================
Key Bullet Points:
• Full-stack healthcare management platform: Native Android client + Python Flask API + MongoDB Atlas cloud database.
• Strict separation of concerns: Native UI presentation, deterministic clinical safety checks, behavioral ML adherence scoring, and AI conversation.
• Clean repository structure: 'Medicare/' (Android client), 'Medicare-Backend/' (Flask REST API), 'Medicare-Web/' (web companion).

Exact Filenames to Feature:
• Medicare/app/src/main/AndroidManifest.xml
• Medicare-Backend/app.py
• README.md & TECH_STACK.md

Suggested Diagram / Screenshot:
• Full system architecture diagram showing Android Client -> Flask Gateway -> Safety Engine / ML Engine / Gemini / MongoDB Atlas.

Spoken Script for Viva:
"Good morning, esteemed examiners. Our project is Medicare+, a full-stack medication management ecosystem. The repository contains a native Android mobile client built in Kotlin with Material Design 3, communicating through an asynchronous Retrofit client with a production Python Flask backend deployed in the cloud and connected to MongoDB Atlas. Over the next few slides, we will demonstrate the implementation of all four assigned ADL practical tasks."
```

```
==================================================================================
SLIDE 2: ANDROID APPLICATION ARCHITECTURE & NAVIGATION
==================================================================================
Key Bullet Points:
• Native Android SDK (Target API 36, Min API 24) built with Kotlin and Material Design Components.
• 5 primary navigation destinations: Home, Medicines, Pharmacy, AI Assistant, and Profile.
• Instant tab transitions using NavigationHelper with Intent.FLAG_ACTIVITY_REORDER_TO_FRONT.
• Dynamic accessibility support: Runtime font scaling and WCAG AAA High Contrast mode in BaseActivity.

Exact Filenames to Feature:
• Medicare/app/src/main/java/com/example/medicare/NavigationHelper.kt
• Medicare/app/src/main/java/com/example/medicare/BaseActivity.kt
• Medicare/app/src/main/res/layout/layout_bottom_navigation.xml

Suggested Diagram / Screenshot:
• Flowchart showing 5 tab activities connected to NavigationHelper with reorder-to-front intent flags.

Spoken Script for Viva:
"For our Android application architecture, we targeted modern Android APIs using Kotlin. Instead of fragment-swapping which can cause UI state destruction, our custom NavigationHelper uses Android's reorder-to-front intent flags. This preserves activity memory and provides instantaneous tab transitions with zero flicker. Furthermore, our BaseActivity dynamically injects user-configured font scales and contrast themes on resume."
```

```
==================================================================================
SLIDE 3: XML-TO-KOTLIN CONNECTION & UI COMPONENTS (TASK 1)
==================================================================================
Key Bullet Points:
• Declarative XML layouts connected to Kotlin via setContentView() and findViewById().
• Custom 2D Canvas progress gauge (CircularProgressView) drawing dynamic adherence rings.
• RecyclerViews with custom adapters (MedicineAdapter, ScheduleAdapter) using optimized ViewHolders.
• Offline-first data caching: MedicineCache stores medication schedules in SharedPreferences via Gson.

Exact Filenames to Feature:
• Medicare/app/src/main/res/layout/activity_home.xml
• Medicare/app/src/main/java/com/example/medicare/HomeActivity.kt
• Medicare/app/src/main/java/com/example/medicare/CircularProgressView.kt
• Medicare/app/src/main/java/com/example/medicare/MedicineCache.kt

Suggested Diagram / Screenshot:
• Screenshot of Dashboard showing the circular adherence gauge, upcoming dose card, and daily schedule list.

Spoken Script for Viva:
"Task 1 focuses on native UI components and local data management. In HomeActivity, the UI is inflated from activity_home.xml. We developed CircularProgressView, which overrides onDraw() to render a smooth circular completion gauge on the Canvas. Our schedule and inventory use RecyclerViews with custom adapters. To ensure the app loads immediately even without network connectivity, MedicineCache loads serialized data from user-scoped SharedPreferences before issuing background network requests."
```

```
==================================================================================
SLIDE 4: CLOUD BACKEND & DATABASE ARCHITECTURE (TASK 2)
==================================================================================
Key Bullet Points:
• Flask RESTful API structured with modular Blueprints (auth, patient, medicine, notification, ai, ml).
• Distributed document storage in MongoDB Atlas with collection indexing (users, patients, medicines).
• Security: Salted bcrypt password hashing with Passlib and HS256 JWT Bearer token authentication.
• Retrofit 2 + OkHttp 3 Android client with 60s timeout handling and automatic JWT header injection.

Exact Filenames to Feature:
• Medicare-Backend/app.py & database/mongo.py
• Medicare-Backend/models/user.py & models/medicine.py
• Medicare/app/src/main/java/com/example/medicare/api/RetrofitClient.kt
• Medicare/app/src/main/java/com/example/medicare/api/ApiService.kt

Suggested Diagram / Screenshot:
• End-to-end data flow diagram showing Android client -> OkHttp JWT interceptor -> Flask API -> MongoDB Atlas document insertion.

Spoken Script for Viva:
"In Task 2, we built the backend data and cloud storage infrastructure. Our backend is written in Python Flask using PyMongo to connect to MongoDB Atlas. User passwords are protected using cryptographic bcrypt hashing. When a user logs in, the backend issues an HS256 JWT token. On the Android side, RetrofitClient uses an OkHttp interceptor that automatically attaches this Bearer token to all outgoing requests. When medicines are created or doses are logged as taken, the updates persist directly to MongoDB."
```

```
==================================================================================
SLIDE 5: MAPS, GEOLOCATION & NAVIGATION FLOW (TASK 3)
==================================================================================
Key Bullet Points:
• Interactive in-app vector map display powered by MapLibre Native Android SDK and Geoapify styles.
• Accurate real-time GPS location fix acquired via Google Play Services FusedLocationProviderClient.
• Dynamic nearby healthcare search across 6 categories (Pharmacies, Hospitals, Clinics, Emergencies, Labs, Dentists).
• Direct navigation handoff: Dispatches explicit Intent (geo:0,0?q=lat,lon) directly into native Google Maps.

Exact Filenames to Feature:
• Medicare/app/src/main/res/layout/activity_pharmacy.xml
• Medicare/app/src/main/java/com/example/medicare/PharmacyActivity.kt
• Medicare/app/src/main/java/com/example/medicare/api/GeoapifyClient.kt

Suggested Diagram / Screenshot:
• Screenshot of PharmacyActivity showing vector map with pins, category filter chips, and bottom facility cards with "Directions" buttons.

Spoken Script for Viva:
"Task 3 implements maps and geolocation. Inside the app, we render vector tiles using the open-source MapLibre Android SDK styled with Geoapify Osm-Bright styles. The user's physical GPS location is retrieved with high accuracy using Google Play Services FusedLocationProviderClient after verifying runtime permissions. Nearby facilities are queried via Geoapify Places API v2. When a user taps 'Directions', an Android Intent is dispatched to native Google Maps, launching turn-by-turn navigation."
```

```
==================================================================================
SLIDE 6: AI CONVERSATIONAL CHATBOT WITH CLINICAL GUARDRAILS (TASK 4 - PART A)
==================================================================================
Key Bullet Points:
• Conversational healthcare assistant powered by Google Gemini (gemini-flash-lite-latest) via official SDK.
• Contextual grounding: Backend injects patient profile, active prescriptions, allergies, and ML risk scores.
• Strict negative guardrails: Zero medical guessing, no disease diagnosis, no dosage alterations.
• Factual rule-based fallback when offline or when external cloud AI services are unreachable.

Exact Filenames to Feature:
• Medicare/app/src/main/java/com/example/medicare/AIAssistantActivity.kt
• Medicare-Backend/routes/ai.py
• Medicare-Backend/services/ai_service.py

Suggested Diagram / Screenshot:
• Screenshot of AIAssistantActivity displaying user questions, quick suggestion chips, and Gemini's formatted, guardrailed answers.

Spoken Script for Viva:
"For Task 4's AI assistant, we integrated the Google GenAI SDK with Gemini Flash Lite. When a patient asks a question in AIAssistantActivity, our Flask service gathers their structured profile, medications, and allergies from MongoDB and injects them into the prompt. Our system prompt enforces strict clinical guardrails: Gemini is forbidden from diagnosing diseases or prescribing dosages, and must urge emergency care for severe symptoms. If the API is unreachable, a verified rule-based fallback ensures patient safety."
```

```
==================================================================================
SLIDE 7: SUPERVISED MACHINE LEARNING ADHERENCE ENGINE (TASK 4 - PART B)
==================================================================================
Key Bullet Points:
• Supervised ML model: RandomForestClassifier (100 decision trees, max depth 6) from scikit-learn.
• Serialized model artifact: adherence_model.joblib (108 KB) loaded into process memory.
• 8 behavioral features: Dose counts, 7-day/30-day missed doses, adherence rate, complexity index.
• 3 output risk classes: LOW (0), MEDIUM (1), HIGH (2) Risk (22.68 ms inference latency).
• Strict ethical boundary: ML evaluates schedule compliance habits only; it never diagnoses pathology.

Exact Filenames to Feature:
• Medicare-Backend/ml/train_model.py
• Medicare-Backend/ml/predictor.py
• Medicare-Backend/ml/adherence_model.joblib & model_metrics.json
• Medicare-Backend/verify_ml_model.py

Suggested Diagram / Screenshot:
• Feature importance bar chart showing adherence_percentage (0.20), reminder_response_rate (0.26), and complexity (0.21) alongside model confusion matrix.

Spoken Script for Viva:
"Our machine learning feature is a trained Random Forest Classifier running locally inside the Flask service. As verified in verify_ml_model.py, it loads adherence_model.joblib and evaluates 8 behavioral adherence metrics extracted from the patient's schedule and logs. It classifies adherence risk into Low, Medium, or High in just 22 milliseconds. Crucially, the model does not attempt clinical diagnosis; it is used strictly to prioritize reminder notifications and encourage consistent medication habits."
```

```
==================================================================================
SLIDE 8: DETERMINISTIC CLINICAL SAFETY ENGINE & BACKGROUND ALARMS (TASK 4 - PART C)
==================================================================================
Key Bullet Points:
• Deterministic safety checks: RxNorm API normalization + openFDA label parsing (cached in MongoDB).
• Allergy cross-reactivity mapping: Detects penicillin, sulfa, NSAID, opioid, and statin conflicts.
• Drug-drug interaction screening against active prescriptions (e.g., Aspirin + Warfarin bleeding risk).
• Background exact alarms: Android AlarmManager.setAlarmClock() + WakeLock + lockscreen overlay.
• Multimodal reminders: Android TextToSpeech voice synthesis + RingtoneManager + Vibrator.

Exact Filenames to Feature:
• Medicare-Backend/services/safety_engine.py & medication_knowledge.py
• Medicare/app/src/main/java/com/example/medicare/AlarmScheduler.kt & AlarmReceiver.kt
• Medicare/app/src/main/java/com/example/medicare/ReminderAlarmActivity.kt
• Medicare/app/src/main/java/com/example/medicare/NotificationHelper.kt

Suggested Diagram / Screenshot:
• Screenshots showing: 1) Red Medication Safety Alert dialog when adding Amoxicillin with Penicillin allergy; 2) Fullscreen ReminderAlarmActivity.

Spoken Script for Viva:
"Patient safety cannot rely on probabilistic AI guessing. Therefore, our clinical safety engine operates deterministically. When adding a medicine, safety_engine.py checks patient allergies against pharmacopeia cross-reactivity maps and flags drug-drug interactions. If a patient with a Penicillin allergy adds Amoxicillin, a high-priority alert is shown immediately with an 'Ask AI to Explain' action. For reminders, AlarmScheduler uses AlarmManager exact alarm clocks, triggering AlarmReceiver which acquires a WakeLock and launches ReminderAlarmActivity over the lockscreen with audible speech and alarm tones."
```

```
==================================================================================
SLIDE 9: CONSOLIDATED TASK-TO-FILE QUICK REFERENCE TABLE
==================================================================================
Key Bullet Points:
• Quick-reference matrix linking all four ADL practical tasks to their exact Android, Backend, and Layout files.
• Clear demonstration verification for each task.
• Independent demonstration readiness: Group Member 1 and Group Member 2 can demonstrate any task.

Exact Filenames to Feature:
• Master reference table from PART 7 of the audit report.

Suggested Diagram / Screenshot:
• A clean, stylized table reproducing the consolidated quick reference matrix.

Spoken Script for Viva:
"This consolidated quick-reference matrix summarizes our entire repository implementation. It maps each of the four ADL practical tasks directly to its corresponding XML layout, Kotlin activity, and Python backend service. Both group members are fully prepared to open any of these files in Android Studio or VS Code and demonstrate the live running code independently."
```

```
==================================================================================
SLIDE 10: CONCLUSION & DEMONSTRATION READINESS
==================================================================================
Key Bullet Points:
• All 4 ADL journal tasks fully implemented, verified, and backed by production source code.
• Zero simulated mock data for core flows: Live MongoDB Atlas persistence, real GPS location, and Gemini integration.
• Robust testing: Automated backend unit/integration tests and verified Android debug build.
• Ready for live examiner interrogation and demonstration.

Exact Filenames to Feature:
• Medicare-Backend/test_backend.py (Unit tests)
• Medicare-Backend/test_ai_medication_ml.py (Integration tests)
• ADL_VIVA_CODE_AUDIT.md (This comprehensive audit report)

Suggested Diagram / Screenshot:
• Summary badge graphic highlighting: 24/24 Tests Passed, 100% Kotlin/Python Implementation, Ready for Live Demo.

Spoken Script for Viva:
"In conclusion, our Medicare+ project satisfies and exceeds all practical requirements of the Application Development Laboratory syllabus. We have maintained complete documentation integrity, implemented clean separation of concerns, and verified every flow with automated tests. We are now ready to demonstrate any of the four tasks live on our test device or answer any architectural questions. Thank you."
```

---

*This document was compiled and verified via read-only inspection of the Medicare+ repository.*  
*Report Deliverable: `ADL_VIVA_CODE_AUDIT.md`*

