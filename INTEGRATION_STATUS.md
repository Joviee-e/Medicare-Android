# Medicare+ Integration & Status Report

This document outlines the system integration status, API specifications, and testing coverage between the Medicare Android app and the Flask/MongoDB Backend.

---

## 1. Backend API & MongoDB Specifications

### MongoDB Collections
* **`users`**: User credentials and account roles (`patient` or `doctor`).
* **`patients`**: Patient profiles, including accessibility configurations.
* **`doctors`**: Doctor profiles, specialties, and schedules.
* **`medicines`**: Patient medication configurations, reminder times, and compliance logs.
* **`appointments`**: Bookings between patients and doctors.

---

## 2. API Reference & Android Client Mappings

### A. Authentication
* **Register**: `POST /api/auth/register`
  * **DTOs**: `RegisterRequest` -> `AuthResponse`
  * **Android screen**: `RegisterActivity`
* **Login**: `POST /api/auth/login`
  * **DTOs**: `LoginRequest` -> `AuthResponse`
  * **Android screen**: `LoginActivity`
* **Logout**: `POST /api/auth/logout`
  * **DTOs**: None -> `BaseResponse`
  * **Android screen**: `ProfileActivity` (Sign Out option)

### B. Patient Profile
* **Get Profile**: `GET /api/patients/profile`
  * **DTOs**: None -> `ProfileResponse`
  * **Android screen**: `ProfileActivity`
* **Update Profile**: `PUT /api/patients/profile`
  * **DTOs**: `UpdateProfileRequest` -> `BaseResponse`
  * **Android screen**: `ProfileActivity` (Edit profile dialog / Accessibility toggles sync)

### C. Medication Management
* **Get Medicines**: `GET /api/medicines`
  * **DTOs**: None -> `GetMedicinesResponse`
  * **Android screens**: `HomeActivity` (Today's schedule), `MedicinesActivity` (All medicines feed)
* **Create Medicine**: `POST /api/medicines`
  * **DTOs**: `MedicineRequest` -> `MedicineResponse`
  * **Android screen**: `AddMedicineActivity` (Create Mode)
* **Update Medicine**: `PUT /api/medicines/{id}`
  * **DTOs**: `MedicineRequest` -> `BaseResponse`
  * **Android screen**: `AddMedicineActivity` (Edit Mode)
* **Delete Medicine**: `DELETE /api/medicines/{id}`
  * **DTOs**: None -> `BaseResponse`
  * **Android screen**: `MedicinesActivity` (Delete confirmation)
* **Log Compliance**: `POST /api/medicines/{id}/log`
  * **DTOs**: `LogRequest` -> `BaseResponse`
  * **Android screen**: `ReminderAlarmActivity` (Actions: Taken, Snoozed, Skipped)

### D. Appointments
* **Endpoints available in Flask**:
  * `GET /api/appointments`
  * `POST /api/appointments`
  * `PUT /api/appointments/{id}`
* **Android integration**:
  * *Status*: Incomplete. Currently, there is no Appointment UI in the Android mobile application. This remains purely a backend functionality.

---

## 3. Integration & Flow Validation Status

### Completed
* **End-to-End Authentication**: Connected Android's registration and login forms to Flask/MongoDB. Credentials and JWT session tokens are validated, saved locally via `SessionManager`, and sent in the headers of all protected requests.
* **Patient Profile & Accessibility Sync**: Connected profile views to read/write details and haptic/voice/contrast configurations from MongoDB.
* **Medication CRUD & Log Compliance**: Connected list fetching, card creation, editing, deletion, and compliance logging actions (mark as Taken/Skipped/Snoozed) to live backend endpoints.
* **Global Error Handling**: Added custom parser logic to decode non-2xx API error payloads and show user-friendly error toast notifications.
* **Global 401 Redirects**: Handled unauthorized or expired user sessions globally inside the Retrofit OkHttpClient pipeline to force clean redirects back to `LoginActivity`.
* **Security & Git Hygiene**: Removed `.env` configuration file from Git indexing and added it to `.gitignore` to prevent leaking connection strings. Checked repository history to verify no sensitive credentials were committed in the past.

### Backend
* **Mock Unit Tests**: 8/8 PASSED. Verified Flask routes and models run correctly with mock database interactions.
* **Live Integration Tests**: 9/9 PASSED. Runs end-to-end tests validating auth, patient profile, and medicine CRUD flows directly against the MongoDB Atlas cloud instance.

### Android
* **Build status**: `.\gradlew compileDebugSources` - **BUILD SUCCESSFUL**.
* **API integration status**: All main dashboard, medicine, creation, settings, and logging screens are connected and functioning with the API.

### MongoDB
* **Collections verified**: `users`, `patients`, `medicines` collections are automatically queried and modified dynamically.
* **CRUD flows verified**: Registration instantiates users and patients; medicines can be created, updated, and deleted; logs are appended correctly on dose compliance submission.

### End-to-End Flow Verification Result
1. **User Registration Flow**: PASSED. Registration creates user document + empty patient details in Atlas.
2. **User Login & Session Flow**: PASSED. Login yields JWT tokens, stored in SharedPreferences. Redirects correctly on start.
3. **Profile Read & Write**: PASSED. Fetches name/emergency information and updates profile dialog fields.
4. **Accessibility Persistence**: PASSED. Switches and seek bars sync settings automatically to MongoDB.
5. **Medicine CRUD Flow**: PASSED. Adding medicines inserts document; editing updates document; deleting shows confirmation and cleans record in database.
6. **Medication Compliance Flow**: PASSED. Logging Taken/Skipped/Snoozed logs compliance records live to database.
7. **Session Expiry (401)**: PASSED. Automatically logs out and launches Login screen on unauthorized requests.

### Known Issues
* **Appointment UI**: The Android app lacks appointment UI interfaces, so the backend appointment endpoints are not visible or integrated in the mobile UI.

### Next Steps
* Implement Appointment UI screens on the Android client if appointment booking is scheduled for mobile support.
