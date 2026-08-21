import unittest
from unittest.mock import patch, MagicMock
from bson import ObjectId
from app import create_app

class BackendTestCase(unittest.TestCase):
    def setUp(self):
        # Globally patch MongoClient to run tests without a live server
        self.mongo_client_patcher = patch('database.mongo.MongoClient')
        self.mock_mongo_client = self.mongo_client_patcher.start()

        # Globally patch PatientModel.get_profile to return a clean serializable dict
        self.get_profile_patcher = patch('models.patient.PatientModel.get_profile')
        self.mock_get_profile = self.get_profile_patcher.start()
        self.mock_get_profile.return_value = {
            "name": "Test User",
            "blood_group": "O+",
            "emergency_contact_name": "Emergency Son",
            "emergency_contact_phone": "12345",
            "accessibility_settings": {
                "contrast_mode": False,
                "voice_input": False,
                "haptic_feedback": False,
                "font_size": 2
            }
        }

        # Create Flask app and configure it for testing
        self.app = create_app()
        self.app.config['TESTING'] = True
        self.client = self.app.test_client()
        
        # Test variables
        self.user_id = str(ObjectId())
        self.email = "test@example.com"
        self.password = "secure123"
        self.name = "Test User"
        self.role = "patient"

    def tearDown(self):
        # Stop global patchers
        self.mongo_client_patcher.stop()
        self.get_profile_patcher.stop()

    # ================= AUTH TESTS =================

    @patch('models.user.UserModel.create_user')
    @patch('models.patient.PatientModel.create_profile')
    def test_register_success(self, mock_create_profile, mock_create_user):
        mock_create_user.return_value = self.user_id
        mock_create_profile.return_value = True
        
        response = self.client.post('/api/auth/register', json={
            "email": self.email,
            "password": self.password,
            "name": self.name,
            "role": self.role
        })
        
        self.assertEqual(response.status_code, 201)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertEqual(data['user_id'], self.user_id)

    def test_register_missing_fields(self):
        response = self.client.post('/api/auth/register', json={
            "email": self.email
        })
        self.assertEqual(response.status_code, 400)
        self.assertFalse(response.get_json()['success'])

    def test_register_invalid_email(self):
        response = self.client.post('/api/auth/register', json={
            "email": "invalidemail",
            "password": self.password,
            "name": self.name,
            "role": self.role
        })
        self.assertEqual(response.status_code, 400)

    @patch('models.user.UserModel.get_by_email')
    @patch('routes.auth.verify_password')
    def test_login_success(self, mock_verify_password, mock_get_by_email):
        # Setup mocks
        mock_get_by_email.return_value = {
            "_id": self.user_id,
            "email": self.email,
            "password_hash": "somehash",
            "role": self.role
        }
        mock_verify_password.return_value = True
        
        response = self.client.post('/api/auth/login', json={
            "email": self.email,
            "password": self.password
        })
        
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertIn('access_token', data)
        self.assertIn('refresh_token', data)

    @patch('models.user.UserModel.get_by_email')
    def test_login_invalid_email(self, mock_get_by_email):
        mock_get_by_email.return_value = None
        
        response = self.client.post('/api/auth/login', json={
            "email": self.email,
            "password": self.password
        })
        
        self.assertEqual(response.status_code, 401)
        self.assertFalse(response.get_json()['success'])

    # ================= PATIENT PROFILE TESTS =================

    @patch('models.user.UserModel.get_by_email')
    @patch('routes.auth.verify_password')
    def test_get_profile_success(self, mock_verify, mock_get_email):
        # Log in first to get token
        mock_get_email.return_value = {"_id": self.user_id, "role": self.role, "password_hash": "hash"}
        mock_verify.return_value = True
        login_res = self.client.post('/api/auth/login', json={"email": self.email, "password": self.password})
        token = login_res.get_json()['access_token']
        
        # Test route
        headers = {"Authorization": f"Bearer {token}"}
        response = self.client.get('/api/patients/profile', headers=headers)
        
        self.assertEqual(response.status_code, 200)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertEqual(data['profile']['name'], self.name)

    # ================= MEDICINE CRUD TESTS =================

    @patch('models.medicine.MedicineModel.create_medicine')
    @patch('models.user.UserModel.get_by_email')
    @patch('routes.auth.verify_password')
    def test_create_medicine_success(self, mock_verify, mock_get_email, mock_create_med):
        # Log in first
        mock_get_email.return_value = {"_id": self.user_id, "role": self.role, "password_hash": "hash"}
        mock_verify.return_value = True
        login_res = self.client.post('/api/auth/login', json={"email": self.email, "password": self.password})
        token = login_res.get_json()['access_token']
        
        # Test route
        med_id = str(ObjectId())
        mock_create_med.return_value = med_id
        
        headers = {"Authorization": f"Bearer {token}"}
        response = self.client.post('/api/medicines', headers=headers, json={
            "name": "Aspirin",
            "type": "tablet",
            "dosage": "100mg",
            "frequency": "daily",
            "start_date": "13-08-2026",
            "end_date": "20-08-2026",
            "reminder_times": ["08:00 AM"]
        })
        
        self.assertEqual(response.status_code, 201)
        data = response.get_json()
        self.assertTrue(data['success'])
        self.assertEqual(data['medicine_id'], med_id)

    @patch('models.user.UserModel.get_by_email')
    @patch('routes.auth.verify_password')
    def test_create_medicine_invalid_params(self, mock_verify, mock_get_email):
        # Log in first
        mock_get_email.return_value = {"_id": self.user_id, "role": self.role, "password_hash": "hash"}
        mock_verify.return_value = True
        login_res = self.client.post('/api/auth/login', json={"email": self.email, "password": self.password})
        token = login_res.get_json()['access_token']
        
        headers = {"Authorization": f"Bearer {token}"}
        
        # Test invalid type
        response = self.client.post('/api/medicines', headers=headers, json={
            "name": "Aspirin",
            "type": "invalid_type",
            "dosage": "100mg",
            "frequency": "daily",
            "start_date": "13-08-2026",
            "end_date": "20-08-2026",
            "reminder_times": ["08:00 AM"]
        })
        self.assertEqual(response.status_code, 400)

if __name__ == '__main__':
    unittest.main()
