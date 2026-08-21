from datetime import datetime
from bson import ObjectId
from database.mongo import get_users_collection
from utils.password import hash_password

class UserModel:
    @staticmethod
    def create_user(email: str, password: str, role: str) -> str:
        """
        Creates a new user record in the users collection.
        Raises ValueError if email already exists.
        Returns the generated user_id string.
        """
        users_col = get_users_collection()
        
        # Check if user already exists
        if users_col.find_one({"email": email}):
            raise ValueError("Email already in use")
            
        hashed = hash_password(password)
        now = datetime.utcnow()
        
        user_doc = {
            "email": email,
            "password_hash": hashed,
            "role": role, # "patient" or "doctor"
            "created_at": now,
            "updated_at": now
        }
        
        result = users_col.insert_one(user_doc)
        return str(result.inserted_id)

    @staticmethod
    def get_by_email(email: str):
        """Find a user by email address."""
        users_col = get_users_collection()
        doc = users_col.find_one({"email": email})
        if doc:
            doc['_id'] = str(doc['_id'])
        return doc

    @staticmethod
    def get_by_id(user_id: str):
        """Find a user by their MongoDB ObjectId string."""
        users_col = get_users_collection()
        try:
            doc = users_col.find_one({"_id": ObjectId(user_id)})
            if doc:
                doc['_id'] = str(doc['_id'])
            return doc
        except Exception:
            return None
