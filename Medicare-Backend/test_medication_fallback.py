"""
Test Suite for Medicare Hybrid ML & Medication Information Fallback
Tests all 6 required verification scenarios:
1. Existing common medicine (Paracetamol 500 mg)
2. Existing medicine with higher dosage (Paracetamol 1000 mg)
3. Less-common medicine triggering web information fallback (e.g. Mebeverine)
4. Unknown / random medication (No crash, no fabricated information)
5. Internet unavailable / network error simulation (Graceful fallback)
6. Existing AI chatbot functionality preservation
"""

import unittest
from unittest.mock import patch, MagicMock
from bson import ObjectId
from app import create_app
from services.medication_knowledge import get_medication_info, fetch_web_medication_info
from services.safety_engine import extract_medication_guidance
from ml.predictor import predict_adherence_risk

class MedicationFallbackTestCase(unittest.TestCase):
    def setUp(self):
        self.app = create_app()
        self.app.config['TESTING'] = True
        self.client = self.app.test_client()

        self.user_id = str(ObjectId())
        self.email = "testpatient@example.com"
        self.password = "securepass123"

        # Log in to acquire JWT token
        with patch('models.user.UserModel.get_by_email', return_value={'_id': self.user_id, 'role': 'patient', 'password_hash': 'hash'}):
            with patch('routes.auth.verify_password', return_value=True):
                login_res = self.client.post('/api/auth/login', json={'email': self.email, 'password': self.password})
                self.token = login_res.get_json()['access_token']
                self.headers = {"Authorization": f"Bearer {self.token}"}

    # =========================================================================
    # TEST 1: Existing common medicine (Paracetamol 500 mg)
    # =========================================================================
    def test_01_existing_common_medicine(self):
        """
        Test 1: Existing common medicine (Paracetamol 500 mg)
        - Uses primary layer (RxNorm / openFDA)
        - Preserves existing ML adherence model
        - Produces verified medication guidance
        """
        mock_rx = {
            "normalized_name": "Acetaminophen",
            "rxcui": "161",
            "ingredients": ["Acetaminophen"],
            "status": "verified"
        }
        mock_fda = {
            "found": True,
            "generic_name": "Acetaminophen",
            "brand_name": "Tylenol",
            "drug_class": "Analgesic and antipyretic",
            "warnings": ["Liver warning: Severe liver damage may occur if you take more than 4,000 mg in 24 hours."],
            "precautions": ["Do not take more than directed."],
            "contraindications": [],
            "interactions": [],
            "adverse_reactions": [],
            "administration": ["Take with a full glass of water. Do not exceed 4,000 mg in 24 hours."],
            "patient_instructions": ["Swallow with a full glass of water."]
        }

        with patch('services.medication_knowledge.normalize_with_rxnorm', return_value=mock_rx):
            with patch('services.medication_knowledge.fetch_openfda_label', return_value=mock_fda):
                with patch('database.mongo.get_medication_cache_collection', return_value=None):
                    med_info = get_medication_info("Paracetamol", dosage="500 mg")
                    self.assertEqual(med_info["normalized_name"].lower(), "acetaminophen")
                    self.assertTrue(med_info["source"]["rxnorm"])
                    self.assertTrue(med_info["source"]["openfda"])

                    guidance = extract_medication_guidance(med_info)
                    self.assertTrue(len(guidance) > 0)

                    # Test ML adherence prediction operates normally
                    meds = [{
                        "name": "Paracetamol",
                        "dosage": "500 mg",
                        "type": "tablet",
                        "frequency": "daily",
                        "reminder_times": ["09:00 AM"],
                        "logs": [{"date": "2026-11-01", "status": "taken"}]
                    }]
                    ml_res = predict_adherence_risk(meds)
                    self.assertTrue(ml_res["available"])
                    self.assertIn(ml_res["adherence_risk"], ["LOW", "MEDIUM", "HIGH"])

                    # Test API endpoint
                    res = self.client.post('/api/medicines/analyze', headers=self.headers, json={
                        "name": "Paracetamol",
                        "dosage": "500 mg"
                    })
                    self.assertEqual(res.status_code, 200)
                    data = res.get_json()
                    self.assertTrue(data["success"])
                    self.assertIn("guidance", data)
                    self.assertIn("ml", data)
                    self.assertEqual(data["ml"]["adherence_risk"], ml_res["adherence_risk"])

    # =========================================================================
    # TEST 2: Existing medicine with higher dosage (Paracetamol 1000 mg)
    # =========================================================================
    def test_02_existing_medicine_higher_dosage(self):
        """
        Test 2: Paracetamol with higher dosage (1000 mg)
        - Dosage is not ignored
        - Cautious evaluation provided (e.g. maximum recommended single dose)
        - System behaves correctly without altering prescription or prescribing
        """
        med_info = get_medication_info("Paracetamol", dosage="1000 mg")
        med_struct = med_info.get("medication_info", {})
        dosage_eval = med_struct.get("dosage_evaluation", "")

        # Verify dosage is reflected in evaluation
        self.assertTrue(len(dosage_eval) > 0)
        self.assertTrue("1000" in dosage_eval or "standard" in dosage_eval.lower() or "4,000" in dosage_eval)

        # Verify guidance contains dosage information
        guidance = extract_medication_guidance(med_info)
        dosage_guidance = [g for g in guidance if g.get("category") == "dosage"]
        self.assertTrue(len(dosage_guidance) > 0)
        self.assertIn("1000", dosage_guidance[0]["message"])

        # API analyze route verifies dosage is evaluated
        res = self.client.post('/api/medicines/analyze', headers=self.headers, json={
            "name": "Paracetamol",
            "dosage": "1000 mg"
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertIn("medication_info", data)
        self.assertIn("dosage_evaluation", data["medication_info"])

    # =========================================================================
    # TEST 3: Less-common medicine triggering web information fallback
    # =========================================================================
    def test_03_less_common_medicine_fallback(self):
        """
        Test 3: Less-common medicine (e.g., Mebeverine / Drotaverine)
        - openFDA has no data for Mebeverine
        - Triggers Layer 2 Web fallback
        - Retrieves structured uses, supportive care, precautions, side effects
        - Preserves existing ML adherence layer
        """
        # Mock the external web lookup to return structured authoritative information
        mock_web_return = {
            "found": True,
            "generic_name": "Mebeverine",
            "brand_name": "Colofac",
            "drug_class": "Antispasmodic",
            "warnings": [],
            "precautions": ["Consult doctor if symptoms do not improve within two weeks."],
            "contraindications": [],
            "interactions": [],
            "adverse_reactions": ["Nausea", "Headache", "Dizziness", "Allergic skin reactions"],
            "administration": ["Take 20 minutes before meals with a glass of water."],
            "patient_instructions": [
                "Common uses: Relief of muscle spasms in the gastrointestinal tract, irritable bowel syndrome (IBS).",
                "Supportive care: Adequate hydration and dietary adjustments may help manage digestive symptoms."
            ],
            "medication_info": {
                "medicine": "Mebeverine",
                "uses": ["Irritable bowel syndrome (IBS)", "Gastrointestinal smooth muscle spasm relief"],
                "common_side_effects": ["Nausea", "Headache", "Dizziness", "Skin rash"],
                "precautions": ["Consult healthcare professional if symptoms persist after two weeks."],
                "supportive_recommendations": ["Stay hydrated with water", "Avoid foods known to trigger digestive irritation"],
                "dosage_evaluation": "The entered dose of 135 mg is consistent with standard adult reference ranges.",
                "source": "NHS / British National Formulary (BNF)",
                "source_url": "https://www.nhs.uk/medicines/mebeverine/",
                "source_updated": "2026-11-02T12:00:00Z"
            }
        }

        with patch('services.medication_knowledge.fetch_web_medication_info', return_value=mock_web_return):
            # Bypass cache for test
            with patch('database.mongo.get_medication_cache_collection', return_value=None):
                med_info = get_medication_info("Mebeverine", dosage="135 mg")

                self.assertTrue(med_info["source"]["web_fallback"])
                self.assertFalse(med_info["source"]["openfda"])

                guidance = extract_medication_guidance(med_info)
                categories = [g.get("category") for g in guidance]
                self.assertIn("common_use", categories)
                self.assertIn("self_care", categories)
                self.assertIn("side_effects", categories)
                self.assertIn("disclaimer", categories)

                # Verify safe phrasing (no diagnosing)
                for g in guidance:
                    self.assertNotIn("you have", g["message"].lower())
                    self.assertNotIn("diagnosed with", g["message"].lower())

    # =========================================================================
    # TEST 4: Unknown / Random medication
    # =========================================================================
    def test_04_unknown_random_medicine(self):
        """
        Test 4: Unknown or random medication (e.g. 'XyzRandomFakeDrug999')
        - Expected: No crash. No fabricated medical information.
        - Graceful fallback with informative disclaimer.
        """
        mock_unrecognized = {
            "found": False,
            "generic_name": "Xyzrandomfakedrug999",
            "brand_name": None,
            "drug_class": None,
            "warnings": [],
            "precautions": [],
            "contraindications": [],
            "interactions": [],
            "adverse_reactions": [],
            "administration": [],
            "patient_instructions": [],
            "medication_info": {
                "medicine": "Xyzrandomfakedrug999",
                "uses": [],
                "common_side_effects": [],
                "precautions": ["Medication information could not be verified at this time."],
                "supportive_recommendations": [],
                "dosage_evaluation": "Requires confirmation with a pharmacist or healthcare professional.",
                "source": "Unverified",
                "source_url": "",
                "source_updated": "2026-11-02T12:00:00Z"
            }
        }

        with patch('services.medication_knowledge.fetch_web_medication_info', return_value=mock_unrecognized):
            with patch('database.mongo.get_medication_cache_collection', return_value=None):
                med_info = get_medication_info("XyzRandomFakeDrug999", dosage="250 mg")
                self.assertFalse(med_info["source"]["openfda"])
                self.assertFalse(med_info["source"]["web_fallback"])

                # Zero fabricated uses
                self.assertEqual(len(med_info["medication_info"]["uses"]), 0)
                self.assertIn("could not be verified", med_info["medication_info"]["precautions"][0])

                # Endpoint returns HTTP 200 without crashing
                res = self.client.post('/api/medicines/analyze', headers=self.headers, json={
                    "name": "XyzRandomFakeDrug999",
                    "dosage": "250 mg"
                })
                self.assertEqual(res.status_code, 200)
                data = res.get_json()
                self.assertTrue(data["success"])

    # =========================================================================
    # TEST 5: Internet unavailable / Network error simulation
    # =========================================================================
    def test_05_internet_unavailable_simulation(self):
        """
        Test 5: Internet unavailable / API connection timeout
        - External web search / RxNorm / openFDA raise connection errors
        - Existing ML functionality should still work
        - Graceful fallback when web information is unavailable
        - No crash or 500 error
        """
        with patch('requests.get', side_effect=ConnectionError("Simulated DNS / Network failure")):
            with patch('services.medication_knowledge.fetch_web_medication_info', side_effect=RuntimeError("Web lookup unavailable")):
                # Should not crash
                med_info = get_medication_info("Colchicine", dosage="0.5 mg")
                self.assertIsNotNone(med_info)
                self.assertIn("Colchicine", med_info["normalized_name"])

                # Endpoint should not crash
                res = self.client.post('/api/medicines/analyze', headers=self.headers, json={
                    "name": "Colchicine",
                    "dosage": "0.5 mg"
                })
                self.assertEqual(res.status_code, 200)
                data = res.get_json()
                self.assertTrue(data["success"])
                # ML adherence remains available
                self.assertIn("ml", data)
                self.assertIn("adherence_risk", data["ml"])

    # =========================================================================
    # TEST 6: Existing AI Chatbot behavior preservation
    # =========================================================================
    def test_06_existing_chatbot_behavior_preserved(self):
        """
        Test 6: Existing AI Chatbot functionality remains intact
        - Chat route continues to operate correctly
        - Rejects empty queries
        - Successfully generates responses with disclaimers
        """
        # Empty message validation
        res = self.client.post('/api/ai/chat', headers=self.headers, json={"message": ""})
        self.assertEqual(res.status_code, 400)

        # Standard conversation query
        res = self.client.post('/api/ai/chat', headers=self.headers, json={
            "message": "How does staying hydrated help during medication regimens?"
        })
        self.assertEqual(res.status_code, 200)
        data = res.get_json()
        self.assertTrue(data["success"])
        self.assertTrue(len(data.get("reply", "")) > 10)
        self.assertIn("disclaimer", data)

if __name__ == '__main__':
    unittest.main()
