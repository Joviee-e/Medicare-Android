import logging
import secrets
from datetime import datetime, timedelta
from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity, create_access_token, get_jwt
from models.user import UserModel
from models.patient import PatientModel
from models.doctor import DoctorModel
from utils.validators import is_valid_email, is_valid_password
from utils.jwt_handler import generate_tokens
from utils.password import verify_password, hash_password
from config import Config

from google.oauth2 import id_token
from google.auth.transport import requests as google_requests

logger = logging.getLogger(__name__)
auth_bp = Blueprint('auth', __name__)

@auth_bp.route('/register', methods=['POST'])
def register():
    data = request.get_json() or {}
    email = data.get('email', '').strip().lower()
    password = data.get('password', '')
    role = data.get('role', 'patient').strip().lower()
    name = data.get('name', '').strip()

    # Input validations
    if not email or not password or not name:
        return jsonify({"success": False, "message": "Missing required fields"}), 400
    
    if not is_valid_email(email):
        return jsonify({"success": False, "message": "Invalid email format"}), 400
        
    if not is_valid_password(password):
        return jsonify({"success": False, "message": "Password must be at least 6 characters"}), 400
        
    if role not in ('patient', 'doctor'):
        return jsonify({"success": False, "message": "Invalid role specified"}), 400

    try:
        # Create user credentials
        user_id = UserModel.create_user(email, password, role)
        
        # Initialize profile according to role
        if role == 'patient':
            PatientModel.create_profile(user_id, name)
            onboarding_status = "NOT_STARTED"
        else:
            specialization = data.get('specialization', 'General Medicine').strip()
            DoctorModel.create_profile(user_id, name, specialization)
            onboarding_status = "COMPLETED"
            
        # Automatically generate session tokens upon registration
        access_token, refresh_token = generate_tokens(user_id, role)
            
        return jsonify({
            "success": True, 
            "message": "User registered successfully", 
            "access_token": access_token,
            "refresh_token": refresh_token,
            "role": role,
            "user_id": user_id,
            "name": name,
            "onboarding_status": onboarding_status
        }), 201
        
    except ValueError as e:
        return jsonify({"success": False, "message": str(e)}), 409
    except Exception as e:
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500


@auth_bp.route('/login', methods=['POST'])
def login():
    data = request.get_json() or {}
    email = data.get('email', '').strip().lower()
    password = data.get('password', '')

    if not email or not password:
        return jsonify({"success": False, "message": "Missing email or password"}), 400

    user = UserModel.get_by_email(email)
    if not user:
        return jsonify({"success": False, "message": "Invalid email or password"}), 401

    if not user.get('password_hash') or not verify_password(password, user['password_hash']):
        return jsonify({"success": False, "message": "Invalid email or password"}), 401

    # Update last login
    UserModel.update_last_login(user['_id'])

    access_token, refresh_token = generate_tokens(user['_id'], user['role'])
    
    # Get user name and onboarding status for response
    name = ""
    onboarding_status = "COMPLETED"
    if user['role'] == 'patient':
        profile = PatientModel.get_profile(user['_id'])
        if profile:
            name = profile['name']
            onboarding_status = profile.get('onboarding_status', 'COMPLETED')
    else:
        profile = DoctorModel.get_profile(user['_id'])
        if profile:
            name = profile['name']

    return jsonify({
        "success": True,
        "access_token": access_token,
        "refresh_token": refresh_token,
        "role": user['role'],
        "user_id": user['_id'],
        "name": name,
        "onboarding_status": onboarding_status
    }), 200


@auth_bp.route('/google', methods=['POST'])
def google_auth():
    data = request.get_json() or {}
    token = data.get('id_token')
    role = data.get('role', 'patient').strip().lower()
    
    if not token:
        return jsonify({"success": False, "message": "Google ID token is required"}), 400
        
    client_id = Config.GOOGLE_CLIENT_ID
    if not client_id:
        logger.error("Google OAuth client ID is not configured (GOOGLE_CLIENT_ID missing in .env)")
        return jsonify({
            "success": False, 
            "message": "Google authentication is currently unconfigured on the server"
        }), 500
        
    try:
        # Verify Google ID Token strictly
        idinfo = id_token.verify_oauth2_token(token, google_requests.Request(), client_id)
        
        # Verify issuer
        if idinfo['iss'] not in ['accounts.google.com', 'https://accounts.google.com']:
            raise ValueError('Wrong issuer.')
            
        email = idinfo.get('email', '').strip().lower()
        provider_id = idinfo.get('sub') # Google account unique identifier
        name = idinfo.get('name', 'Google User').strip()
        
        if not email or not provider_id:
            return jsonify({"success": False, "message": "Invalid Google token payload content"}), 400
            
    except ValueError as e:
        logger.error(f"Google ID token verification failed: {str(e)}")
        return jsonify({"success": False, "message": f"Google authentication failed: {str(e)}"}), 401
    except Exception as e:
        logger.error(f"Error checking Google verification: {str(e)}")
        return jsonify({"success": False, "message": "Google verification service error"}), 500
        
    # Successfully verified Google account!
    try:
        user_id = UserModel.create_social_user(email, "google", provider_id, role)
        
        # Initialize profile if it doesn't exist
        profile = PatientModel.get_profile(user_id)
        if not profile:
            if role == 'patient':
                PatientModel.create_profile(user_id, name)
                onboarding_status = "NOT_STARTED"
            else:
                specialization = data.get('specialization', 'General Medicine').strip()
                DoctorModel.create_profile(user_id, name, specialization)
                onboarding_status = "COMPLETED"
        else:
            onboarding_status = profile.get('onboarding_status', 'COMPLETED')
            name = profile.get('name', name)
            
        # Track last login
        UserModel.update_last_login(user_id)
        
        access_token, refresh_token = generate_tokens(user_id, role)
        
        return jsonify({
            "success": True,
            "access_token": access_token,
            "refresh_token": refresh_token,
            "role": role,
            "user_id": user_id,
            "name": name,
            "onboarding_status": onboarding_status
        }), 200
        
    except Exception as e:
        logger.error(f"Database error registering social user: {str(e)}")
        return jsonify({"success": False, "message": "Failed to complete social login registration"}), 500


@auth_bp.route('/forgot-password', methods=['POST'])
def forgot_password():
    data = request.get_json() or {}
    email = data.get('email', '').strip().lower()

    if not email:
        return jsonify({"success": False, "message": "Email is required"}), 400

    # Locate user to generate reset code
    user = UserModel.get_by_email(email)
    
    # Generic message prevents account enumeration
    response_msg = "If an account exists for this email, a reset code has been generated."
    
    if user:
        # Create a secure, random 6-digit verification code
        reset_code = "".join([str(secrets.randbelow(10)) for _ in range(6)])
        hashed_code = hash_password(reset_code)
        expires_at = datetime.utcnow() + timedelta(minutes=10)
        
        UserModel.set_reset_code(email, hashed_code, expires_at)
        
        # DEVELOPMENT ONLY: Output verification code to standard terminal logs
        print(f"\n=======================================================")
        print(f"[DEVELOPMENT ONLY] Medicare Password Reset Verification")
        print(f"User Email: {email}")
        print(f"Verification Code: {reset_code}")
        print(f"Expires At (UTC): {expires_at}")
        print(f"=======================================================\n")
        
    return jsonify({
        "success": True,
        "message": response_msg
    }), 200


@auth_bp.route('/reset-password', methods=['POST'])
def reset_password():
    data = request.get_json() or {}
    email = data.get('email', '').strip().lower()
    code = data.get('code', '').strip()
    new_password = data.get('new_password', '')

    if not email or not code or not new_password:
        return jsonify({"success": False, "message": "Email, code, and new_password are required"}), 400

    if not is_valid_password(new_password):
        return jsonify({"success": False, "message": "New password must be at least 6 characters"}), 400

    user = UserModel.get_by_email(email)
    if not user:
        # Avoid account enumeration confirmation
        return jsonify({"success": False, "message": "Invalid email or verification code"}), 400

    reset_code = user.get("reset_code")
    reset_code_expires = user.get("reset_code_expires")
    reset_attempts = user.get("reset_attempts", 0)

    if not reset_code or not reset_code_expires:
        return jsonify({"success": False, "message": "No active password reset request found"}), 400

    # Rate limiting: Maximum 3 attempts
    if reset_attempts >= 3:
        UserModel.clear_reset_code(email)
        return jsonify({"success": False, "message": "Too many incorrect attempts. Please request a new code."}), 400

    # Expiry validation
    if datetime.utcnow() > reset_code_expires:
        UserModel.clear_reset_code(email)
        return jsonify({"success": False, "message": "Verification code has expired"}), 400

    # Log/track the attempt
    UserModel.increment_reset_attempts(email)

    # Verify code using bcrypt password checks
    if verify_password(code, reset_code):
        # Update credentials and clean code status
        hashed_password = hash_password(new_password)
        UserModel.update_password(email, hashed_password)
        UserModel.clear_reset_code(email)
        
        return jsonify({
            "success": True,
            "message": "Password has been successfully reset"
        }), 200
    else:
        return jsonify({"success": False, "message": "Invalid email or verification code"}), 400


@auth_bp.route('/logout', methods=['POST'])
@jwt_required()
def logout():
    return jsonify({"success": True, "message": "Successfully logged out"}), 200


@auth_bp.route('/refresh', methods=['POST'])
@jwt_required(refresh=True)
def refresh():
    identity = get_jwt_identity()
    user = UserModel.get_by_id(identity)
    if not user:
        return jsonify({"success": False, "message": "User not found"}), 404
        
    new_access_token = create_access_token(
        identity=identity,
        additional_claims={"role": user['role']}
    )
    return jsonify({
        "success": True,
        "access_token": new_access_token
    }), 200


@auth_bp.route('/me', methods=['GET'])
@jwt_required()
def me():
    user_id = get_jwt_identity()
    user = UserModel.get_by_id(user_id)
    if not user:
        return jsonify({"success": False, "message": "User not found"}), 404
        
    onboarding_status = "COMPLETED"
    if user['role'] == 'patient':
        profile = PatientModel.get_profile(user_id)
        if profile:
            onboarding_status = profile.get('onboarding_status', 'COMPLETED')

    return jsonify({
        "success": True,
        "user_id": user['_id'],
        "email": user['email'],
        "role": user['role'],
        "onboarding_status": onboarding_status
    }), 200
