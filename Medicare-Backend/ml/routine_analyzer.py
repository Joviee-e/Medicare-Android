"""
Medication Routine Analyzer (K-Means Clustering)
Identifies behavioral patterns and archetypes in the user's medication-taking schedule:
- Consistent Routine
- Occasionally Delayed
- Irregular Routine / Weekend Drop-off
- Frequently Missed Doses
"""

import os
import json
import logging
import numpy as np
import joblib
from ml.adherence_predictor import extract_patient_behavioral_features

logger = logging.getLogger(__name__)

_kmeans_model = None
_scaler_model = None
_cluster_metadata = None


def get_routine_models():
    """Loads and caches the KMeans clustering model and StandardScaler."""
    global _kmeans_model, _scaler_model, _cluster_metadata
    if _kmeans_model is not None and _scaler_model is not None:
        return _kmeans_model, _scaler_model, _cluster_metadata

    base_dir = os.path.dirname(os.path.abspath(__file__))
    kmeans_path = os.path.join(base_dir, "routine_kmeans.joblib")
    scaler_path = os.path.join(base_dir, "routine_scaler.joblib")
    metrics_path = os.path.join(base_dir, "ml_metrics.json")

    try:
        if os.path.exists(kmeans_path) and os.path.exists(scaler_path):
            _kmeans_model = joblib.load(kmeans_path)
            _scaler_model = joblib.load(scaler_path)
            logger.info("Loaded routine KMeans and Scaler models.")
    except Exception as e:
        logger.error(f"Failed to load routine KMeans models: {e}", exc_info=True)

    try:
        if os.path.exists(metrics_path):
            with open(metrics_path, "r") as f:
                data = json.load(f)
                _cluster_metadata = data.get("routine_clustering", {})
    except Exception as e:
        logger.warning(f"Could not load routine cluster metadata: {e}")

    return _kmeans_model, _scaler_model, _cluster_metadata


def determine_archetype_fallback(stats: dict) -> tuple:
    """Fallback archetype derivation if model files are unavailable."""
    hist = stats["historical_adherence_pct"]
    delay = stats["avg_delay_minutes"]
    morn = stats["morning_adherence_pct"]
    eve = stats["evening_adherence_pct"]
    wknd = stats["weekend_adherence_pct"]

    if hist >= 90 and delay < 15:
        return (
            "Consistent Routine",
            "High adherence across all dose times with prompt reminder responses and minimal delay."
        )
    elif morn >= 80 and (wknd < 70 or eve < 70):
        return (
            "Irregular Routine / Weekend Drop-off",
            "Consistent on weekday mornings, but adherence drops significantly during evenings and weekends."
        )
    elif delay >= 25 and hist >= 65:
        return (
            "Occasionally Delayed",
            "Doses are generally taken but frequently delayed or snoozed past the scheduled time."
        )
    else:
        return (
            "Frequently Missed Doses",
            "Noticeable adherence challenges across morning, afternoon, and evening schedules with higher miss rates."
        )


def analyze_routine(medicines: list) -> dict:
    """
    Performs routine clustering on patient medication logs.
    Includes cold-start protection (< 5 doses).
    """
    stats = extract_patient_behavioral_features(medicines)
    total_logs = stats["total_logs"]

    # Cold-start safeguard
    if total_logs < 5:
        return {
            "status": "insufficient_data",
            "message": "At least 5 logged doses are required to analyze routine patterns. Continue taking and logging your medicines to build a baseline profile.",
            "minimum_required_doses": 5,
            "current_logged_doses": total_logs,
            "archetype": "Establishing Baseline",
            "description": "Currently gathering dose timing observations. An archetype will be assigned once 5 doses are recorded.",
            "cluster_id": -1,
            "time_of_day_breakdown": {
                "morning": stats["morning_adherence_pct"],
                "afternoon": stats["afternoon_adherence_pct"],
                "evening": stats["evening_adherence_pct"],
                "weekend": stats["weekend_adherence_pct"]
            },
            "recommendations": [
                "Keep your medicine within sight near your morning or evening routine",
                "Log your doses as close as possible to the scheduled time"
            ]
        }

    # Extract the 7 routine features
    # morning, afternoon, evening, weekend, avg_delay, missed_doses, resp_latency
    routine_features = [
        stats["morning_adherence_pct"],
        stats["afternoon_adherence_pct"],
        stats["evening_adherence_pct"],
        stats["weekend_adherence_pct"],
        stats["avg_delay_minutes"],
        stats["missed_logs"],
        stats["avg_latency_seconds"]
    ]

    kmeans, scaler, meta = get_routine_models()

    if kmeans is not None and scaler is not None:
        X_raw = np.array([routine_features], dtype=np.float32)
        X_scaled = scaler.transform(X_raw)
        cluster_id = int(kmeans.predict(X_scaled)[0])

        # Lookup cluster label from saved metadata
        archetype = None
        description = None
        if meta and "cluster_profiles" in meta:
            for cp in meta["cluster_profiles"]:
                if cp.get("cluster_id") == cluster_id:
                    archetype = cp.get("label")
                    description = cp.get("description")
                    break

        if not archetype:
            archetype, description = determine_archetype_fallback(stats)
    else:
        cluster_id = 0
        archetype, description = determine_archetype_fallback(stats)

    # Contextual behavioral recommendations
    recommendations = []
    if "Consistent" in archetype:
        recommendations.append("Your routine is exceptionally stable! Continue maintaining your current reminder windows.")
        recommendations.append("Ensure your refill prescriptions are requested well before your supply runs low.")
    elif "Delayed" in archetype:
        recommendations.append("You often take your doses 20–45 minutes after notification. Consider shifting reminder times slightly later to align with your free periods.")
        recommendations.append("Avoid repeated snoozing by setting reminders when you are physically near your medication.")
    elif "Weekend" in archetype or "Irregular" in archetype:
        recommendations.append("Noticeable drop in evening or weekend adherence. Try pairing weekend doses with a routine habit like weekend breakfast.")
        recommendations.append("Enable high-priority alarm sound for weekend reminders so they are not dismissed while on the go.")
    else: # Frequently Missed
        recommendations.append("Consider using a daily pill organizer with morning and evening compartments.")
        recommendations.append("Discuss with your healthcare provider whether any medications can be consolidated to simplify your schedule.")
        recommendations.append("Involve a family caregiver or reminder buddy to provide supportive check-ins.")

    return {
        "status": "ready",
        "archetype": archetype,
        "description": description,
        "cluster_id": cluster_id,
        "time_of_day_breakdown": {
            "morning": stats["morning_adherence_pct"],
            "afternoon": stats["afternoon_adherence_pct"],
            "evening": stats["evening_adherence_pct"],
            "weekend": stats["weekend_adherence_pct"]
        },
        "observed_metrics": {
            "avg_delay_minutes": stats["avg_delay_minutes"],
            "total_missed_doses": stats["missed_logs"],
            "avg_response_latency_sec": stats["avg_latency_seconds"]
        },
        "recommendations": recommendations
    }

