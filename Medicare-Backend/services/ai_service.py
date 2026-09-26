"""
Medicare AI Service
Powers the MediCare+ AI Assistant via the official Google GenAI Python SDK.
Gathers relevant structured health & medication context from MongoDB, enforces strict
medical guardrails (no diagnosing, no prescribing, clear emergency protocols),
and provides graceful fallback when external AI services are unavailable.
"""

import logging
from config import Config
from models.patient import PatientModel
from models.medicine import MedicineModel
from ml.predictor import predict_adherence_risk
from services.medication_knowledge import get_medication_info

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """You are the MediCare+ AI Healthcare & Medication Assistant.
You provide clear, empathetic, and evidence-based explanations of healthcare, medications, allergies, and reminder schedules.

CRITICAL SAFETY & ETHICAL RULES:
1. NEVER diagnose diseases, conditions, or medical symptoms.
2. NEVER prescribe medications or recommend modifying prescription dosages.
3. NEVER tell users to stop taking or begin taking prescription medications on their own.
4. NEVER invent or hallucinate drug interactions, allergies, or medical instructions that are not documented in the provided context or verified pharmacopeia.
5. If a safety finding, allergy alert, or medication interaction concern is present in the context, clearly explain what the verified medication information states, and advise the user to review the finding directly with their prescribing physician or pharmacist.
6. For severe or life-threatening symptoms (e.g., severe chest pain, sudden difficulty breathing, signs of stroke, anaphylaxis, severe bleeding), immediately urge the user to seek emergency medical care (dial 911 / 112 / local emergency services).
7. Always provide a concise, natural, and helpful answer, followed by an appropriate medical disclaimer.
"""

_client = None

def get_genai_client():
    global _client
    if _client is None and Config.GEMINI_API_KEY:
        try:
            from google import genai
            _client = genai.Client(api_key=Config.GEMINI_API_KEY)
        except Exception as e:
            logger.error(f"Error initializing GenAI client: {e}")
            _client = None
    return _client

def build_user_context(patient_id: str, extra_context: dict = None) -> str:
    """
    Constructs safe, structured medical context for the AI prompt.
    Excludes sensitive credentials, tokens, and database IDs.
    """
    context_lines = []

    # 1. Patient Profile Summary
    profile = PatientModel.get_profile(patient_id)
    if profile:
        name = profile.get("name", "User")
        age = profile.get("age")
        blood_group = profile.get("blood_group")
        med_info = profile.get("medical_information", {}) or {}
        allergies = med_info.get("allergies", "").strip() or "None recorded"
        conditions = med_info.get("conditions", "").strip() or "None recorded"

        context_lines.append(f"Patient Name: {name}")
        if age:
            context_lines.append(f"Age: {age}")
        if blood_group and blood_group != "Not Specified":
            context_lines.append(f"Blood Group: {blood_group}")
        context_lines.append(f"Recorded Allergies: {allergies}")
        context_lines.append(f"Chronic Health Conditions: {conditions}")

    # 2. Current Medications
    medicines = MedicineModel.get_by_patient(patient_id)
    if medicines:
        context_lines.append("\nCurrent Active Medications:")
        for idx, med in enumerate(medicines, 1):
            m_name = med.get("name", "Unknown")
            m_dose = med.get("dosage", "")
            m_freq = med.get("frequency", "")
            m_times = ", ".join(med.get("reminder_times", []))
            context_lines.append(f"{idx}. {m_name} ({m_dose}) - Frequency: {m_freq}, Scheduled Times: [{m_times}]")

        # 3. Behavioral ML Adherence Insights (safely isolated)
        try:
            ml_insight = predict_adherence_risk(medicines)
            if ml_insight and ml_insight.get("available", True) and ml_insight.get("adherence_risk") not in (None, "UNAVAILABLE"):
                context_lines.append(f"\nAdherence Pattern Summary:")
                features = ml_insight.get("features", {})
                if "adherence_percentage" in features:
                    context_lines.append(f"- Adherence Rate: {features['adherence_percentage']}%")
                context_lines.append(f"- Adherence Risk Level: {ml_insight.get('adherence_risk', 'LOW')}")
                if ml_insight.get("insight"):
                    context_lines.append(f"- Adherence Insight: {ml_insight['insight']}")
            else:
                context_lines.append("\nAdherence Pattern Summary: ML adherence insight is currently unavailable.")
        except Exception as e:
            logger.warning(f"ML adherence evaluation failed gracefully: {e}")
            context_lines.append("\nAdherence Pattern Summary: ML adherence insight is currently unavailable.")
    else:
        context_lines.append("\nCurrent Active Medications: None currently scheduled in Medicare.")

    # 4. Specific Extra Context (e.g. when tapping a notification or asking about a specific medicine)
    if extra_context:
        med_name = extra_context.get("medication_name")
        alert_info = extra_context.get("alert")
        guidance_info = extra_context.get("guidance")
        prompt_topic = extra_context.get("topic")

        if med_name:
            context_lines.append(f"\nSpecific Medication Inquired: {med_name}")
            med_knowledge = get_medication_info(med_name)
            if med_knowledge:
                context_lines.append(f"- Normalized RxNorm Concept: {med_knowledge.get('normalized_name')}")
                context_lines.append(f"- Active Ingredients: {', '.join(med_knowledge.get('ingredients', []))}")
                lbl = med_knowledge.get("label_information", {})
                if lbl.get("warnings"):
                    context_lines.append(f"- Documented Warnings: {' '.join(lbl['warnings'])}")
                if lbl.get("precautions"):
                    context_lines.append(f"- Precautions: {' '.join(lbl['precautions'])}")
                if lbl.get("administration"):
                    context_lines.append(f"- Administration Directions: {' '.join(lbl['administration'])}")

        if alert_info:
            context_lines.append(f"\nActive Safety Finding: {alert_info}")
        if guidance_info:
            context_lines.append(f"\nActive Guidance Finding: {guidance_info}")
        if prompt_topic:
            context_lines.append(f"\nInquiry Topic: {prompt_topic}")

    return "\n".join(context_lines)

def generate_ai_chat_response(patient_id: str, user_message: str, extra_context: dict = None) -> dict:
    """
    Sends contextual prompt to Gemini and formats response.
    Falls back gracefully if API is unavailable.
    """
    if not user_message or not user_message.strip():
        return {
            "success": False,
            "reply": "Please provide a question or message for MediCare+ AI Assistant.",
            "disclaimer": "AI Assistant does not replace professional medical advice."
        }

    # Assemble structured clinical context safely
    try:
        user_context = build_user_context(patient_id, extra_context)
    except Exception as e:
        logger.error(f"Error assembling user context: {e}", exc_info=True)
        user_context = "Patient profile and medication context currently unavailable."

    prompt = f"""{SYSTEM_PROMPT}

PATIENT STRUCTURED CONTEXT:
===========================
{user_context}
===========================

USER INQUIRY:
"{user_message.strip()}"

Please respond accurately, kindly, and concisely based on the verified context above:"""

    client = get_genai_client()
    if client:
        # Candidate models list with preferred default first
        preferred_model = Config.GEMINI_MODEL or "gemini-flash-lite-latest"
        models_to_try = [preferred_model, "gemini-flash-lite-latest", "gemini-3.8-flash", "gemini-3-flash-preview"]
        # Deduplicate while preserving order
        candidate_models = []
        for m in models_to_try:
            if m not in candidate_models:
                candidate_models.append(m)

        for model_name in candidate_models:
            try:
                response = client.models.generate_content(
                    model=model_name,
                    contents=prompt
                )
                if response and response.text:
                    return {
                        "success": True,
                        "reply": response.text.strip(),
                        "model": model_name,
                        "disclaimer": "MediCare+ AI Assistant provides educational information and does not replace professional medical consultation."
                    }
            except Exception as e:
                logger.warning(f"Gemini call failed with model '{model_name}': {e}")
                continue

    # Graceful Fallback if Gemini API is unreachable or exhausted
    logger.info("Using clinical rule-based fallback response for AI assistant.")
    fallback_reply = _generate_rule_based_fallback(user_message, user_context, extra_context)
    return {
        "success": True,
        "reply": fallback_reply,
        "model": "rule_based_fallback",
        "disclaimer": "AI explanation is temporarily unavailable. Structured medication information was retrieved from verified local records."
    }

def _generate_rule_based_fallback(user_message: str, user_context: str, extra_context: dict = None) -> str:
    """Provides a safe, rule-based response when cloud AI API is unavailable."""
    msg_lower = user_message.lower()

    if extra_context and extra_context.get("alert"):
        return (
            f"Here is the verified information regarding this notification:\n\n"
            f"• {extra_context['alert']}\n\n"
            f"Please review this finding with your prescribing physician or pharmacist before making any changes."
        )

    if "allergy" in msg_lower or "allergic" in msg_lower:
        return (
            "Regarding allergy safety: Always ensure your prescribing doctor and pharmacist are aware of all recorded allergies "
            "before starting any new medication. If you suspect an allergic reaction (rash, swelling, difficulty breathing), "
            "stop the medication immediately and seek medical attention."
        )

    if any(k in msg_lower for k in ["missed", "forgot", "skip"]):
        return (
            "If you miss a dose, generally take it as soon as you remember unless it is almost time for your next scheduled dose. "
            "Never double up on doses to make up for a missed one. Check your specific medication label or speak to your pharmacist."
        )

    return (
        "Thank you for your question. While the AI explanation service is temporarily unavailable, your scheduled medications "
        "and verified precautions remain recorded in Medicare. For specific medical questions or symptoms, please consult your doctor or pharmacist."
    )
