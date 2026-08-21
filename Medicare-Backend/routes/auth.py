from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity, create_access_token
from models.user import UserModel
from models.patient import PatientModel
from models.doctor import DoctorModel
from utils.validators import is_valid_email, is_valid_password
from utils.jwt_handler import generate_tokens
from utils.password import verify_password

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
        else:
            specialization = data.get('specialization', 'General Medicine').strip()
            DoctorModel.create_profile(user_id, name, specialization)
            
        return jsonify({
            "success": True, 
            "message": "User registered successfully", 
            "user_id": user_id
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

    if not verify_password(password, user['password_hash']):
        return jsonify({"success": False, "message": "Invalid email or password"}), 401

    access_token, refresh_token = generate_tokens(user['_id'], user['role'])
    
    # Get user name for response
    name = ""
    if user['role'] == 'patient':
        profile = PatientModel.get_profile(user['_id'])
        if profile:
            name = profile['name']
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
        "name": name
    }), 200


@auth_bp.route('/logout', methods=['POST'])
@jwt_required()
def logout():
    # In a full production build, we would add the token JTI to a Redis blacklist.
    # For this architecture, we return success and let the client discard the token.
    return jsonify({"success": True, "message": "Successfully logged out"}), 200


@auth_bp.route('/refresh', methods=['POST'])
@jwt_required(refresh=True)
def refresh():
    identity = get_jwt_identity()
    # Read the role from current user to keep claims accurate
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
        
    return jsonify({
        "success": True,
        "user_id": user['_id'],
        "email": user['email'],
        "role": user['role']
    }), 200
