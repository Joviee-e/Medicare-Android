# MediCare+ Backend

This backend powers the MediCare+ mobile ecosystem with a Flask-based API, MongoDB persistence, AI-assisted health support, and medication intelligence services.

## Overview

The backend provides the application logic behind:

- user registration and login
- patient and doctor profile management
- medication tracking and reminder metadata
- safety checks for allergies and drug interactions
- AI chatbot conversations with context-aware guardrails
- adherence risk prediction using a trained ML model
- notification delivery and health insights

## Tech stack

- Python 3.10+
- Flask
- Flask-JWT-Extended
- PyMongo
- MongoDB
- scikit-learn
- Google Gemini API
- RxNorm and openFDA integrations
- pytest for automated testing

## Project structure

```text
Medicare-Backend/
├── app.py
├── config.py
├── requirements.txt
├── BACKEND_DOCUMENTATION.md
├── test_backend.py
├── test_ai_medication_ml.py
├── test_integration.py
├── verify_ml_model.py
├── database/
│   └── mongo.py
├── ml/
│   ├── adherence_model.joblib
│   ├── model_metrics.json
│   ├── predictor.py
│   └── train_model.py
├── models/
│   ├── appointment.py
│   ├── doctor.py
│   ├── medicine.py
│   ├── notification.py
│   ├── patient.py
│   └── user.py
├── routes/
│   ├── ai.py
│   ├── appointment.py
│   ├── auth.py
│   ├── doctor.py
│   ├── medicine.py
│   ├── ml.py
│   ├── notification.py
│   └── patient.py
├── services/
│   ├── ai_service.py
│   ├── medication_knowledge.py
│   └── safety_engine.py
├── utils/
│   ├── jwt_handler.py
│   ├── password.py
│   └── validators.py
└── .env.example (if present in your environment)
```

## API entry points

The main Flask app registers the following route groups:

- `/api/auth`
- `/api/patients`
- `/api/doctors`
- `/api/medicines`
- `/api/medications`
- `/api/appointments`
- `/api/ai`
- `/api/notifications`
- `/api/ml`

Core endpoints include:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/patients/profile`
- `GET /api/medicines`
- `POST /api/medicines`
- `POST /api/medicines/{id}/log`
- `POST /api/ai/chat`
- `GET /api/ml/adherence`
- `GET /api/health`

## Environment configuration

Create a `.env` file inside `Medicare-Backend/`:

```env
MONGO_URI=mongodb://localhost:27017/
MONGO_DB_NAME=medicare_db
JWT_SECRET_KEY=change-me
GEMINI_API_KEY=your_gemini_key
OPENFDA_API_KEY=your_openfda_key
GOOGLE_CLIENT_ID=your_google_client_id
FLASK_ENV=development
PORT=5000
```

## Local setup

```bash
cd Medicare-Backend
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

The server will run on:

- `http://localhost:5000`
- health check: `http://localhost:5000/api/health`

## Health and safety model

The backend is designed around safe medication decision support:

- deterministic medication safety validation
- allergy and interaction checks
- transparent, explainable AI guidance
- human-safe boundaries for emergency symptoms
- no unsafe clinical prescription generation from ML alone

## ML adherence engine

The ML component uses a `RandomForestClassifier` trained on behavioral adherence factors such as:

- missed doses
- reminder response rate
- dose count patterns
- schedule complexity

It produces risk categories such as `LOW`, `MEDIUM`, or `HIGH` for personalization and notifications.

## Testing

Run the automated test suite:

```bash
cd Medicare-Backend
pytest
```

This repository includes tests for:

- backend API behavior
- AI safety flows
- medication intelligence logic
- ML prediction service

## Documentation

For more detail, refer to:

- [Backend technical documentation](BACKEND_DOCUMENTATION.md)
- [AI and ML integration guide](../AI_ML_INTEGRATION_GUIDE.md)
- [Integration status report](../INTEGRATION_STATUS.md)
- [Repository README](../README.md)

## Notes

This project is built as an academic and demonstration-grade healthcare platform, and it is best used with valid environment variables and a live MongoDB connection for full feature behavior.
