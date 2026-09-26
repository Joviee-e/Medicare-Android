"""
Direct Verification Script for Medicare Adherence ML Model
Validates:
1. scikit-learn import and version
2. adherence_model.joblib loading time
3. Inference on a valid feature vector
"""

import sys
import time
import os

def run_verification():
    print("==================================================")
    print("MEDICARE ML MODEL DIRECT VERIFICATION")
    print("==================================================")

    # 1. Import sklearn
    t_start = time.perf_counter()
    import sklearn
    import joblib
    import numpy as np
    t_import = time.perf_counter()
    import_time_ms = (t_import - t_start) * 1000
    print(f"sklearn version:        {sklearn.__version__}")
    print(f"joblib version:         {joblib.__version__}")
    print(f"Import time:            {import_time_ms:.2f} ms")

    # 2. Locate and load model
    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(script_dir, "ml", "adherence_model.joblib")
    if not os.path.exists(model_path):
        print(f"ERROR: Model file not found at {model_path}")
        sys.exit(1)

    t_load_start = time.perf_counter()
    model = joblib.load(model_path)
    t_load_end = time.perf_counter()
    load_time_ms = (t_load_end - t_load_start) * 1000
    print(f"Model file:             {model_path}")
    print(f"Model load time:        {load_time_ms:.2f} ms")

    # 3. Predict one valid feature vector
    # Features: [medication_count, daily_medication_events, missed_doses_7d, missed_doses_30d, adherence_percentage, reminder_response_rate, consecutive_missed_days, medication_complexity]
    test_feature_vector = [2, 3, 0, 1, 95.0, 95.0, 0, 2.5]
    X = np.array([test_feature_vector], dtype=np.float32)

    t_infer_start = time.perf_counter()
    prediction = model.predict(X)
    probabilities = model.predict_proba(X)
    t_infer_end = time.perf_counter()
    infer_time_ms = (t_infer_end - t_infer_start) * 1000

    target_map = {0: "LOW", 1: "MEDIUM", 2: "HIGH"}
    pred_label = target_map.get(int(prediction[0]), "UNKNOWN")

    print(f"Inference time:         {infer_time_ms:.2f} ms")
    print(f"Predicted class:        {prediction[0]} ({pred_label})")
    print(f"Class probabilities:    {probabilities[0]}")
    print("==================================================")
    print(f"VERIFICATION STATUS:    SUCCESS")
    print("==================================================")

    # Sanity checks
    assert load_time_ms < 5000, f"Model loading took too long ({load_time_ms} ms)"
    assert pred_label in ["LOW", "MEDIUM", "HIGH"], f"Invalid prediction label: {pred_label}"
    return {
        "sklearn_version": sklearn.__version__,
        "import_time_ms": round(import_time_ms, 2),
        "load_time_ms": round(load_time_ms, 2),
        "infer_time_ms": round(infer_time_ms, 2),
        "predicted_label": pred_label
    }

if __name__ == "__main__":
    run_verification()
