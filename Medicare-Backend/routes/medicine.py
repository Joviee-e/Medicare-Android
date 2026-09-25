from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models.medicine import MedicineModel
from models.patient import PatientModel
from models.notification import NotificationModel
from services.medication_knowledge import get_medication_info
from services.safety_engine import check_allergy_conflict, check_drug_interactions, extract_medication_guidance
from ml.predictor import predict_adherence_risk
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

        # -------------------------------------------------------------
        # MEDICATION INTELLIGENCE & SAFETY PIPELINE
        # -------------------------------------------------------------
        alerts = []
        guidance = []

        try:
            # 1. Normalize with RxNorm and fetch openFDA label
            med_info = get_medication_info(name)

            # 2. Check Patient Allergies (Deterministic, High Priority)
            profile = PatientModel.get_profile(patient_id)
            user_allergies = ""
            if profile and isinstance(profile.get("medical_information"), dict):
                user_allergies = profile["medical_information"].get("allergies", "")

            allergy_alert = check_allergy_conflict(med_info, user_allergies)
            if allergy_alert:
                alerts.append(allergy_alert)
                NotificationModel.create_notification(
                    patient_id=patient_id,
                    title=allergy_alert["title"],
                    message=allergy_alert["message"],
                    priority="high",
                    notif_type="allergy",
                    medicine_id=med_id,
                    medicine_name=name,
                    context_for_ai=f"Allergy conflict detected: {allergy_alert['message']}"
                )

            # 3. Check Interactions with Existing Medications
            existing_meds_raw = MedicineModel.get_by_patient(patient_id)
            existing_meds = [m for m in existing_meds_raw if isinstance(m, dict) and str(m.get("_id")) != med_id] if isinstance(existing_meds_raw, list) else []
            interaction_alerts = check_drug_interactions(med_info, existing_meds)
            for ia in interaction_alerts:
                alerts.append(ia)
                NotificationModel.create_notification(
                    patient_id=patient_id,
                    title=ia["title"],
                    message=ia["message"],
                    priority=ia.get("priority", "medium"),
                    notif_type="interaction",
                    medicine_id=med_id,
                    medicine_name=name,
                    context_for_ai=f"Medication interaction detected: {ia['message']}"
                )

            # 4. Extract Verified Administration Guidance and Precautions
            guidance = extract_medication_guidance(med_info)
            if guidance and not allergy_alert:
                primary_guidance = guidance[0]
                NotificationModel.create_notification(
                    patient_id=patient_id,
                    title=f"Medication Guidance: {name}",
                    message=primary_guidance["message"],
                    priority="low",
                    notif_type="guidance",
                    medicine_id=med_id,
                    medicine_name=name,
                    context_for_ai=f"Administration guidance for {name}: {primary_guidance['message']}"
                )

            # 5. ML Adherence Risk Evaluation & Personalization
            all_active_meds = [m for m in existing_meds_raw if isinstance(m, dict)] if isinstance(existing_meds_raw, list) else []
            ml_result = predict_adherence_risk(all_active_meds)
            if ml_result.get("adherence_risk") in ("HIGH", "MEDIUM") and ml_result.get("features", {}).get("missed_doses_7d", 0) > 0:
                NotificationModel.create_notification(
                    patient_id=patient_id,
                    title="Medication Reminder Suggestion",
                    message=ml_result["insight"],
                    priority="low",
                    notif_type="adherence",
                    medicine_id=med_id,
                    medicine_name=name,
                    context_for_ai=f"Adherence suggestion: {ml_result['insight']}"
                )

        except Exception as pipeline_err:
            # External API failure fallback: Ensure medication creation succeeds even if network lookup fails
            alerts = []
            guidance = []
            med_info = {
                "normalized_name": name,
                "rxcui": None
            }
            ml_result = {"adherence_risk": "LOW", "confidence": 1.0, "insight": "Normal schedule active."}

        return jsonify({
            "success": True,
            "message": "Medicine added successfully",
            "medicine_id": str(med_id),
            "medication": {
                "name": name,
                "normalized_name": str(med_info.get("normalized_name", name)),
                "rxcui": str(med_info.get("rxcui")) if med_info.get("rxcui") else None
            },
            "alerts": alerts,
            "guidance": guidance,
            "ml": {
                "adherence_risk": str(ml_result.get("adherence_risk", "LOW")),
                "confidence": float(ml_result.get("confidence", 1.0)),
                "insight": str(ml_result.get("insight", ""))
            }
        }), 201
    except Exception as e:
        return jsonify({"success": False, "message": f"Server error: {str(e)}"}), 500


@medicine_bp.route('/analyze', methods=['POST'])
@jwt_required()
def analyze_medication():
    """
    On-demand medication safety and intelligence analysis without persisting the schedule.
    """
    patient_id = get_jwt_identity()
    data = request.get_json() or {}
    name = data.get('name', '').strip()

    if not name:
        return jsonify({"success": False, "message": "Medicine name is required"}), 400

    try:
        med_info = get_medication_info(name)
        profile = PatientModel.get_profile(patient_id)
        user_allergies = ""
        if profile and isinstance(profile.get("medical_information"), dict):
            user_allergies = profile["medical_information"].get("allergies", "")

        alerts = []
        allergy_alert = check_allergy_conflict(med_info, user_allergies)
        if allergy_alert:
            alerts.append(allergy_alert)

        existing_meds = MedicineModel.get_by_patient(patient_id)
        interaction_alerts = check_drug_interactions(med_info, existing_meds)
        alerts.extend(interaction_alerts)

        guidance = extract_medication_guidance(med_info)
        ml_result = predict_adherence_risk(existing_meds)

        return jsonify({
            "success": True,
            "medication": {
                "name": name,
                "normalized_name": med_info.get("normalized_name", name),
                "rxcui": med_info.get("rxcui"),
                "ingredients": med_info.get("ingredients", [name]),
                "drug_class": med_info.get("drug_class")
            },
            "alerts": alerts,
            "guidance": guidance,
            "ml": {
                "adherence_risk": ml_result.get("adherence_risk", "LOW"),
                "confidence": ml_result.get("confidence", 1.0),
                "insight": ml_result.get("insight", "")
            }
        }), 200
    except Exception as e:
        return jsonify({"success": False, "message": f"Analysis error: {str(e)}"}), 500



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
