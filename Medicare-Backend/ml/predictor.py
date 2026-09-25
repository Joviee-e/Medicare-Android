"""
Medicare Medication Adherence ML Predictor
Predicts patient adherence risk from actual medication schedules and compliance logs.
Provides personalized insight and prioritization recommendations without making medical diagnoses.
"""

import os
import logging
from datetime import datetime, timedelta
import numpy as np
import joblib

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

def get_model():
    global _model
    if _model is None:
        model_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "adherence_model.joblib")
        if os.path.exists(model_path):
            try:
                _model = joblib.load(model_path)
                logger.info(f"Loaded adherence ML model from {model_path}")
            except Exception as e:
                logger.error(f"Error loading adherence ML model: {e}")
                _model = None
    return _model

def extract_features_from_patient_data(medicines: list) -> dict:
    """
    Extracts quantifiable behavioral adherence features from active medicines and logs.
    """
    now = datetime.utcnow()
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
    """
    features_dict = extract_features_from_patient_data(medicines)
    feature_vector = [features_dict[name] for name in FEATURE_NAMES]

    model = get_model()
    if model is not None:
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
            logger.warning(f"Error during ML inference, using heuristic: {e}")
            risk_label, confidence, prob_dict = _heuristic_adherence_risk(features_dict)
    else:
        risk_label, confidence, prob_dict = _heuristic_adherence_risk(features_dict)

    # Generate personalized recommendations
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
        "adherence_risk": risk_label,
        "confidence": round(confidence, 3),
        "probabilities": prob_dict,
        "insight": insight,
        "priority": priority,
        "recommended_action": recommended_action,
        "features": features_dict,
        "disclaimer": "This prediction assists reminder personalization and does not constitute a clinical evaluation."
    }

def _heuristic_adherence_risk(features: dict):
    """Fallback rule-based risk evaluation if ML model is unavailable."""
    missed_7d = features.get("missed_doses_7d", 0)
    adherence_pct = features.get("adherence_percentage", 100.0)
    med_count = features.get("medication_count", 1)

    if missed_7d >= 3 or adherence_pct < 65.0 or (med_count >= 5 and missed_7d >= 2):
        return "HIGH", 0.85, {"LOW": 0.05, "MEDIUM": 0.15, "HIGH": 0.80}
    elif missed_7d >= 1 or adherence_pct < 85.0 or med_count >= 3:
        return "MEDIUM", 0.75, {"LOW": 0.15, "MEDIUM": 0.70, "HIGH": 0.15}
    else:
        return "LOW", 0.90, {"LOW": 0.85, "MEDIUM": 0.10, "HIGH": 0.05}
