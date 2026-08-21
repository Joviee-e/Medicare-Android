from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models.patient import PatientModel

patient_bp = Blueprint('patient', __name__)

@patient_bp.route('/profile', methods=['GET'])
@jwt_required()
def get_profile():
    user_id = get_jwt_identity()
    profile = PatientModel.get_profile(user_id)
    
    if not profile:
        return jsonify({"success": False, "message": "Profile not found"}), 404
        
    return jsonify({
        "success": True,
        "profile": profile
    }), 200


@patient_bp.route('/profile', methods=['PUT'])
@jwt_required()
def update_profile():
    user_id = get_jwt_identity()
    data = request.get_json() or {}
    
    name = data.get('name', '').strip()
    blood_group = data.get('blood_group', '').strip()
    emergency_contact_name = data.get('emergency_contact_name', '').strip()
    emergency_contact_phone = data.get('emergency_contact_phone', '').strip()
    accessibility_settings = data.get('accessibility_settings')

    if not name:
        return jsonify({"success": False, "message": "Name field is required"}), 400

    success = PatientModel.update_profile(
        user_id=user_id,
        name=name,
        blood_group=blood_group,
        emergency_contact_name=emergency_contact_name,
        emergency_contact_phone=emergency_contact_phone,
        accessibility_settings=accessibility_settings
    )

    if not success:
        return jsonify({"success": False, "message": "Failed to update profile"}), 500
        
    return jsonify({
        "success": True,
        "message": "Profile updated successfully"
    }), 200
