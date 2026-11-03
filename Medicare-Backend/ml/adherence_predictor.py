"""
Medication Adherence Predictor (Random Forest Classifier)
Predicts the likelihood that a patient will miss upcoming medication doses
based on historical medication-taking behavior.
"""

import os
import logging
from datetime import datetime, timezone, timedelta
import numpy as np
import joblib

logger = logging.getLogger(__name__)

FEATURE_NAMES = [
    "historical_adherence_pct",
    "missed_doses_total",
    "delayed_doses_total",
    "avg_delay_minutes",
    "reminder_response_latency_sec",
    "morning_adherence_pct",
    "afternoon_adherence_pct",
    "evening_adherence_pct",
    "weekend_adherence_pct",
    "recent_missed_7d"
]

RISK_LEVEL_MAP = {0: "Low", 1: "Moderate", 2: "High"}

_clf_model = None


def get_adherence_model():
    """Load and cache the trained Random Forest adherence classifier."""
    global _clf_model
    if _clf_model is not None:
        return _clf_model

    model_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "adherence_classifier.joblib")
    if not os.path.exists(model_path):
        logger.warning(f"Adherence classifier not found at {model_path}")
        return None

    try:
        _clf_model = joblib.load(model_path)
        logger.info(f"Loaded adherence classifier from {model_path}")
    except Exception as e:
        logger.error(f"Failed to load adherence classifier: {e}", exc_info=True)
        _clf_model = None

    return _clf_model


def parse_dose_time(time_str: str) -> tuple:
    """Parse time string like '08:00 AM', '14:30', '8:00' into (hour, minute)."""
    if not time_str:
        return (9, 0)
    time_str = time_str.strip()
    for fmt in ("%I:%M %p", "%I:%M%p", "%H:%M", "%H:%M:%S"):
        try:
            dt = datetime.strptime(time_str, fmt)
            return (dt.hour, dt.minute)
        except ValueError:
            pass
    return (9, 0)


def classify_time_bucket(hour: int) -> str:
    """Classify an hour into morning, afternoon, evening, or night."""
    if 5 <= hour < 12:
        return "morning"
    elif 12 <= hour < 17:
        return "afternoon"
    elif 17 <= hour < 22:
        return "evening"
    else:
        return "night"


def extract_patient_behavioral_features(medicines: list) -> dict:
    """
    Extracts quantifiable behavioral adherence features from active medicines and their logs.
    Handles variable log schemas gracefully.
    """
    total_logs = 0
    taken_logs = 0
    missed_logs = 0
    delayed_logs = 0
    delay_sum = 0.0
    delay_count = 0
    latency_sum = 0.0
    latency_count = 0

    bucket_stats = {
        "morning": {"total": 0, "taken": 0},
        "afternoon": {"total": 0, "taken": 0},
        "evening": {"total": 0, "taken": 0},
        "night": {"total": 0, "taken": 0}
    }
    weekend_stats = {"total": 0, "taken": 0}

    now_utc = datetime.now(timezone.utc)
    seven_days_cutoff = now_utc - timedelta(days=7)
    recent_missed_7d = 0

    all_logs = []
    for med in medicines:
        logs = med.get("logs", [])
        if isinstance(logs, list):
            for l in logs:
                if isinstance(l, dict):
                    all_logs.append(l)

    for l in all_logs:
        total_logs += 1
        status = str(l.get("status", "")).lower()
        date_str = str(l.get("date", "")).strip()
        time_str = str(l.get("time", "")).strip()

        # Parse date
        log_dt = None
        is_weekend = False
        if date_str:
            for dfmt in ("%Y-%m-%d", "%d-%m-%Y", "%d/%m/%Y", "%Y/%m/%d"):
                try:
                    parsed_d = datetime.strptime(date_str, dfmt)
                    log_dt = parsed_d
                    # Saturday=5, Sunday=6
                    if parsed_d.weekday() in (5, 6):
                        is_weekend = True
                    break
                except ValueError:
                    pass

        # Time bucket
        bucket = l.get("time_bucket")
        hour, minute = parse_dose_time(time_str)
        if not bucket or bucket not in bucket_stats:
            bucket = classify_time_bucket(hour)

        bucket_stats[bucket]["total"] += 1
        if is_weekend:
            weekend_stats["total"] += 1

        # Delay
        delay_min = l.get("delay_minutes")
        if delay_min is not None:
            try:
                d_val = float(delay_min)
                delay_sum += max(0.0, d_val)
                delay_count += 1
                if d_val > 15:
                    delayed_logs += 1
            except (ValueError, TypeError):
                pass

        # Response latency
        resp_sec = l.get("response_time_seconds")
        if resp_sec is not None:
            try:
                r_val = float(resp_sec)
                latency_sum += max(0.0, r_val)
                latency_count += 1
            except (ValueError, TypeError):
                pass

        # Status categorization
        if status in ("taken", "completed"):
            taken_logs += 1
            bucket_stats[bucket]["taken"] += 1
            if is_weekend:
                weekend_stats["taken"] += 1
        elif status in ("missed", "skipped"):
            missed_logs += 1
            if log_dt and log_dt.date() >= seven_days_cutoff.date():
                recent_missed_7d += 1
        elif status == "snoozed":
            delayed_logs += 1

    # Calculations with fallback defaults
    if total_logs > 0:
        hist_adh = float(round((taken_logs / total_logs) * 100.0, 1))
    else:
        hist_adh = 100.0

    def calc_pct(stat_dict):
        tot = stat_dict["total"]
        if tot == 0:
            return 100.0
        return float(round((stat_dict["taken"] / tot) * 100.0, 1))

    morn_adh = calc_pct(bucket_stats["morning"])
    aft_adh = calc_pct(bucket_stats["afternoon"])
    eve_adh = calc_pct(bucket_stats["evening"])
    wknd_adh = calc_pct(weekend_stats)

    avg_delay = float(round(delay_sum / delay_count, 1)) if delay_count > 0 else 0.0
    avg_latency = float(round(latency_sum / latency_count, 1)) if latency_count > 0 else 30.0

    feature_vector = [
        hist_adh,
        missed_logs,
        delayed_logs,
        avg_delay,
        avg_latency,
        morn_adh,
        aft_adh,
        eve_adh,
        wknd_adh,
        recent_missed_7d
    ]

    return {
        "features": feature_vector,
        "feature_dict": dict(zip(FEATURE_NAMES, feature_vector)),
        "total_logs": total_logs,
        "taken_logs": taken_logs,
        "missed_logs": missed_logs,
        "delayed_logs": delayed_logs,
        "avg_delay_minutes": avg_delay,
        "avg_latency_seconds": avg_latency,
        "morning_adherence_pct": morn_adh,
        "afternoon_adherence_pct": aft_adh,
        "evening_adherence_pct": eve_adh,
        "weekend_adherence_pct": wknd_adh,
        "recent_missed_7d": recent_missed_7d,
        "historical_adherence_pct": hist_adh
    }


def predict_adherence(medicines: list) -> dict:
    """
    Evaluates patient adherence risk using the Random Forest Classifier.
    Includes cold-start protection (< 5 doses).
    Distinguishes historical adherence percentage from predicted future miss probability.
    """
    stats = extract_patient_behavioral_features(medicines)
    total_logs = stats["total_logs"]

    # Cold-start safeguard
    if total_logs < 5:
        return {
            "status": "insufficient_data",
            "message": "At least 5 logged doses are required to generate personalized ML predictions. Continue logging your doses over the next few days.",
            "minimum_required_doses": 5,
            "current_logged_doses": total_logs,
            "historical_adherence_pct": stats["historical_adherence_pct"],
            "predicted_miss_probability": 0.10,
            "risk_level": "Low",
            "confidence_score": 0.50,
            "contributing_factors": [
                "New patient baseline — insufficient history to establish behavioural pattern",
                "Keep logging scheduled doses on time"
            ]
        }

    clf = get_adherence_model()
    X = np.array([stats["features"]], dtype=np.float32)

    if clf is not None:
        pred_class = int(clf.predict(X)[0])
        pred_proba = clf.predict_proba(X)[0]
        risk_level = RISK_LEVEL_MAP.get(pred_class, "Low")
        
        # Predicted probability of missing an upcoming dose
        # Low risk (~0.05-0.18), Moderate (~0.25-0.45), High (~0.50-0.90)
        # We blend the predicted probability distribution
        p_low = pred_proba[0] if len(pred_proba) > 0 else 0.0
        p_mod = pred_proba[1] if len(pred_proba) > 1 else 0.0
        p_high = pred_proba[2] if len(pred_proba) > 2 else 0.0
        miss_prob = float(np.clip(p_low * 0.08 + p_mod * 0.32 + p_high * 0.75, 0.02, 0.95))
        confidence = float(np.max(pred_proba))
    else:
        # Rule-based fallback if model artifact is missing
        hist = stats["historical_adherence_pct"]
        if hist >= 85 and stats["recent_missed_7d"] == 0:
            risk_level = "Low"
            miss_prob = 0.09
        elif hist >= 65:
            risk_level = "Moderate"
            miss_prob = 0.30
        else:
            risk_level = "High"
            miss_prob = 0.70
        confidence = 0.70

    # Behavioral contributing factors derived from features
    factors = []
    if stats["avg_delay_minutes"] >= 25:
        factors.append(f"Frequent dose delay averaging {stats['avg_delay_minutes']} minutes past scheduled reminder")
    if stats["recent_missed_7d"] >= 2:
        factors.append(f"{stats['recent_missed_7d']} doses missed within the past 7 days")
    elif stats["recent_missed_7d"] == 1:
        factors.append("1 dose missed within the past 7 days")
    if stats["weekend_adherence_pct"] < (stats["morning_adherence_pct"] - 15) and stats["weekend_adherence_pct"] < 80:
        factors.append(f"Weekend adherence drops ({stats['weekend_adherence_pct']}%) compared to weekdays ({stats['morning_adherence_pct']}%)")
    if stats["evening_adherence_pct"] < (stats["morning_adherence_pct"] - 15) and stats["evening_adherence_pct"] < 80:
        factors.append(f"Evening dose compliance ({stats['evening_adherence_pct']}%) is noticeably lower than morning ({stats['morning_adherence_pct']}%)")
    if stats["avg_latency_seconds"] > 180:
        factors.append(f"Slow reminder interaction latency ({int(stats['avg_latency_seconds'])} seconds)")

    if not factors:
        if risk_level == "Low":
            factors.append("Consistent on-time dose intake across all scheduled intervals")
            factors.append("Fast reminder acknowledgment within normal range")
        else:
            factors.append("Moderate variation in daily medication timing")

    return {
        "status": "ready",
        "historical_adherence_pct": stats["historical_adherence_pct"],
        "predicted_miss_probability": round(miss_prob, 3),
        "risk_level": risk_level,
        "confidence_score": round(confidence, 3),
        "total_logged_doses": total_logs,
        "missed_doses_total": stats["missed_logs"],
        "delayed_doses_total": stats["delayed_logs"],
        "contributing_factors": factors,
        "feature_summary": stats["feature_dict"]
    }

