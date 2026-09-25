"""
Medicare ML Routes
Exposes ML-driven medication adherence evaluation and personalization insights.
"""

from flask import Blueprint, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models.medicine import MedicineModel
from ml.predictor import predict_adherence_risk

ml_bp = Blueprint('ml', __name__)

@ml_bp.route('/adherence', methods=['GET'])
@jwt_required()
def get_adherence_insight():
    patient_id = get_jwt_identity()
    medicines = MedicineModel.get_by_patient(patient_id)
    result = predict_adherence_risk(medicines)

    return jsonify({
        "success": True,
        "ml": result
    }), 200
