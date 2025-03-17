#!/bin/sh
echo "Starting COMPUTING $1 WORKERS=$WORKERS TIMEOUT=$TIMEOUT PORT=$PORT"

exec gunicorn --workers=${WORKERS:-2} --timeout=${TIMEOUT:-1} --bind=0.0.0.0:${PORT:-8000} --enable-stdio-inheritance $1 
