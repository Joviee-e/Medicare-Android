"""
Medicare Notification Routes
Exposes prioritized patient notifications (HIGH, MEDIUM, LOW) for the top-right notification indicator.
"""

from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models.notification import NotificationModel
from utils.validators import is_valid_object_id

notification_bp = Blueprint('notification', __name__)

@notification_bp.route('', methods=['GET'])
@jwt_required()
def get_notifications():
    patient_id = get_jwt_identity()
    unread_param = request.args.get('unread_only', 'false').lower() == 'true'

    notifs = NotificationModel.get_by_patient(patient_id, unread_only=unread_param)
    return jsonify({
        "success": True,
        "count": len(notifs),
        "notifications": notifs
    }), 200

@notification_bp.route('/<notification_id>/read', methods=['POST'])
@jwt_required()
def mark_read(notification_id):
    if not is_valid_object_id(notification_id):
        return jsonify({"success": False, "message": "Invalid notification ID format"}), 400

    patient_id = get_jwt_identity()
    success = NotificationModel.mark_as_read(notification_id, patient_id)
    if not success:
        return jsonify({"success": False, "message": "Notification not found or access denied"}), 404

    return jsonify({
        "success": True,
        "message": "Notification marked as read"
    }), 200

@notification_bp.route('/clear', methods=['POST', 'DELETE'])
@jwt_required()
def clear_all_notifications():
    patient_id = get_jwt_identity()
    NotificationModel.clear_all(patient_id)
    return jsonify({
        "success": True,
        "message": "All notifications cleared"
    }), 200
