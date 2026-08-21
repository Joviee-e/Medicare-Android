from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models.medicine import MedicineModel
from utils.validators import parse_date, parse_time, is_valid_object_id

medicine_bp = Blueprint('medicine', __name__)

@medicine_bp.route('', methods=['GET'])
@jwt_required()
def get_medicines():
    patient_id = get_jwt_identity()
    medicines = MedicineModel.get_by_patient(patient_id)
    return jsonify({
        "success": True,
        "medicines": medicines
    }), 200


@medicine_bp.route('', methods=['POST'])
@jwt_required()
def create_medicine():
    patient_id = get_jwt_identity()
    data = request.get_json() or {}
    
    name = data.get('name', '').strip()
    med_type = data.get('type', '').strip().lower()
    dosage = data.get('dosage', '').strip()
    frequency = data.get('frequency', '').strip().lower()
    start_date = data.get('start_date', '').strip()
    end_date = data.get('end_date', '').strip()
    reminder_times = data.get('reminder_times', [])

    # Validations
    if not name or not med_type or not dosage or not frequency or not start_date or not end_date or not reminder_times:
        return jsonify({"success": False, "message": "Missing required fields"}), 400

    if med_type not in ("tablet", "capsule", "syrup", "injection"):
        return jsonify({"success": False, "message": "Invalid medicine type"}), 400

    if frequency not in ("daily", "weekly", "as_needed"):
        return jsonify({"success": False, "message": "Invalid frequency value"}), 400

    try:
        parse_date(start_date)
        parse_date(end_date)
    except ValueError as e:
        return jsonify({"success": False, "message": str(e)}), 400

    if not isinstance(reminder_times, list):
        return jsonify({"success": False, "message": "reminder_times must be a list of times"}), 400

    for t in reminder_times:
        try:
            parse_time(t)
        except ValueError as e:
            return jsonify({"success": False, "message": str(e)}), 400

    try:
        med_id = MedicineModel.create_medicine(
            patient_id=patient_id,
            name=name,
            med_type=med_type,
            dosage=dosage,
            frequency=frequency,
            start_date=start_date,
            end_date=end_date,
            reminder_times=reminder_times
        )
        return jsonify({
            "success": True,
            "message": "Medicine added successfully",
            "medicine_id": med_id
        }), 201
    except Exception as e:
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500


@medicine_bp.route('/<medicine_id>', methods=['PUT'])
@jwt_required()
def update_medicine(medicine_id):
    if not is_valid_object_id(medicine_id):
        return jsonify({"success": False, "message": "Invalid medicine ID format"}), 400
        
    patient_id = get_jwt_identity()
    data = request.get_json() or {}
    
    name = data.get('name', '').strip()
    med_type = data.get('type', '').strip().lower()
    dosage = data.get('dosage', '').strip()
    frequency = data.get('frequency', '').strip().lower()
    start_date = data.get('start_date', '').strip()
    end_date = data.get('end_date', '').strip()
    reminder_times = data.get('reminder_times', [])

    if not name or not med_type or not dosage or not frequency or not start_date or not end_date or not reminder_times:
        return jsonify({"success": False, "message": "Missing required fields"}), 400

    if med_type not in ("tablet", "capsule", "syrup", "injection"):
        return jsonify({"success": False, "message": "Invalid medicine type"}), 400

    if frequency not in ("daily", "weekly", "as_needed"):
        return jsonify({"success": False, "message": "Invalid frequency value"}), 400

    try:
        parse_date(start_date)
        parse_date(end_date)
    except ValueError as e:
        return jsonify({"success": False, "message": str(e)}), 400

    if not isinstance(reminder_times, list):
        return jsonify({"success": False, "message": "reminder_times must be a list"}), 400

    for t in reminder_times:
        try:
            parse_time(t)
        except ValueError as e:
            return jsonify({"success": False, "message": str(e)}), 400

    success = MedicineModel.update_medicine(
        medicine_id=medicine_id,
        patient_id=patient_id,
        name=name,
        med_type=med_type,
        dosage=dosage,
        frequency=frequency,
        start_date=start_date,
        end_date=end_date,
        reminder_times=reminder_times
    )

    if not success:
        return jsonify({"success": False, "message": "Medicine not found or access denied"}), 404

    return jsonify({
        "success": True,
        "message": "Medicine updated successfully"
    }), 200


@medicine_bp.route('/<medicine_id>', methods=['DELETE'])
@jwt_required()
def delete_medicine(medicine_id):
    if not is_valid_object_id(medicine_id):
        return jsonify({"success": False, "message": "Invalid medicine ID format"}), 400
        
    patient_id = get_jwt_identity()
    success = MedicineModel.delete_medicine(medicine_id, patient_id)

    if not success:
        return jsonify({"success": False, "message": "Medicine not found or access denied"}), 404

    return jsonify({
        "success": True,
        "message": "Medicine deleted successfully"
    }), 200


@medicine_bp.route('/<medicine_id>/log', methods=['POST'])
@jwt_required()
def log_compliance(medicine_id):
    if not is_valid_object_id(medicine_id):
        return jsonify({"success": False, "message": "Invalid medicine ID format"}), 400
        
    patient_id = get_jwt_identity()
    data = request.get_json() or {}
    
    date_str = data.get('date', '').strip()
    time_str = data.get('time', '').strip()
    status = data.get('status', '').strip().lower()

    if not date_str or not time_str or not status:
        return jsonify({"success": False, "message": "Missing date, time or status fields"}), 400

    if status not in ("taken", "snoozed", "skipped", "missed"):
        return jsonify({"success": False, "message": "Invalid status value"}), 400

    try:
        parse_date(date_str)
        parse_time(time_str)
    except ValueError as e:
        return jsonify({"success": False, "message": str(e)}), 400

    success = MedicineModel.add_log(
        medicine_id=medicine_id,
        patient_id=patient_id,
        date=date_str,
        time=time_str,
        status=status
    )

    if not success:
        return jsonify({"success": False, "message": "Medicine not found or access denied"}), 404

    return jsonify({
        "success": True,
        "message": "Compliance log added successfully"
    }), 200
