# MediCare+

MediCare+ is a full-stack medication management and health support platform designed to help patients stay on top of prescriptions, understand medication safety, and receive AI-assisted guidance in a clean mobile experience.

This repository contains the complete project stack:

- `Medicare/` — Android app built with Kotlin and Material Design
- `Medicare-Backend/` — Python Flask API with MongoDB, JWT auth, AI, and ML services
- Supporting design and integration documentation in the root directory

## Why this project

MediCare+ combines:

- medication tracking and reminder workflows
- safety checks for allergies and contraindications
- AI-powered patient guidance with guardrails
- adherence risk prediction using a trained ML model
- nearby pharmacy discovery and appointment-ready architecture

It is aimed at making personal medication management more proactive, understandable, and safer for everyday use.

## Project architecture

```text
Android app (Kotlin + XML)
        │
        ▼
Flask REST API (Python)
        │
        ├── Auth & profile services
        ├── Medicine CRUD + compliance logging
        ├── AI clinical chat assistant
        ├── Safety & medication intelligence engine
        ├── ML adherence prediction
        └── Notification and patient insights
        │
        ▼
MongoDB
```

## Core features

### Android app
- Medication dashboard with progress summaries
- Add, edit, delete, and filter prescriptions
- Reminder scheduling and alarm interactions
- AI chatbot interface with contextual responses
- Nearby pharmacy search and navigation flow
- Accessibility settings and profile management
- Notification center and user activity flows

### Backend services
- JWT-based authentication and session protection
- Patient and doctor profile management
- Medication safety analysis using RxNorm and openFDA lookups
- Clinical risk detection for allergies and interactions
- AI chat integration with Gemini
- Adherence prediction with a machine learning model
- Notification APIs and system health monitoring

### AI and ML layer
- Deterministic safety checks instead of model-based medical guessing
- Patient-friendly explanations through Gemini
- Adherence forecasting based on patient behavior indicators
- Guardrails designed to avoid diagnosis and unsafe recommendations

## Repository structure

```text
updated_version/
├── README.md
├── INTEGRATION_STATUS.md
├── AI_ML_INTEGRATION_GUIDE.md
├── IMPLEMENTATION_CHANGELOG.md
├── Medicare/
│   ├── README.md
│   ├── app/
│   ├── gradle/
│   ├── icons/
│   └── UI/
├── Medicare-Backend/
│   ├── app.py
│   ├── config.py
│   ├── requirements.txt
│   ├── database/
│   ├── models/
│   ├── routes/
│   ├── services/
│   ├── ml/
│   ├── utils/
│   └── BACKEND_DOCUMENTATION.md
└── ...
```

## Tech stack

| Layer | Stack |
|---|---|
| Mobile app | Kotlin, Android SDK, Material Components, Retrofit, OkHttp |
| Backend | Python, Flask, JWT, CORS |
| Database | MongoDB via PyMongo |
| AI | Google Gemini via official SDK |
| ML | scikit-learn, joblib |
| Data enrichment | RxNorm, openFDA |
| Location | Geoapify API |
| Testing | pytest, Android unit/instrumentation tests |

## Quick start

### Prerequisites

- Android Studio with Android SDK
- JDK 11 or newer
- Python 3.10+
- MongoDB instance (local or Atlas)
- API keys for:
  - Gemini / Google AI
  - openFDA (optional but recommended for medication intelligence)
  - Geoapify for pharmacy map features

### 1) Backend setup

```bash
cd Medicare-Backend
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

Create a `.env` file in `Medicare-Backend/` with values like:

```env
MONGO_URI=mongodb://localhost:27017/
MONGO_DB_NAME=medicare_db
JWT_SECRET_KEY=your-super-secret-key
GEMINI_API_KEY=your_gemini_key
OPENFDA_API_KEY=your_openfda_key
GOOGLE_CLIENT_ID=your_google_client_id
FLASK_ENV=development
PORT=5000
```

Run the backend:

```bash
python app.py
```

The API will be available at:

- `http://localhost:5000/`
- `http://localhost:5000/api/health`

### 2) Android app setup

Open `Medicare/` in Android Studio and let Gradle sync.

The Android app reads the Geoapify key from a local `.env` file or the backend `.env` file when building. Add the following locally if needed:

```env
GEOAPIFY_API_KEY=your_geoapify_key
```

Then build and run:

```bash
cd Medicare
./gradlew assembleDebug
```

or run from Android Studio directly.

## Running and testing

### Backend tests

```bash
cd Medicare-Backend
pytest
```

### Android build validation

```bash
cd Medicare
./gradlew compileDebugSources
./gradlew assembleDebug
```

## Documentation index

- [Medicare app README](Medicare/README.md)
- [Backend documentation](Medicare-Backend/BACKEND_DOCUMENTATION.md)
- [AI and ML integration guide](AI_ML_INTEGRATION_GUIDE.md)
- [Integration status report](INTEGRATION_STATUS.md)
- [Implementation changelog](IMPLEMENTATION_CHANGELOG.md)

## Notes

- The app is designed as a healthcare-oriented mobile experience with patient safety in mind.
- Clinical AI features are intentionally guardrailed and deterministic where relevant.
- The system is built for real deployment workflows, but local environment values and credentials must be supplied securely.

## License

This project is intended for academic and demonstration use. Please check your local institutional or repository policy before distribution or commercial deployment.
