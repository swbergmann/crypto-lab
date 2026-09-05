#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"

echo "Patterns that CodeQL/SonarQube should inspect"
rg --line-number \
  'AES/ECB|CUSTOMER_DATA_KEY|SecretKeySpec\(' \
  "$project_root/backend/src/main/java" \
  || true

echo
echo "Transport configuration"
rg --line-number \
  'port:|ssl:|Strict-Transport-Security' \
  "$project_root/backend/src/main/resources/application.yml" \
  "$project_root/backend/src/main/java" \
  || true

