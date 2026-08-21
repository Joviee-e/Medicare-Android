from datetime import datetime
from bson import ObjectId
from database.mongo import get_appointments_collection, get_patients_collection, get_doctors_collection

class AppointmentModel:
    @staticmethod
    def create_appointment(patient_id: str, doctor_id: str, date_time_str: str, notes: str = None) -> str:
        """Create a new doctor appointment."""
        appointments_col = get_appointments_collection()
        now = datetime.utcnow()
        
        # Parse ISO date time
        try:
            date_time = datetime.fromisoformat(date_time_str)
        except ValueError:
            raise ValueError("Invalid date_time format. Must be ISO-8601 (e.g. 2026-08-15T14:30:00)")
            
        doc = {
            "patient_id": ObjectId(patient_id),
            "doctor_id": ObjectId(doctor_id),
            "date_time": date_time,
            "status": "scheduled", # "scheduled", "completed", "cancelled"
            "notes": notes or "",
            "created_at": now,
            "updated_at": now
        }
        
        result = appointments_col.insert_one(doc)
        return str(result.inserted_id)

    @staticmethod
    def get_by_patient(patient_id: str):
        """Retrieve appointments for a patient, joining doctor names."""
        appointments_col = get_appointments_collection()
        doctors_col = get_doctors_collection()
        
        try:
            docs = list(appointments_col.find({"patient_id": ObjectId(patient_id)}).sort("date_time", 1))
            res = []
            for doc in docs:
                doc_id = str(doc['doctor_id'])
                doctor = doctors_col.find_one({"_id": ObjectId(doc_id)})
                doctor_name = doctor['name'] if doctor else "Unknown Doctor"
                
                res.append({
                    "id": str(doc['_id']),
                    "patient_id": str(doc['patient_id']),
                    "doctor_id": doc_id,
                    "doctor_name": doctor_name,
                    "date_time": doc['date_time'].isoformat(),
                    "status": doc['status'],
                    "notes": doc['notes']
                })
            return res
        except Exception:
            return []

    @staticmethod
    def get_by_doctor(doctor_id: str):
        """Retrieve appointments for a doctor, joining patient names."""
        appointments_col = get_appointments_collection()
        patients_col = get_patients_collection()
        
        try:
            docs = list(appointments_col.find({"doctor_id": ObjectId(doctor_id)}).sort("date_time", 1))
            res = []
            for doc in docs:
                pat_id = str(doc['patient_id'])
                patient = patients_col.find_one({"_id": ObjectId(pat_id)})
                patient_name = patient['name'] if patient else "Unknown Patient"
                
                res.append({
                    "id": str(doc['_id']),
                    "patient_id": pat_id,
                    "patient_name": patient_name,
                    "doctor_id": str(doc['doctor_id']),
                    "date_time": doc['date_time'].isoformat(),
                    "status": doc['status'],
                    "notes": doc['notes']
                })
            return res
        except Exception:
            return []

    @staticmethod
    def update_appointment(appointment_id: str, user_id: str, role: str, status: str, notes: str = None) -> bool:
        """Update an appointment status or notes. Ensures user is a party to the appointment."""
        appointments_col = get_appointments_collection()
        now = datetime.utcnow()
        
        query = {"_id": ObjectId(appointment_id)}
        if role == 'patient':
            query["patient_id"] = ObjectId(user_id)
        else:
            query["doctor_id"] = ObjectId(user_id)
            
        update_fields = {"status": status, "updated_at": now}
        if notes is not None:
            update_fields["notes"] = notes
            
        try:
            result = appointments_col.update_one(query, {"$set": update_fields})
            return result.matched_count > 0
        except Exception:
            return False
