"""
Medicare Medication Adherence ML Predictor
Predicts patient adherence risk from actual medication schedules and compliance logs.
Provides personalized insight and prioritization recommendations without making medical diagnoses.
"""

import os
import logging
from datetime import datetime, timezone, timedelta
import numpy as np
import joblib

import threading

logger = logging.getLogger(__name__)

FEATURE_NAMES = [
    "medication_count",
    "daily_medication_events",
    "missed_doses_7d",
    "missed_doses_30d",
    "adherence_percentage",
    "reminder_response_rate",
    "consecutive_missed_days",
    "medication_complexity"
]

TARGET_MAP = {0: "LOW", 1: "MEDIUM", 2: "HIGH"}

_model = None
_model_lock = threading.Lock()
_model_load_attempted = False

def init_ml_model():
    """
    Initializes and warms up the ML adherence model at application startup.
    Ensures the model and all scikit-learn modules are loaded once into process memory
    before incoming web requests arrive.
    """
    return get_model()

def reset_model():
    """Reset model cache for testing or hot reloading."""
    global _model, _model_load_attempted
    with _model_lock:
        _model = None
        _model_load_attempted = False

def get_model():
    """
    Thread-safe model retrieval. Loads adherence_model.joblib once into process memory.
    If the model fails to load, logs the error and returns None without blocking subsequent requests.
    """
    global _model, _model_load_attempted
    if _model is not None:
        return _model

    with _model_lock:
        if _model is not None:
            return _model
        if _model_load_attempted and _model is None:
            return None

        _model_load_attempted = True
        model_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "adherence_model.joblib")
        if not os.path.exists(model_path):
            logger.warning(f"Adherence ML model file not found at {model_path}")
            _model = None
            return None

        try:
            # Pre-import scikit-learn modules explicitly under lock
            import sklearn
            import sklearn.ensemble
            logger.info(f"Loading adherence ML model from {model_path} (scikit-learn {sklearn.__version__})...")
            _model = joblib.load(model_path)
            logger.info("Adherence ML model successfully loaded and cached in process memory.")
        except Exception as e:
            logger.error(f"Error loading adherence ML model: {e}", exc_info=True)
            _model = None

    return _model

def extract_features_from_patient_data(medicines: list) -> dict:
    """
    Extracts quantifiable behavioral adherence features from active medicines and logs.
    """
    now = datetime.now(timezone.utc)
    seven_days_ago = (now - timedelta(days=7)).strftime("%Y-%m-%d")
    thirty_days_ago = (now - timedelta(days=30)).strftime("%Y-%m-%d")

    med_count = len(medicines)
    daily_events = 0
    types_set = set()
    freq_set = set()

    total_logs = 0
    taken_logs = 0
    missed_7d = 0
    missed_30d = 0
    missed_dates = set()

    for med in medicines:
        types_set.add(med.get("type", "tablet"))
        freq_set.add(med.get("frequency", "daily"))
        reminder_times = med.get("reminder_times", [])
        daily_events += len(reminder_times) if reminder_times else 1

        logs = med.get("logs", [])
        for log in logs:
            total_logs += 1
            status = log.get("status", "").lower()
            log_date = log.get("date", "")

            if status == "taken":
                taken_logs += 1
            elif status in ("missed", "skipped"):
                if log_date >= thirty_days_ago:
                    missed_30d += 1
                if log_date >= seven_days_ago:
                    missed_7d += 1
                if log_date:
                    missed_dates.add(log_date)

    # Adherence Percentage
    if total_logs > 0:
        adherence_pct = round((taken_logs / total_logs) * 100.0, 2)
        response_rate = round(((taken_logs + (total_logs - taken_logs)) / max(total_logs, 1)) * 100.0, 2)
    else:
        # Default for newly registered patients with no history
        adherence_pct = 100.0
        response_rate = 100.0

    # Calculate consecutive missed days
    consec_missed = 0
    check_day = now
    while consec_missed < 14:
        d_str = check_day.strftime("%Y-%m-%d")
        if d_str in missed_dates:
            consec_missed += 1
            check_day -= timedelta(days=1)
        else:
            break

    # Complexity score: combination of count, dosage forms, and frequency diversity
    complexity = round(float(med_count * 1.0 + len(types_set) * 0.4 + len(freq_set) * 0.3), 2)

    return {
        "medication_count": med_count,
        "daily_medication_events": daily_events,
        "missed_doses_7d": missed_7d,
        "missed_doses_30d": missed_30d,
        "adherence_percentage": adherence_pct,
        "reminder_response_rate": response_rate,
        "consecutive_missed_days": consec_missed,
        "medication_complexity": complexity
    }

def predict_adherence_risk(medicines: list) -> dict:
    """
    Evaluates medication adherence risk based on behavioral schedule metrics.
    Returns risk level (LOW, MEDIUM, HIGH), probabilities, and personalized insights.
    If the ML model is unavailable or inference fails, safely returns an unavailable
    status without fabricating an ML prediction.
    """
    try:
        features_dict = extract_features_from_patient_data(medicines)
    except Exception as e:
        logger.warning(f"Error extracting adherence features: {e}")
        return {
            "available": False,
            "adherence_risk": "UNAVAILABLE",
            "confidence": 0.0,
            "probabilities": {},
            "insight": "Adherence pattern evaluation is currently unavailable.",
            "priority": "low",
            "recommended_action": "none",
            "features": {},
            "disclaimer": "Adherence evaluation is currently unavailable."
        }

    feature_vector = [features_dict.get(name, 0) for name in FEATURE_NAMES]

    model = get_model()
    if model is None:
        logger.info("Adherence ML model is unavailable; omitting ML prediction.")
        return {
            "available": False,
            "adherence_risk": "UNAVAILABLE",
            "confidence": 0.0,
            "probabilities": {},
            "insight": "Adherence pattern evaluation is currently unavailable.",
            "priority": "low",
            "recommended_action": "none",
            "features": features_dict,
            "disclaimer": "Adherence ML model is currently unavailable."
        }

    try:
        X = np.array([feature_vector], dtype=np.float32)
        pred_class = int(model.predict(X)[0])
        risk_label = TARGET_MAP.get(pred_class, "LOW")
        probs = model.predict_proba(X)[0]
        confidence = float(np.max(probs))
        prob_dict = {
            "LOW": round(float(probs[0]), 3),
            "MEDIUM": round(float(probs[1]), 3),
            "HIGH": round(float(probs[2]), 3)
        }
    except Exception as e:
        logger.warning(f"Error during ML inference: {e}")
        return {
            "available": False,
            "adherence_risk": "UNAVAILABLE",
            "confidence": 0.0,
            "probabilities": {},
            "insight": "Adherence pattern evaluation is currently unavailable.",
            "priority": "low",
            "recommended_action": "none",
            "features": features_dict,
            "disclaimer": "Adherence ML inference encountered an error."
        }

    # Generate personalized recommendations based on verified model prediction
    if risk_label == "HIGH":
        insight = (
            f"High adherence-risk pattern detected based on recent schedule complexity ({features_dict['medication_count']} medications) "
            f"and {features_dict['missed_doses_7d']} missed doses in the last 7 days. "
            f"Consider reviewing medication reminder times or clustering dose schedules."
        )
        priority = "high"
        recommended_action = "review_schedule"
    elif risk_label == "MEDIUM":
        insight = (
            f"Moderate adherence pattern detected ({features_dict['adherence_percentage']}% adherence). "
            f"Setting up proactive reminders before high-frequency dose times can help avoid missed events."
        )
        priority = "medium"
        recommended_action = "setup_checkins"
    else:
        insight = (
            f"Consistent medication adherence pattern observed ({features_dict['adherence_percentage']}% compliance). "
            f"Keep up the regular routine!"
        )
        priority = "low"
        recommended_action = "maintain_routine"

    return {
        "available": True,
        "adherence_risk": risk_label,
        "confidence": round(confidence, 3),
        "probabilities": prob_dict,
        "insight": insight,
        "priority": priority,
        "recommended_action": recommended_action,
        "features": features_dict,
        "disclaimer": "This prediction assists reminder personalization and does not constitute a clinical evaluation."
    }

