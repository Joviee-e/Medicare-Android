"""
Smart Reminder Timing Optimizer (Random Forest Regressor & Delay Analytics)
Analyzes medication schedules and dose delay patterns to recommend optimal, personalized
reminder notification times while strictly respecting clinical safety boundaries.

SAFETY BOUNDARIES:
- Never changes prescribed medication name, dosage, or frequency.
- Only optimizes the reminder notification trigger time.
- All timing adjustments require explicit user confirmation.
"""

import os
import logging
from datetime import datetime, timezone, timedelta
import numpy as np
import joblib
from ml.adherence_predictor import parse_dose_time, classify_time_bucket, extract_patient_behavioral_features

logger = logging.getLogger(__name__)

_reg_model = None


def get_reminder_model():
    """Loads and caches the Random Forest reminder regression model."""
    global _reg_model
    if _reg_model is not None:
        return _reg_model

    model_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reminder_optimizer.joblib")
    if not os.path.exists(model_path):
        logger.warning(f"Reminder optimizer model not found at {model_path}")
        return None

    try:
        _reg_model = joblib.load(model_path)
        logger.info(f"Loaded reminder optimizer model from {model_path}")
    except Exception as e:
        logger.error(f"Failed to load reminder optimizer model: {e}", exc_info=True)
        _reg_model = None

    return _reg_model


def format_time_12h(hour: int, minute: int) -> str:
    """Format hour (0-23) and minute (0-59) to 'hh:mm AM/PM'."""
    h12 = hour % 12
    if h12 == 0:
        h12 = 12
    ampm = "AM" if hour < 12 else "PM"
    return f"{h12:02d}:{minute:02d} {ampm}"


def optimize_reminders(medicines: list) -> dict:
    """
    Analyzes historical dose delays across scheduled reminder slots and produces
    safe, personalized adjustment recommendations.
    """
    stats = extract_patient_behavioral_features(medicines)
    total_logs = stats["total_logs"]

    # Cold start safeguard
    if total_logs < 5:
        return {
            "status": "insufficient_data",
            "message": "At least 5 logged doses are required to calculate delay patterns and optimize reminder timings.",
            "minimum_required_doses": 5,
            "current_logged_doses": total_logs,
            "most_responsive_period": "Not enough data",
            "least_responsive_period": "Not enough data",
            "suggestions_count": 0,
            "suggestions": [],
            "safety_notice": "Reminder optimizations adjust notification times only. Never change your prescribed dosage or frequency."
        }

    reg_model = get_reminder_model()

    # Aggregate delays by scheduled reminder time across medicines
    # key: (medicine_id, medicine_name, reminder_time_str) -> list of delay values
    slot_delays = {}
    period_stats = {
        "Morning": {"delays": [], "count": 0, "on_time": 0},
        "Afternoon": {"delays": [], "count": 0, "on_time": 0},
        "Evening": {"delays": [], "count": 0, "on_time": 0},
        "Night": {"delays": [], "count": 0, "on_time": 0}
    }

    for med in medicines:
        med_id = str(med.get("_id", ""))
        med_name = med.get("name", "Medication")
        reminder_times = med.get("reminder_times", [])
        logs = med.get("logs", [])

        # Index delays for each reminder time
        for r_time in reminder_times:
            slot_key = (med_id, med_name, r_time)
            slot_delays[slot_key] = []

        for l in logs:
            status = str(l.get("status", "")).lower()
            t_str = str(l.get("time", "")).strip()
            sched = str(l.get("scheduled_time", "")).strip()
            delay_val = l.get("delay_minutes")

            hour, minute = parse_dose_time(t_str)
            bucket = classify_time_bucket(hour).capitalize()

            actual_delay = 0.0
            if delay_val is not None:
                try:
                    actual_delay = float(delay_val)
                except (ValueError, TypeError):
                    actual_delay = 0.0

            if bucket in period_stats:
                period_stats[bucket]["count"] += 1
                period_stats[bucket]["delays"].append(actual_delay)
                if status in ("taken", "completed") and actual_delay <= 15:
                    period_stats[bucket]["on_time"] += 1

            # Match to slot
            matched_slot = None
            if sched:
                for (mid, mname, rtime) in slot_delays.keys():
                    if mid == med_id and rtime == sched:
                        matched_slot = (mid, mname, rtime)
                        break
            if not matched_slot:
                for (mid, mname, rtime) in slot_delays.keys():
                    if mid == med_id:
                        r_h, r_m = parse_dose_time(rtime)
                        if abs(r_h - hour) <= 2:
                            matched_slot = (mid, mname, rtime)
                            break

            if matched_slot and matched_slot in slot_delays:
                slot_delays[matched_slot].append(actual_delay)

    # Determine most and least responsive periods
    period_summaries = []
    for p_name, p_data in period_stats.items():
        if p_data["count"] > 0:
            on_time_pct = (p_data["on_time"] / p_data["count"]) * 100.0
            avg_d = np.mean(p_data["delays"]) if p_data["delays"] else 0.0
            period_summaries.append({
                "period": p_name,
                "on_time_pct": round(on_time_pct, 1),
                "avg_delay": round(float(avg_d), 1),
                "count": p_data["count"]
            })

    if period_summaries:
        # Most responsive has highest on_time_pct and lowest delay
        sorted_by_resp = sorted(period_summaries, key=lambda x: (x["on_time_pct"], -x["avg_delay"]), reverse=True)
        most_resp = f"{sorted_by_resp[0]['period']} ({sorted_by_resp[0]['on_time_pct']}% on-time, avg {sorted_by_resp[0]['avg_delay']}m delay)"
        least_resp = f"{sorted_by_resp[-1]['period']} ({sorted_by_resp[-1]['on_time_pct']}% on-time, avg {sorted_by_resp[-1]['avg_delay']}m delay)"
    else:
        most_resp = "Morning (Consistent)"
        least_resp = "Afternoon (Frequent delays)"

    # Generate smart adjustment suggestions
    suggestions = []
    hist_adh = stats["historical_adherence_pct"]

    for (med_id, med_name, r_time), delays in slot_delays.items():
        if not delays:
            continue
        avg_slot_delay = float(np.mean(delays))
        r_hour, r_minute = parse_dose_time(r_time)

        # If model is loaded, also predict regression delay
        bucket_code = 0 if r_hour < 12 else (1 if r_hour < 17 else (2 if r_hour < 22 else 3))
        predicted_delay = avg_slot_delay
        if reg_model is not None:
            try:
                # Features: [scheduled_hour, is_weekend (0), historical_user_adherence, time_bucket_code]
                x_reg = np.array([[r_hour, 0, hist_adh, bucket_code]], dtype=np.float32)
                pred = float(reg_model.predict(x_reg)[0])
                # Blend observed and predicted delay
                predicted_delay = (avg_slot_delay * 0.7) + (pred * 0.3)
            except Exception:
                pass

        # If dose is persistently delayed by >= 20 minutes on average
        if avg_slot_delay >= 20.0 or predicted_delay >= 25.0:
            # Recommend shifting 15 to 45 minutes later, rounded to nearest 5/15 minutes
            shift_minutes = int(min(45, max(15, round(predicted_delay / 15.0) * 15)))
            
            # Compute new time
            base_total_min = r_hour * 60 + r_minute
            new_total_min = (base_total_min + shift_minutes) % 1440
            new_hour = new_total_min // 60
            new_minute = new_total_min % 60
            suggested_time = format_time_12h(new_hour, new_minute)

            suggestions.append({
                "suggestion_id": f"sug_{med_id}_{r_time.replace(' ', '_').replace(':', '')}",
                "medicine_id": med_id,
                "medicine_name": med_name,
                "current_reminder_time": r_time,
                "suggested_reminder_time": suggested_time,
                "delay_minutes_observed": round(avg_slot_delay, 1),
                "predicted_delay_minutes": round(predicted_delay, 1),
                "shift_minutes": shift_minutes,
                "rationale": f"You take {med_name} an average of {int(avg_slot_delay)} minutes after the {r_time} reminder. Shifting the reminder notification to {suggested_time} matches your natural daily schedule and avoids snoozing.",
                "status": "pending_user_acceptance",
                "safety_confirmation_required": True
            })

    return {
        "status": "ready",
        "most_responsive_period": most_resp,
        "least_responsive_period": least_resp,
        "suggestions_count": len(suggestions),
        "suggestions": suggestions,
        "safety_notice": "Important: Reminder time adjustments modify notification alarms only. Never alter your prescribed dosage, medication, or frequency without consulting your physician."
    }
