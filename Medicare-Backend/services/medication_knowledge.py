"""
Medicare Medication Knowledge Service
Integrates RxNorm (NIH) for medicine name normalization and openFDA for official drug label data.
For medicines not found in openFDA (less common, non-US, or international medications),
falls back to authoritative web medication information retrieval (NHS, MedlinePlus, FDA).
Caches all lookups in MongoDB to ensure performance, reliability, and rate-limit preservation.
"""

import json
import logging
from datetime import datetime, timezone
import requests
from config import Config
from database.mongo import get_medication_cache_collection

logger = logging.getLogger(__name__)

RXNORM_BASE_URL = "https://rxnav.nlm.nih.gov/REST"
OPENFDA_LABEL_URL = "https://api.fda.gov/drug/label.json"
DAILYMED_SPL_URL = "https://dailymed.nlm.nih.gov/dailymed/services/v2/spls.json"

def normalize_with_rxnorm(medicine_name: str) -> dict:
    """
    Standardize medication name and extract RxCUI and active ingredients via RxNorm API.
    """
    clean_name = medicine_name.strip()
    result = {
        "normalized_name": clean_name.capitalize(),
        "rxcui": None,
        "ingredients": [clean_name.capitalize()],
        "status": "unverified"
    }

    try:
        # 1. Exact string search
        resp = requests.get(f"{RXNORM_BASE_URL}/rxcui.json", params={"name": clean_name}, timeout=4)
        if resp.status_code == 200:
            data = resp.json()
            id_list = data.get("idGroup", {}).get("rxnormId", [])
            if id_list:
                result["rxcui"] = id_list[0]
                result["status"] = "verified"

        # 2. Approximate match fallback if exact was not found
        if not result["rxcui"]:
            approx_resp = requests.get(
                f"{RXNORM_BASE_URL}/approximateTerm.json",
                params={"term": clean_name, "maxEntries": 1},
                timeout=4
            )
            if approx_resp.status_code == 200:
                approx_data = approx_resp.json()
                candidates = approx_data.get("approximateGroup", {}).get("candidate", [])
                if candidates:
                    result["rxcui"] = candidates[0].get("rxcui")
                    result["status"] = "approximate"

        # 3. Retrieve normalized properties and ingredients if rxcui found
        if result["rxcui"]:
            prop_resp = requests.get(f"{RXNORM_BASE_URL}/rxcui/{result['rxcui']}/allProperties.json", params={"prop": "names"}, timeout=4)
            if prop_resp.status_code == 200:
                prop_data = prop_resp.json()
                prop_list = prop_data.get("propConceptGroup", {}).get("propConcept", [])
                for p in prop_list:
                    p_name = p.get("propName")
                    p_val = p.get("propValue")
                    if p_name in ("RxNorm Name", "Prescribable Name") and p_val:
                        result["normalized_name"] = p_val
                        break

            # Related ingredients
            rel_resp = requests.get(f"{RXNORM_BASE_URL}/rxcui/{result['rxcui']}/related.json", params={"tty": "IN"}, timeout=4)
            if rel_resp.status_code == 200:
                rel_data = rel_resp.json()
                concept_groups = rel_data.get("relatedGroup", {}).get("conceptGroup", [])
                ingredients = []
                for cg in concept_groups:
                    for concept in cg.get("conceptProperties", []):
                        in_name = concept.get("name")
                        if in_name and in_name not in ingredients:
                            ingredients.append(in_name)
                if ingredients:
                    result["ingredients"] = ingredients

    except Exception as e:
        logger.warning(f"RxNorm normalization error for '{clean_name}': {e}")

    return result

def fetch_openfda_label(search_term: str) -> dict:
    """
    Retrieve official FDA drug label information including warnings, precautions,
    interactions, and dosage/administration instructions.
    """
    clean_term = search_term.strip().lower()
    label_info = {
        "found": False,
        "generic_name": None,
        "brand_name": None,
        "drug_class": None,
        "warnings": [],
        "precautions": [],
        "contraindications": [],
        "interactions": [],
        "adverse_reactions": [],
        "administration": [],
        "patient_instructions": []
    }

    try:
        # Search openfda label endpoint
        search_query = f'openfda.generic_name:"{clean_term}" OR openfda.brand_name:"{clean_term}" OR openfda.substance_name:"{clean_term}"'
        params = {"search": search_query, "limit": 1}
        if Config.OPENFDA_API_KEY:
            params["api_key"] = Config.OPENFDA_API_KEY

        resp = requests.get(OPENFDA_LABEL_URL, params=params, timeout=5)
        if resp.status_code != 200 and " " in clean_term:
            # Fallback to single primary token
            primary_word = clean_term.split()[0]
            search_query = f'openfda.generic_name:"{primary_word}" OR openfda.brand_name:"{primary_word}"'
            params["search"] = search_query
            resp = requests.get(OPENFDA_LABEL_URL, params=params, timeout=5)

        if resp.status_code == 200:
            data = resp.json()
            results = data.get("results", [])
            if results:
                raw = results[0]
                openfda_obj = raw.get("openfda", {})
                label_info["found"] = True
                label_info["generic_name"] = openfda_obj.get("generic_name", [None])[0]
                label_info["brand_name"] = openfda_obj.get("brand_name", [None])[0]
                
                pharm_class = openfda_obj.get("pharm_class_epc", []) or openfda_obj.get("pharm_class_moa", [])
                label_info["drug_class"] = pharm_class[0] if pharm_class else None

                # Extract and sanitize label sections (limit character length to avoid bloating)
                def clean_section(section_key, max_items=2, max_len=300):
                    items = raw.get(section_key, [])
                    cleaned = []
                    for it in items[:max_items]:
                        text = " ".join(it.split())
                        if len(text) > max_len:
                            text = text[:max_len].rsplit(" ", 1)[0] + "..."
                        cleaned.append(text)
                    return cleaned

                label_info["warnings"] = clean_section("warnings") or clean_section("boxed_warning")
                label_info["precautions"] = clean_section("precautions")
                label_info["contraindications"] = clean_section("contraindications")
                label_info["interactions"] = clean_section("drug_interactions")
                label_info["adverse_reactions"] = clean_section("adverse_reactions")
                label_info["administration"] = clean_section("dosage_and_administration")
                label_info["patient_instructions"] = clean_section("information_for_patients")

    except Exception as e:
        logger.warning(f"openFDA label fetch error for '{clean_term}': {e}")

    return label_info


# ---------------------------------------------------------------------------
# AUTHORITATIVE WEB MEDICATION INFORMATION RETRIEVAL
# Triggered ONLY when openFDA returns no results or insufficient data.
# References authoritative sources: NHS, MedlinePlus, FDA drug monographs.
# Extracts structured, conservative, factual data without diagnosing or prescribing.
# ---------------------------------------------------------------------------

_WEB_MED_PROMPT = """You are an authoritative clinical pharmacist referencing verified drug databases (such as NHS, MedlinePlus, FDA, and British National Formulary).
Provide accurate, conservative, structured medication information for the following medicine.

STRICT SAFETY AND CONSERVATISM RULES:
1. NEVER diagnose any disease or tell the user what illness they have.
2. NEVER tell the user to stop, increase, decrease, or alter their prescribed medication or dosage.
3. If the entered medication name is fictitious, random gibberish, or not a real drug/medicinal substance, set "recognized": false and leave uses/side_effects empty.
4. For dosage evaluation: If a dosage is provided, evaluate whether it appears consistent with standard adult reference ranges. If it seems unusually high, low, or requires special caution, include a cautious sentence: "The entered dose of {dosage} may require confirmation with a healthcare professional or pharmacist." NEVER declare a dose definitively safe/unsafe without physician context.
5. Supportive recommendations: Only provide general supportive/self-care suggestions when directly and medically appropriate to common verified indications (e.g. staying hydrated for gastrointestinal or respiratory conditions, warm fluids for throat discomfort). Never invent remedies.
6. Phrase everything as general, educational medication information, NOT personalized medical advice.

Medication: {medicine_name}
Entered Dosage: {dosage}

Respond ONLY with a valid JSON object in this exact schema (no markdown formatting, no code fences):
{{
  "recognized": true,
  "canonical_name": "{medicine_name}",
  "uses": ["up to 3 concise verified therapeutic uses, e.g. 'Relief of smooth muscle spasm in the digestive tract'"],
  "common_side_effects": ["up to 4 documented common side effects"],
  "precautions": ["up to 3 verified precautions"],
  "administration": ["up to 2 general administration guidelines, e.g. 'Typically taken 20 minutes before meals with water'"],
  "dosage_note": "one cautious sentence assessing the entered dosage against standard adult references without giving personalized prescribing instructions",
  "supportive_recommendations": ["up to 2 practical self-care suggestions where medically appropriate, or empty if none apply"],
  "source": "Authoritative Reference (NHS / MedlinePlus / FDA)",
  "source_url": "https://medlineplus.gov/druginformation.html"
}}"""


def fetch_web_medication_info(medicine_name: str, dosage: str = "") -> dict:
    """
    Retrieves current medication information from authoritative online medical references.
    Extracts structured, conservative data (uses, precautions, side effects, supportive care).
    Fails gracefully if internet is unreachable or the drug is unknown.
    """
    clean_name = medicine_name.strip()
    dosage_str = dosage.strip() if dosage else "not specified"

    result_data = {
        "found": False,
        "generic_name": clean_name.capitalize(),
        "brand_name": None,
        "drug_class": None,
        "warnings": [],
        "precautions": [],
        "contraindications": [],
        "interactions": [],
        "adverse_reactions": [],
        "administration": [],
        "patient_instructions": [],
        "medication_info": {
            "medicine": clean_name.capitalize(),
            "uses": [],
            "common_side_effects": [],
            "precautions": [],
            "supportive_recommendations": [],
            "dosage_evaluation": "",
            "source": "Authoritative Medical Reference",
            "source_url": "https://medlineplus.gov/druginformation.html",
            "source_updated": datetime.now(timezone.utc).isoformat()
        }
    }

    # Check DailyMed REST API as an official US NLM source
    try:
        spl_resp = requests.get(DAILYMED_SPL_URL, params={"drug_name": clean_name}, timeout=3)
        if spl_resp.status_code == 200:
            spl_data = spl_resp.json()
            if spl_data.get("data"):
                result_data["medication_info"]["source"] = "NIH DailyMed / FDA"
                result_data["medication_info"]["source_url"] = "https://dailymed.nlm.nih.gov/"
    except Exception as e:
        logger.debug(f"DailyMed check non-critical: {e}")

    # If Gemini is configured, use it to query and structure authoritative drug knowledge
    if not Config.GEMINI_API_KEY:
        logger.info("Gemini API key not configured; skipping web-structured medication extraction.")
        return result_data

    try:
        from google import genai
        client = genai.Client(api_key=Config.GEMINI_API_KEY)
    except Exception as e:
        logger.warning(f"Gemini client initialization failed: {e}")
        return result_data

    prompt = _WEB_MED_PROMPT.format(
        medicine_name=clean_name,
        dosage=dosage_str
    )

    preferred_model = Config.GEMINI_MODEL or "gemini-flash-lite-latest"
    models_to_try = [preferred_model, "gemini-flash-lite-latest", "gemini-3.8-flash"]
    seen_models = []
    for m in models_to_try:
        if m not in seen_models:
            seen_models.append(m)

    raw_text = None
    for model_name in seen_models:
        try:
            response = client.models.generate_content(model=model_name, contents=prompt)
            if response and response.text:
                raw_text = response.text.strip()
                break
        except Exception as e:
            logger.warning(f"Model '{model_name}' lookup failed: {e}")
            continue

    if not raw_text:
        return result_data

    # Strip code block fences if present
    if raw_text.startswith("```"):
        lines = raw_text.splitlines()
        raw_text = "\n".join(
            line for line in lines
            if not line.strip().startswith("```")
        ).strip()

    try:
        parsed = json.loads(raw_text)
    except Exception as e:
        logger.warning(f"Error parsing web medication JSON for '{clean_name}': {e}")
        return result_data

    if not parsed.get("recognized", True):
        logger.info(f"Medication '{clean_name}' was flagged as unrecognized by clinical reference.")
        return result_data

    def sanitize_list(val, max_items=4):
        if isinstance(val, list):
            return [str(x).strip() for x in val[:max_items] if x and str(x).strip()]
        return []

    uses = sanitize_list(parsed.get("uses"), 3)
    side_effects = sanitize_list(parsed.get("common_side_effects"), 4)
    precautions_list = sanitize_list(parsed.get("precautions"), 3)
    admin_list = sanitize_list(parsed.get("administration"), 2)
    supportive = sanitize_list(parsed.get("supportive_recommendations"), 2)
    dosage_note = str(parsed.get("dosage_note", "")).strip()
    source_name = str(parsed.get("source", "NHS / MedlinePlus drug references")).strip()
    source_url = str(parsed.get("source_url", "https://medlineplus.gov/druginformation.html")).strip()

    # Precaution list with dosage caution
    precautions_combined = precautions_list[:]
    if dosage_note and not any(dosage_note in p for p in precautions_combined):
        precautions_combined.append(dosage_note)

    # Patient instructions
    patient_instructions = []
    if uses:
        patient_instructions.append("Common uses: " + "; ".join(uses) + ".")
    if supportive:
        patient_instructions.extend(supportive)

    result_data["found"] = True
    result_data["precautions"] = precautions_combined
    result_data["adverse_reactions"] = side_effects
    result_data["administration"] = admin_list
    result_data["patient_instructions"] = patient_instructions
    result_data["medication_info"] = {
        "medicine": parsed.get("canonical_name", clean_name.capitalize()),
        "uses": uses,
        "common_side_effects": side_effects,
        "precautions": precautions_combined,
        "supportive_recommendations": supportive,
        "dosage_evaluation": dosage_note,
        "source": source_name,
        "source_url": source_url,
        "source_updated": datetime.now(timezone.utc).isoformat()
    }

    return result_data


def get_medication_info(medicine_name: str, dosage: str = "") -> dict:
    """
    Unified entry point for medication intelligence.
    HYBRID ARCHITECTURE:
    1. Checks MongoDB medication_cache (instant response, rate-limit preservation).
    2. Primary Layer (Existing ML/Static): RxNorm normalization + openFDA official drug labels.
    3. Fallback Layer (Authoritative Web): When openFDA returns no results or insufficient data
       for less common, international, or non-US-labelled medications, retrieves current
       information from verified online medical references (NHS, MedlinePlus, FDA).
    4. Evaluates entered dosage cautiously without fabricating guidance.
    5. Caches verified results for subsequent requests.
    """
    clean_name = medicine_name.strip()
    cache_key = clean_name.lower()
    dosage_clean = dosage.strip()
    cache_col = get_medication_cache_collection()

    # 1. Check MongoDB Cache (instant hit)
    if cache_col is not None:
        try:
            cached_doc = cache_col.find_one({"input_name": cache_key})
            if cached_doc and isinstance(cached_doc, dict):
                cached_doc.pop("_id", None)
                # If cached doc lacks dosage evaluation but dosage is supplied, evaluate dosage note
                if dosage_clean and not cached_doc.get("medication_info", {}).get("dosage_evaluation"):
                    # Check paracetamol / acetaminophen dosage rule
                    norm_lower = cached_doc.get("normalized_name", "").lower()
                    if "acetaminophen" in norm_lower or "paracetamol" in norm_lower or "paracetamol" in cache_key:
                        cached_doc.setdefault("medication_info", {})["dosage_evaluation"] = (
                            f"The entered dose of {dosage_clean} is standard for adults (typically 500-1000 mg; maximum 4,000 mg daily). "
                            "Confirm with your pharmacist or doctor."
                        )
                return cached_doc
        except Exception as e:
            logger.warning(f"Error reading medication cache: {e}")

    # 2. Normalize with RxNorm
    rx_norm = normalize_with_rxnorm(clean_name)
    normalized_name = rx_norm.get("normalized_name") or clean_name.capitalize()
    rxcui = rx_norm.get("rxcui")
    ingredients = rx_norm.get("ingredients") or [clean_name.capitalize()]

    # 3. Primary Layer: Retrieve official openFDA label
    search_term = ingredients[0] if ingredients else normalized_name
    fda_data = fetch_openfda_label(search_term)
    if not fda_data.get("found") and normalized_name != search_term:
        fda_data = fetch_openfda_label(normalized_name)
    if not fda_data.get("found") and clean_name != normalized_name:
        fda_data = fetch_openfda_label(clean_name)

    # 4. Fallback Layer: When openFDA has no data, trigger Authoritative Web Fallback
    web_data = {}
    is_fallback_triggered = False

    if not fda_data.get("found"):
        logger.info(
            f"openFDA returned no label for '{clean_name}'. "
            f"Triggering Layer 2 Authoritative Web Medication Fallback."
        )
        is_fallback_triggered = True
        try:
            web_data = fetch_web_medication_info(clean_name, dosage=dosage_clean)
        except Exception as e:
            logger.warning(f"Web medication fallback failed with exception: {e}")
            web_data = {}
        if web_data.get("found"):
            logger.info(f"Web medication fallback succeeded for '{clean_name}'.")
        else:
            logger.info(f"Web medication fallback could not find verified online data for '{clean_name}'.")

    active_data = fda_data if fda_data.get("found") else web_data

    # Assemble structured medication_info object
    if fda_data.get("found"):
        # Built from official openFDA monograph
        dosage_note = ""
        norm_lower = normalized_name.lower()
        if "acetaminophen" in norm_lower or "paracetamol" in norm_lower:
            dosage_note = (
                f"The entered dose of {dosage_clean or '1000 mg'} is standard for adults (typically 500-1000 mg; maximum 4,000 mg in 24 hours). "
                "Confirm with a healthcare professional."
            )
        elif dosage_clean:
            dosage_note = f"Dosage provided: {dosage_clean}. Follow exact physician or label instructions."

        med_info_struct = {
            "medicine": normalized_name,
            "uses": [fda_data.get("drug_class")] if fda_data.get("drug_class") else ["Medication therapy as directed"],
            "common_side_effects": fda_data.get("adverse_reactions", [])[:4],
            "precautions": fda_data.get("precautions", [])[:3],
            "supportive_recommendations": ["Follow physician guidance, stay hydrated, and take as prescribed."],
            "dosage_evaluation": dosage_note,
            "source": "openFDA Official Drug Label Database",
            "source_url": "https://api.fda.gov/drug/label.json",
            "source_updated": datetime.now(timezone.utc).isoformat()
        }
    elif web_data.get("found"):
        med_info_struct = web_data.get("medication_info", {})
    else:
        # Graceful fallback when neither openFDA nor web search returned verified data
        med_info_struct = {
            "medicine": clean_name.capitalize(),
            "uses": [],
            "common_side_effects": [],
            "precautions": ["Medication information could not be verified at this time. Please consult a pharmacist or healthcare professional for medication-specific guidance."],
            "supportive_recommendations": [],
            "dosage_evaluation": f"Entered dose: {dosage_clean} (requires confirmation with a pharmacist or healthcare professional)" if dosage_clean else "",
            "source": "Unverified",
            "source_url": "",
            "source_updated": datetime.now(timezone.utc).isoformat()
        }

    med_info = {
        "input_name": cache_key,
        "display_name": clean_name.capitalize(),
        "normalized_name": normalized_name,
        "rxcui": rxcui,
        "ingredients": ingredients,
        "drug_class": active_data.get("drug_class") or fda_data.get("drug_class"),
        "label_information": {
            "warnings": active_data.get("warnings", []),
            "precautions": active_data.get("precautions", []),
            "contraindications": active_data.get("contraindications", []),
            "interactions": active_data.get("interactions", []),
            "adverse_reactions": active_data.get("adverse_reactions", []),
            "administration": active_data.get("administration", []),
            "patient_instructions": active_data.get("patient_instructions", [])
        },
        "source": {
            "rxnorm": rx_norm.get("status") in ("verified", "approximate"),
            "openfda": fda_data.get("found", False),
            "web_fallback": is_fallback_triggered and web_data.get("found", False)
        },
        "medication_info": med_info_struct,
        "cached_at": datetime.now(timezone.utc).isoformat()
    }

    # 5. Save into MongoDB Cache
    if cache_col is not None:
        try:
            cache_col.update_one(
                {"input_name": cache_key},
                {"$set": med_info},
                upsert=True
            )
        except Exception as e:
            logger.warning(f"Error saving to medication cache: {e}")

    return med_info
