/**
 * MediCare+ Web Data Store & Initial State
 * Aligned with the Android UI models and MongoDB schemas.
 */

const INITIAL_PROFILE = {
  id: "patient_001",
  name: "Eleanor Vance",
  email: "eleanor.vance@medicare.health",
  age: 68,
  gender: "Female",
  bloodGroup: "O+",
  emergencyContact: {
    name: "Sarah Vance (Daughter)",
    relationship: "Daughter",
    phone: "+1 (555) 234-5678"
  },
  doctor: {
    name: "Dr. Robert Chen, MD",
    specialty: "Internal Medicine & Geriatrics",
    clinic: "Metro Health Pavilion, Suite 402",
    phone: "+1 (555) 890-1234"
  },
  conditions: ["Type 2 Diabetes", "Mild Hypertension"],
  allergies: ["Penicillin", "Sulfa drugs"],
  accessibility: {
    highContrast: false,
    largeFont: false,
    soundVolume: 80,
    voiceGuidance: false,
    theme: "light"
  }
};

const INITIAL_MEDICINES = [
  {
    id: "med_1",
    name: "Metformin",
    brand: "Glucophage",
    type: "tablet", // tablet, capsule, syrup, injection
    dosage: "500 mg",
    instructions: "Take with meals to prevent stomach upset",
    frequency: "daily",
    scheduleTimes: ["08:00 AM", "08:00 PM"],
    mealTiming: "with_food",
    startDate: "2026-01-10",
    endDate: "2026-12-31",
    inventoryRemaining: 24,
    totalPills: 60,
    color: "#00677f",
    status: "upcoming", // taken, upcoming, scheduled, missed
    history: [
      { date: "Today, 08:00 AM", time: "08:05 AM", status: "taken", loggedAt: "08:05 AM" }
    ],
    precautions: "Avoid excessive alcohol intake. Monitor kidney function annually.",
    prescribedFor: "Blood sugar regulation in Type 2 Diabetes"
  },
  {
    id: "med_2",
    name: "Lisinopril",
    brand: "Zestril",
    type: "tablet",
    dosage: "10 mg",
    instructions: "Take once daily in the morning with water",
    frequency: "daily",
    scheduleTimes: ["08:00 AM"],
    mealTiming: "after_food",
    startDate: "2026-02-01",
    endDate: "2026-12-31",
    inventoryRemaining: 18,
    totalPills: 30,
    color: "#005146",
    status: "taken",
    history: [
      { date: "Today, 08:00 AM", time: "07:55 AM", status: "taken", loggedAt: "07:55 AM" }
    ],
    precautions: "Do not use potassium supplements without consulting your doctor. May cause dry cough.",
    prescribedFor: "Blood pressure management"
  },
  {
    id: "med_3",
    name: "Atorvastatin",
    brand: "Lipitor",
    type: "tablet",
    dosage: "20 mg",
    instructions: "Take once daily in the evening at bedtime",
    frequency: "daily",
    scheduleTimes: ["09:00 PM"],
    mealTiming: "before_food",
    startDate: "2026-01-15",
    endDate: "2026-12-31",
    inventoryRemaining: 12,
    totalPills: 30,
    color: "#4a6267",
    status: "scheduled",
    history: [],
    precautions: "Avoid grapefruit juice. Report unexplained muscle aches or weakness immediately.",
    prescribedFor: "Cholesterol control & vascular health"
  },
  {
    id: "med_4",
    name: "Vitamin D3 + K2",
    brand: "CalciHealth",
    type: "capsule",
    dosage: "2000 IU",
    instructions: "Take with breakfast or fatty meal for optimal absorption",
    frequency: "daily",
    scheduleTimes: ["08:00 AM"],
    mealTiming: "with_food",
    startDate: "2026-01-01",
    endDate: "2026-12-31",
    inventoryRemaining: 45,
    totalPills: 90,
    color: "#f59e0b",
    status: "taken",
    history: [
      { date: "Today, 08:00 AM", time: "08:10 AM", status: "taken", loggedAt: "08:10 AM" }
    ],
    precautions: "Supports bone density and immune function.",
    prescribedFor: "Bone density and immunity"
  },
  {
    id: "med_5",
    name: "Salbutamol Inhaler",
    brand: "Ventolin",
    type: "injection", // Using injection category for fast-acting rescue device in UI
    dosage: "100 mcg / puff",
    instructions: "1 to 2 puffs as needed for shortness of breath or wheezing",
    frequency: "as_needed",
    scheduleTimes: ["As Needed"],
    mealTiming: "none",
    startDate: "2026-03-01",
    endDate: "2026-12-31",
    inventoryRemaining: 80,
    totalPills: 200,
    color: "#0284c7",
    status: "scheduled",
    history: [],
    precautions: "Rinse mouth with water after use. Keep within reach at all times.",
    prescribedFor: "Rapid respiratory relief"
  }
];

const INITIAL_PHARMACIES = [
  {
    id: "ph_1",
    name: "HealthPlus Wellness Chemist",
    address: "742 Evergreen Terrace, Downtown Health District",
    distance: "0.4 km away",
    rating: 4.9,
    reviewsCount: 142,
    phone: "+1 (555) 345-6789",
    status: "Open Now",
    hours: "Open until 10:00 PM",
    features: ["24/7 Drive-thru", "Free Delivery", "Senior Discount", "Flu Shots"],
    coordinates: { x: 38, y: 42 }
  },
  {
    id: "ph_2",
    name: "MediQuick 24/7 Pharmacy",
    address: "1204 Pine Crest Ave, Medical Square",
    distance: "0.8 km away",
    rating: 4.8,
    reviewsCount: 310,
    phone: "+1 (555) 789-0123",
    status: "Open 24/7",
    hours: "Always Open (24 Hours)",
    features: ["24/7 Open", "Home Delivery", "Prescription Compounding", "Wheelchair Accessible"],
    coordinates: { x: 65, y: 28 }
  },
  {
    id: "ph_3",
    name: "CareFirst Community Pharmacy",
    address: "518 Maple Boulevard, Suite 10",
    distance: "1.2 km away",
    rating: 4.7,
    reviewsCount: 88,
    phone: "+1 (555) 456-7890",
    status: "Open Now",
    hours: "Open until 8:00 PM",
    features: ["Curbside Pickup", "Diabetes Supply Hub", "Blood Pressure Kiosk"],
    coordinates: { x: 22, y: 70 }
  },
  {
    id: "ph_4",
    name: "Apex Central Dispensary",
    address: "880 Lexington Avenue, South Wing",
    distance: "1.9 km away",
    rating: 4.6,
    reviewsCount: 65,
    phone: "+1 (555) 901-2345",
    status: "Closes Soon",
    hours: "Open until 7:00 PM",
    features: ["Insurance Verification", "Travel Vaccines", "Mobile Refills"],
    coordinates: { x: 80, y: 75 }
  }
];

const AI_KNOWLEDGE_BASE = [
  {
    keywords: ["metformin", "sugar", "diabetes", "food"],
    response: `**Metformin Guidelines & Safe Administration:**\n\n• **How to take:** Metformin should always be taken **with or immediately after a meal** to reduce common gastrointestinal side effects (like nausea, indigestion, or upset stomach).\n• **Missed Dose:** Take it as soon as you remember with a snack. If it is already close to your next scheduled evening dose, skip the missed one. **Never double up doses**.\n• **Safety Alert:** Avoid excessive alcohol while on Metformin. Stay well-hydrated throughout the day.`
  },
  {
    keywords: ["lisinopril", "blood pressure", "cough", "potassium"],
    response: `**Lisinopril Overview (ACE Inhibitor):**\n\n• **Purpose:** Helps relax blood vessels, lower blood pressure, and protect heart & kidney health.\n• **Administration:** Best taken in the morning at the same time each day with a full glass of water.\n• **Important Precautions:** Do not take potassium supplements or salt substitutes containing potassium without consulting Dr. Chen, as Lisinopril can elevate potassium levels.\n• **Common Note:** A mild, dry cough is a documented harmless side effect. If severe, contact your physician for alternative options.`
  },
  {
    keywords: ["atorvastatin", "cholesterol", "grapefruit", "muscle"],
    response: `**Atorvastatin Instructions & Safety:**\n\n• **Timing:** Typically taken in the **evening or at bedtime**, as the liver synthesizes cholesterol predominantly overnight.\n• **Dietary Notice:** Avoid large amounts of **grapefruit or grapefruit juice**, as it inhibits liver enzymes (CYP3A4) and increases the risk of medication accumulation.\n• **Watch for:** Unexplained muscle soreness, tenderness, or unusual fatigue. Contact your physician if these occur.`
  },
  {
    keywords: ["interaction", "combine", "penicillin", "allergy", "safe"],
    response: `**Drug Interaction & Allergy Verification:**\n\n• **Allergy Record:** You have documented allergies to **Penicillin** and **Sulfa drugs** in your MediCare+ profile.\n• **Current Active Regimen:** Metformin 500mg, Lisinopril 10mg, Atorvastatin 20mg, and Vitamin D3.\n• **Interaction Status:** No major adverse pharmacokinetic interactions detected between your current scheduled daily medications.\n• *Reminder:* Always verify any new over-the-counter NSAIDs (like Ibuprofen/Naproxen) with your doctor, as they may reduce the blood pressure lowering effect of Lisinopril.`
  },
  {
    keywords: ["missed", "forgot", "skip", "alarm"],
    response: `**Protocol for Missed Medication Doses:**\n\n1. **Check the Clock:** If you are only 1 to 2 hours past your scheduled alarm, take the dose immediately with a glass of water.\n2. **Close to Next Dose:** If your next scheduled dose is less than 4 hours away, **skip the missed dose** and resume your routine at the regular time.\n3. **Safety Golden Rule:** Never take two doses at once to make up for a missed pill.\n4. Log the dose in your MediCare+ app so your weekly adherence record stays precise.`
  }
];
