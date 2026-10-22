from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity, get_jwt
from models.appointment import AppointmentModel
from utils.validators import is_valid_object_id

appointment_bp = Blueprint('appointment', __name__)

@appointment_bp.route('', methods=['GET'])
@jwt_required()
def get_appointments():
    user_id = get_jwt_identity()
    claims = get_jwt()
    role = claims.get('role', 'patient')
    
    if role == 'patient':
        appointments = AppointmentModel.get_by_patient(user_id)
    else:
        appointments = AppointmentModel.get_by_doctor(user_id)
        
    return jsonify({
        "success": True,
        "appointments": appointments
    }), 200


@appointment_bp.route('', methods=['POST'])
@jwt_required()
def create_appointment():
    claims = get_jwt()
    role = claims.get('role', 'patient')
    
    if role != 'patient':
        return jsonify({"success": False, "message": "Only patients can book appointments"}), 403

    patient_id = get_jwt_identity()
    data = request.get_json() or {}
    
    doctor_id = data.get('doctor_id', '').strip()
    date_time_str = data.get('date_time', '').strip()
    notes = data.get('notes', '').strip()

    if not doctor_id or not date_time_str:
        return jsonify({"success": False, "message": "doctor_id and date_time are required"}), 400

    if not is_valid_object_id(doctor_id):
        return jsonify({"success": False, "message": "Invalid doctor ID format"}), 400

    try:
        app_id = AppointmentModel.create_appointment(
            patient_id=patient_id,
            doctor_id=doctor_id,
            date_time_str=date_time_str,
            notes=notes
        )
        return jsonify({
            "success": True,
            "message": "Appointment booked successfully",
            "appointment_id": app_id
        }), 201
    except ValueError as e:
        return jsonify({"success": False, "message": str(e)}), 400
    except Exception as e:
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500


@appointment_bp.route('/<appointment_id>', methods=['PUT'])
@jwt_required()
def update_appointment(appointment_id):
    if not is_valid_object_id(appointment_id):
        return jsonify({"success": False, "message": "Invalid appointment ID format"}), 400
        
    user_id = get_jwt_identity()
    claims = get_jwt()
    role = claims.get('role', 'patient')
    
    data = request.get_json() or {}
    status = data.get('status', '').strip().lower()
    notes = data.get('notes')

    if not status:
        return jsonify({"success": False, "message": "Status field is required"}), 400

    if status not in ("scheduled", "completed", "cancelled"):
        return jsonify({"success": False, "message": "Invalid status value"}), 400

    success = AppointmentModel.update_appointment(
        appointment_id=appointment_id,
        user_id=user_id,
        role=role,
        status=status,
        notes=notes
    )

    if not success:
        return jsonify({"success": False, "message": "Appointment not found or access denied"}), 404

    return jsonify({
        "success": True,
        "message": "Appointment updated successfully"
    }), 200
