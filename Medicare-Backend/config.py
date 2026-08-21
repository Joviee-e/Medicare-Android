import os

class Config:
    # Manual .env parser to avoid external dependencies
    @staticmethod
    def load_env():
        env_path = os.path.join(os.path.dirname(__file__), '.env')
        if os.path.exists(env_path):
            with open(env_path, 'r') as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith('#'):
                        continue
                    if '=' in line:
                        key, val = line.split('=', 1)
                        key = key.strip()
                        val = val.strip().strip('"').strip("'")
                        os.environ[key] = val

    @classmethod
    def init_config(cls):
        cls.load_env()

        # Database configuration
        cls.MONGO_URI = os.getenv('MONGO_URI', 'mongodb://localhost:27017/')
        cls.MONGO_DB_NAME = os.getenv('MONGO_DB_NAME', 'medicare_db')

        # JWT security configuration
        cls.JWT_SECRET_KEY = os.getenv('JWT_SECRET_KEY', 'default-secure-jwt-key-change-me')

        # Google OAuth configuration
        cls.GOOGLE_CLIENT_ID = os.getenv('GOOGLE_CLIENT_ID', '')

        # Flask runtime configuration
        cls.PORT = int(os.getenv('PORT', 5000))
        cls.FLASK_ENV = os.getenv('FLASK_ENV', 'development')
        cls.DEBUG = cls.FLASK_ENV == 'development'

Config.init_config()
