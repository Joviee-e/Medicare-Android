from datetime import datetime
from bson import ObjectId
from database.mongo import get_patients_collection

def calculate_completeness(profile):
    if not profile:
        return 0
        
    total_fields = 9
    filled_fields = 0
    
    # 1. name
    if profile.get("name") and profile.get("name").strip() != "Not Specified":
        filled_fields += 1
    # 2. date_of_birth
    if profile.get("date_of_birth") and profile.get("date_of_birth").strip():
        filled_fields += 1
    # 3. age
    if profile.get("age") and str(profile.get("age")).strip():
        filled_fields += 1
    # 4. gender
    if profile.get("gender") and profile.get("gender").strip():
        filled_fields += 1
    # 5. phone
    if profile.get("phone") and profile.get("phone").strip():
        filled_fields += 1
    # 6. blood_group
    if profile.get("blood_group") and profile.get("blood_group").strip() != "Not Specified":
        filled_fields += 1
        
    # 7. medical info (allergies / conditions / medications)
    med_info = profile.get("medical_information", {})
    if isinstance(med_info, dict) and (med_info.get("allergies") or med_info.get("conditions") or med_info.get("medications")):
        filled_fields += 1
        
    # 8. emergency contact
    contacts = profile.get("emergency_contacts", [])
    if isinstance(contacts, list) and len(contacts) > 0:
        first_c = contacts[0]
        if first_c.get("name") and first_c.get("relationship") and first_c.get("phone") and first_c.get("name") != "Not Specified":
            filled_fields += 1
    elif profile.get("emergency_contact_name") and profile.get("emergency_contact_name").strip() != "Not Specified" and profile.get("emergency_contact_phone") and profile.get("emergency_contact_phone").strip() != "Not Specified":
        filled_fields += 1
        
    # 9. address
    if profile.get("address") and profile.get("address").strip():
        filled_fields += 1
        
    return int((filled_fields / total_fields) * 100)


class PatientModel:
    @staticmethod
    def create_profile(user_id: str, name: str, blood_group: str = None, 
                       emergency_contact_name: str = None, emergency_contact_phone: str = None) -> bool:
        """Initialize patient profile linked to users._id."""
        patients_col = get_patients_collection()
        now = datetime.utcnow()
        
        # Populate initial emergency contact list element if fields are supplied
        contacts = []
        if emergency_contact_name or emergency_contact_phone:
            contacts.append({
                "name": emergency_contact_name or "Not Specified",
                "relationship": "Family",
                "phone": emergency_contact_phone or "Not Specified"
            })
        else:
            contacts.append({
                "name": "Not Specified",
                "relationship": "Not Specified",
                "phone": "Not Specified"
            })
            
        profile = {
            "_id": ObjectId(user_id),
            "name": name,
            "blood_group": blood_group or "O+", # Default placeholder
            "emergency_contact_name": emergency_contact_name or "Not Specified",
            "emergency_contact_phone": emergency_contact_phone or "Not Specified",
            "emergency_contacts": contacts,
            "date_of_birth": "",
            "age": "",
            "gender": "",
            "phone": "",
            "address": "",
            "medical_information": {
                "allergies": "",
                "conditions": "",
                "medications": ""
            },
            "onboarding_status": "NOT_STARTED",
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
    def migrate_patient_doc(doc):
        """Migrate older patient profile records to support onboarding and contacts schema."""
        if not doc:
            return doc
        updated = False
        updates = {}
        
        if "onboarding_status" not in doc:
            # Existing active users bypass onboarding on next login
            doc["onboarding_status"] = "COMPLETED"
            updates["onboarding_status"] = "COMPLETED"
            updated = True
            
        if "emergency_contacts" not in doc:
            old_name = doc.get("emergency_contact_name", "Not Specified")
            old_phone = doc.get("emergency_contact_phone", "Not Specified")
            doc["emergency_contacts"] = [{
                "name": old_name,
                "relationship": "Family" if old_name != "Not Specified" else "Not Specified",
                "phone": old_phone
            }]
            updates["emergency_contacts"] = doc["emergency_contacts"]
            updated = True
            
        if "date_of_birth" not in doc:
            doc["date_of_birth"] = ""
            updates["date_of_birth"] = ""
            updated = True
        if "age" not in doc:
            doc["age"] = ""
            updates["age"] = ""
            updated = True
        if "gender" not in doc:
            doc["gender"] = ""
            updates["gender"] = ""
            updated = True
        if "phone" not in doc:
            doc["phone"] = ""
            updates["phone"] = ""
            updated = True
        if "address" not in doc:
            doc["address"] = ""
            updates["address"] = ""
            updated = True
        if "medical_information" not in doc:
            doc["medical_information"] = {
                "allergies": "",
                "conditions": "",
                "medications": ""
            }
            updates["medical_information"] = doc["medical_information"]
            updated = True
            
        if updated:
            patients_col = get_patients_collection()
            patients_col.update_one({"_id": doc["_id"]}, {"$set": updates})
            
        doc['_id'] = str(doc['_id'])
        doc['completion_percentage'] = calculate_completeness(doc)
        return doc

    @staticmethod
    def get_profile(user_id: str):
        """Retrieve patient profile by user_id."""
        patients_col = get_patients_collection()
        try:
            doc = patients_col.find_one({"_id": ObjectId(user_id)})
            return PatientModel.migrate_patient_doc(doc)
        except Exception:
            return None

    @staticmethod
    def update_profile(user_id: str, name: str, blood_group: str,
                       emergency_contacts: list, date_of_birth: str, age: str,
                       gender: str, phone: str, address: str, medical_information: dict,
                       onboarding_status: str, accessibility_settings: dict,
                       phone_country_code: str = "", phone_national: str = "") -> bool:
        """Update patient details and accessibility parameters."""
        patients_col = get_patients_collection()
        now = datetime.utcnow()
        
        # Format/validate emergency contact parameters
        formatted_contacts = []
        if emergency_contacts:
            for c in emergency_contacts:
                formatted_contacts.append({
                    "name": c.get("name", "").strip(),
                    "relationship": c.get("relationship", "").strip(),
                    "phone": c.get("phone", "").strip(),
                    "country_code": c.get("country_code", "").strip() if c.get("country_code") else "",
                    "phone_national": c.get("phone_national", "").strip() if c.get("phone_national") else ""
                })
        
        # Keep deprecated fields synchronized to prevent breaking older layouts
        legacy_name = "Not Specified"
        legacy_phone = "Not Specified"
        if formatted_contacts:
            legacy_name = formatted_contacts[0].get("name", "Not Specified")
            legacy_phone = formatted_contacts[0].get("phone", "Not Specified")
            
        update_fields = {
            "name": name,
            "blood_group": blood_group,
            "emergency_contact_name": legacy_name,
            "emergency_contact_phone": legacy_phone,
            "emergency_contacts": formatted_contacts,
            "date_of_birth": date_of_birth,
            "age": age,
            "gender": gender,
            "phone": phone,
            "phone_country_code": phone_country_code,
            "phone_national": phone_national,
            "address": address,
            "medical_information": {
                "allergies": medical_information.get("allergies", "").strip() if medical_information else "",
                "conditions": medical_information.get("conditions", "").strip() if medical_information else "",
                "medications": medical_information.get("medications", "").strip() if medical_information else ""
            },
            "onboarding_status": onboarding_status,
            "updated_at": now
        }
        
        # Merge accessibility settings if passed
        if accessibility_settings:
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
