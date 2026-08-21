from datetime import datetime
from bson import ObjectId
from database.mongo import get_doctors_collection

class DoctorModel:
    @staticmethod
    def create_profile(user_id: str, name: str, specialization: str,
                       clinic_address: str = None, phone: str = None) -> bool:
        """Initialize doctor profile linked to users._id."""
        doctors_col = get_doctors_collection()
        now = datetime.utcnow()
        
        profile = {
            "_id": ObjectId(user_id),
            "name": name,
            "specialization": specialization,
            "clinic_address": clinic_address or "Not Specified",
            "phone": phone or "Not Specified",
            "schedule": [], # List of availability objects: {"day": "Monday", "start_time": "09:00", "end_time": "17:00"}
            "created_at": now,
            "updated_at": now
        }
        
        doctors_col.insert_one(profile)
        return True

    @staticmethod
    def get_profile(user_id: str):
        """Retrieve doctor profile by user_id."""
        doctors_col = get_doctors_collection()
        try:
            doc = doctors_col.find_one({"_id": ObjectId(user_id)})
            if doc:
                doc['_id'] = str(doc['_id'])
            return doc
        except Exception:
            return None

    @staticmethod
    def list_doctors(specialization: str = None):
        """List doctors with optional specialization filter."""
        doctors_col = get_doctors_collection()
        query = {}
        if specialization:
            query["specialization"] = {"$regex": specialization, "$options": "i"}
            
        docs = list(doctors_col.find(query))
        for doc in docs:
            doc['_id'] = str(doc['_id'])
        return docs
