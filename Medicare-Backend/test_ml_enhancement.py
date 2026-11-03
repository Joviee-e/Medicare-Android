"""
Unit and Integration Tests for Predictive Intelligence & ML Enhancements
Tests:
1. Feature extraction from logs with time buckets, delays, and latency
2. Model 1: Adherence Risk Classifier (Low / Moderate / High & Predicted Miss Probability)
3. Model 2: Routine K-Means Clustering & Archetype Mapping
4. Model 3: Reminder Delay Optimization & Safety Bounds
5. Cold-Start Handling (< 5 doses) across all 3 models
6. Reminder Suggestion Acceptance API (Safety confirmation required, preserves dosage/frequency)
7. Demo User Seeder execution and data consistency
"""

import unittest
from datetime import datetime, timezone, timedelta
from bson import ObjectId
from app import create_app
from database.mongo import get_db, get_medicines_collection, get_users_collection, get_patients_collection
from models.user import UserModel
from models.patient import PatientModel
from models.medicine import MedicineModel
from ml.adherence_predictor import extract_patient_behavioral_features, predict_adherence
from ml.routine_analyzer import analyze_routine
from ml.reminder_optimizer import optimize_reminders
from ml.insights_service import get_patient_predictive_insights, apply_reminder_suggestion
from seed_demo_user import seed_demo_data


class MLEnhancementTestCase(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app = create_app()
        cls.app.config['TESTING'] = True
        cls.client = cls.app.test_client()

    def setUp(self):
        self.patient_id = str(ObjectId())
        self.med_id = MedicineModel.create_medicine(
            patient_id=self.patient_id,
            name="Test Medicine",
            med_type="tablet",
            dosage="500 mg",
            frequency="daily",
            start_date="2026-01-01",
            end_date="2026-12-31",
            reminder_times=["08:00 AM", "01:00 PM"]
        )

    def test_cold_start_handling(self):
        """When fewer than 5 doses are logged, models return safe insufficient_data payload."""
        # Only add 2 logs
        MedicineModel.add_log(self.med_id, self.patient_id, "2026-11-01", "08:00 AM", "taken")
        MedicineModel.add_log(self.med_id, self.patient_id, "2026-11-02", "08:00 AM", "taken")

        meds = MedicineModel.get_by_patient(self.patient_id)
        
        adh = predict_adherence(meds)
        self.assertEqual(adh["status"], "insufficient_data")
        self.assertEqual(adh["risk_level"], "Low")
        self.assertEqual(adh["minimum_required_doses"], 5)
        self.assertEqual(adh["current_logged_doses"], 2)

        routine = analyze_routine(meds)
        self.assertEqual(routine["status"], "insufficient_data")
        self.assertEqual(routine["archetype"], "Establishing Baseline")

        rem = optimize_reminders(meds)
        self.assertEqual(rem["status"], "insufficient_data")
        self.assertEqual(rem["suggestions_count"], 0)

    def test_adherence_prediction_with_sufficient_data(self):
        """With >= 5 logs, adherence classifier computes risk and distinct future miss probability."""
        # Add 10 logs with mixed on-time and delay
        for i in range(10):
            d_str = (datetime.now(timezone.utc) - timedelta(days=i)).strftime("%Y-%m-%d")
            status = "taken" if i < 8 else "missed"
            delay = 5 if status == "taken" else 0
            MedicineModel.add_log(
                self.med_id, self.patient_id, d_str, "08:00 AM", status,
                scheduled_time="08:00 AM", delay_minutes=delay, response_time_seconds=20, time_bucket="morning"
            )

        meds = MedicineModel.get_by_patient(self.patient_id)
        adh = predict_adherence(meds)

        self.assertEqual(adh["status"], "ready")
        self.assertIn(adh["risk_level"], ["Low", "Moderate", "High"])
        self.assertGreaterEqual(adh["historical_adherence_pct"], 75.0)
        self.assertGreaterEqual(adh["predicted_miss_probability"], 0.0)
        self.assertLessEqual(adh["predicted_miss_probability"], 1.0)
        # Historical adherence and predicted miss prob must be distinct metrics
        self.assertNotEqual(adh["historical_adherence_pct"], adh["predicted_miss_probability"])
        self.assertIsInstance(adh["contributing_factors"], list)

    def test_routine_analyzer_archetype(self):
        """K-Means clustering assigns one of the 4 clinical archetypes."""
        for i in range(15):
            d_str = (datetime.now(timezone.utc) - timedelta(days=i)).strftime("%Y-%m-%d")
            MedicineModel.add_log(
                self.med_id, self.patient_id, d_str, "08:00 AM", "taken",
                scheduled_time="08:00 AM", delay_minutes=5, response_time_seconds=20, time_bucket="morning"
            )

        meds = MedicineModel.get_by_patient(self.patient_id)
        routine = analyze_routine(meds)

        self.assertEqual(routine["status"], "ready")
        self.assertIn(routine["archetype"], [
            "Consistent Routine",
            "Occasionally Delayed",
            "Irregular Routine / Weekend Drop-off",
            "Frequently Missed Doses"
        ])
        self.assertIn("morning", routine["time_of_day_breakdown"])
        self.assertIsInstance(routine["recommendations"], list)

    def test_smart_reminder_optimization_and_safety(self):
        """Reminder optimizer detects delayed dose and suggests timing shift without modifying dosage/frequency."""
        # Simulate afternoon doses at 01:00 PM consistently delayed by 40 minutes
        for i in range(12):
            d_str = (datetime.now(timezone.utc) - timedelta(days=i)).strftime("%Y-%m-%d")
            MedicineModel.add_log(
                self.med_id, self.patient_id, d_str, "01:40 PM", "taken",
                scheduled_time="01:00 PM", delay_minutes=40, response_time_seconds=150, time_bucket="afternoon"
            )

        meds = MedicineModel.get_by_patient(self.patient_id)
        rem = optimize_reminders(meds)

        self.assertEqual(rem["status"], "ready")
        self.assertGreater(rem["suggestions_count"], 0)
        first_sug = rem["suggestions"][0]
        self.assertEqual(first_sug["medicine_name"], "Test Medicine")
        self.assertEqual(first_sug["current_reminder_time"], "01:00 PM")
        self.assertTrue(first_sug["suggested_reminder_time"] != "01:00 PM")
        self.assertTrue(first_sug["safety_confirmation_required"])

    def test_apply_reminder_suggestion_safety_checks(self):
        """Applying reminder adjustment requires confirmation and modifies only reminder times."""
        # Unconfirmed request must fail
        ok, msg = apply_reminder_suggestion(self.patient_id, self.med_id, "01:00 PM", "01:30 PM", confirmed=False)
        self.assertFalse(ok)
        self.assertIn("confirmation", msg.lower())

        # Non-matching current time must fail
        ok, msg = apply_reminder_suggestion(self.patient_id, self.med_id, "04:00 PM", "04:30 PM", confirmed=True)
        self.assertFalse(ok)

        # Valid confirmed request succeeds
        ok, msg = apply_reminder_suggestion(self.patient_id, self.med_id, "01:00 PM", "01:30 PM", confirmed=True)
        self.assertTrue(ok)

        # Verify medicine record in DB
        med = MedicineModel.get_by_id(self.med_id)
        self.assertIn("01:30 PM", med["reminder_times"])
        self.assertNotIn("01:00 PM", med["reminder_times"])
        # Prescriptions must be completely intact!
        self.assertEqual(med["dosage"], "500 mg")
        self.assertEqual(med["frequency"], "daily")
        self.assertEqual(med["name"], "Test Medicine")

    def test_demo_user_seed_and_insights(self):
        """Verify demo seeder populates ~90 days across 5 medications and produces valid insights."""
        demo_id = seed_demo_data(days=90, reset=True)
        self.assertTrue(bool(demo_id))

        insights = get_patient_predictive_insights(demo_id)
        self.assertEqual(insights["status"], "ready")
        self.assertEqual(insights["total_active_medications"], 5)
        self.assertGreaterEqual(insights["total_logged_doses"], 400)
        self.assertIn(insights["adherence"]["risk_level"], ["Low", "Moderate", "High"])
        self.assertIn(insights["routine"]["archetype"], [
            "Consistent Routine",
            "Occasionally Delayed",
            "Irregular Routine / Weekend Drop-off",
            "Frequently Missed Doses"
        ])
        self.assertGreaterEqual(insights["reminder_optimization"]["suggestions_count"], 1)


if __name__ == "__main__":
    unittest.main()

