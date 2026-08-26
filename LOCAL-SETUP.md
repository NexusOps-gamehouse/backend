# 로컬 실행 가이드 (MSA 구조)

브랜치 `msa-structure` · 백엔드가 앱 1개 → **4개**로 나뉘었습니다.

```
Before                      After
DuoApplication :8080        UserApplication :8081   인증·프로필·친구·알림
                            PostApplication :8082   모집글·지원
                            ChatApplication :8083   채팅·WebSocket
                            RiotApplication :8084   라이엇 API (내부 전용)
```

**먼저 알아둘 것 4가지**

1. `DuoApplication`은 없어졌습니다. 기존 Run Configuration을 삭제하세요.
2. **4개를 다 띄워야** 정상 동작합니다.
3. `application-secret.yml`이 `src/main/resources/` → **`backend/` 루트**로 이동했습니다.
4. DB에 **스키마 3개 + 계정 3개**가 필요합니다. (서비스별 데이터 소유권 분리)

---

## 0. 준비물

| | 확인 |
|---|---|
| JDK 17 | `java -version` |
| Docker | `docker ps` |
| PostgreSQL (localhost:5432) | `lsof -i :5432` |

---

## 1. 코드 받기 + Gradle 재임포트

```bash
cd ~/github/gamehouse/backend
git fetch origin && git checkout msa-structure
```

**IntelliJ에서 Gradle 재임포트가 필수입니다.** `settings.gradle`이 멀티모듈로 바뀌어서,
재임포트 전에는 새 폴더들이 그냥 회색 폴더로 보입니다.

> Gradle 툴윈도우 → **⟳ Reload All Gradle Projects**
> 안 되면 `File → Invalidate Caches → Invalidate and Restart`

**확인** — 프로젝트 뷰에 모듈 5개(`common` `user` `post` `chat` `riot`)가 파란 아이콘이면 성공.

```bash
./gradlew build -x test     # BUILD SUCCESSFUL
```

---

## 2. PostgreSQL

이미 떠 있으면 건너뛰세요.

```bash
lsof -i :5432
```

없으면:

```bash
docker run -d --name gamehouse-db \
  -e POSTGRES_DB=duo -e POSTGRES_USER=duo \
  -e POSTGRES_PASSWORD='아무거나' \
  -p 5432:5432 postgres:16
```

### 스키마 · 계정 만들기 ⭐ 새로 생긴 단계

```bash
C=gamehouse-db      # 컨테이너 이름. docker ps 로 확인

docker exec -i $C psql -U duo -d duo -v ON_ERROR_STOP=1 \
  < db/migration/local-bootstrap.sql
```

스키마 3개(`user_svc` `post_svc` `chat_svc`)와 계정 3개(`duo_user` `duo_post` `duo_chat`)가 만들어집니다.
**테이블은 앱이 뜨면서 자동으로 생깁니다.**

> 이미 데이터가 있는 DB라면 `local-bootstrap.sql` 대신 `V1__split_schemas.sql`을 쓰세요.
> 그건 기존 테이블을 옮기는 스크립트입니다.

---

## 3. 시크릿 파일

```bash
cp application-secret-example.yml application-secret.yml
```

`JWT_SECRET`, `riot.api.key`, `DB_PASSWORD`만 실제 값으로 채우세요.
**DB 계정 3개와 RabbitMQ 계정은 예시 기본값 그대로 두면 됩니다** —
`local-bootstrap.sql`이 같은 값으로 계정을 만듭니다.

---

## 4. RabbitMQ

`chat`이 필요로 합니다. 포트 두 개를 씁니다.

```
AMQP  :5672    서비스 간 이벤트 (post ↔ chat ↔ user)
STOMP :61613   채팅 메시지 릴레이 (브라우저 ↔ chat)
```

```bash
cd ~/github/gamehouse/infra
cp .env.example .env.local          # 없으면. RABBITMQ_* 값 채우기
./scripts/local.sh up -d --build rabbitmq
```

`local.sh`는 `--env-file .env.local`과 `-f docker-compose.local.yml`을 대신 붙여주는 래퍼입니다.
**둘 다 없으면 포트가 안 열립니다.**

```bash
lsof -i :5672 && lsof -i :61613
open http://localhost:15672         # 관리 UI
```

> 브로커 계정이 안 맞으면 `ACCESS_REFUSED`가 납니다. 그럴 땐:
> ```bash
> docker exec $(docker ps -qf name=rabbitmq) \
>   rabbitmqctl change_password appuser apppass
> ```

---

## 5. 실행

기존 `DuoApplication` Run Configuration을 지우고, 각 파일의 ▶ 를 눌러 새로 만드세요.

```
riot/src/main/java/gg/duo/riot/RiotApplication.java     ← 먼저 (DB 불필요)
user/src/main/java/gg/duo/user/UserApplication.java
post/src/main/java/gg/duo/post/PostApplication.java
chat/src/main/java/gg/duo/chat/ChatApplication.java
```

터미널이 편하면 탭 4개로:

```bash
./gradlew :riot:bootRun
./gradlew :user:bootRun
./gradlew :post:bootRun
./gradlew :chat:bootRun
```

**성공 로그**

```
Tomcat started on port 8081 (http)
Started UserApplication in 3.2 seconds
CachingConnectionFactory : Created new connection: ... 5672    ← 이벤트 연결
```

chat은 한 줄 더:

```
StompBrokerRelayMessageHandler : "System" session connected    ← 채팅 릴레이
```

---

## 6. 프론트

```bash
cd ~/github/gamehouse/frontend
git checkout msa-structure
npm install && npm run dev          # localhost:5173
```

`vite.config.js`가 경로별로 갈라줍니다. **프론트 코드는 바뀐 게 없습니다.**

```
/api/auth · /api/users · /api/friends · /api/notifications · /uploads → user :8081
/api/posts · /api/applications · /api/my                             → post :8082
/api/chat · /ws                                                      → chat :8083
```

---

## 7. 확인

```bash
curl -s localhost:8181/actuator/health    # user
curl -s localhost:8182/actuator/health    # post
curl -s localhost:8183/actuator/health    # chat
curl -s localhost:8184/actuator/health    # riot
```

앱 포트는 **8081~8084**, health는 **8181~8184**입니다.

### 동작 확인

`localhost:5173`에서 회원가입 → 글쓰기 → **새로고침**.

DevTools Network에서 `GET /api/posts/{id}` 응답의 **`chatRoomId`에 숫자**가 있으면 성공입니다.

```
post: 글 저장 → RabbitMQ → chat: 방 생성 → RabbitMQ → post: chat_room_id 기록
```

> 글 작성 **직후** 응답의 `chatRoomId`가 `null`인 건 정상입니다. 비동기라 왕복에 시간이 걸립니다.

### 경계 확인 (선택)

```bash
docker exec -it $C psql -U duo_post -d duo
```
```sql
select count(*) from posts;              -- ✅ 됨
select count(*) from user_svc.users;     -- ❌ permission denied  ← 이게 정상
```

---

## 자주 만나는 에러

| 증상 | 원인 | 해결 |
|---|---|---|
| `ClassNotFoundException: gg.duo.DuoApplication` | 옛 Run Configuration | 삭제 후 재생성 |
| 모듈 폴더가 회색 | Gradle 재임포트 안 함 | Gradle ⟳ |
| `Connection refused :5432` | Postgres 미기동 | 2번 |
| `permission denied for schema` | 스키마·계정 미생성 | `local-bootstrap.sql` 실행 |
| `Connection refused :5672` | RabbitMQ 포트 미공개 | `local.sh` 사용 (`-f local.yml` 필수) |
| `ACCESS_REFUSED` (RabbitMQ) | 브로커 계정 불일치 | `rabbitmqctl change_password` |
| 라이엇 연동 "연결할 수 없습니다" | riot 미기동 | **정상 동작.** riot 띄우면 됨 |

마지막 항목이 중요합니다. **riot이 죽어도 로그인·글쓰기·채팅은 됩니다.** 서비스를 나눈 목적(장애 격리)이 동작하는 겁니다.

## 무시해도 되는 로그

```
ERROR ... MacOSDnsServerAddressStreamProvider ...
```
macOS Netty 경고. ERROR로 찍히지만 무해하고, 리눅스에선 안 나옵니다.

```
WARN ... Using generated security password: xxxx
```
Spring Security 개발용 임시 계정. JWT만 쓰므로 사용되지 않습니다.
