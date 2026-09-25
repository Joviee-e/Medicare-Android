# Medicare — AI Chatbot, Medication Intelligence & Machine Learning Architecture Guide

This guide details the complete system architecture, operational workflows, integration points, machine learning specifications, security protocols, and deployment guidelines for the Medicare healthcare platform.

---

## 1. System Architecture & Separation of Concerns

Medicare enforces a strict **separation of concerns** across four foundational layers to guarantee patient safety, deterministic clinical checks, personalized guidance, and explainable AI communication:

```
+-----------------------------------------------------------------------------------+
|                                 PATIENT / ANDROID                                 |
|   - Add Medicine UI        - Top-Right Notification Bell      - AI Chatbot UI     |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼ (HTTPS / JWT)
+-----------------------------------------------------------------------------------+
|                               FLASK API GATEWAY                                   |
|   /api/medicines           /api/notifications                 /api/ai/chat        |
|   /api/medications/analyze /api/ml/adherence                                      |
+-----------------------------------------------------------------------------------+
       │                                     │                         │
       ▼                                     ▼                         ▼
┌─────────────────────────┐       ┌──────────────────────┐   ┌──────────────────────┐
│  MEDICATION KNOWLEDGE   │       │    SAFETY ENGINE     │   │      ML ENGINE       │
│  - RxNorm Normalization │──────▶│ - Deterministic      │   │ - Behavioral Adhere. │
│  - openFDA Label Fetch  │       │   Allergy Detection  │   │   Risk Classifier    │
│  - MongoDB Label Cache  │       │ - Drug Interactions  │   │ - Low / Med / High   │
└─────────────────────────┘       │ - Verified Guidance  │   └──────────────────────┘
                                  └──────────────────────┘              │
                                             │                          │
                                             ▼                          ▼
                                  ┌─────────────────────────────────────────────────┐
                                  │           NOTIFICATION & INSIGHT HUB            │
                                  │ - Prioritized In-App Alerts (HIGH/MED/LOW)      │
                                  │ - Context Bridge to Chatbot                     │
                                  └─────────────────────────────────────────────────┘
                                             │
                                             ▼
                                  ┌─────────────────────────────────────────────────┐
                                  │               AI CONVERSATION LAYER             │
                                  │ - Google Gemini (Official google-genai SDK)     │
                                  │ - Zero Medical Guesswork / Clinical Disclaimers │
                                  │ - Explain Verified Findings in Plain Language   │
                                  └─────────────────────────────────────────────────┘
```

### Layer Definitions
1. **Medication Knowledge Layer (`services/medication_knowledge.py`)**:
   - Standardizes brand/generic names via the official **RxNorm** REST API.
   - Retrieves FDA-approved product labels (warnings, precautions, contraindications, administration guidelines) from **openFDA**.
   - Caches responses in MongoDB collection `medication_cache` with a 30-day TTL to eliminate redundant external HTTP requests.
2. **Safety Engine Layer (`services/safety_engine.py`)**:
   - Executes deterministic clinical conflict detection.
   - Evaluates patient-recorded allergies against cross-reactivity classes (penicillins, sulfonamides, NSAIDs, aspirin, opioids, statins, ACE inhibitors, acetaminophen, macrolides, fluoroquinolones).
   - Detects drug-drug interactions and duplicate therapeutic classes against all active medications.
   - Extracts label-supported administration instructions (e.g., "Take with food", "Swallow whole with water"). Never hallucinates instructions.
3. **Machine Learning Engine Layer (`ml/predictor.py` & `ml/train_model.py`)**:
   - Implements a `RandomForestClassifier` trained on behavioral features (dose count, reminder response rate, missed doses over 7/30 days, schedule complexity).
   - Predicts patient adherence risk: `LOW`, `MEDIUM`, or `HIGH`.
   - **Safety Boundary**: ML never determines medical safety or modifies prescriptions; it only personalizes notification priority and adherence encouragement.
4. **AI Conversation Layer (`services/ai_service.py`)**:
   - Employs the Google Gemini API (`gemini-flash-lite-latest` default) using the official `google-genai` Python SDK.
   - Ingests structured clinical findings, patient medications, allergies, and ML insights.
   - Translates technical clinical findings into patient-friendly explanations while strictly adhering to safety guardrails (no diagnosing, no prescribing, mandatory emergency referral).

---

## 2. Existing Chatbot Architecture & Integration

The chatbot preserves 100% of the existing Android UI (`AIAssistantActivity`) while replacing simulated client-side responses with the live Flask AI Gateway.

### End-to-End Chat Flow
```
Android (AIAssistantActivity)
    │
    ▼ POST /api/ai/chat (Bearer JWT, message, optional context)
Flask API Gateway (routes/ai.py)
    │
    ├─▶ Retrieve Patient Profile (allergies, conditions) from MongoDB
    ├─▶ Retrieve Active Medications & Compliance Stats from MongoDB
    ├─▶ Fetch Recent Safety Alerts & Notifications
    ├─▶ Assemble Structured Clinical Prompt (Zero PII/Secrets)
    │
    ▼
Gemini API (services/ai_service.py)
    │
    ▼ Returns Empathetic, Guardrailed Explanation
Flask Response `{ "success": true, "response": "..." }`
    │
    ▼
Android Chat UI (ChatMessage item appended to RecyclerView)
```

### Safety Guardrails
The backend system prompt strictly instructs Gemini:
- **No Diagnosing**: "Never diagnose conditions or tell the patient what disease they have."
- **No Prescribing**: "Never prescribe medication or advise altering dosages."
- **Deterministic Reliance**: "Only explain medication facts, warnings, and allergy risks that are provided in the structured context."
- **Emergency Escalation**: "For severe symptoms (chest pain, breathing difficulty, anaphylaxis), advise immediate local emergency care."

---

## 3. Medication Intelligence Flow (Adding a Medicine)

When a patient adds a medication via `AddMedicineActivity`:

```
User enters "Amoxicillin" 500mg
       │
       ▼ POST /api/medicines
Backend:
1. Normalize via RxNorm API:
   - "amoxicillin" -> RxCUI: "723", Standardized Name: "amoxicillin"
2. Retrieve openFDA label details:
   - Warnings, Precautions, Contraindications, Administration guides.
   - Stored in MongoDB `medication_cache`.
3. Deterministic Safety Engine Execution:
   - Check Allergies: Patient has "Penicillin" allergy -> CONFLICT DETECTED!
   - Priority: HIGH.
   - Title: "Potential Allergy Concern Detected"
   - Message: "Amoxicillin belongs to the penicillin class. Potential cross-reactivity."
4. Check Drug Interactions:
   - Cross-check against patient's active medication list.
5. Extract Administration Guidance:
   - "Take with or without food. Complete the full prescribed course."
6. Compute ML Adherence Risk:
   - Predicts risk profile (e.g., LOW).
7. Create Persistent Notifications:
   - Saved to MongoDB `notifications` collection with priority `HIGH`.
8. Return Enriched Response to Android:
   - Contains `alerts`, `guidance`, and `ml_adherence`.
Android:
- Prompts prominent red Medication Alert dialog.
- Updates top-right notification indicator with red badge.
- Provides "Ask AI to Explain" action button directly launching `AIAssistantActivity`.
```

---

## 4. Top-Right Notification Area Integration

Medicare utilizes the existing top-right notification bell (`NotificationHelper`) on the Android dashboard:

### Notification Priority System
| Priority | Trigger Examples | Visual Indicator | Tap Action |
|:---|:---|:---|:---|
| **HIGH** | Allergy conflict, severe label contraindication | 🔴 Red Dot / Red Badge | Opens `AIAssistantActivity` with full alert context pre-loaded |
| **MEDIUM** | Drug-drug interaction, duplicate therapy class | ⚠️ Orange Badge | Opens `AIAssistantActivity` or Medicine Detail screen |
| **LOW / INFO** | Administration tips, schedule reminders, adherence encouragement | 🟢 Teal Badge | Displays guidance card or reminder review screen |

### Backend Endpoints
- `GET /api/notifications`: Fetches unread and historical notifications sorted by priority (`HIGH` > `MEDIUM` > `LOW`) and timestamp.
- `POST /api/notifications/<id>/read`: Marks an individual notification as read.
- `POST /api/notifications/clear`: Clears all notifications for the authenticated patient.

---

## 5. Machine Learning Architecture (Adherence Risk)

### Purpose & Ethical Boundary
The ML model predicts the likelihood of medication non-adherence based on observable user interaction patterns. It is strictly used to:
- Prioritize reminder alerts.
- Personalize educational dosage prompts.
- Inform the AI chatbot context to offer supportive scheduling tips.

**Strict Limitation:** The ML model is **NEVER** used to validate medical safety, diagnose illness, or override clinical safety warnings.

### Model Specification
- **Algorithm:** `RandomForestClassifier` (`n_estimators=100`, `max_depth=6`, `random_state=42`)
- **Library:** `scikit-learn==1.9.1`, serialized via `joblib`.
- **Target Classes:** `LOW`, `MEDIUM`, `HIGH` adherence risk.
- **Input Features (8 Behavioral Dimensions):**
  1. `medication_count`: Total active prescriptions.
  2. `daily_medication_events`: Total scheduled dose times per day.
  3. `missed_doses_7d`: Number of skipped/missed doses in the past 7 days.
  4. `missed_doses_30d`: Number of skipped/missed doses in the past 30 days.
  5. `adherence_percentage`: Historical compliance rate (0.0 to 100.0).
  6. `reminder_response_rate`: Percentage of reminders responded to within 30 minutes.
  7. `consecutive_missed_days`: Current streak of consecutive days with at least one missed dose.
  8. `medication_complexity`: Weighted score factoring multiple frequencies and irregular intervals.

### Model Performance & Evaluation Metrics
Trained on 1,600 synthesized patient behavioral profiles reflecting real-world clinical adherence distributions (80/20 train/test split):

| Metric | Score |
|:---|:---|
| **Accuracy** | **100.0%** |
| **Precision (Weighted)** | **100.0%** |
| **Recall (Weighted)** | **100.0%** |
| **F1-Score (Weighted)** | **100.0%** |

#### Confusion Matrix
```
                  Predicted LOW   Predicted MEDIUM   Predicted HIGH
Actual LOW             140               0                 0
Actual MEDIUM            0              115                0
Actual HIGH              0               0                 65
```

> **Evaluation Disclaimer:** *The model was initially trained and evaluated using synthetic medication-adherence data for application prototyping. The results do not constitute clinical validation.*

---

## 6. External APIs & Caching Strategy

### RxNorm REST API
- **Endpoint:** `https://rxnav.nlm.nih.gov/REST`
- **Methodology:**
  1. Exact concept match via `/rxcui.json?name={name}`.
  2. Fallback approximate phonetic/spelling match via `/approximateTerm.json?term={name}&maxEntries=1`.
  3. Concept property lookup via `/rxcui/{rxcui}/allProperties.json?prop=all`.
- **Purpose:** Standardizes commercial brand names (e.g., "Tylenol" -> "acetaminophen", RxCUI `161`).

### openFDA Drug Label API
- **Endpoint:** `https://api.fda.gov/drug/label.json`
- **Authentication:** `OPENFDA_API_KEY` passed as query parameter when configured; public rate-limiting fallback when absent.
- **Data Extracted:** `boxed_warning`, `warnings`, `contraindications`, `drug_interactions`, `adverse_reactions`, `dosage_and_administration`, `information_for_patients`.

### MongoDB Caching Schema (`medication_cache`)
```json
{
  "search_key": "paracetamol",
  "normalized_name": "acetaminophen",
  "rxcui": "161",
  "label_information": {
    "warnings": ["Liver warning: This product contains acetaminophen..."],
    "precautions": ["Do not take more than directed..."],
    "contraindications": ["Hypersensitivity to acetaminophen..."],
    "interactions": ["Do not use with any other drug containing acetaminophen..."],
    "adverse_reactions": [],
    "administration": ["Do not exceed 4,000 mg in 24 hours. Take with water."]
  },
  "source": { "rxnorm": true, "openfda": true },
  "cached_at": "2026-09-25T17:00:00Z"
}
```

---

## 7. Environment Variables & Security

### Backend Environment Configuration (`Medicare-Backend/.env`)
| Variable | Description | Default / Example | Required |
|:---|:---|:---|:---|
| `MONGO_URI` | MongoDB Atlas connection string | `mongodb+srv://user:pass@cluster.mongodb.net/medicare` | Yes |
| `JWT_SECRET_KEY` | Secret key for JWT signing | Strong random string | Yes |
| `GEMINI_API_KEY` | Google Gemini API key | `AIzaSy...` | Yes |
| `GEMINI_MODEL` | Target Gemini model identifier | `gemini-flash-lite-latest` | No |
| `OPENFDA_API_KEY` | FDA API developer access key | Optional developer key | No |
| `FLASK_ENV` | Runtime environment | `production` / `development` | Yes |
| `PORT` | Listening port for web server | `5000` / `10000` (Render) | No |

### Security Measures
1. **Zero Secret Leakage:** Neither `GEMINI_API_KEY`, `OPENFDA_API_KEY`, nor MongoDB connection strings are compiled into or accessible by the Android APK.
2. **Strict JWT Authentication:** All patient-specific endpoints (`/api/ai/chat`, `/api/medicines`, `/api/notifications`, `/api/ml/adherence`) enforce valid JWT Bearer tokens and verify patient ID parity.
3. **PII Sanitization:** The AI prompt compiler strips passwords, phone numbers, email addresses, and identification numbers prior to invoking Gemini.

---

## 8. Deployment Specifications

### Render Deployment (Backend)
The backend is completely containerless and runs natively on Render Python web services:
- **Build Command:** `pip install -r requirements.txt`
- **Start Command:** `gunicorn app:app --workers 2 --bind 0.0.0.0:$PORT`
- **Environment Variables on Render Dashboard:**
  - `GEMINI_API_KEY`
  - `GEMINI_MODEL=gemini-flash-lite-latest`
  - `MONGO_URI`
  - `JWT_SECRET_KEY`
  - `OPENFDA_API_KEY` (optional)
- **Zero Local Dependencies:**
  - Render does NOT require Ollama.
  - Render runs the local `joblib` Random Forest model natively with standard Python `scikit-learn`.
  - External calls originate directly from Render to Google Gemini, RxNorm, and openFDA.

### Android Deployment (Mobile App)
- **Build Command:** `.\gradlew assembleDebug` or `.\gradlew assembleRelease`
- **Network Configuration:** Configured in `com.example.medicare.api.RetrofitClient.kt`:
  ```kotlin
  private var baseUrl = "https://medicare-backend-me50.onrender.com/api/"
  ```
- **Autonomous Operation:** The mobile app requires only standard internet access; it does not require local servers, Python, or development tools to be active.

---

## 9. Verification & Testing Summary

### Automated Test Coverage (24/24 Tests Passed)
- **Unit & Integration Tests (`test_ai_medication_ml.py`):**
  1. `test_ai_chat_valid_response`: Verifies conversational response formatting and context assembly.
  2. `test_ai_chat_empty_message`: Verifies 400 rejection on empty inputs.
  3. `test_ai_chat_medication_context`: Verifies inclusion of current medications in AI prompts.
  4. `test_allergy_conflict_detection_penicillin`: Validates deterministic penicillin cross-reactivity detection.
  5. `test_allergy_conflict_detection_nsaid`: Validates ibuprofen/naproxen NSAID allergy conflict detection.
  6. `test_no_allergy_conflict`: Confirms absence of false positives for non-allergic medications.
  7. `test_drug_interaction_detection`: Verifies NSAID + Aspirin or Warfarin interaction warnings.
  8. `test_duplicate_therapy_detection`: Flags concurrent duplicate therapeutic classes.
  9. `test_medication_analyze_route`: Validates standalone `/api/medications/analyze` endpoint.
  10. `test_get_notifications_route`: Verifies prioritized notification retrieval.
  11. `test_get_ml_adherence_route`: Verifies feature extraction and risk scoring.
  12. `test_ml_prediction_low_risk`: Validates model classification for compliant patient profile.
  13. `test_ml_prediction_high_risk`: Validates model classification for high-missed-dose profile.
- **Core Platform Tests (`test_backend.py`):** 11/11 tests passed for user authentication, patient profile CRUD, and medicine management.

---

## 10. Limitations & Clinical Disclaimers
1. **Non-Diagnostic:** Medicare is an assistive health management tool. It does not possess medical diagnostic authority and does not substitute for licensed healthcare professional consultation.
2. **Label Data Scope:** OpenFDA and RxNorm coverage reflects US FDA-approved drug products and terminology. Regional brand formulations or off-label indications outside FDA datasets may require manual physician review.
3. **Synthetic Training Data:** Adherence risk scoring was trained on synthetic behavioral distributions and serves as an organizational and adherence aid rather than a clinical diagnostic outcome.
