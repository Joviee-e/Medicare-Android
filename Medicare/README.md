# MediCare+ Android UI & Interactive Prototype

A high-fidelity native Android user interface and navigation prototype recreated from visual screens. 

This is a UI-focused prototype built to demonstrate navigation flows, native pickers, runtime lists modification, and interactive chatbot bubbles without any backend/network dependencies.

---

## 📱 Key Prototype Features

1. **Dashboard Home**: Dynamic circular progress bar, upcoming dose cards, and click actions navigating directly to medication alarms.
2. **prescriptions List**: Multi-state filter chips (All, Morning, Evening, As Needed) that update the medicine cards feed. Support for editing values and deleting items with popup confirmations.
3. **Add Medicine Form**:
   - Single-select cards for type (Tablet, Capsule, Syrup, Injection).
   - Frequency configuration chips (Daily, Weekly, As Needed).
   - Android `DatePickerDialog` for start/end calendar dates.
   - `TimePickerDialog` to append reminder alerts to the RecyclerView times feed.
4. **AI Health Chatbot**: Message entry bar enabling text input, auto-scrolling message threads, suggestion chips prompts, delayed placeholder AI responses, and warning toasts for unimplemented voice/attachments.
5. **Nearby Pharmacy Map**: Simulated street grid map overlay with pins, location FAB targets, search card fields, and pharmacy lists with navigate/call buttons. Tapping cards displays schedule and distance details.
6. **Profile Settings**: Accessibility switches (contrast, voice, haptics), sliders, document backup links, and a Sign Out validation dialog redirecting clean stacks back to the dashboard.
7. **Reminder Alarm Overlay**: Standalone alarm overlay with take/snooze/skip loggers.

---

## 🛠 Tech Stack

- **Platform**: Native Android (API Level 33+)
- **Language**: Kotlin
- **Layouts**: ConstraintLayout (Primary), LinearLayout, FrameLayout, NestedScrollView
- **UI Libraries**: Material Design Components (`com.google.android.material:material`)
- **Graphics**: Custom Canvas painting (`CircularProgressView`) and Vector Drawables

---

## 🚀 Setup & Build Instructions

Open this repository in Android Studio or compile using the Gradle wrapper:

### Compile Sources
```bash
./gradlew compileDebugSources
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
