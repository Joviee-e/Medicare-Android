import logging
from pymongo import MongoClient
from pymongo.errors import ServerSelectionTimeoutError, ConnectionFailure
from config import Config

logger = logging.getLogger(__name__)

# Initialize PyMongo Client
# We set serverSelectionTimeoutMS to 2000ms to fail fast if no instance is active.
client = None
db = None

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
        
    except (ServerSelectionTimeoutError, ConnectionFailure) as e:
        logger.warning(f"MongoDB connection pending/failed: {str(e)}")
        # Client lazy evaluation still works even if the server is offline at start
        client = MongoClient(Config.MONGO_URI, serverSelectionTimeoutMS=2000)
        db = client[Config.MONGO_DB_NAME]

def get_db():
    if db is None:
        init_db()
    return db

def check_connection():
    try:
        if client:
            client.admin.command('ping')
            return True
    except Exception:
        pass
    return False

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
