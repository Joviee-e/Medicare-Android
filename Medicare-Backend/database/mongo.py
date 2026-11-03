import logging
from pymongo import MongoClient
from pymongo.errors import ServerSelectionTimeoutError, ConnectionFailure
from config import Config

logger = logging.getLogger(__name__)

# Initialize PyMongo Client
# We set serverSelectionTimeoutMS to 2000ms to fail fast if no instance is active.
client = None
db = None
_fallback_db = None

class _InMemoryCollection:
    def __init__(self, name=""):
        self.name = name
        self._docs = {}

    def create_index(self, *args, **kwargs):
        pass

    def _matches(self, doc, query):
        if not query:
            return True
        for k, v in query.items():
            if k == "_id":
                if str(doc.get("_id")) != str(v):
                    return False
            elif k == "patient_id":
                if str(doc.get("patient_id")) != str(v):
                    return False
            elif doc.get(k) != v and str(doc.get(k)) != str(v):
                return False
        return True

    def find_one(self, query=None, *args, **kwargs):
        import copy
        query = query or {}
        for d in self._docs.values():
            if self._matches(d, query):
                return copy.deepcopy(d)
        return None

    def find(self, query=None, *args, **kwargs):
        import copy
        query = query or {}
        matches = [copy.deepcopy(d) for d in self._docs.values() if self._matches(d, query)]
        class _Cursor(list):
            def sort(self, *args, **kwargs):
                return self
            def limit(self, *args, **kwargs):
                return self
        return _Cursor(matches)

    def insert_one(self, doc, *args, **kwargs):
        import copy
        from bson import ObjectId
        d = copy.deepcopy(doc)
        if "_id" not in d:
            d["_id"] = ObjectId()
        doc_id = d["_id"]
        self._docs[str(doc_id)] = d
        class _InsertResult:
            inserted_id = doc_id
        return _InsertResult()

    def update_one(self, query, update, *args, **kwargs):
        import copy
        target_id = None
        for doc_id, d in self._docs.items():
            if self._matches(d, query):
                target_id = doc_id
                break
        if not target_id:
            class _UpdateResult:
                matched_count = 0
                modified_count = 0
            return _UpdateResult()

        target = self._docs[target_id]
        if "$set" in update:
            for k, v in update["$set"].items():
                target[k] = copy.deepcopy(v)
        if "$push" in update:
            for k, v in update["$push"].items():
                if k not in target or not isinstance(target[k], list):
                    target[k] = []
                target[k].append(copy.deepcopy(v))

        class _UpdateResult:
            matched_count = 1
            modified_count = 1
        return _UpdateResult()

    def update_many(self, query, update, *args, **kwargs):
        for doc_id, d in list(self._docs.items()):
            if self._matches(d, query):
                self.update_one({"_id": d.get("_id")}, update)

    def delete_one(self, query, *args, **kwargs):
        for doc_id, d in list(self._docs.items()):
            if self._matches(d, query):
                del self._docs[doc_id]
                class _DelResult:
                    deleted_count = 1
                return _DelResult()
        class _DelResult:
            deleted_count = 0
        return _DelResult()

    def delete_many(self, query, *args, **kwargs):
        count = 0
        for doc_id, d in list(self._docs.items()):
            if self._matches(d, query):
                del self._docs[doc_id]
                count += 1
        class _DelResult:
            deleted_count = count
        return _DelResult()

    def count_documents(self, query=None, *args, **kwargs):
        query = query or {}
        return sum(1 for d in self._docs.values() if self._matches(d, query))

class _InMemoryDatabase:
    def __init__(self):
        self._collections = {}
    def __getattr__(self, name):
        if name not in self._collections:
            self._collections[name] = _InMemoryCollection(name)
        return self._collections[name]
    def __getitem__(self, name):
        return getattr(self, name)

def init_db():
    global client, db
    try:
        client = MongoClient(Config.MONGO_URI, serverSelectionTimeoutMS=2000)
        db = client[Config.MONGO_DB_NAME]
        
        # Test connection immediately
        client.admin.command('ping')
        logger.info(f"Successfully connected to MongoDB at {Config.MONGO_URI}")
        
        # Create indexes
        create_indexes()
        
    except Exception as e:
        logger.warning(f"MongoDB connection pending/failed: {str(e)}")
        # Client lazy evaluation if server is temporarily unreachable
        try:
            client = MongoClient(Config.MONGO_URI, serverSelectionTimeoutMS=2000)
            db = client[Config.MONGO_DB_NAME]
        except Exception:
            client = None
            db = None

def get_db():
    global db, _fallback_db
    if db is not None:
        return db
    init_db()
    if db is not None:
        return db
    if _fallback_db is None:
        _fallback_db = _InMemoryDatabase()
    return _fallback_db

def check_connection():
    try:
        if client:
            client.admin.command('ping')
            return True
    except Exception:
        pass
    return False

def close_db():
    global client, db
    try:
        if client:
            client.close()
    except Exception:
        pass
    client = None
    db = None

def create_indexes():
    """Ensure database collection indexes are created."""
    database = get_db()
    try:
        # User collection unique email index
        database.users.create_index("email", unique=True)
        # Medicine collection indexes
        database.medicines.create_index([("patient_id", 1), ("start_date", 1)])
        # Appointment collection indexes
        database.appointments.create_index([("patient_id", 1), ("date_time", 1)])
        database.appointments.create_index([("doctor_id", 1), ("date_time", 1)])
        # Medication cache index
        database.medication_cache.create_index("input_name", unique=True)
        # Notifications collection indexes
        database.notifications.create_index([("patient_id", 1), ("created_at", -1)])
        # ML predictions index
        database.ml_predictions.create_index([("patient_id", 1), ("created_at", -1)])
        logger.info("MongoDB indexes successfully created.")
    except Exception as e:
        logger.error(f"Error creating indexes: {str(e)}")

# Collection access helpers
def get_users_collection():
    return get_db().users

def get_patients_collection():
    return get_db().patients

def get_doctors_collection():
    return get_db().doctors

def get_medicines_collection():
    return get_db().medicines

def get_appointments_collection():
    return get_db().appointments

def get_medication_cache_collection():
    return get_db().medication_cache

def get_notifications_collection():
    return get_db().notifications

def get_ml_predictions_collection():
    return get_db().ml_predictions

