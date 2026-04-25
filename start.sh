#!/bin/bash
set -e
PORT=${1:-8080}
PORT=$PORT docker compose up --build -d
echo "Stock Market Simulator running at http://localhost:$PORT"
