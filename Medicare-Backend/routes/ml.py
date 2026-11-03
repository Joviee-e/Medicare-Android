"""
Medicare ML Routes
Exposes ML-driven medication adherence evaluation, routine analysis,
smart reminder optimization, and model performance metrics.
"""

import os
import json
from flask import Blueprint, jsonify, request
from flask_jwt_extended import jwt_required, get_jwt_identity
from models.medicine import MedicineModel
from ml.predictor import predict_adherence_risk
from ml.insights_service import get_patient_predictive_insights, apply_reminder_suggestion

ml_bp = Blueprint('ml', __name__)

@ml_bp.route('/adherence', methods=['GET'])
@jwt_required()
def get_adherence_insight():
    """Legacy ML adherence risk route for backward compatibility."""
    patient_id = get_jwt_identity()
    medicines = MedicineModel.get_by_patient(patient_id)
    result = predict_adherence_risk(medicines)

    return jsonify({
        "success": True,
        "ml": result
    }), 200


@ml_bp.route('/insights', methods=['GET'])
@jwt_required()
def get_predictive_insights():
    """
    Unified endpoint for all 3 interconnected ML components:
    1. Medication Adherence Prediction (Random Forest)
    2. Medication Routine Analysis (K-Means Clustering)
    3. Smart Reminder Optimization (Regression & Delay Analytics)
    """
    patient_id = get_jwt_identity()
    insights = get_patient_predictive_insights(patient_id)

    return jsonify({
        "success": True,
        "insights": insights
    }), 200


@ml_bp.route('/reminders/accept-suggestion', methods=['POST'])
@jwt_required()
def accept_reminder_suggestion():
    """
    Applies an ML-recommended reminder timing shift.
    SAFETY RULES:
    - Never changes medicine, dosage, or frequency.
    - Explicit confirmation flag 'confirmed_by_user' is required.
    """
    patient_id = get_jwt_identity()
    data = request.get_json() or {}

    medicine_id = data.get("medicine_id")
    current_time = data.get("current_reminder_time")
    suggested_time = data.get("suggested_reminder_time")
    confirmed = bool(data.get("confirmed_by_user", False))

    if not confirmed:
        return jsonify({
            "success": False,
            "message": "Explicit user confirmation is required to adjust reminder times."
        }), 400

    success, msg = apply_reminder_suggestion(
        patient_id=patient_id,
        medicine_id=medicine_id,
        current_time=current_time,
        suggested_time=suggested_time,
        confirmed=confirmed
    )

    if not success:
        return jsonify({
            "success": False,
            "message": msg
        }), 400

    return jsonify({
        "success": True,
        "message": msg
    }), 200


@ml_bp.route('/metrics', methods=['GET'])
def get_model_metrics():
    """Public endpoint returning verified evaluation metrics for the 3 ML components."""
    metrics_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "ml", "ml_metrics.json")
    if os.path.exists(metrics_path):
        with open(metrics_path, "r") as f:
            data = json.load(f)
        return jsonify({"success": True, "metrics": data}), 200
    return jsonify({"success": False, "message": "ML metrics not generated yet"}), 404
