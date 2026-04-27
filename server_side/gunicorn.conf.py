import multiprocessing

# Server socket
bind = "0.0.0.0:8000"
backlog = 2048

certfile = "/etc/letsencrypt/live/sensors.example.com/fullchain.pem"
keyfile = "/etc/letsencrypt/live/sensors.example\.com/privkey.pem"

# Worker processes
workers = 1
worker_class = 'gevent'
timeout = 30
threads = 2
# Logging
accesslog = "-"      # Log to stdout
errorlog = "-"       # Log to stderr
loglevel = "info"

# Process management
daemon = False
pidfile = "gunicorn.pid"