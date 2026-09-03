#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"
SERVER_PORT_OVERRIDE="${SERVER_PORT:-}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Eksik local env dosyası: ${ENV_FILE}"
  echo "Başlangıç için: cp .env.example .env"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

if [[ -n "${SERVER_PORT_OVERRIDE}" ]]; then
  export SERVER_PORT="${SERVER_PORT_OVERRIDE}"
fi

required_variables=(
  DB_URL
  DB_USERNAME
  DB_PASSWORD
  JWT_SECRET
  PROFILE_ENCRYPTION_KEY
  CORS_ALLOWED_ORIGIN
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Zorunlu değişken boş: ${variable_name}"
    exit 1
  fi
done

if (( ${#JWT_SECRET} < 32 )); then
  echo "JWT_SECRET en az 32 karakter olmalıdır."
  exit 1
fi

if [[ "${MAIL_SMTP_AUTH:-false}" == "true" ]]; then
  for variable_name in MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD MAIL_FROM; do
    if [[ -z "${!variable_name:-}" ]]; then
      echo "SMTP doğrulaması açıkken zorunlu değişken boş: ${variable_name}"
      exit 1
    fi
  done
fi

cd "${SCRIPT_DIR}"
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
