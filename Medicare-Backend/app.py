import logging
from flask import Flask, jsonify
from flask_cors import CORS
from flask_jwt_extended import JWTManager
from config import Config
from database.mongo import init_db

# Configure logger
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def create_app():
    app = Flask(__name__)
    
    # Load Configurations
    app.config['JWT_SECRET_KEY'] = Config.JWT_SECRET_KEY
    
    # Enable CORS
    CORS(app, resources={r"/api/*": {"origins": "*"}})
    
    # Initialize JWT Extension
    jwt = JWTManager(app)
    
    # Register JWT token invalidation/claims error handlers
    @jwt.invalid_token_loader
    def invalid_token_callback(error):
        return jsonify({"success": False, "message": "Invalid JWT token", "error": error}), 401

    @jwt.expired_token_loader
    def expired_token_callback(jwt_header, jwt_payload):
        return jsonify({"success": False, "message": "Token has expired"}), 401

    @jwt.unauthorized_loader
    def missing_token_callback(error):
        return jsonify({"success": False, "message": "Authorization header is missing", "error": error}), 401

    # Initialize MongoDB Connection
    init_db()

    # Import Blueprints
    from routes.auth import auth_bp
    from routes.patient import patient_bp
    from routes.doctor import doctor_bp
    from routes.medicine import medicine_bp
    from routes.appointment import appointment_bp

    # Register Blueprints
    app.register_blueprint(auth_bp, url_prefix='/api/auth')
    app.register_blueprint(patient_bp, url_prefix='/api/patients')
    app.register_blueprint(doctor_bp, url_prefix='/api/doctors')
    app.register_blueprint(medicine_bp, url_prefix='/api/medicines')
    app.register_blueprint(appointment_bp, url_prefix='/api/appointments')

    # Health Check API
    @app.route('/api/health', methods=['GET'])
    def health_check():
        from database.mongo import check_connection
        db_connected = check_connection()
        return jsonify({
            "success": True,
            "status": "healthy",
            "database_connected": db_connected,
            "environment": Config.FLASK_ENV
        }), 200

    # Global Error Handlers
    @app.errorhandler(404)
    def resource_not_found(e):
        return jsonify({"success": False, "message": "Resource not found"}), 404

    @app.errorhandler(500)
    def internal_server_error(e):
        logger.error(f"Internal server error: {str(e)}")
        return jsonify({"success": False, "message": "Internal server error"}), 500

    return app

if __name__ == '__main__':
    app = create_app()
    logger.info(f"Starting MediCare+ Flask Server on port {Config.PORT} in {Config.FLASK_ENV} mode")
    app.run(host='0.0.0.0', port=Config.PORT, debug=Config.DEBUG)
