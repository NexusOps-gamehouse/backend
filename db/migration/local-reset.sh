#!/usr/bin/env bash
#
# 로컬 DB 를 application-secret.yml 에 맞춰 다시 만든다.
#
#   ./db/migration/local-reset.sh [postgres 컨테이너 이름]
#   ./db/migration/local-reset.sh --dry-run     # 실행 없이 SQL 만 보여준다
#
# [무엇을 하나]
#   application-secret.yml 에 적힌 계정 이름·비밀번호를 그대로 읽어서
#   user_svc / post_svc / chat_svc / match_svc / crew_svc 스키마와 duo_* 계정들을
#   지우고 다시 만든다. 앱이 뜨면서 ddl-auto: update 로 테이블을 새로 만든다.
#
# [왜 이런 게 필요한가]
#   서비스가 늘 때마다 DB 계정도 같이 는다. match 가 추가되면서 duo_match 가
#   필요해졌는데, 이미 부트스트랩을 끝낸 사람의 DB 에는 그 계정이 없다.
#   Postgres 는 "계정이 없음"과 "비밀번호가 틀림"을 똑같이
#   password authentication failed 로 알려줘서 원인을 구분할 수 없다.
#   그래서 맞춰보는 대신, 시크릿 파일을 기준으로 통째로 다시 만든다.
#
# ⚠️ 위 스키마의 데이터가 전부 사라진다. 로컬 개발용으로만 쓸 것.
#
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SECRET="$BACKEND_DIR/application-secret.yml"

[ -f "$SECRET" ] || { echo "application-secret.yml 이 없습니다: $SECRET" >&2; exit 1; }

# 최상위 키만 읽는다. 값의 앞뒤 공백과 따옴표를 벗겨낸다.
secret_value() {
  sed -n "s/^$1:[[:space:]]*//p" "$SECRET" | head -1 \
    | sed -e 's/[[:space:]]*$//' -e "s/^['\"]//" -e "s/['\"]$//"
}

DB_URL=$(secret_value DB_URL)
ADMIN_USER=$(secret_value DB_USERNAME)
ADMIN_PASS=$(secret_value DB_PASSWORD)

# jdbc:postgresql://localhost:5432/duo → host=localhost port=5432 db=duo
DB_NAME="${DB_URL##*/}"; DB_NAME="${DB_NAME%%\?*}"
HOSTPORT="${DB_URL#*//}"; HOSTPORT="${HOSTPORT%%/*}"
DB_HOST="${HOSTPORT%%:*}"
DB_PORT="${HOSTPORT##*:}"
[ "$DB_PORT" = "$DB_HOST" ] && DB_PORT=5432

: "${DB_NAME:=duo}"
: "${ADMIN_USER:=duo}"
: "${DB_HOST:=localhost}"

# 서비스 = 스키마 이름 : 계정 키 접두어
SERVICES="user:USER post:POST chat:CHAT match:MATCH crew:CREW"

# SQL 문자열 리터럴용 이스케이프 — 비밀번호에 작은따옴표가 있어도 깨지지 않게.
sql_quote() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/''/g")"; }

SQL=$'\\set ON_ERROR_STOP on\nBEGIN;\n'

for pair in $SERVICES; do
  svc="${pair%%:*}"; prefix="${pair##*:}"
  user=$(secret_value "${prefix}_DB_USERNAME")
  pass=$(secret_value "${prefix}_DB_PASSWORD")

  if [ -z "$user" ] || [ -z "$pass" ]; then
    echo "application-secret.yml 에 ${prefix}_DB_USERNAME / ${prefix}_DB_PASSWORD 가 없습니다." >&2
    exit 1
  fi

  # 스키마를 먼저 지운다. 계정이 소유한 객체가 남아 있으면 DROP ROLE 이 거부된다.
  SQL+="DROP SCHEMA IF EXISTS ${svc}_svc CASCADE;"$'\n'
  # 계정이 다른 스키마에 만들어둔 것까지 정리한 뒤에야 계정을 지울 수 있다.
  SQL+="DO \$\$ BEGIN IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${user}') THEN"
  SQL+=" EXECUTE 'REASSIGN OWNED BY ${user} TO ${ADMIN_USER}';"
  SQL+=" EXECUTE 'DROP OWNED BY ${user}';"
  SQL+=" EXECUTE 'DROP ROLE ${user}'; END IF; END \$\$;"$'\n'

  SQL+="CREATE USER ${user} WITH PASSWORD $(sql_quote "$pass");"$'\n'
  # AUTHORIZATION 이 핵심이다. 소유자가 아니면 ddl-auto: update 가 부팅 때
  # 테이블을 못 만들어, 접속은 되는데 그 다음에서 막힌다.
  SQL+="CREATE SCHEMA ${svc}_svc AUTHORIZATION ${user};"$'\n'
  # Hibernate 의 default_schema 는 Hibernate 가 만든 쿼리에만 적용된다.
  # 네이티브 쿼리까지 덮으려면 search_path 도 같이 걸어둔다.
  SQL+="ALTER ROLE ${user} SET search_path TO ${svc}_svc;"$'\n\n'
done

SQL+=$'REVOKE CREATE ON SCHEMA public FROM PUBLIC;\nCOMMIT;\n\n'
SQL+=$'\\echo \'=== 스키마와 소유자 ===\'\n'
SQL+=$'SELECT nspname AS schema, pg_get_userbyid(nspowner) AS owner\n'
SQL+=$'  FROM pg_namespace WHERE nspname LIKE \'%_svc\' ORDER BY 1;\n'

# 무엇이 실행될지 먼저 보고 싶을 때. 비밀번호가 그대로 찍히니 화면 공유 중엔 주의.
if [ "${1:-}" = "--dry-run" ]; then
  printf '%s' "$SQL"
  exit 0
fi

# 이미지 이름까지 본다. 컨테이너 이름이 'db' 처럼 붙어 있는 경우가 흔하다.
CONTAINER="${1:-$(docker ps --format '{{.Names}}\t{{.Image}}' 2>/dev/null \
  | grep -i -m1 postgres | cut -f1 || true)}"

echo "DB       : $DB_NAME @ $DB_HOST:$DB_PORT (admin: $ADMIN_USER)"
echo "계정     : $(for p in $SERVICES; do printf '%s ' "$(secret_value "${p##*:}_DB_USERNAME")"; done)"
echo "실행 대상: ${CONTAINER:+컨테이너 $CONTAINER}${CONTAINER:-psql → $DB_HOST:$DB_PORT}"
echo
read -r -p "위 스키마의 데이터가 전부 삭제됩니다. 진행할까요? [y/N] " ok
[ "$ok" = "y" ] || [ "$ok" = "Y" ] || { echo "취소했습니다."; exit 0; }

if [ -n "$CONTAINER" ]; then
  # 컨테이너 안에서는 유닉스 소켓으로 붙는다. 비밀번호가 필요 없는 경우가 많다.
  printf '%s' "$SQL" | docker exec -i "$CONTAINER" \
    psql -U "$ADMIN_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1
else
  # -h 를 반드시 준다. 빼면 psql 이 /tmp/.s.PGSQL.5432 유닉스 소켓을 찾는데,
  # Postgres 가 Docker 안에 있으면 그 소켓은 호스트에 존재하지 않는다.
  # (그때 나오는 메시지가 "No such file or directory" 다 — 서버가 죽은 게 아니다)
  printf '%s' "$SQL" | PGPASSWORD="$ADMIN_PASS" \
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$ADMIN_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1
fi

echo
echo "완료. 이제 각 서비스를 띄우면 자기 스키마 안에 테이블을 새로 만듭니다."
