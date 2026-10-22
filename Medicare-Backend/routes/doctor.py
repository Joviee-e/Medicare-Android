from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required
from models.doctor import DoctorModel

doctor_bp = Blueprint('doctor', __name__)

@doctor_bp.route('', methods=['GET'])
@jwt_required()
def list_doctors():
    specialization = request.args.get('specialization', '').strip()
    doctors = DoctorModel.list_doctors(specialization=specialization)
    return jsonify({
        "success": True,
        "doctors": doctors
    }), 200


@doctor_bp.route('/<doctor_id>', methods=['GET'])
@jwt_required()
def get_doctor_profile(doctor_id):
    profile = DoctorModel.get_profile(doctor_id)
    if not profile:
        return jsonify({"success": False, "message": "Doctor not found"}), 404
        
    return jsonify({
        "success": True,
        "doctor": profile
    }), 200
