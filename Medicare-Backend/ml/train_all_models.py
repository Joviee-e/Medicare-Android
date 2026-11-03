"""
Medicare Training Pipeline for 3 Interconnected ML Components:
1. Medication Adherence Prediction (Random Forest Classifier)
2. Medication Routine Analysis (K-Means Clustering)
3. Smart Reminder Optimization (Delay Regression / Response Pattern Mining)

Evaluates on synthetic patient profiles using patient-level train/test splits (zero data leakage).
Serializes models to joblib artifacts and saves evaluation metrics to JSON.
"""

import os
import json
import numpy as np
from datetime import datetime, timezone
import joblib

from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report,
    silhouette_score,
    mean_absolute_error,
    r2_score
)

# Random seed for full reproducibility
SEED = 42
np.random.seed(SEED)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# -----------------------------------------------------------------------------
# 1. SYNTHETIC PATIENT DATASET GENERATOR
# Generates realistic patient behavioral histories across 4 distinct archetypes:
# - Consistent Users (~40%)
# - Occasionally Delayed / Midday Snoozers (~25%)
# - Irregular / Weekend Drop-off Users (~20%)
# - Frequently Missing / High Risk Users (~15%)
# -----------------------------------------------------------------------------

def generate_patient_profiles(n_patients: int = 1200):
    """
    Generates synthetic patient behavioral summaries representing 30-90 days of logs.
    Features:
      0: historical_adherence_pct (0 - 100)
      1: missed_doses_total (int)
      2: delayed_doses_total (int)
      3: avg_delay_minutes (float)
      4: reminder_response_latency_sec (float)
      5: morning_adherence_pct (0 - 100)
      6: afternoon_adherence_pct (0 - 100)
      7: evening_adherence_pct (0 - 100)
      8: weekend_adherence_pct (0 - 100)
      9: recent_missed_7d (int)
    Targets:
      y_adherence_risk: 0: Low Risk, 1: Moderate Risk, 2: High Risk
      y_future_miss_prob: continuous probability (0.0 to 1.0) of missing an upcoming dose
    """
    profiles = []
    archetypes = ["consistent", "delayed", "irregular_weekend", "frequent_miss"]
    probabilities = [0.40, 0.25, 0.20, 0.15]

    for pid in range(n_patients):
        arch = np.random.choice(archetypes, p=probabilities)

        if arch == "consistent":
            # Highly adherent, minimal delay, quick reminder response
            morn = float(np.clip(np.random.normal(96, 3), 85, 100))
            aft = float(np.clip(np.random.normal(94, 4), 80, 100))
            eve = float(np.clip(np.random.normal(92, 5), 75, 100))
            wknd = float(np.clip(np.random.normal(93, 4), 78, 100))
            hist_adh = float(round((morn + aft + eve + wknd) / 4.0, 1))
            missed_tot = int(np.random.poisson(1.5))
            delayed_tot = int(np.random.poisson(3.0))
            avg_delay = float(np.clip(np.random.normal(6.0, 2.5), 1.0, 18.0))
            resp_lat = float(np.clip(np.random.normal(25.0, 8.0), 5.0, 60.0))
            rec_miss = 1 if (missed_tot > 0 and np.random.rand() < 0.2) else 0
            risk_label = 0 # Low Risk
            miss_prob = float(np.clip(np.random.normal(0.08, 0.03), 0.02, 0.18))

        elif arch == "delayed":
            # Takes medicine but frequently delays / snoozes, especially afternoon
            morn = float(np.clip(np.random.normal(88, 5), 70, 98))
            aft = float(np.clip(np.random.normal(72, 8), 50, 88))
            eve = float(np.clip(np.random.normal(82, 6), 65, 95))
            wknd = float(np.clip(np.random.normal(76, 7), 55, 92))
            hist_adh = float(round((morn + aft + eve + wknd) / 4.0, 1))
            missed_tot = int(np.random.poisson(4.5))
            delayed_tot = int(np.random.poisson(14.0))
            avg_delay = float(np.clip(np.random.normal(38.0, 9.0), 18.0, 75.0))
            resp_lat = float(np.clip(np.random.normal(140.0, 30.0), 50.0, 300.0))
            rec_miss = int(np.random.choice([0, 1, 2], p=[0.5, 0.35, 0.15]))
            risk_label = 1 # Moderate Risk
            miss_prob = float(np.clip(np.random.normal(0.28, 0.05), 0.18, 0.45))

        elif arch == "irregular_weekend":
            # Good weekday morning adherence, but significant evening & weekend drop-off
            morn = float(np.clip(np.random.normal(90, 5), 75, 98))
            aft = float(np.clip(np.random.normal(78, 7), 60, 90))
            eve = float(np.clip(np.random.normal(60, 9), 38, 80))
            wknd = float(np.clip(np.random.normal(54, 9), 32, 75))
            hist_adh = float(round((morn + aft + eve + wknd) / 4.0, 1))
            missed_tot = int(np.random.poisson(8.0))
            delayed_tot = int(np.random.poisson(9.0))
            avg_delay = float(np.clip(np.random.normal(45.0, 12.0), 15.0, 90.0))
            resp_lat = float(np.clip(np.random.normal(180.0, 45.0), 60.0, 450.0))
            rec_miss = int(np.random.choice([1, 2, 3], p=[0.4, 0.4, 0.2]))
            risk_label = 1 # Moderate Risk (or boundary High)
            if hist_adh < 68.0 or rec_miss >= 3:
                risk_label = 2
            miss_prob = float(np.clip(np.random.normal(0.38, 0.07), 0.25, 0.58))

        else: # frequent_miss
            # Low compliance across the board, frequent missed doses, delayed response
            morn = float(np.clip(np.random.normal(62, 10), 30, 80))
            aft = float(np.clip(np.random.normal(48, 10), 20, 70))
            eve = float(np.clip(np.random.normal(42, 11), 15, 65))
            wknd = float(np.clip(np.random.normal(40, 11), 10, 62))
            hist_adh = float(round((morn + aft + eve + wknd) / 4.0, 1))
            missed_tot = int(np.random.poisson(16.0))
            delayed_tot = int(np.random.poisson(12.0))
            avg_delay = float(np.clip(np.random.normal(70.0, 18.0), 30.0, 150.0))
            resp_lat = float(np.clip(np.random.normal(320.0, 70.0), 120.0, 700.0))
            rec_miss = int(np.random.randint(3, 8))
            risk_label = 2 # High Risk
            miss_prob = float(np.clip(np.random.normal(0.68, 0.08), 0.48, 0.92))

        feature_vector = [
            round(hist_adh, 1),
            int(missed_tot),
            int(delayed_tot),
            round(avg_delay, 1),
            round(resp_lat, 1),
            round(morn, 1),
            round(aft, 1),
            round(eve, 1),
            round(wknd, 1),
            int(rec_miss)
        ]

        profiles.append({
            "patient_id": f"synthetic_patient_{pid}",
            "archetype": arch,
            "features": feature_vector,
            "risk_label": risk_label,
            "miss_prob": round(miss_prob, 3)
        })

    return profiles

# Feature name reference
FEATURE_NAMES_ADHERENCE = [
    "historical_adherence_pct",
    "missed_doses_total",
    "delayed_doses_total",
    "avg_delay_minutes",
    "reminder_response_latency_sec",
    "morning_adherence_pct",
    "afternoon_adherence_pct",
    "evening_adherence_pct",
    "weekend_adherence_pct",
    "recent_missed_7d"
]

FEATURE_NAMES_ROUTINE = [
    "morning_adherence_pct",
    "afternoon_adherence_pct",
    "evening_adherence_pct",
    "weekend_adherence_pct",
    "avg_delay_minutes",
    "missed_doses_total",
    "reminder_response_latency_sec"
]

# -----------------------------------------------------------------------------
# 2. MODEL 1: ADHERENCE PREDICTION (Random Forest Classifier)
# -----------------------------------------------------------------------------
def train_adherence_model(profiles):
    print("\n--- Training Model 1: Random Forest Adherence Predictor ---")
    X = np.array([p["features"] for p in profiles], dtype=np.float32)
    y = np.array([p["risk_label"] for p in profiles], dtype=np.int64)

    # Patient-level 80/20 train/test split to prevent data leakage
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=SEED, stratify=y
    )

    clf = RandomForestClassifier(
        n_estimators=120,
        max_depth=7,
        min_samples_split=4,
        min_samples_leaf=2,
        random_state=SEED
    )
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)
    y_prob = clf.predict_proba(X_test)

    acc = float(accuracy_score(y_test, y_pred))
    prec_macro = float(precision_score(y_test, y_pred, average="macro"))
    prec_weighted = float(precision_score(y_test, y_pred, average="weighted"))
    rec_macro = float(recall_score(y_test, y_pred, average="macro"))
    rec_weighted = float(recall_score(y_test, y_pred, average="weighted"))
    f1_mac = float(f1_score(y_test, y_pred, average="macro"))
    f1_wgt = float(f1_score(y_test, y_pred, average="weighted"))
    cm = confusion_matrix(y_test, y_pred).tolist()
    target_names = ["Low", "Moderate", "High"]
    clf_rep = classification_report(y_test, y_pred, target_names=target_names, output_dict=True)

    feature_importances = dict(zip(
        FEATURE_NAMES_ADHERENCE,
        [round(float(v), 4) for v in clf.feature_importances_]
    ))

    print(f"Adherence Model Accuracy:  {acc:.4f}")
    print(f"F1-Score (Macro):          {f1_mac:.4f}")
    print(f"Confusion Matrix [Low, Moderate, High]:")
    for row in cm:
        print(f"  {row}")

    model_path = os.path.join(SCRIPT_DIR, "adherence_classifier.joblib")
    joblib.dump(clf, model_path)
    print(f"Saved: {model_path}")

    return {
        "model_file": "adherence_classifier.joblib",
        "algorithm": "RandomForestClassifier",
        "n_estimators": 120,
        "max_depth": 7,
        "n_train_samples": len(X_train),
        "n_test_samples": len(X_test),
        "accuracy": round(acc, 4),
        "precision_macro": round(prec_macro, 4),
        "precision_weighted": round(prec_weighted, 4),
        "recall_macro": round(rec_macro, 4),
        "recall_weighted": round(rec_weighted, 4),
        "f1_macro": round(f1_mac, 4),
        "f1_weighted": round(f1_wgt, 4),
        "confusion_matrix": cm,
        "feature_importances": feature_importances,
        "classification_report": clf_rep
    }

# -----------------------------------------------------------------------------
# 3. MODEL 2: ROUTINE ANALYSIS (K-Means Clustering)
# -----------------------------------------------------------------------------
def train_routine_clustering(profiles):
    print("\n--- Training Model 2: K-Means Routine Analyzer ---")
    # Subselect routine features: morning, afternoon, evening, weekend, avg_delay, missed_doses, resp_latency
    routine_indices = [5, 6, 7, 8, 3, 1, 4]
    X_raw = np.array([[p["features"][i] for i in routine_indices] for p in profiles], dtype=np.float32)

    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X_raw)

    k = 4
    kmeans = KMeans(n_clusters=k, random_state=SEED, n_init=15)
    cluster_labels = kmeans.fit_predict(X_scaled)
    sil_score = float(silhouette_score(X_scaled, cluster_labels))

    print(f"K-Means Silhouette Score (k=4): {sil_score:.4f}")

    # Inspect cluster centers in unscaled space to map clusters to meaningful labels
    centroids_unscaled = scaler.inverse_transform(kmeans.cluster_centers_)
    
    cluster_profiles = []
    cluster_mapping = {}

    for c_id in range(k):
        c_size = int(np.sum(cluster_labels == c_id))
        c_mean = centroids_unscaled[c_id]
        morn_m, aft_m, eve_m, wknd_m, delay_m, missed_m, lat_m = c_mean

        overall_adh = (morn_m + aft_m + eve_m + wknd_m) / 4.0

        cluster_profiles.append({
            "cluster_id": c_id,
            "size": c_size,
            "overall_adherence": float(round(overall_adh, 1)),
            "morning": float(round(morn_m, 1)),
            "afternoon": float(round(aft_m, 1)),
            "evening": float(round(eve_m, 1)),
            "weekend": float(round(wknd_m, 1)),
            "avg_delay": float(round(delay_m, 1)),
            "missed_doses": float(round(missed_m, 1)),
            "response_latency": float(round(lat_m, 1))
        })

    # Sort clusters by overall adherence descending to deterministically assign clinical labels
    sorted_by_adh = sorted(cluster_profiles, key=lambda x: x["overall_adherence"], reverse=True)

    # 1st highest adherence: Consistent Routine
    c_consistent = sorted_by_adh[0]["cluster_id"]
    cluster_mapping[c_consistent] = {
        "title": "Consistent Routine",
        "description": "High adherence across all dose times with prompt reminder responses and minimal delay."
    }

    # 4th lowest adherence: Frequently Missed Doses
    c_frequent_miss = sorted_by_adh[3]["cluster_id"]
    cluster_mapping[c_frequent_miss] = {
        "title": "Frequently Missed Doses",
        "description": "Noticeable adherence challenges across morning, afternoon, and evening schedules with higher miss rates."
    }

    # Between 2nd and 3rd: check whether delay or weekend drop is more prominent
    mid_a = sorted_by_adh[1]
    mid_b = sorted_by_adh[2]

    # The one with larger delay is "Occasionally Delayed"; the one with larger weekend drop is "Irregular Weekend Routine"
    if mid_a["avg_delay"] >= mid_b["avg_delay"]:
        c_delayed = mid_a["cluster_id"]
        c_irregular = mid_b["cluster_id"]
    else:
        c_delayed = mid_b["cluster_id"]
        c_irregular = mid_a["cluster_id"]

    cluster_mapping[c_delayed] = {
        "title": "Occasionally Delayed",
        "description": "Doses are generally taken but frequently delayed or snoozed past the scheduled time."
    }
    cluster_mapping[c_irregular] = {
        "title": "Irregular Routine / Weekend Drop-off",
        "description": "Consistent on weekday mornings, but adherence drops significantly during evenings and weekends."
    }

    for cp in cluster_profiles:
        cp["label"] = cluster_mapping[cp["cluster_id"]]["title"]
        cp["description"] = cluster_mapping[cp["cluster_id"]]["description"]
        print(f"  Cluster {cp['cluster_id']} ({cp['size']} patients): {cp['label']} - Adh: {cp['overall_adherence']}% - Delay: {cp['avg_delay']} min")

    # Save artifacts
    model_path = os.path.join(SCRIPT_DIR, "routine_kmeans.joblib")
    scaler_path = os.path.join(SCRIPT_DIR, "routine_scaler.joblib")
    joblib.dump(kmeans, model_path)
    joblib.dump(scaler, scaler_path)

    return {
        "model_file": "routine_kmeans.joblib",
        "scaler_file": "routine_scaler.joblib",
        "algorithm": "KMeans",
        "k_clusters": k,
        "silhouette_score": round(sil_score, 4),
        "cluster_profiles": cluster_profiles,
        "cluster_mapping": cluster_mapping
    }

# -----------------------------------------------------------------------------
# 4. MODEL 3: SMART REMINDER OPTIMIZER (Regression & Response Timing)
# -----------------------------------------------------------------------------
def train_reminder_optimizer(n_samples: int = 1500):
    print("\n--- Training Model 3: Smart Reminder Timing Optimizer ---")
    # Generates reminder events modeling response delay based on hour of day, day of week, and historical routine
    # Features:
    # 0: scheduled_hour (0-23)
    # 1: is_weekend (0 or 1)
    # 2: historical_user_adherence (0-100)
    # 3: time_bucket_code (0: morning, 1: afternoon, 2: evening, 3: night)
    # Target: expected_delay_minutes (regression) and acknowledgement_probability (continuous)
    X_rows = []
    y_delay = []

    for _ in range(n_samples):
        hour = np.random.choice([8, 9, 12, 13, 18, 19, 20, 21, 22])
        is_wknd = int(np.random.choice([0, 1], p=[0.71, 0.29]))
        user_adh = float(np.random.uniform(50.0, 98.0))

        if hour < 12:
            bucket = 0 # Morning
            base_delay = 5.0 if is_wknd == 0 else 12.0
        elif hour < 17:
            bucket = 1 # Afternoon
            base_delay = 25.0 if is_wknd == 0 else 35.0
        elif hour < 22:
            bucket = 2 # Evening
            base_delay = 35.0 if is_wknd == 0 else 55.0
        else:
            bucket = 3 # Night
            base_delay = 45.0 if is_wknd == 0 else 65.0

        adh_factor = (100.0 - user_adh) * 0.4
        delay = float(np.clip(np.random.normal(base_delay + adh_factor, 10.0), 1.0, 180.0))

        X_rows.append([hour, is_wknd, round(user_adh, 1), bucket])
        y_delay.append(round(delay, 1))

    X = np.array(X_rows, dtype=np.float32)
    y = np.array(y_delay, dtype=np.float32)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=SEED)

    reg = RandomForestRegressor(n_estimators=100, max_depth=6, random_state=SEED)
    reg.fit(X_train, y_train)

    y_pred = reg.predict(X_test)
    mae = float(mean_absolute_error(y_test, y_pred))
    r2 = float(r2_score(y_test, y_pred))

    print(f"Reminder Optimizer MAE: {mae:.2f} minutes, R2: {r2:.4f}")

    model_path = os.path.join(SCRIPT_DIR, "reminder_optimizer.joblib")
    joblib.dump(reg, model_path)
    print(f"Saved: {model_path}")

    return {
        "model_file": "reminder_optimizer.joblib",
        "algorithm": "RandomForestRegressor",
        "n_estimators": 100,
        "max_depth": 6,
        "mean_absolute_error_minutes": round(mae, 2),
        "r2_score": round(r2, 4)
    }

# -----------------------------------------------------------------------------
# MAIN EXECUTION
# -----------------------------------------------------------------------------
def train_and_export_all():
    print("===================================================================")
    print("MEDICARE PLUS: 3-COMPONENT ML TRAINING & EVALUATION PIPELINE")
    print("===================================================================")

    profiles = generate_patient_profiles(n_patients=1400)
    print(f"Generated {len(profiles)} synthetic patient behavioral records.")

    m1_metrics = train_adherence_model(profiles)
    m2_metrics = train_routine_clustering(profiles)
    m3_metrics = train_reminder_optimizer()

    master_metrics = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "disclaimer": "Models were trained and evaluated on synthetic patient behavioral histories modeled from clinical pharmacotherapy adherence literature. Results do not substitute clinical diagnostic validation.",
        "adherence_prediction": m1_metrics,
        "routine_clustering": m2_metrics,
        "reminder_optimization": m3_metrics
    }

    metrics_path = os.path.join(SCRIPT_DIR, "ml_metrics.json")
    with open(metrics_path, "w") as f:
        json.dump(master_metrics, f, indent=2)

    print(f"\nAll models trained and exported successfully!")
    print(f"Metrics saved to: {metrics_path}")
    print("===================================================================")

    return master_metrics

if __name__ == "__main__":
    train_and_export_all()

