#!/usr/bin/env bash
set -euo pipefail

target_url="${1:-http://localhost:8080}"
curl_options=(--silent --show-error)

if [[ "$target_url" == https://* ]]; then
  curl_options+=(--cacert "$(mkcert -CAROOT)/rootCA.pem")
fi

status="$(curl "${curl_options[@]}" "$target_url/api/labs/status")"
weak_result="$(curl "${curl_options[@]}" \
  --request POST \
  --header 'Content-Type: application/json' \
  --data '{"value":"same synthetic customer value"}' \
  "$target_url/api/labs/weak/encrypt-twice")"

echo "Runtime security observations"
echo "$status" | jq '{weakLabAlgorithm, hardcodedLabKeySource, requestScheme, secureTransport, database}'
echo "$weak_result" | jq '{algorithm, identical}'

