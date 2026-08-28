#!/usr/bin/env bash
#
# crew 의 DB 계정·스키마를 만든다. (crew 는 아직 backend 레포에 있다)
#
#   ./db/init-crew.sh
#
# 비밀번호는 application-secret.yml 의 CREW_DB_PASSWORD 에서 읽는다.
# 스크립트에도 SQL 에도 값이 들어 있지 않다.
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECRET="$REPO/application-secret.yml"
[ -f "$SECRET" ] || { echo "application-secret.yml 이 없습니다: $SECRET" >&2; exit 1; }

secret_value() {
  sed -n "s/^$1:[[:space:]]*//p" "$SECRET" | head -1 \
    | sed -e 's/[[:space:]]*$//' -e "s/^['\"]//" -e "s/['\"]$//"
}

DB_URL=$(secret_value DB_URL)
HOSTPORT="${DB_URL#*//}"; HOSTPORT="${HOSTPORT%%/*}"
URL_HOST="${HOSTPORT%%:*}"; URL_PORT="${HOSTPORT##*:}"
[ "$URL_PORT" = "$URL_HOST" ] && URL_PORT=5432
DB_NAME="${DB_URL##*/}"; DB_NAME="${DB_NAME%%\?*}"

DB_HOST="${1:-${URL_HOST:-localhost}}"
DB_PORT="${2:-${URL_PORT:-5432}}"
: "${DB_NAME:=duo}"

ADMIN_USER=$(secret_value DB_USERNAME); : "${ADMIN_USER:=duo}"
ADMIN_PASS=$(secret_value DB_PASSWORD)
SVC_PASS=$(secret_value CREW_DB_PASSWORD)
[ -n "$SVC_PASS" ] || { echo "application-secret.yml 에 CREW_DB_PASSWORD 가 없습니다." >&2; exit 1; }

# ⚠️ 값을 날것으로 넘긴다. init-crew.sql 의 :'svc_pw' 가 SQL 리터럴로 감싸주므로
# 여기서 따옴표를 씌우면 비밀번호에 따옴표가 포함돼 저장된다.
PW_ARG="svc_pw=$SVC_PASS"

# postgres 가 Docker 안에 있으면 컨테이너 안에서 돈다 — 유닉스 소켓이라
# 관리 계정 비밀번호가 필요 없다.
CONTAINER="${CONTAINER:-$(docker ps --format '{{.Names}}\t{{.Image}}' 2>/dev/null \
  | grep -i -m1 postgres | cut -f1 || true)}"

if [ -n "$CONTAINER" ]; then
  echo "대상: ${DB_NAME} (컨테이너 ${CONTAINER}, 관리 계정 ${ADMIN_USER})"
  docker exec -i "$CONTAINER" \
    psql -U "$ADMIN_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -v "$PW_ARG" \
    < "$REPO/db/init-crew.sql"
else
  echo "대상: ${DB_NAME} @ ${DB_HOST}:${DB_PORT} (관리 계정 ${ADMIN_USER})"
  PGPASSWORD="$ADMIN_PASS" psql \
    -h "$DB_HOST" -p "$DB_PORT" -U "$ADMIN_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -v "$PW_ARG" -f "$REPO/db/init-crew.sql"
fi
