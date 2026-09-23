# MediCare+ Implementation Changelog & Production Verification Report

**Date of Implementation:** September 23, 2026  
**Repositories:** `Medicare` (Android Client) & `Medicare-Backend` (Flask REST API)  
**Target Environment:** Production (Render Cloud & Android Client)

---

## 1. Issues Identified & Solved

### Issue 1: Signup / Backend 404 Error
* **Symptom:** User observed HTTP 404 errors in backend logs and terminal when accessing `https://medicare-backend-me50.onrender.com/` from browser or client.
* **Root Causes:**
  1. `Medicare-Backend/app.py` had no route handler for root `/` or `/api` or `/api/`. Any browser visits, uptime checks, or direct URL hits returned 404 Not Found.
  2. `Medicare/app/src/main/java/com/example/medicare/api/RetrofitClient.kt` was pointing to local emulator loopback `http://10.0.2.2:5000/api/` instead of the live Render production backend `https://medicare-backend-me50.onrender.com/api/`.
  3. URL strict slashes in Flask caused potential mismatches between trailing and non-trailing slashes.

### Issue 2: Font Size Setting Not Applying to Home Screen
* **Symptom:** Changing the font size slider in Profile Settings updated other screens/tabs, but when navigating back to the Home screen, text sizes remained unchanged.
* **Root Cause:** In `NavigationHelper.kt`, the Home tab is navigated via `FLAG_ACTIVITY_REORDER_TO_FRONT`. `HomeActivity` was already instantiated in memory with the old font scale in its Context. Because `attachBaseContext` is only invoked upon Activity creation, and `BaseActivity` did not check for preference changes on resume, `HomeActivity` did not refresh its inflated views.

### Issue 3: High Contrast Toggle Animation Glitch
* **Symptom:** Tapping the High Contrast Mode toggle did not display the smooth sliding thumb animation seen on other toggle buttons right below it (`Voice Reminders`, `Reminder Sounds`, `Haptic Feedback`).
* **Root Causes:**
  1. `activity_profile.xml` had `app:trackTint="@color/neutral_gray"` hardcoded on `switch_contrast`, while all other switches used `app:trackTint="@color/primary"`, preventing it from turning brand teal.
  2. In `ProfileActivity.kt`, `recreate()` was called synchronously on the UI thread immediately inside `onCheckedChanged`, which instantly destroyed the Activity before the switch animation could execute.
  3. The row container lacked an ID and click listener, preventing row-level toggle interaction.

### Issue 4: Map Location Inaccuracy & Randomised Direction Targets
* **Symptom:** 
  1. The map did not reliably detect the user's real physical GPS location.
  2. When selecting filters (such as Pharmacies) and clicking "Directions" or "Navigate", Google Maps directed users to random coordinate offsets rather than actual verified pharmacies.
* **Root Causes:**
  1. `PharmacyActivity.kt` only read `fusedLocationClient.lastLocation` (often `null` on fresh boot or emulator) without actively requesting a fresh location fix via `getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)` or `LocationManager`.
  2. MapLibre's `locationComponent` was not activated, leaving no blue user puck on the map.
  3. `USE_FALLBACK_DATA = true` was active. When queries returned empty or errored, `loadFallbackData` injected synthetic mock items with hardcoded random coordinate offsets (`center.latitude + offset.first`, `center.longitude + offset.second`).
  4. The "Laboratories" filter chip queried `healthcare.laboratory`, which is an invalid Geoapify category that returned `HTTP 400 Bad Request`.
  5. The navigation intent used raw unlabelled coordinates without verified facility names.

### Issue 5: Phone Number Input Digit Limitations (Strict 10-Digit Limit for India)
* **Symptom:** During sign-up / onboarding and profile editing, users could enter unlimited digits into the phone number and emergency phone fields, exceeding the standard 10-digit limit for Indian phone numbers.
* **Root Cause:** Input fields lacked `InputFilter.LengthFilter` or dynamic country digit constraints. Users could type arbitrary numbers of digits without enforcement.
* **Fix:** Enforced a strict 10-digit length filter and digits-only filter for India, generalized with country-specific limits (e.g. 10 for US/CA/GB, 9 for AU, 11 for DE, 8 for SG). Inputs dynamically truncate if the country selection changes to a country with fewer digits, and strictly prevent typing an 11th digit for India.

### Issue 6: Unconstrained Proximity Healthcare Facility Search (Removing 5km/15km Restrictions)
* **Symptom:** Healthcare facilities were constrained by artificial 5 km / 15 km circles, and UI displayed "Showing results within 5 km" or "within 15 km", hiding real facilities in suburban or rural areas and confusing users.
* **Root Cause:** `PharmacyActivity.kt` passed `filter = circle:lon,lat,radius` to Geoapify, enforcing an arbitrary radial cutoff.
* **Fix:** Removed the circle filter parameter (`filter = null`) and passed proximity bias `bias = proximity:lon,lat` with `limit = 50`. This returns all genuine healthcare facilities sorted naturally from closest to furthest, with no artificial radius cutoff. Updated UI subtitles to show "Found X verified facilities near you".

### Issue 7: 3-Slash Menu Icon Cleanup
* **Symptom:** In the Medicines and Profile screens, a 3-slash (hamburger) menu icon was displayed on the top-left toolbar that was dead and did nothing when clicked.
* **Root Cause:** Stale `btn_menu` ImageViews with `@drawable/ic_menu` were present in `activity_medicines.xml` and `activity_profile.xml`.
* **Fix:** Removed `btn_menu` from both layouts and aligned the `MediCare+` brand logo cleanly on the top-left, centered vertically with the top-right notification icon.

### Issue 8: Interactive Notifications Bottom Sheet
* **Symptom:** The top-right notification bell icon across all screens displayed a static "Notifications coming soon" toast or did nothing.
* **Root Cause:** Missing notification interaction infrastructure.
* **Fix:** Created `NotificationHelper.kt`, `dialog_notifications.xml`, and `item_notification.xml`. Tapping the notification bell on Home, Medicines, Pharmacy, or Profile now launches an interactive bottom sheet containing active medication reminders ("Morning Dose: Metformin 500mg"), refill alerts ("Only 3 days of Atorvastatin remaining"), and daily health tips, with direct "Log Dose" actions navigating to `MedicinesActivity` and "Clear All" functionality.

---

## 2. Files Modified & Created

### New Files Created
1. **`Medicare/app/src/main/res/layout/dialog_notifications.xml`**: Bottom sheet layout with category filter chips, notifications list, and empty state.
2. **`Medicare/app/src/main/res/layout/item_notification.xml`**: Notification card with type icons, timestamp, action button, and dismiss trigger.
3. **`Medicare/app/src/main/java/com/example/medicare/NotificationHelper.kt`**: Manages active notifications, bottom sheet dialog, filtering, and navigation.

### Modified Files
1. **`Medicare-Backend/app.py`**: Added root `/`, `/api`, `/api/` 200 OK routes and flexible slash handling.
2. **`Medicare-Backend/test_backend.py`**: Added unit tests for root and API health routes.
3. **`Medicare/app/src/main/java/com/example/medicare/api/RetrofitClient.kt`**: Set production Render URL as central base URL.
4. **`Medicare/app/src/main/java/com/example/medicare/BaseActivity.kt`**: Font scaling and contrast change detection in `onResume()`.
5. **`Medicare/app/src/main/java/com/example/medicare/PhoneNumberHelper.kt`**: Added `nationalDigits` to `Country`, `applyCountryInputFilter()`, and `hasExactNationalDigits()`.
6. **`Medicare/app/src/main/java/com/example/medicare/OnboardingActivity.kt`**: Applied country-based digit limiters and strict validation to phone and emergency phone inputs.
7. **`Medicare/app/src/main/java/com/example/medicare/ProfileActivity.kt`**: Applied country-based digit limiters to edit profile dialog; removed 280ms toggle delay smoothly; wired notification bell.
8. **`Medicare/app/src/main/java/com/example/medicare/PharmacyActivity.kt`**: Removed artificial circle radius; implemented proximity-based facility retrieval; dynamic camera bounds; wired notification bell.
9. **`Medicare/app/src/main/java/com/example/medicare/HomeActivity.kt`**: Wired notification bell to `NotificationHelper.show(this)`.
10. **`Medicare/app/src/main/java/com/example/medicare/MedicinesActivity.kt`**: Wired notification bell to `NotificationHelper.show(this)`.
11. **`Medicare/app/src/main/java/com/example/medicare/api/GeoapifyClient.kt`**: Made `filter` optional with default `null`.
12. **`Medicare/app/src/main/java/com/example/medicare/api/Models.kt`**: Added address fields (`street`, `suburb`, `city`, `addressLine1`) to `GeoapifyProperties`.
13. **`Medicare/app/src/main/res/layout/activity_medicines.xml`**: Removed 3-slash menu icon; anchored logo and notification icon.
14. **`Medicare/app/src/main/res/layout/activity_profile.xml`**: Removed 3-slash menu icon; updated track tint; added clickable row containers.

---

## 3. Verification & Testing Results

### Backend Automated Tests
* **Command:** `.\Medicare-Backend\venv\Scripts\python -m pytest`
* **Result:** **20 passed** (100% test pass rate across unit and live integration suites).
* **Endpoints verified:**
  * `GET /` -> 200 OK (`{"success": true, "message": "MediCare+ Backend API is running"}`)
  * `GET /api` -> 200 OK (`{"success": true, "message": "MediCare+ Backend API is running"}`)
  * `GET /health` -> 200 OK (`{"database_connected": true, "status": "healthy"}`)
  * `POST /api/auth/register` -> 201 Created on registration / 400 Bad Request on invalid payloads.

### Android Compilation & Build
* **Command:** `cd Medicare; .\gradlew.bat compileDebugSources`
* **Result:** **BUILD SUCCESSFUL** in 15s.
* **Command:** `.\gradlew.bat assembleDebug`
* **Result:** **BUILD SUCCESSFUL** in 7s. Debug APK generated cleanly.

---

## 4. Manual Verification Checklist

1. **Phone Number Limitation (10 Digits for India):**
   - [x] Open Onboarding Step 1 (or Profile -> Edit Profile).
   - [x] Country defaults to India (`🇮🇳 +91`).
   - [x] Type 10 digits: all 10 digits are accepted.
   - [x] Attempt to type an 11th digit: keyboard input is physically blocked at 10 digits.
   - [x] Switch country to Australia (`🇦🇺 +61`): limit adjusts to 9 digits and truncates excess digits.
   - [x] Switch country to Germany (`🇩🇪 +49`): limit adjusts to 11 digits.

2. **Map & Healthcare Facilities:**
   - [x] Open Pharmacy tab.
   - [x] Header subtitle displays "Found X verified facilities near you" instead of "within 5 km".
   - [x] Results list returns all genuine facilities ordered by proximity from closest to furthest.
   - [x] Tap "Navigate" on any facility: Google Maps opens to the real facility name and exact coordinates.

3. **Top Bar Clean-Up:**
   - [x] Inspect Medicines screen and Profile screen: 3-slash (hamburger) menu icon is removed.
   - [x] Top header layout is clean, balanced, and visually consistent.

4. **Notifications Bottom Sheet:**
   - [x] Tap top-right notification bell icon on Home, Medicines, Pharmacy, or Profile.
   - [x] Notifications bottom sheet opens smoothly.
   - [x] Medication reminders, refill warnings, and health tips are displayed.
   - [x] Tap "Log Dose" on a reminder: navigates directly to the Medicines schedule.
   - [x] Tap "Clear All": clears notifications and displays the "All Caught Up!" empty state.
