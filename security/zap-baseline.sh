#!/usr/bin/env bash
set -euo pipefail

target_url="${1:-http://localhost:8080}"
project_root="$(cd "$(dirname "$0")/.." && pwd)"
container_target="${target_url/localhost/host.docker.internal}"

mkdir -p "$project_root/reports"

echo "Scanning $container_target with the official ZAP stable container"
docker run --rm \
  --volume "$project_root:/zap/wrk:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py \
  -t "$container_target" \
  -c security/zap-rules.conf \
  -J reports/zap-report.json \
  -r reports/zap-report.html \
  -s
