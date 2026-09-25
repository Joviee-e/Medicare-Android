"""
Medicare Medication Knowledge Service
Integrates RxNorm (NIH) for medicine name normalization and openFDA for official drug label data.
Caches all successful lookups in MongoDB to ensure high availability and rate-limit preservation.
"""

import logging
from datetime import datetime, timezone
import requests
from config import Config
from database.mongo import get_medication_cache_collection

logger = logging.getLogger(__name__)

RXNORM_BASE_URL = "https://rxnav.nlm.nih.gov/REST"
OPENFDA_LABEL_URL = "https://api.fda.gov/drug/label.json"

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

def get_medication_info(medicine_name: str) -> dict:
    """
    Unified entry point for medication intelligence.
    Checks MongoDB medication_cache first; fetches from RxNorm and openFDA on cache miss.
    """
    clean_name = medicine_name.strip()
    cache_key = clean_name.lower()
    cache_col = get_medication_cache_collection()

    # 1. Check MongoDB Cache
    try:
        cached_doc = cache_col.find_one({"input_name": cache_key})
        if cached_doc:
            cached_doc.pop("_id", None)
            return cached_doc
    except Exception as e:
        logger.warning(f"Error checking medication cache: {e}")

    # 2. Normalize with RxNorm
    rx_norm = normalize_with_rxnorm(clean_name)
    normalized_name = rx_norm.get("normalized_name") or clean_name.capitalize()
    rxcui = rx_norm.get("rxcui")
    ingredients = rx_norm.get("ingredients") or [clean_name.capitalize()]

    # 3. Retrieve openFDA label (try normalized name, then ingredient, then raw name)
    search_term = ingredients[0] if ingredients else normalized_name
    fda_data = fetch_openfda_label(search_term)
    if not fda_data.get("found") and normalized_name != search_term:
        fda_data = fetch_openfda_label(normalized_name)
    if not fda_data.get("found") and clean_name != normalized_name:
        fda_data = fetch_openfda_label(clean_name)

    med_info = {
        "input_name": cache_key,
        "display_name": clean_name.capitalize(),
        "normalized_name": normalized_name,
        "rxcui": rxcui,
        "ingredients": ingredients,
        "drug_class": fda_data.get("drug_class"),
        "label_information": {
            "warnings": fda_data.get("warnings", []),
            "precautions": fda_data.get("precautions", []),
            "contraindications": fda_data.get("contraindications", []),
            "interactions": fda_data.get("interactions", []),
            "adverse_reactions": fda_data.get("adverse_reactions", []),
            "administration": fda_data.get("administration", []),
            "patient_instructions": fda_data.get("patient_instructions", [])
        },
        "source": {
            "rxnorm": rx_norm.get("status") in ("verified", "approximate"),
            "openfda": fda_data.get("found", False)
        },
        "cached_at": datetime.now(timezone.utc).isoformat()
    }

    # 4. Save into MongoDB Cache
    try:
        cache_col.update_one(
            {"input_name": cache_key},
            {"$set": med_info},
            upsert=True
        )
    except Exception as e:
        logger.warning(f"Error saving to medication cache: {e}")

    return med_info
