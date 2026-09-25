"""
Medicare Safety Engine
Deterministic medication safety checks:
1. Patient allergy conflict detection (high priority)
2. Drug-drug interaction screening (medium priority)
3. Medication precaution and verified administration guidance extraction (low/medium priority)

NOTE: This layer operates deterministically on verified clinical data and knowledge maps.
Safety findings do NOT rely on probabilistic ML or AI hallucination.
"""

import re
import logging
from typing import List, Dict, Tuple, Optional

logger = logging.getLogger(__name__)

# Common cross-reactivity and pharmacological drug classes
ALLERGY_CLASS_MAP = {
    "penicillin": [
        "penicillin", "amoxicillin", "ampicillin", "augmentin", "piperacillin", 
        "oxacillin", "nafcillin", "cloxacillin", "dicloxacillin", "ticarcillin",
        "amoxicillin-potassium clavulanate", "amoxil"
    ],
    "sulfa": [
        "sulfa", "sulfamethoxazole", "trimethoprim-sulfamethoxazole", "bactrim",
        "septra", "sulfadiazine", "sulfasalazine", "sulfisoxazole"
    ],
    "nsaid": [
        "nsaid", "nsaids", "aspirin", "ibuprofen", "advil", "motrin", "naproxen",
        "aleve", "celecoxib", "celebrex", "diclofenac", "voltaren", "meloxicam",
        "mobic", "ketorolac", "toradol", "indomethacin", "piroxicam"
    ],
    "aspirin": [
        "aspirin", "acetylsalicylic acid", "bayer", "ecotrin", "ibuprofen",
        "naproxen", "diclofenac", "meloxicam", "ketorolac"
    ],
    "opioid": [
        "opioid", "codeine", "morphine", "tramadol", "ultram", "oxycodone",
        "oxycontin", "percocet", "hydrocodone", "vicodin", "norco", "fentanyl",
        "hydromorphone", "dilaudid", "methadone", "buprenorphine"
    ],
    "statin": [
        "statin", "atorvastatin", "lipitor", "simvastatin", "zocor", "rosuvastatin",
        "crestor", "pravastatin", "pravachol", "lovastatin", "fluvastatin"
    ],
    "ace inhibitor": [
        "ace inhibitor", "lisinopril", "prinivil", "zestril", "enalapril",
        "vasotec", "ramipril", "altace", "benazepril", "lotensin", "captopril"
    ],
    "acetaminophen": [
        "acetaminophen", "paracetamol", "tylenol", "panadol", "calpol"
    ],
    "paracetamol": [
        "paracetamol", "acetaminophen", "tylenol", "panadol", "calpol"
    ],
    "macrolide": [
        "azithromycin", "zithromax", "clarithromycin", "biaxin", "erythromycin"
    ],
    "fluoroquinolone": [
        "ciprofloxacin", "cipro", "levofloxacin", "levaquin", "moxifloxacin", "avelox"
    ]
}

# Major clinical drug-drug interactions (substance pairs -> clinical summary)
KNOWN_INTERACTIONS = [
    {
        "pair": ({"aspirin", "ibuprofen", "naproxen", "diclofenac", "meloxicam", "ketorolac", "celecoxib"},
                 {"warfarin", "clopidogrel", "plavix", "apixaban", "eliquis", "rivaroxaban", "xarelto", "dabigatran", "heparin"}),
        "title": "Increased Bleeding Risk",
        "severity": "medium",
        "description": "Combining an NSAID/antiplatelet with an anticoagulant significantly elevates the risk of gastrointestinal and systemic bleeding."
    },
    {
        "pair": ({"lisinopril", "enalapril", "ramipril", "losartan", "valsartan", "candesartan"},
                 {"spironolactone", "potassium", "eplerenone", "triamterene"}),
        "title": "Risk of Hyperkalemia",
        "severity": "medium",
        "description": "Concomitant use can cause severe elevations in serum potassium levels, potentially affecting cardiac rhythm."
    },
    {
        "pair": ({"fluoxetine", "prozac", "sertraline", "zoloft", "citalopram", "celexa", "escitalopram", "lexapro", "paroxetine", "duloxetine", "venlafaxine"},
                 {"tramadol", "ultram", "selegiline", "rasagiline", "phenelzine", "linezolid"}),
        "title": "Risk of Serotonin Syndrome",
        "severity": "medium",
        "description": "Combining serotonergic agents increases the risk of excessive serotonin accumulation, which can cause neuromuscular hyperactivity and autonomic instability."
    },
    {
        "pair": ({"atorvastatin", "simvastatin", "lovastatin"},
                 {"clarithromycin", "erythromycin", "itraconazole", "ketoconazole", "gemfibrozil"}),
        "title": "Increased Statin Toxicity (Myopathy Risk)",
        "severity": "medium",
        "description": "CYP3A4 inhibition can substantially increase statin plasma concentrations, raising the risk of muscle pain, myopathy, and rhabdomyolysis."
    },
    {
        "pair": ({"nitroglycerin", "nitrostat", "isosorbide", "isordil"},
                 {"sildenafil", "viagra", "tadalafil", "cialis", "vardenafil"}),
        "title": "Severe Hypotension Risk",
        "severity": "high",
        "description": "Co-administration may precipitate profound, life-threatening drops in blood pressure and coronary perfusion."
    },
    {
        "pair": ({"metformin", "glucophage"},
                 {"iodinated contrast", "furosemide", "alcohol"}),
        "title": "Lactic Acidosis / Renal Warning",
        "severity": "medium",
        "description": "Co-administration under compromised renal conditions or during radiologic contrast procedures may elevate lactic acidosis risk."
    }
]

def check_allergy_conflict(medication_info: dict, user_allergies_str: str) -> Optional[dict]:
    """
    Deterministic check: compares medicine name, normalized name, and ingredients
    against patient's recorded allergies.
    """
    if not user_allergies_str or not user_allergies_str.strip():
        return None

    # Parse individual user allergies
    raw_allergies = [a.strip().lower() for a in re.split(r'[,;\n/]+', user_allergies_str) if a.strip()]
    if not raw_allergies:
        return None

    med_terms = set()
    for key in ["input_name", "display_name", "normalized_name"]:
        val = medication_info.get(key)
        if val:
            med_terms.add(val.lower().strip())
    for ing in medication_info.get("ingredients", []):
        if ing:
            med_terms.add(ing.lower().strip())

    # Check for direct or cross-reactive matches
    matched_allergen = None
    matched_term = None

    for user_allergy in raw_allergies:
        # 1. Direct exact or substring match
        for term in med_terms:
            if user_allergy in term or term in user_allergy:
                matched_allergen = user_allergy
                matched_term = term
                break
        if matched_allergen:
            break

        # 2. Check class cross-reactivity mapping
        for class_key, class_members in ALLERGY_CLASS_MAP.items():
            if class_key in user_allergy or any(m in user_allergy for m in class_members):
                # User has this class allergy. Check if the medicine is in this class
                for term in med_terms:
                    if any(m in term for m in class_members) or class_key in term:
                        matched_allergen = user_allergy
                        matched_term = term
                        break
            if matched_allergen:
                break
        if matched_allergen:
            break

    if matched_allergen:
        med_display = medication_info.get("display_name") or medication_info.get("normalized_name")
        return {
            "type": "allergy",
            "priority": "high",
            "title": f"Potential Allergy Concern: {med_display}",
            "message": (
                f"Potential allergy-related concern detected. {med_display} relates to your recorded allergy '{matched_allergen}'. "
                f"Please review this medication with your doctor or pharmacist before taking it."
            ),
            "matched_allergen": matched_allergen,
            "source": "safety_engine"
        }

    return None

def check_drug_interactions(new_med_info: dict, existing_medicines: list) -> List[dict]:
    """
    Screens for potential drug-drug interactions between the new medicine
    and existing active medications of the patient.
    """
    alerts = []
    if not existing_medicines:
        return alerts

    new_terms = set()
    for key in ["input_name", "display_name", "normalized_name"]:
        val = new_med_info.get(key)
        if val:
            new_terms.add(val.lower().strip())
    for ing in new_med_info.get("ingredients", []):
        if ing:
            new_terms.add(ing.lower().strip())

    new_name_display = new_med_info.get("display_name") or new_med_info.get("normalized_name")

    for existing in existing_medicines:
        ex_name = existing.get("name", "").strip()
        if not ex_name:
            continue
        ex_name_lower = ex_name.lower()

        # 1. Check duplicate therapy (same medication already scheduled)
        if ex_name_lower in new_terms or any(t in ex_name_lower for t in new_terms if len(t) > 3):
            alerts.append({
                "type": "duplicate_therapy",
                "priority": "medium",
                "title": f"Duplicate Medication Detected: {new_name_display}",
                "message": (
                    f"You already have '{ex_name}' in your active medications. "
                    f"Please verify dosage and frequency with your provider to prevent inadvertent double-dosing."
                ),
                "other_medication": ex_name,
                "source": "safety_engine"
            })
            continue

        # 2. Check clinically documented interaction pairs
        for rule in KNOWN_INTERACTIONS:
            group_a, group_b = rule["pair"]
            new_in_a = any(any(m in t for m in group_a) for t in new_terms)
            new_in_b = any(any(m in t for m in group_b) for t in new_terms)
            ex_in_a = any(m in ex_name_lower for m in group_a)
            ex_in_b = any(m in ex_name_lower for m in group_b)

            if (new_in_a and ex_in_b) or (new_in_b and ex_in_a):
                alerts.append({
                    "type": "interaction",
                    "priority": rule["severity"],
                    "title": f"Potential Interaction: {new_name_display} & {ex_name}",
                    "message": f"{rule['title']}: {rule['description']} Please consult your healthcare provider or pharmacist.",
                    "other_medication": ex_name,
                    "source": "safety_engine"
                })
                break

        # 3. Check openFDA drug label interactions text
        interactions_text = " ".join(new_med_info.get("label_information", {}).get("interactions", [])).lower()
        if ex_name_lower in interactions_text and len(ex_name_lower) > 3:
            # Avoid duplicate if already reported
            if not any(a.get("other_medication") == ex_name for a in alerts):
                alerts.append({
                    "type": "interaction",
                    "priority": "medium",
                    "title": f"Documented Interaction Info: {new_name_display} & {ex_name}",
                    "message": (
                        f"Official FDA label documentation notes potential interaction concerns when {new_name_display} "
                        f"is taken alongside {ex_name}. Please consult your pharmacist or prescribing doctor."
                    ),
                    "other_medication": ex_name,
                    "source": "openfda_label"
                })

    return alerts

def extract_medication_guidance(medication_info: dict) -> List[dict]:
    """
    Extracts verified administration guidelines and precautions from openFDA label data.
    Strictly avoids inventing non-factual lifestyle tips.
    """
    guidance = []
    label_info = medication_info.get("label_information", {})

    # 1. Administration instructions
    admin_texts = label_info.get("administration", [])
    for text in admin_texts:
        lower = text.lower()
        if any(keyword in lower for keyword in ["with food", "with meals", "after meal", "take with food"]):
            guidance.append({
                "type": "administration",
                "category": "food",
                "message": "Verified guidance: Take with food or meals as directed on label.",
                "source": "openfda_label"
            })
            break
        elif any(keyword in lower for keyword in ["empty stomach", "before meal", "1 hour before"]):
            guidance.append({
                "type": "administration",
                "category": "food",
                "message": "Verified guidance: Take on an empty stomach (typically 1 hour before or 2 hours after meals).",
                "source": "openfda_label"
            })
            break

    # Water instructions
    for text in admin_texts + label_info.get("patient_instructions", []):
        lower = text.lower()
        if any(keyword in lower for keyword in ["full glass of water", "with water", "plenty of fluids"]):
            guidance.append({
                "type": "administration",
                "category": "hydration",
                "message": "Verified guidance: Swallow with a full glass of water.",
                "source": "openfda_label"
            })
            break

    # Do not crush instructions
    for text in admin_texts + label_info.get("precautions", []):
        lower = text.lower()
        if any(keyword in lower for keyword in ["do not crush", "swallow whole", "do not chew"]):
            guidance.append({
                "type": "administration",
                "category": "formulation",
                "message": "Verified precaution: Swallow whole; do not crush, chew, or break tablet.",
                "source": "openfda_label"
            })
            break

    # Important Precaution extraction
    precautions = label_info.get("precautions", [])
    if precautions:
        snippet = precautions[0]
        if len(snippet) > 180:
            snippet = snippet[:180].rsplit(" ", 1)[0] + "..."
        guidance.append({
            "type": "precaution",
            "category": "precaution",
            "message": f"Medication precaution: {snippet}",
            "source": "openfda_label"
        })

    # Warnings extraction (if boxed warning or serious warning exists)
    warnings = label_info.get("warnings", [])
    if warnings:
        warn_snippet = warnings[0]
        if len(warn_snippet) > 180:
            warn_snippet = warn_snippet[:180].rsplit(" ", 1)[0] + "..."
        guidance.append({
            "type": "warning",
            "category": "warning",
            "message": f"Key safety warning: {warn_snippet}",
            "source": "openfda_label"
        })

    return guidance
