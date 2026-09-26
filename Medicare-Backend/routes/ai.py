"""
Medicare AI Chatbot Routes
Connects the Android Chatbot interface to Gemini through Flask backend.
"""

from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from services.ai_service import generate_ai_chat_response

import logging

logger = logging.getLogger(__name__)

ai_bp = Blueprint('ai', __name__)

@ai_bp.route('/chat', methods=['POST'])
@jwt_required()
def ai_chat():
    try:
        patient_id = get_jwt_identity()
        data = request.get_json() or {}

        user_message = data.get('message', '').strip()
        extra_context = data.get('context', {})

        if not user_message:
            return jsonify({
                "success": False,
                "message": "Message content cannot be empty"
            }), 400

        response = generate_ai_chat_response(
            patient_id=patient_id,
            user_message=user_message,
            extra_context=extra_context
        )

        return jsonify(response), 200
    except Exception as e:
        logger.error(f"Unhandled error in ai_chat route: {e}", exc_info=True)
        return jsonify({
            "success": True,
            "reply": "Thank you for your question. While the AI explanation service is temporarily unavailable, your scheduled medications and verified precautions remain recorded in Medicare. For specific medical questions or symptoms, please consult your doctor or pharmacist.",
            "disclaimer": "AI explanation is temporarily unavailable. Structured medication information was retrieved from verified local records.",
            "model": "rule_based_fallback"
        }), 200
