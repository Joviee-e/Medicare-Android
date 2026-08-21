from datetime import datetime
from bson import ObjectId
from database.mongo import get_patients_collection

class PatientModel:
    @staticmethod
    def create_profile(user_id: str, name: str, blood_group: str = None, 
                       emergency_contact_name: str = None, emergency_contact_phone: str = None) -> bool:
        """Initialize patient profile linked to users._id."""
        patients_col = get_patients_collection()
        now = datetime.utcnow()
        
        profile = {
            "_id": ObjectId(user_id),
            "name": name,
            "blood_group": blood_group or "O+", # Default placeholder
            "emergency_contact_name": emergency_contact_name or "Not Specified",
            "emergency_contact_phone": emergency_contact_phone or "Not Specified",
            "accessibility_settings": {
                "contrast_mode": False,
                "voice_input": False,
                "haptic_feedback": False,
                "font_size": 2 # Default progress index 2 (Medium)
            },
            "created_at": now,
            "updated_at": now
        }
        
        patients_col.insert_one(profile)
        return True

    @staticmethod
    def get_profile(user_id: str):
        """Retrieve patient profile by user_id."""
        patients_col = get_patients_collection()
        try:
            doc = patients_col.find_one({"_id": ObjectId(user_id)})
            if doc:
                doc['_id'] = str(doc['_id'])
            return doc
        except Exception:
            return None

    @staticmethod
    def update_profile(user_id: str, name: str, blood_group: str,
                       emergency_contact_name: str, emergency_contact_phone: str,
                       accessibility_settings: dict) -> bool:
        """Update patient details and accessibility parameters."""
        patients_col = get_patients_collection()
        now = datetime.utcnow()
        
        update_fields = {
            "name": name,
            "blood_group": blood_group,
            "emergency_contact_name": emergency_contact_name,
            "emergency_contact_phone": emergency_contact_phone,
            "updated_at": now
        }
        
        # Merge accessibility settings if passed
        if accessibility_settings:
            # Enforce defaults and safety checks
            current_settings = {}
            if "contrast_mode" in accessibility_settings:
                current_settings["contrast_mode"] = bool(accessibility_settings["contrast_mode"])
            if "voice_input" in accessibility_settings:
                current_settings["voice_input"] = bool(accessibility_settings["voice_input"])
            if "haptic_feedback" in accessibility_settings:
                current_settings["haptic_feedback"] = bool(accessibility_settings["haptic_feedback"])
            if "font_size" in accessibility_settings:
                current_settings["font_size"] = int(accessibility_settings["font_size"])
                
            for k, v in current_settings.items():
                update_fields[f"accessibility_settings.{k}"] = v
                
        try:
            result = patients_col.update_one(
                {"_id": ObjectId(user_id)},
                {"$set": update_fields}
            )
            return result.matched_count > 0
        except Exception:
            return False
