"""
Medicare Notification Model
Manages patient medication alerts, safety warnings, and adherence recommendations.
Provides structured storage, priority sorting (HIGH > MEDIUM > LOW), and retrieval.
"""

from datetime import datetime
from bson import ObjectId
from database.mongo import get_notifications_collection

PRIORITY_ORDER = {"high": 0, "medium": 1, "low": 2}

class NotificationModel:
    @staticmethod
    def create_notification(patient_id: str, title: str, message: str, priority: str = "low",
                            notif_type: str = "guidance", medicine_id: str = None,
                            medicine_name: str = "", context_for_ai: str = "") -> str:
        """Create and store a prioritized notification record."""
        col = get_notifications_collection()
        now = datetime.utcnow()

        clean_priority = priority.lower() if priority.lower() in ("high", "medium", "low") else "low"

        doc = {
            "patient_id": ObjectId(patient_id),
            "medicine_id": ObjectId(medicine_id) if medicine_id else None,
            "medicine_name": medicine_name,
            "title": title,
            "message": message,
            "priority": clean_priority, # "high", "medium", "low"
            "type": notif_type, # "allergy", "interaction", "warning", "guidance", "adherence"
            "context_for_ai": context_for_ai,
            "is_read": False,
            "created_at": now
        }

        res = col.insert_one(doc)
        return str(res.inserted_id)

    @staticmethod
    def get_by_patient(patient_id: str, unread_only: bool = False) -> list:
        """
        Retrieve all notifications for a patient, strictly ordered by priority
        (HIGH first, then MEDIUM, then LOW), sub-sorted by creation date descending.
        """
        col = get_notifications_collection()
        query = {"patient_id": ObjectId(patient_id)}
        if unread_only:
            query["is_read"] = False

        try:
            docs = list(col.find(query).sort("created_at", -1))
            formatted = []
            for d in docs:
                p = d.get("priority", "low").lower()
                order_rank = PRIORITY_ORDER.get(p, 2)
                d["_id"] = str(d["_id"])
                d["patient_id"] = str(d["patient_id"])
                if d.get("medicine_id"):
                    d["medicine_id"] = str(d["medicine_id"])
                if isinstance(d.get("created_at"), datetime):
                    d["created_at"] = d["created_at"].isoformat()
                d["_rank"] = order_rank
                formatted.append(d)

            # Sort by rank ascending (0: high, 1: medium, 2: low), maintaining date order
            formatted.sort(key=lambda x: x["_rank"])
            for item in formatted:
                item.pop("_rank", None)

            return formatted
        except Exception:
            return []

    @staticmethod
    def mark_as_read(notification_id: str, patient_id: str) -> bool:
        """Mark a notification as read."""
        col = get_notifications_collection()
        try:
            res = col.update_one(
                {"_id": ObjectId(notification_id), "patient_id": ObjectId(patient_id)},
                {"$set": {"is_read": True}}
            )
            return res.matched_count > 0
        except Exception:
            return False

    @staticmethod
    def clear_all(patient_id: str) -> bool:
        """Delete all notifications for a patient."""
        col = get_notifications_collection()
        try:
            col.delete_many({"patient_id": ObjectId(patient_id)})
            return True
        except Exception:
            return False
