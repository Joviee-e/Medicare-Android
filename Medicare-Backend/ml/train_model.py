"""
Medicare Medication Adherence ML Model Training Pipeline
Algorithm: RandomForestClassifier (scikit-learn)
Purpose: Predict adherence risk (LOW, MEDIUM, HIGH) to personalize reminder schedules
         and educational guidance.

NOTE ON CLINICAL SCOPE:
The model was initially trained and evaluated using synthetic medication-adherence data
for application prototyping. The results do not constitute clinical validation.
The system does NOT use ML to diagnose disease or independently determine medical safety.
"""

import os
import json
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report
)
import joblib

FEATURE_NAMES = [
    "medication_count",
    "daily_medication_events",
    "missed_doses_7d",
    "missed_doses_30d",
    "adherence_percentage",
    "reminder_response_rate",
    "consecutive_missed_days",
    "medication_complexity"
]

TARGET_CLASSES = ["LOW", "MEDIUM", "HIGH"]
TARGET_MAP = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
REVERSE_TARGET_MAP = {0: "LOW", 1: "MEDIUM", 2: "HIGH"}

def generate_synthetic_adherence_data(n_samples: int = 1600, random_seed: int = 42):
    """
    Generate realistic synthetic adherence data modeled after established patterns
    in clinical pharmacotherapy literature (e.g., complexity & polypharmacy correlation
    with missed doses and adherence fatigue).
    Returns (X, y) as numpy arrays.
    """
    np.random.seed(random_seed)
    X_rows = []
    y_rows = []

    for _ in range(n_samples):
        archetype = np.random.choice(["adherent", "moderate", "high_risk"], p=[0.45, 0.35, 0.20])

        if archetype == "adherent":
            # LOW RISK (0)
            med_count = np.random.randint(1, 4)
            daily_events = int(med_count * np.random.uniform(1.0, 2.0))
            missed_7d = int(np.random.choice([0, 1], p=[0.85, 0.15]))
            missed_30d = int(missed_7d + np.random.randint(0, 3))
            adherence_pct = float(np.clip(100.0 - (missed_30d / max(1, daily_events * 30)) * 100 + np.random.normal(0, 2), 85.0, 100.0))
            response_rate = float(np.clip(adherence_pct + np.random.normal(0, 3), 80.0, 100.0))
            consec_missed = 0 if missed_7d == 0 else 1
            complexity = float(np.round(med_count * 1.0 + np.random.uniform(0.1, 0.5), 2))
            label = 0

        elif archetype == "moderate":
            # MEDIUM RISK (1)
            med_count = np.random.randint(2, 6)
            daily_events = int(med_count * np.random.uniform(1.2, 2.5))
            missed_7d = int(np.random.choice([1, 2, 3], p=[0.5, 0.35, 0.15]))
            missed_30d = int(missed_7d * 3 + np.random.randint(1, 6))
            adherence_pct = float(np.clip(100.0 - (missed_30d / max(1, daily_events * 30)) * 100 + np.random.normal(0, 3), 60.0, 84.9))
            response_rate = float(np.clip(adherence_pct - np.random.uniform(2, 8), 55.0, 85.0))
            consec_missed = int(np.random.choice([0, 1, 2], p=[0.4, 0.45, 0.15]))
            complexity = float(np.round(med_count * 1.5 + np.random.uniform(0.5, 1.5), 2))
            label = 1

        else:
            # HIGH RISK (2)
            med_count = np.random.randint(4, 10)
            daily_events = int(med_count * np.random.uniform(2.0, 3.5))
            missed_7d = int(np.random.randint(3, 8))
            missed_30d = int(missed_7d * 3 + np.random.randint(5, 15))
            adherence_pct = float(np.clip(100.0 - (missed_30d / max(1, daily_events * 30)) * 100 + np.random.normal(0, 4), 25.0, 59.9))
            response_rate = float(np.clip(adherence_pct - np.random.uniform(5, 15), 20.0, 60.0))
            consec_missed = int(np.random.randint(2, 6))
            complexity = float(np.round(med_count * 2.0 + np.random.uniform(1.0, 3.0), 2))
            label = 2

        X_rows.append([
            med_count,
            daily_events,
            missed_7d,
            missed_30d,
            round(adherence_pct, 2),
            round(response_rate, 2),
            consec_missed,
            round(complexity, 2)
        ])
        y_rows.append(label)

    return np.array(X_rows, dtype=np.float32), np.array(y_rows, dtype=np.int64)

def train_and_evaluate():
    """Train the RandomForestClassifier and output performance metrics."""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(script_dir, "adherence_model.joblib")
    metrics_path = os.path.join(script_dir, "model_metrics.json")

    X, y = generate_synthetic_adherence_data(n_samples=1600, random_seed=42)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    clf = RandomForestClassifier(
        n_estimators=100,
        max_depth=6,
        min_samples_split=4,
        min_samples_leaf=2,
        random_state=42
    )

    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)

    acc = float(accuracy_score(y_test, y_pred))
    prec_macro = float(precision_score(y_test, y_pred, average="macro"))
    prec_weighted = float(precision_score(y_test, y_pred, average="weighted"))
    rec_macro = float(recall_score(y_test, y_pred, average="macro"))
    rec_weighted = float(recall_score(y_test, y_pred, average="weighted"))
    f1_mac = float(f1_score(y_test, y_pred, average="macro"))
    f1_wgt = float(f1_score(y_test, y_pred, average="weighted"))
    cm = confusion_matrix(y_test, y_pred).tolist()
    clf_report = classification_report(y_test, y_pred, target_names=TARGET_CLASSES, output_dict=True)

    # Feature importances
    feature_importances = dict(zip(FEATURE_NAMES, [round(float(v), 4) for v in clf.feature_importances_]))

    metrics = {
        "algorithm": "RandomForestClassifier",
        "n_estimators": 100,
        "max_depth": 6,
        "n_train_samples": len(X_train),
        "n_test_samples": len(X_test),
        "features": FEATURE_NAMES,
        "target_classes": TARGET_CLASSES,
        "accuracy": round(acc, 4),
        "precision_macro": round(prec_macro, 4),
        "precision_weighted": round(prec_weighted, 4),
        "recall_macro": round(rec_macro, 4),
        "recall_weighted": round(rec_weighted, 4),
        "f1_score_macro": round(f1_mac, 4),
        "f1_score_weighted": round(f1_wgt, 4),
        "confusion_matrix": cm,
        "feature_importances": feature_importances,
        "classification_report": clf_report,
        "disclaimer": "The model was initially trained and evaluated using synthetic medication-adherence data for application prototyping. The results do not constitute clinical validation."
    }

    # Save trained model and metrics
    joblib.dump(clf, model_path)
    with open(metrics_path, "w") as f:
        json.dump(metrics, f, indent=2)

    print("=====================================================")
    print("MEDICARE+ ADHERENCE ML MODEL TRAINING COMPLETED")
    print("=====================================================")
    print(f"Algorithm:           {metrics['algorithm']}")
    print(f"Accuracy:            {metrics['accuracy']:.4f}")
    print(f"Precision (macro):   {metrics['precision_macro']:.4f}")
    print(f"Recall (macro):      {metrics['recall_macro']:.4f}")
    print(f"F1-Score (macro):    {metrics['f1_score_macro']:.4f}")
    print("\nConfusion Matrix [LOW, MEDIUM, HIGH]:")
    for row in cm:
        print(" ", row)
    print("\nFeature Importances:")
    for feat, imp in sorted(feature_importances.items(), key=lambda x: x[1], reverse=True):
        print(f"  {feat:26s}: {imp:.4f}")
    print(f"\nModel saved to:   {model_path}")
    print(f"Metrics saved to: {metrics_path}")
    print("=====================================================")

    return metrics

if __name__ == "__main__":
    train_and_evaluate()
