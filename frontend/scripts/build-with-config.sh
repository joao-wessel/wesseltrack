#!/usr/bin/env sh
set -eu

API_BASE_URL_VALUE="${API_BASE_URL:-http://localhost:8080/api}"

cat > public/app-config.json <<EOF
{
  "apiBaseUrl": "${API_BASE_URL_VALUE}"
}
EOF

npm ci
npm run build

