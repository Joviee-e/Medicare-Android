"""
Idempotent Demonstration Data Seeder for Medicare Plus
Creates a dedicated demonstration user 'medicare_demo' (email: demo@medicare.com)
with ~90 days of synthetic medication compliance history across 5 realistic medications.

BEHAVIORAL PROFILE:
1. Metformin 500mg (08:00 AM): Morning On-Time (~95% adherence, avg delay 5 min)
2. Lisinopril 10mg (08:30 AM): Morning On-Time (~92% adherence, avg delay 6 min)
3. Multivitamin (01:00 PM): Afternoon Delayed (~76% adherence, avg delay 35 min, snoozed often)
4. Atorvastatin 20mg (08:00 PM): Evening Irregular (~62% adherence, frequent evening misses)
5. Calcium + Vitamin D (09:30 PM): Weekend Drop-off (~85% weekday adherence, ~48% weekend drop)

USAGE:
    python seed_demo_user.py [--reset] [--days 90]
"""

import sys
import argparse
from datetime import datetime, timezone, timedelta
from bson import ObjectId
import numpy as np

# Ensure backend root is on sys.path
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from database.mongo import get_users_collection, get_patients_collection, get_medicines_collection
from utils.password import hash_password
from models.user import UserModel
from models.patient import PatientModel
from models.medicine import MedicineModel

DEMO_EMAIL = "demo@medicare.com"
DEMO_PASSWORD = "DemoUser123!"
DEMO_NAME = "Medicare Demo Patient"


def seed_demo_data(days: int = 90, reset: bool = False):
    print("===================================================================")
    print("MEDICARE PLUS: DEMO USER & 90-DAY BEHAVIORAL HISTORY SEEDER")
    print("===================================================================")
    print(f"Target Account: {DEMO_EMAIL} | Days of History: {days} | Reset Mode: {reset}")

    users_col = get_users_collection()
    patients_col = get_patients_collection()
    medicines_col = get_medicines_collection()

    existing_user = users_col.find_one({"email": DEMO_EMAIL})

    if existing_user:
        user_id = str(existing_user["_id"])
        if reset:
            print(f"Resetting existing demo user {user_id} and clearing associated medicines...")
            medicines_col.delete_many({"patient_id": ObjectId(user_id)})
            patients_col.delete_one({"_id": ObjectId(user_id)})
            users_col.delete_one({"_id": ObjectId(user_id)})
            print("Existing records purged successfully.")
            existing_user = None
        else:
            print(f"Demo user already exists (id: {user_id}). Use --reset to re-create.")
            # Purge existing medicines to maintain clean idempotent state
            medicines_col.delete_many({"patient_id": ObjectId(user_id)})

    if not existing_user or reset:
        # Create user
        print(f"Creating new demo account for {DEMO_EMAIL}...")
        now = datetime.now(timezone.utc)
        user_doc = {
            "email": DEMO_EMAIL,
            "password_hash": hash_password(DEMO_PASSWORD),
            "role": "patient",
            "auth_provider": "local",
            "provider_id": None,
            "created_at": now - timedelta(days=days),
            "updated_at": now,
            "last_login": now
        }
        res = users_col.insert_one(user_doc)
        user_id = str(res.inserted_id)

        # Create patient profile
        PatientModel.create_profile(
            user_id=user_id,
            name=DEMO_NAME,
            blood_group="A+",
            emergency_contact_name="Sarah Miller",
            emergency_contact_phone="+1 (555) 234-5678"
        )
        print(f"Created patient profile for user {user_id}.")

    # Generate 5 medications with realistic schedules
    now_utc = datetime.now(timezone.utc)
    start_date_str = (now_utc - timedelta(days=days)).strftime("%Y-%m-%d")
    end_date_str = (now_utc + timedelta(days=180)).strftime("%Y-%m-%d")

    med_configs = [
        {
            "name": "Metformin",
            "type": "tablet",
            "dosage": "500 mg",
            "frequency": "daily",
            "reminder_times": ["08:00 AM"],
            "pattern": "morning_consistent"
        },
        {
            "name": "Lisinopril",
            "type": "tablet",
            "dosage": "10 mg",
            "frequency": "daily",
            "reminder_times": ["08:30 AM"],
            "pattern": "morning_consistent"
        },
        {
            "name": "Multivitamin Complex",
            "type": "capsule",
            "dosage": "1 capsule",
            "frequency": "daily",
            "reminder_times": ["01:00 PM"],
            "pattern": "afternoon_delayed"
        },
        {
            "name": "Atorvastatin",
            "type": "tablet",
            "dosage": "20 mg",
            "frequency": "daily",
            "reminder_times": ["08:00 PM"],
            "pattern": "evening_missed"
        },
        {
            "name": "Calcium + Vitamin D3",
            "type": "tablet",
            "dosage": "500 mg",
            "frequency": "daily",
            "reminder_times": ["09:30 PM"],
            "pattern": "weekend_dropoff"
        }
    ]

    np.random.seed(42) # Reproducible telemetry
    total_logs_seeded = 0

    for cfg in med_configs:
        med_id = MedicineModel.create_medicine(
            patient_id=user_id,
            name=cfg["name"],
            med_type=cfg["type"],
            dosage=cfg["dosage"],
            frequency=cfg["frequency"],
            start_date=start_date_str,
            end_date=end_date_str,
            reminder_times=cfg["reminder_times"]
        )

        reminder_str = cfg["reminder_times"][0]
        pattern = cfg["pattern"]
        logs = []

        for d in range(days, 0, -1):
            log_date = (now_utc - timedelta(days=d)).date()
            is_weekend = (log_date.weekday() in (5, 6))

            status = "taken"
            delay_minutes = 0
            response_time_sec = 25
            time_bucket = "morning"

            if pattern == "morning_consistent":
                time_bucket = "morning"
                # 95% on time, 5% slight delay
                if np.random.rand() < 0.04:
                    status = "missed"
                    delay_minutes = 0
                    response_time_sec = 0
                else:
                    delay_minutes = int(np.clip(np.random.normal(5, 3), 0, 18))
                    response_time_sec = int(np.clip(np.random.normal(25, 8), 10, 60))

            elif pattern == "afternoon_delayed":
                time_bucket = "afternoon"
                # Usually taken but consistently delayed past 01:00 PM (avg 35 min late)
                rand_val = np.random.rand()
                if rand_val < 0.08:
                    status = "missed"
                    delay_minutes = 0
                    response_time_sec = 0
                elif rand_val < 0.25:
                    status = "snoozed"
                    delay_minutes = int(np.clip(np.random.normal(45, 10), 25, 75))
                    response_time_sec = int(np.clip(np.random.normal(180, 40), 90, 300))
                else:
                    status = "taken"
                    delay_minutes = int(np.clip(np.random.normal(36, 8), 15, 60))
                    response_time_sec = int(np.clip(np.random.normal(140, 35), 60, 240))

            elif pattern == "evening_missed":
                time_bucket = "evening"
                # Significant misses in the evening (~38% missed)
                miss_chance = 0.55 if is_weekend else 0.32
                if np.random.rand() < miss_chance:
                    status = "missed"
                    delay_minutes = 0
                    response_time_sec = 0
                else:
                    status = "taken"
                    delay_minutes = int(np.clip(np.random.normal(42, 14), 10, 80))
                    response_time_sec = int(np.clip(np.random.normal(200, 50), 60, 400))

            elif pattern == "weekend_dropoff":
                time_bucket = "evening"
                # Weekdays high adherence (~90%), weekend drops to (~50%)
                miss_chance = 0.52 if is_weekend else 0.10
                if np.random.rand() < miss_chance:
                    status = "missed"
                    delay_minutes = 0
                    response_time_sec = 0
                else:
                    status = "taken"
                    delay_minutes = int(np.clip(np.random.normal(22, 10), 5, 55))
                    response_time_sec = int(np.clip(np.random.normal(90, 30), 30, 200))

            # Compute actual logged time
            # Scheduled hour & minute
            from ml.adherence_predictor import parse_dose_time
            sh, sm = parse_dose_time(reminder_str)
            tot_min = sh * 60 + sm + delay_minutes
            act_h = (tot_min // 60) % 24
            act_m = tot_min % 60
            act_time_str = f"{act_h:02d}:{act_m:02d}"

            log_entry = {
                "date": log_date.strftime("%Y-%m-%d"),
                "time": act_time_str,
                "status": status,
                "scheduled_time": reminder_str,
                "delay_minutes": delay_minutes,
                "response_time_seconds": response_time_sec,
                "time_bucket": time_bucket,
                "logged_at": datetime(log_date.year, log_date.month, log_date.day, act_h, act_m, tzinfo=timezone.utc)
            }
            logs.append(log_entry)

        # Update medicine with logs
        medicines_col.update_one(
            {"_id": ObjectId(med_id)},
            {"$set": {"logs": logs}}
        )
        total_logs_seeded += len(logs)
        print(f"  [+] Seeded {len(logs)} logs for {cfg['name']} ({cfg['dosage']}) - Pattern: {pattern}")

    print("-------------------------------------------------------------------")
    print(f"Summary: Seeded 5 medications and {total_logs_seeded} compliance logs for demo user.")
    print(f"User Credentials:")
    print(f"  Email:    {DEMO_EMAIL}")
    print(f"  Password: {DEMO_PASSWORD}")
    print("===================================================================")

    # Run ML prediction on the seeded demo user to verify outputs
    from ml.insights_service import get_patient_predictive_insights
    insights = get_patient_predictive_insights(user_id)
    print("\nVerified ML Insights for Demo User:")
    print(f"  Status:               {insights['status']}")
    print(f"  Historical Adherence: {insights['adherence']['historical_adherence_pct']}%")
    print(f"  Predicted Miss Prob:  {insights['adherence']['predicted_miss_probability']}")
    print(f"  Adherence Risk Level: {insights['adherence']['risk_level']}")
    print(f"  Routine Archetype:    {insights['routine']['archetype']}")
    print(f"  Most Responsive:      {insights['reminder_optimization']['most_responsive_period']}")
    print(f"  Least Responsive:     {insights['reminder_optimization']['least_responsive_period']}")
    print(f"  Suggestions Count:    {insights['reminder_optimization']['suggestions_count']}")
    for s in insights['reminder_optimization']['suggestions']:
        print(f"    -> {s['medicine_name']}: {s['current_reminder_time']} -> {s['suggested_reminder_time']} ({s['rationale']})")
    print("===================================================================")

    return user_id


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Seed Medicare demonstration user and 90-day medication logs.")
    parser.add_argument("--days", type=int, default=90, help="Number of history days to simulate (default: 90)")
    parser.add_argument("--reset", action="store_true", help="Purge existing demo user and recreate")
    args = parser.parse_args()

    seed_demo_data(days=args.days, reset=args.reset)

