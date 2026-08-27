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
    
    # Get DOB, age, gender, phone, address, medical info, onboarding status
    date_of_birth = data.get('date_of_birth', '').strip()
    age = str(data.get('age', '')).strip()
    gender = data.get('gender', '').strip()
    phone = data.get('phone', '').strip()
    phone_country_code = data.get('phone_country_code', '').strip()
    phone_national = data.get('phone_national', '').strip()
    address = data.get('address', '').strip()
    medical_information = data.get('medical_information') or {}
    onboarding_status = data.get('onboarding_status', 'COMPLETED').strip()
    accessibility_settings = data.get('accessibility_settings')

    # Parse emergency contacts: support new list format or fallback to legacy individual parameters
    emergency_contacts = data.get('emergency_contacts')
    if emergency_contacts is None:
        emerg_name = data.get('emergency_contact_name', '').strip()
        emerg_phone = data.get('emergency_contact_phone', '').strip()
        if emerg_name or emerg_phone:
            emergency_contacts = [{
                "name": emerg_name or "Not Specified",
                "relationship": "Family",
                "phone": emerg_phone or "Not Specified"
            }]
        else:
            # Maintain previous contacts if not supplied
            existing = PatientModel.get_profile(user_id)
            if existing:
                emergency_contacts = existing.get('emergency_contacts', [])

    if not name:
        return jsonify({"success": False, "message": "Name field is required"}), 400

    # Backend phone number validation using python-phonenumbers library
    import phonenumbers
    if phone:
        try:
            if phone.startswith("+"):
                parsed = phonenumbers.parse(phone, None)
            else:
                parsed = phonenumbers.parse(phone, "IN")
            if not phonenumbers.is_valid_number(parsed):
                return jsonify({"success": False, "message": "Invalid phone number formatting"}), 400
        except Exception:
            return jsonify({"success": False, "message": "Invalid phone number formatting"}), 400

    if emergency_contacts:
        for c in emergency_contacts:
            c_phone = c.get("phone", "").strip()
            if c_phone and c_phone != "Not Specified":
                try:
                    if c_phone.startswith("+"):
                        parsed_c = phonenumbers.parse(c_phone, None)
                    else:
                        parsed_c = phonenumbers.parse(c_phone, "IN")
                    if not phonenumbers.is_valid_number(parsed_c):
                        return jsonify({"success": False, "message": f"Invalid phone number formatting for emergency contact {c.get('name')}"}), 400
                except Exception:
                    return jsonify({"success": False, "message": f"Invalid phone number formatting for emergency contact {c.get('name')}"}), 400

    # Ensure status is valid
    if onboarding_status not in ("NOT_STARTED", "IN_PROGRESS", "SKIPPED", "COMPLETED"):
        return jsonify({"success": False, "message": "Invalid onboarding status"}), 400

    success = PatientModel.update_profile(
        user_id=user_id,
        name=name,
        blood_group=blood_group,
        emergency_contacts=emergency_contacts,
        date_of_birth=date_of_birth,
        age=age,
        gender=gender,
        phone=phone,
        address=address,
        medical_information=medical_information,
        onboarding_status=onboarding_status,
        accessibility_settings=accessibility_settings,
        phone_country_code=phone_country_code,
        phone_national=phone_national
    )

    if not success:
        return jsonify({"success": False, "message": "Failed to update profile"}), 500
        
    return jsonify({
        "success": True,
        "message": "Profile updated successfully"
    }), 200
