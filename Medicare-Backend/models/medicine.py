from datetime import datetime
from bson import ObjectId
from database.mongo import get_medicines_collection

class MedicineModel:
    @staticmethod
    def create_medicine(patient_id: str, name: str, med_type: str, dosage: str, 
                        frequency: str, start_date: str, end_date: str, reminder_times: list) -> str:
        """Create a new medication schedule."""
        medicines_col = get_medicines_collection()
        now = datetime.utcnow()
        
        medicine_doc = {
            "patient_id": ObjectId(patient_id),
            "name": name,
            "type": med_type, # "tablet", "capsule", "syrup", "injection"
            "dosage": dosage,
            "frequency": frequency, # "daily", "weekly", "as_needed"
            "start_date": start_date, # String YYYY-MM-DD or DD-MM-YYYY
            "end_date": end_date,
            "reminder_times": reminder_times, # List of times (e.g. ["08:00 AM"])
            "logs": [], # List of compliance status logs: {"date": "YYYY-MM-DD", "time": "HH:MM", "status": "taken"}
            "created_at": now,
            "updated_at": now
        }
        
        result = medicines_col.insert_one(medicine_doc)
        return str(result.inserted_id)

    @staticmethod
    def get_by_id(medicine_id: str):
        """Retrieve medicine by its ObjectId."""
        medicines_col = get_medicines_collection()
        try:
            doc = medicines_col.find_one({"_id": ObjectId(medicine_id)})
            if doc:
                doc['_id'] = str(doc['_id'])
                doc['patient_id'] = str(doc['patient_id'])
            return doc
        except Exception:
            return None

    @staticmethod
    def get_by_patient(patient_id: str):
        """Retrieve all medicines for a patient."""
        medicines_col = get_medicines_collection()
        try:
            docs = list(medicines_col.find({"patient_id": ObjectId(patient_id)}))
            for doc in docs:
                doc['_id'] = str(doc['_id'])
                doc['patient_id'] = str(doc['patient_id'])
            return docs
        except Exception:
            return []

    @staticmethod
    def update_medicine(medicine_id: str, patient_id: str, name: str, med_type: str, 
                        dosage: str, frequency: str, start_date: str, end_date: str, 
                        reminder_times: list) -> bool:
        """Update a medicine record. Ensures correct patient ownership."""
        medicines_col = get_medicines_collection()
        now = datetime.utcnow()
        try:
            result = medicines_col.update_one(
                {"_id": ObjectId(medicine_id), "patient_id": ObjectId(patient_id)},
                {"$set": {
                    "name": name,
                    "type": med_type,
                    "dosage": dosage,
                    "frequency": frequency,
                    "start_date": start_date,
                    "end_date": end_date,
                    "reminder_times": reminder_times,
                    "updated_at": now
                }}
            )
            return result.matched_count > 0
        except Exception:
            return False

    @staticmethod
    def delete_medicine(medicine_id: str, patient_id: str) -> bool:
        """Delete a medicine record. Ensures correct patient ownership."""
        medicines_col = get_medicines_collection()
        try:
            result = medicines_col.delete_one(
                {"_id": ObjectId(medicine_id), "patient_id": ObjectId(patient_id)}
            )
            return result.deleted_count > 0
        except Exception:
            return False

    @staticmethod
    def add_log(medicine_id: str, patient_id: str, date: str, time: str, status: str) -> bool:
        """Add a dose compliance log. Ensures correct patient ownership."""
        medicines_col = get_medicines_collection()
        now = datetime.utcnow()
        log_entry = {
            "date": date,
            "time": time,
            "status": status, # "taken", "snoozed", "skipped", "missed"
            "logged_at": now
        }
        try:
            result = medicines_col.update_one(
                {"_id": ObjectId(medicine_id), "patient_id": ObjectId(patient_id)},
                {
                    "$push": {"logs": log_entry},
                    "$set": {"updated_at": now}
                }
            )
            return result.matched_count > 0
        except Exception:
            return False
