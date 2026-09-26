"""
Gunicorn Configuration for Medicare Backend
Optimized for single-worker deployment with multi-threading to prevent OOM
and worker timeout on memory-constrained hosting environments (e.g. Render 512MB RAM).
"""
import os

# Server socket
port = os.environ.get("PORT", "5000")
bind = f"0.0.0.0:{port}"

# Worker processes: 1 worker prevents duplicate scikit-learn / scipy memory allocation
workers = 1

# Threads per worker: 4 threads handle concurrent requests within the single worker
threads = 4
worker_class = "gthread"

# Worker timeout: 120 seconds to provide headroom for external API calls and cold starts
timeout = 120
keepalive = 5

# Logging
accesslog = "-"
errorlog = "-"
loglevel = "info"
