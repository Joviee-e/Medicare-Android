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
            "auth_provider": "local",
            "provider_id": None,
            "created_at": now,
            "updated_at": now,
            "last_login": now
        }
        
        result = users_col.insert_one(user_doc)
        return str(result.inserted_id)

    @staticmethod
    def create_social_user(email: str, auth_provider: str, provider_id: str, role: str) -> str:
        """
        Finds or creates a user authenticated through a social provider (e.g. Google).
        Returns the user_id string.
        """
        users_col = get_users_collection()
        existing = users_col.find_one({"email": email})
        
        now = datetime.utcnow()
        if existing:
            # If user exists, ensure they are linked to this provider
            users_col.update_one(
                {"_id": existing["_id"]},
                {"$set": {
                    "auth_provider": auth_provider,
                    "provider_id": provider_id,
                    "last_login": now,
                    "updated_at": now
                }}
            )
            return str(existing["_id"])
            
        user_doc = {
            "email": email,
            "password_hash": None,
            "role": role,
            "auth_provider": auth_provider,
            "provider_id": provider_id,
            "created_at": now,
            "updated_at": now,
            "last_login": now
        }
        
        result = users_col.insert_one(user_doc)
        return str(result.inserted_id)

    @staticmethod
    def migrate_user_doc(doc):
        """Migrate older user records to support the new schema dynamically."""
        if not doc:
            return doc
        updated = False
        updates = {}
        
        if "auth_provider" not in doc:
            doc["auth_provider"] = "local"
            updates["auth_provider"] = "local"
            updated = True
        if "provider_id" not in doc:
            doc["provider_id"] = None
            updates["provider_id"] = None
            updated = True
        if "last_login" not in doc:
            doc["last_login"] = doc.get("created_at", datetime.utcnow())
            updates["last_login"] = doc.get("created_at", datetime.utcnow())
            updated = True
            
        if updated:
            users_col = get_users_collection()
            users_col.update_one({"_id": doc["_id"]}, {"$set": updates})
            
        doc['_id'] = str(doc['_id'])
        return doc

    @staticmethod
    def get_by_email(email: str):
        """Find a user by email address."""
        users_col = get_users_collection()
        doc = users_col.find_one({"email": email})
        return UserModel.migrate_user_doc(doc)

    @staticmethod
    def get_by_id(user_id: str):
        """Find a user by their MongoDB ObjectId string."""
        users_col = get_users_collection()
        try:
            doc = users_col.find_one({"_id": ObjectId(user_id)})
            return UserModel.migrate_user_doc(doc)
        except Exception:
            return None

    @staticmethod
    def update_last_login(user_id: str):
        """Update last login timestamp."""
        users_col = get_users_collection()
        try:
            users_col.update_one(
                {"_id": ObjectId(user_id)},
                {"$set": {"last_login": datetime.utcnow()}}
            )
        except Exception:
            pass

    @staticmethod
    def set_reset_code(email: str, hashed_code: str, expires_at: datetime):
        """Store hashed password reset code details."""
        users_col = get_users_collection()
        users_col.update_one(
            {"email": email},
            {"$set": {
                "reset_code": hashed_code,
                "reset_code_expires": expires_at,
                "reset_attempts": 0
            }}
        )

    @staticmethod
    def increment_reset_attempts(email: str):
        """Increment incorrect reset verification attempts."""
        users_col = get_users_collection()
        users_col.update_one(
            {"email": email},
            {"$inc": {"reset_attempts": 1}}
        )

    @staticmethod
    def clear_reset_code(email: str):
        """Clear reset code details once validated or replaced."""
        users_col = get_users_collection()
        users_col.update_one(
            {"email": email},
            {"$set": {
                "reset_code": None,
                "reset_code_expires": None,
                "reset_attempts": 0
            }}
        )

    @staticmethod
    def update_password(email: str, hashed_password: str):
        """Update a user's password."""
        users_col = get_users_collection()
        users_col.update_one(
            {"email": email},
            {"$set": {
                "password_hash": hashed_password,
                "updated_at": datetime.utcnow()
            }}
        )
