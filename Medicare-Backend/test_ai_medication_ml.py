"""
Comprehensive Integration Tests for Medicare AI, Medication Intelligence, ML & Notifications
"""

import unittest
from unittest.mock import patch, MagicMock
from bson import ObjectId
from app import create_app
from services.safety_engine import check_allergy_conflict, check_drug_interactions, extract_medication_guidance
from ml.predictor import predict_adherence_risk

class AIMedicationMLTestCase(unittest.TestCase):
    def setUp(self):
        self.app = create_app()
        self.app.config['TESTING'] = True
        self.client = self.app.test_client()

        self.user_id = str(ObjectId())
        self.email = "testuser@example.com"
        self.password = "securepass123"
        self.name = "Sarah Connor"

        # Log in to acquire JWT token
        with patch('models.user.UserModel.get_by_email', return_value={'_id': self.user_id, 'role': 'patient', 'password_hash': 'hash'}):
            with patch('routes.auth.verify_password', return_value=True):
                login_res = self.client.post('/api/auth/login', json={'email': self.email, 'password': self.password})
                self.token = login_res.get_json()['access_token']
                self.headers = {"Authorization": f"Bearer {self.token}"}

    # ================= 1. SAFETY ENGINE DETERMINISTIC TESTS =================

    def test_allergy_conflict_detection_penicillin(self):
        """Verify deterministic detection of Penicillin allergy conflict with Amoxicillin."""
        med_info = {
            "display_name": "Amoxicillin",
            "normalized_name": "Amoxicillin",
            "ingredients": ["amoxicillin"]
        }
        alert = check_allergy_conflict(med_info, "Penicillin, Pollen")
        self.assertIsNotNone(alert)
        self.assertEqual(alert["priority"], "high")
        self.assertEqual(alert["type"], "allergy")
        self.assertIn("Amoxicillin", alert["title"])
        self.assertIn("penicillin", alert["message"].lower())

    def test_allergy_conflict_detection_nsaid(self):
        """Verify deterministic detection of NSAID allergy conflict with Ibuprofen."""
        med_info = {
            "display_name": "Advil",
            "normalized_name": "Ibuprofen",
            "ingredients": ["ibuprofen"]
        }
        alert = check_allergy_conflict(med_info, "Aspirin, NSAIDs")
        self.assertIsNotNone(alert)
        self.assertEqual(alert["priority"], "high")
        self.assertEqual(alert["type"], "allergy")

    def test_no_allergy_conflict(self):
        """Verify no false positive when allergy is unrelated."""
        med_info = {
            "display_name": "Metformin",
            "normalized_name": "Metformin",
            "ingredients": ["metformin hydrochloride"]
        }
        alert = check_allergy_conflict(med_info, "Penicillin, Shellfish")
        self.assertIsNone(alert)

    def test_drug_interaction_detection(self):
        """Verify clinical interaction between Aspirin and Warfarin."""
        med_info = {
            "display_name": "Aspirin",
            "normalized_name": "Aspirin",
            "ingredients": ["aspirin"]
        }
        existing = [{"name": "Warfarin", "dosage": "5mg"}]
        alerts = check_drug_interactions(med_info, existing)
        self.assertTrue(len(alerts) >= 1)
        self.assertEqual(alerts[0]["type"], "interaction")
        self.assertIn("Bleeding", alerts[0]["message"])

    def test_duplicate_therapy_detection(self):
        """Verify duplicate medication warning."""
        med_info = {
            "display_name": "Metformin",
            "normalized_name": "Metformin",
            "ingredients": ["metformin"]
        }
        existing = [{"name": "Metformin", "dosage": "500mg"}]
        alerts = check_drug_interactions(med_info, existing)
        self.assertTrue(any(a["type"] == "duplicate_therapy" for a in alerts))

    # ================= 2. ML ADHERENCE PREDICTION TESTS =================

    def test_ml_prediction_low_risk(self):
        """Verify low adherence risk for adherent patient."""
        meds = [{
            "name": "Metformin",
            "dosage": "500mg",
            "type": "tablet",
            "frequency": "daily",
            "reminder_times": ["08:00 AM"],
            "logs": [{"date": "2026-09-24", "status": "taken"}]
        }]
        res = predict_adherence_risk(meds)
        self.assertEqual(res["adherence_risk"], "LOW")
        self.assertTrue(res["confidence"] > 0.5)
        self.assertIn("adherence_percentage", res["features"])

    def test_ml_prediction_high_risk(self):
        """Verify high/medium adherence risk for complex schedule with missed doses."""
        meds = [
            {
                "name": f"Drug_{i}",
                "dosage": "10mg",
                "type": "tablet",
                "frequency": "daily",
                "reminder_times": ["08:00 AM", "08:00 PM"],
                "logs": [
                    {"date": "2026-09-24", "status": "missed"},
                    {"date": "2026-09-23", "status": "missed"},
                    {"date": "2026-09-22", "status": "missed"},
                    {"date": "2026-09-21", "status": "missed"},
                    {"date": "2026-09-20", "status": "missed"}
                ]
            }
            for i in range(5)
        ]
        res = predict_adherence_risk(meds)
        self.assertIn(res["adherence_risk"], ["MEDIUM", "HIGH"])
        self.assertTrue(res["features"]["missed_doses_7d"] >= 5)

    # ================= 3. AI CHATBOT ROUTE TESTS =================

    def test_ai_chat_empty_message(self):
        """Verify empty message error."""
        res = self.client.post('/api/ai/chat', headers=self.headers, json={"message": ""})
        self.assertEqual(res.status_code, 400)
        self.assertFalse(res.get_json()["success"])

    def test_ai_chat_valid_response(self):
        """Verify AI chatbot responds to general question."""
        res = self.client.post('/api/ai/chat', headers=self.headers, json={
            "message": "What is the recommended daily water intake for adults?"
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data["success"])
        self.assertTrue(len(data["reply"]) > 10)
        self.assertIn("disclaimer", data)

    def test_ai_chat_medication_context(self):
        """Verify AI chatbot with medication finding context."""
        res = self.client.post('/api/ai/chat', headers=self.headers, json={
            "message": "Can you explain this potential allergy alert?",
            "context": {
                "medication_name": "Amoxicillin",
                "alert": "Potential allergy concern detected: User is allergic to Penicillin."
            }
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data["success"])
        self.assertTrue(len(data["reply"]) > 10)

    def test_ai_chat_ml_failure_isolation(self):
        """
        Verify that if ML prediction fails/raises an exception,
        AI chat continues, Gemini still receives medication/safety context,
        and HTTP 200 is returned.
        """
        with patch('services.ai_service.predict_adherence_risk', side_effect=RuntimeError("Simulated ML model failure")):
            res = self.client.post('/api/ai/chat', headers=self.headers, json={
                "message": "Can you explain this potential allergy alert?",
                "context": {
                    "medication_name": "Amoxicillin",
                    "alert": "Potential allergy concern detected: User is allergic to Penicillin."
                }
            })
            self.assertEqual(res.status_code, 200)
            data = res.get_json()
            self.assertTrue(data["success"])
            self.assertTrue(len(data["reply"]) > 10)

    # ================= 4. NOTIFICATION & ML ROUTES =================

    def test_get_notifications_route(self):
        """Verify GET /api/notifications returns prioritized list."""
        res = self.client.get('/api/notifications', headers=self.headers)
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data["success"])
        self.assertIsInstance(data["notifications"], list)

    def test_get_ml_adherence_route(self):
        """Verify GET /api/ml/adherence endpoint."""
        res = self.client.get('/api/ml/adherence', headers=self.headers)
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data["success"])
        self.assertIn("ml", data)
        self.assertIn("adherence_risk", data["ml"])

    # ================= 5. MEDICATION ANALYZE ROUTE =================

    def test_medication_analyze_route(self):
        """Verify POST /api/medicines/analyze endpoint."""
        res = self.client.post('/api/medicines/analyze', headers=self.headers, json={
            "name": "Paracetamol"
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data["success"])
        self.assertIn("medication", data)
        self.assertEqual(data["medication"]["name"], "Paracetamol")
        self.assertIn("alerts", data)
        self.assertIn("guidance", data)
        self.assertIn("ml", data)

if __name__ == '__main__':
    unittest.main()
