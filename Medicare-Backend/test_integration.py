import unittest
from bson import ObjectId
from app import create_app
from database.mongo import get_db, init_db, check_connection
from config import Config

class LiveBackendIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Initialize configuration and database
        Config.init_config()
        init_db()
        if not check_connection():
            raise unittest.SkipTest("MongoDB cloud instance is not reachable. Skipping live integration tests.")
        
        # Clean up database from previous runs
        cls.db = get_db()
        cls.db.users.delete_many({"email": {"$in": ["test_patient@example.com", "test_doctor@example.com"]}})

        # Create flask client
        cls.app = create_app()
        cls.app.config['TESTING'] = True
        cls.client = cls.app.test_client()

    @classmethod
    def tearDownClass(cls):
        # Clean up database records created during test
        if hasattr(cls, 'db'):
            cls.db.users.delete_many({"email": {"$in": ["test_patient@example.com", "test_doctor@example.com"]}})
            if hasattr(cls, 'patient_id'):
                cls.db.patients.delete_one({"_id": ObjectId(cls.patient_id)})
                cls.db.medicines.delete_many({"patient_id": ObjectId(cls.patient_id)})

    def test_01_register_patient(self):
        response = self.client.post('/api/auth/register', json={
            "email": "test_patient@example.com",
            "password": "password123",
            "name": "Live Patient User",
            "role": "patient"
        })
        self.assertEqual(response.status_code, 201)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertIn('user_id', data)
        LiveBackendIntegrationTest.patient_id = data['user_id']

    def test_02_register_duplicate_email(self):
        response = self.client.post('/api/auth/register', json={
            "email": "test_patient@example.com",
            "password": "password123",
            "name": "Live Patient User",
            "role": "patient"
        })
        self.assertEqual(response.status_code, 409)
        data = response.get_json()
        self.assertFalse(data['success'])

    def test_03_login_patient(self):
        response = self.client.post('/api/auth/login', json={
            "email": "test_patient@example.com",
            "password": "password123"
        })
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertIn('access_token', data)
        self.assertIn('refresh_token', data)
        self.assertEqual(data['role'], 'patient')
        self.assertEqual(data['name'], 'Live Patient User')
        LiveBackendIntegrationTest.access_token = data['access_token']

    def test_04_get_profile(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}
        response = self.client.get('/api/patients/profile', headers=headers)
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertIn('profile', data)
        self.assertEqual(data['profile']['name'], 'Live Patient User')

    def test_05_update_profile_and_accessibility(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}
        response = self.client.put('/api/patients/profile', headers=headers, json={
            "name": "Updated Patient User",
            "blood_group": "AB+",
            "emergency_contact_name": "Family Member",
            "emergency_contact_phone": "9876543210",
            "accessibility_settings": {
                "contrast_mode": True,
                "voice_input": False,
                "haptic_feedback": True,
                "font_size": 3
            }
        })
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])

        # Verify details were updated
        response_profile = self.client.get('/api/patients/profile', headers=headers)
        profile_data = response_profile.get_json()['profile']
        self.assertEqual(profile_data['name'], 'Updated Patient User')
        self.assertEqual(profile_data['blood_group'], 'AB+')
        self.assertEqual(profile_data['accessibility_settings']['contrast_mode'], True)
        self.assertEqual(profile_data['accessibility_settings']['font_size'], 3)

    def test_06_create_medicine(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}
        response = self.client.post('/api/medicines', headers=headers, json={
            "name": "Amoxicillin",
            "type": "capsule",
            "dosage": "500 mg",
            "frequency": "daily",
            "start_date": "2026-08-18",
            "end_date": "2026-08-25",
            "reminder_times": ["08:00 AM", "08:00 PM"]
        })
        self.assertEqual(response.status_code, 201)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertIn('medicine_id', data)
        LiveBackendIntegrationTest.medicine_id = data['medicine_id']

    def test_07_get_medicines(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}
        response = self.client.get('/api/medicines', headers=headers)
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertEqual(len(data['medicines']), 1)
        self.assertEqual(data['medicines'][0]['name'], 'Amoxicillin')

    def test_08_log_compliance(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}
        response = self.client.post(f'/api/medicines/{self.medicine_id}/log', headers=headers, json={
            "date": "2026-08-18",
            "time": "08:00",
            "status": "taken"
        })
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])

        # Verify medicine now contains log
        response_meds = self.client.get('/api/medicines', headers=headers)
        medicines = response_meds.get_json()['medicines']
        logs = medicines[0]['logs']
        self.assertEqual(len(logs), 1)
        self.assertEqual(logs[0]['status'], 'taken')

    def test_09_delete_medicine(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}
        response = self.client.delete(f'/api/medicines/{self.medicine_id}', headers=headers)
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])

        # Verify medicine list is empty again
        response_meds = self.client.get('/api/medicines', headers=headers)
        self.assertEqual(len(response_meds.get_json()['medicines']), 0)

if __name__ == '__main__':
    unittest.main()
