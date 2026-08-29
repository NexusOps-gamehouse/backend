# crew (House) API

베이스 URL `http://localhost:8086`
인증 헤더 `Authorization: Bearer <JWT>`
에러 응답 `{"message": "..."}`

| API | Request (보내는 값) | Response (받는 값) | 용도 |
| --- | --- | --- | --- |
| **로그인** (user :8081) | `email`, `password` | `token`, `user` | 토큰 획득 |
| **House 목록** | 없음 | `id`, `name`, `type`, `memberCount`, `myStatus` … | 목록 화면 |
| **House 상세** | `houseId` | 위 + `members[]`, `pendingCount` | 상세 화면 |
| **House 생성** | `name`, `description`, `type`, `maxMembers` | `Detail` | 생성 |
| **House 수정** | `houseId` + 수정할 필드 | `Detail` | 설정 변경 |
| **내 House** | 없음 | `Summary[]` | 내 목록 |
| **가입 신청** | `houseId` | `Detail` | 신청·즉시가입 |
| **신청취소·탈퇴** | `houseId` | 없음 | 나가기 |
| **가입 대기 목록** | `houseId` | `Member[]` (PENDING) | 승인 화면 |
| **승인 / 거절** | `houseId`, `userId` | `Member` | 가입 처리 |
| **역할 변경** | `houseId`, `userId`, `role` | `Member` | 부리더 임명 |
| **강퇴** | `houseId`, `userId` | 없음 | 멤버 제거 |
| **일정 목록 / 생성** | `houseId` (+ `title`, `scheduledAt`) | `Schedule[]` / `Schedule` | 일정 |
| **일정 참가 / 취소** | `houseId`, `scheduleId` | `Schedule` | 참가 |
| **공지 목록 / 작성** | `houseId` (+ `title`, `content`) | `Notice[]` / `Notice` | 공지 |
| **공지 고정 / 삭제** | `houseId`, `noticeId` | `Notice` / 없음 | 공지 관리 |
| **채팅 기록** | `houseId` | `ChatMessage[]` (최근 50) | 입장 시 |
| **추천** | 없음 | `userId[]` | 함께할 사람 |
| **주간 퀘스트 목록** | `houseId` | `Quest[]` (`questId`,`rewardClaimed`,`rewardXp`,`rewardHc`) | 퀘스트 화면 |
| **퀘스트 보상 수령** | `houseId`, `questId` | 없음 | 보상 받기 |
| **상점 아이템 목록** | 없음 | `ShopItem[]` | 상점 화면 |
| **상점 구매** | `userId`, `houseId`(선택), `itemId` (쿼리 파라미터) | 없음 | 아이템 구매 |

---

## 0. 로그인 (user 서비스 :8081)

### Request
```
POST /api/auth/login
```
```json
{ "email": "user@example.com", "password": "..." }
```

### Response
```json
{
  "token": "eyJhbGciOi...",
  "user": { "id": 7, "nickname": "달빛기사", ... }
}
```
사용하는 값
- token → 이후 모든 crew 요청의 `Authorization: Bearer <token>`
- user.id → 내 userId

---

## 1. House 목록

### Request
```
GET /api/crew/houses
```
```
(요청 값 없음 · 토큰 없어도 호출 가능)
```

### Response
```json
[
  {
    "id": 1,
    "name": "소환사의 쉼터",
    "description": "매너를 먼저 보는 친목 House",
    "type": "PUBLIC",
    "leaderId": 7,
    "maxMembers": 20,
    "memberCount": 3,
    "createdAt": "2026-08-25T08:30:00Z",
    "myRole": "MEMBER",
    "myStatus": "APPROVED"
  }
]
```
- `memberCount` — APPROVED 인원만
- `myRole` / `myStatus` — 비로그인이거나 무관한 House면 `null`

---

## 2. House 상세

### Request
```
GET /api/crew/houses/{houseId}
```
```
houseId = 1
```

### Response
```json
{
  "id": 1,
  "name": "소환사의 쉼터",
  "description": "매너를 먼저 보는 친목 House",
  "type": "PUBLIC",
  "leaderId": 7,
  "maxMembers": 20,
  "createdAt": "2026-08-25T08:30:00Z",
  "members": [
    {
      "memberId": 1,
      "userId": 7,
      "role": "LEADER",
      "status": "APPROVED",
      "joinedAt": "2026-08-25T08:30:00Z",
      "requestedAt": "2026-08-25T08:30:00Z"
    }
  ],
  "myRole": "LEADER",
  "myStatus": "APPROVED",
  "pendingCount": 2
}
```
- `members` — APPROVED만, LEADER → SUB_LEADER → MEMBER 순
- `pendingCount` — 대기자 수만. 목록은 6번 API

---

## 3. House 생성

### Request
```
POST /api/crew/houses
```
```json
{
  "name": "소환사의 쉼터",
  "description": "매너를 먼저 보는 친목 House",
  "type": "PUBLIC",
  "maxMembers": 20
}
```
| 필드 | 필수 | 기본값 |
| --- | --- | --- |
| `name` | O | — |
| `description` | X | `null` |
| `type` | X | `PUBLIC` |
| `maxMembers` | X | `20` |

### Response
```json
{ "id": 1, "name": "소환사의 쉼터", "type": "PUBLIC", "leaderId": 7,
  "members": [ { "memberId": 1, "userId": 7, "role": "LEADER", "status": "APPROVED" } ],
  "myRole": "LEADER", "myStatus": "APPROVED", "pendingCount": 0 }
```
- 만든 사람이 LEADER / APPROVED로 자동 등록됨

---

## 4. House 수정 (방장만)

### Request
```
PUT /api/crew/houses/{houseId}
```
```json
{ "description": "바꾼 설명", "maxMembers": 30 }
```
- 부분 수정. 보내지 않은 필드는 기존 값 유지

### Response
```
Detail (2번과 동일 형태)
```

---

## 5. 가입 신청

### Request
```
POST /api/crew/houses/{houseId}/join
```
```
houseId = 1
(바디 없음)
```

### Response
```json
{ "id": 1, "myRole": "MEMBER", "myStatus": "APPROVED", ... }
```
| House type | 결과 `myStatus` |
| --- | --- |
| `PUBLIC` | `APPROVED` (즉시 가입) |
| `PRIVATE` | `PENDING` (승인 대기) |

- 이미 신청·가입 상태 → `409`
- 정원 초과 (PUBLIC) → `409`

---

## 6. 가입 대기 목록 (관리자)

### Request
```
GET /api/crew/houses/{houseId}/join-requests
```

### Response
```json
[
  { "memberId": 5, "userId": 9, "role": "MEMBER", "status": "PENDING",
    "joinedAt": null, "requestedAt": "2026-08-25T10:12:00Z" }
]
```
사용하는 값
- **userId** → 7번 승인/거절 API의 경로 파라미터 (memberId 아님)

---

## 7. 승인 / 거절 (관리자)

### Request
```
POST /api/crew/houses/{houseId}/members/{userId}/approve
POST /api/crew/houses/{houseId}/members/{userId}/reject
```
```
houseId = 1
userId  = 9
(바디 없음)
```

### Response
```json
{ "memberId": 5, "userId": 9, "role": "MEMBER", "status": "APPROVED",
  "joinedAt": "2026-08-25T10:20:00Z", "requestedAt": "2026-08-25T10:12:00Z" }
```
- 이미 승인 / 정원 초과 → `409`
- 거절 시 `status: "REJECTED"` (재신청 가능)

---

## 8. 역할 변경 (방장만)

### Request
```
PUT /api/crew/houses/{houseId}/members/{userId}/role
```
```json
{ "role": "SUB_LEADER" }
```
- `LEADER`로는 지정 불가 → `400` (방장 위임 API 없음)

### Response
```json
{ "memberId": 5, "userId": 9, "role": "SUB_LEADER", "status": "APPROVED" }
```

---

## 9. 강퇴 / 탈퇴

### Request
```
DELETE /api/crew/houses/{houseId}/members/{userId}   ← 강퇴 (관리자)
DELETE /api/crew/houses/{houseId}/join               ← 신청취소·탈퇴 (본인)
```

### Response
```
200 (본문 없음)
```
- 방장은 강퇴·탈퇴 불가 → `403`

---

## 10. 내가 속한 House

### Request
```
GET /api/crew/my/houses
```

### Response
```
Summary[] (1번과 동일 형태, APPROVED인 것만)
```

---

## 11. 일정

### Request
```
GET    /api/crew/houses/{houseId}/schedules
POST   /api/crew/houses/{houseId}/schedules
POST   /api/crew/houses/{houseId}/schedules/{scheduleId}/participants
DELETE /api/crew/houses/{houseId}/schedules/{scheduleId}/participants
```
생성 바디
```json
{
  "title": "주말 5인 랭크",
  "scheduledAt": "2026-08-30T21:00:00",
  "maxParticipants": 5
}
```
- `scheduledAt` — 타임존 없음. `Z` 붙이면 `400`
- `maxParticipants` 생략 시 `5`

### Response
```json
{
  "id": 1,
  "houseId": 1,
  "title": "주말 5인 랭크",
  "scheduledAt": "2026-08-30T21:00:00",
  "maxParticipants": 5,
  "participantCount": 2,
  "participantUserIds": [7, 9],
  "joined": true
}
```
- 생성자는 자동 참가 (`joined: true`)
- 정원 초과 / 이미 참가 → `409`

---

## 12. 공지

### Request
```
GET    /api/crew/houses/{houseId}/notices
POST   /api/crew/houses/{houseId}/notices          ← 관리자
PUT    /api/crew/houses/{houseId}/notices/{noticeId}/pin   ← 관리자
DELETE /api/crew/houses/{houseId}/notices/{noticeId}       ← 관리자
```
작성 바디
```json
{ "title": "정기 모임 안내", "content": "매주 토요일 9시", "pinned": true }
```
고정 토글 바디
```json
{ "pinned": false }
```

### Response
```json
{
  "id": 1,
  "houseId": 1,
  "authorId": 7,
  "title": "정기 모임 안내",
  "content": "매주 토요일 9시",
  "pinned": true,
  "createdAt": "2026-08-25T09:00:00Z"
}
```
- 목록은 고정 공지 먼저, 그다음 최신순

---

## 13. 채팅 기록

### Request
```
GET /api/crew/houses/{houseId}/chat/messages
```

### Response
```json
[
  { "houseId": 1, "senderId": 7, "senderName": "달빛기사",
    "message": "안녕하세요", "timestamp": "2026-08-25T09:10:00" }
]
```
- 최근 50개를 **오래된 순**으로

---

## 14. 함께할 사람 추천

### Request
```
GET /api/crew/recommendations/playmates
```

### Response
```json
[9, 14, 22]
```
- 최근 7일 3판 이상 같이한 `users.id`
- 닉네임 등 표시 정보는 user 서비스에서 따로 조회
- match 서비스가 `MatchFoundEvent`를 아직 발행하지 않아 **현재는 항상 `[]`**

---

## 15. 채팅 실시간 (STOMP)

### Request
```
연결   http://localhost:8086/ws-house        (SockJS)
헤더   Authorization: Bearer <JWT>            (CONNECT 프레임)
보내기 SEND /pub/house/chat
받기   SUBSCRIBE /sub/house/{houseId}
```
SEND 바디
```json
{ "houseId": 1, "message": "안녕하세요" }
```

### Response (구독으로 수신)
```json
{ "houseId": 1, "senderId": 7, "senderName": "달빛기사",
  "message": "안녕하세요", "timestamp": "2026-08-25T09:10:00" }
```
- `senderId`는 서버가 토큰으로 덮어씀 (보내도 무시)
- chat 서비스의 `/ws`와 다른 엔드포인트

---

## 16. 주간 퀘스트

### Request
```
GET  /api/houses/{houseId}/quests
POST /api/houses/{houseId}/quests/{questId}/claim
```
```
houseId = 1
questId = 3
(바디 없음)
```

### Response (목록)
```json
[
  { "questId": 1, "rewardClaimed": false, "rewardXp": 200, "rewardHc": 0 },
  { "questId": 2, "rewardClaimed": false, "rewardXp": 150, "rewardHc": 0 }
]
```
- 매주 월요일 0시(서버 기동 시점에도 1회)에 House마다 고정 퀘스트 4종이 새로 생성됨
  - `WIN_TOGETHER` 7회 → 200xp · `PLAY_TOGETHER` 5회 → 150xp
  - `SCHEDULE_JOIN` 3회 → 150xp · `DAILY_ACTIVE` 3회 → 150xp
- 응답에 퀘스트 종류·설명·목표치·진행도(`currentCount`)가 없다 — `questId`, 보상액, 수령 여부만 내려온다
- `rewardHc`는 현재 모든 퀘스트에서 `0`

### Response (보상 수령)
```
200 (본문 없음)
```
- 완료 전이거나 이미 수령한 퀘스트 → `400` ("보상을 수령할 수 없는 상태입니다.")
- 없는 퀘스트 / House → `400` ("퀘스트가 존재하지 않습니다." / "존재하지 않는 하우스입니다.") — 다른 API와 달리 `404`가 아님
- 수령한 보상은 House의 `xp`/`hc`에 적립됨 (유저 개인에게는 적립되지 않음)

⚠️ 다른 API와 다른 점
- 경로가 `/api/crew/...`가 아니라 `/api/houses/...`
- 컨트롤러가 `Authentication`을 받지 않는다 — 호출자가 그 House의 멤버인지 전혀 확인하지 않음
- `SecurityConfig`가 `/api/houses/**`를 `permitAll`로 열어 둬서, 토큰 없이 아무나 아무 House의 퀘스트를 조회·수령할 수 있다

---

## 17. 상점

### Request
```
GET  /api/shop/items
POST /api/shop/buy?userId={userId}&houseId={houseId}&itemId={itemId}
```
- `buy`는 JSON 바디가 아니라 쿼리 파라미터로 받는다
- `houseId`는 선택 — House 전용 아이템이 아니면 생략

### Response (목록)
```json
[
  { "id": 1, "name": "황금 테두리", "category": "BORDER", "priceHc": 500, "imageUrl": "https://.../border.png" }
]
```
`category`: `BORDER` · `TITLE` · `BANNER` · `THEME` · `CHAT_SKIN` · `NICKNAME_DECO` · `HOUSE_ICON`

### Response (구매)
```
200 (본문 없음)
```
- 없는 상품 → `400` ("상품이 존재하지 않습니다.")

⚠️ 다른 API와 다른 점
- `userId`를 토큰이 아니라 쿼리 파라미터로 직접 받는다 — 남의 userId를 넣으면 그 사람 명의로 구매가 기록된다
- HC 잔액 확인·차감 로직이 주석 처리되어 있다 — 잔액이 부족해도 항상 구매에 성공하고, 어떤 재화도 차감되지 않는다
- `SecurityConfig`가 `/api/shop/**`를 `permitAll`로 열어 둬서 토큰 없이도 호출 가능

---

# 전체 호출 흐름

```
① 로그인 (user :8081)
Request
- email
- password
Response
- token
- user.id
        │
        ▼
② House 목록
Request
- (없음)
Response
- id
- name
- type
- memberCount
- myStatus
        │
        ├──────────────── 만들기 ────────────────┐
        │                                        ▼
        │                              ③ House 생성
        │                              Request
        │                              - name
        │                              - type
        │                              - maxMembers
        │                              Response
        │                              - id  → houseId
        │                              - myRole = LEADER
        ▼
④ 가입 신청
Request
- houseId
Response
- myStatus  (PUBLIC → APPROVED / PRIVATE → PENDING)
        │
        ▼  (PRIVATE 인 경우만)
⑤ 가입 대기 목록  ← 방장·부리더
Request
- houseId
Response
- userId
- requestedAt
        │
        ▼
⑥ 승인 / 거절
Request
- houseId
- userId
Response
- status  (APPROVED / REJECTED)
        │
        ▼
────── 여기부터 멤버 전용 ──────
        │
        ├──▶ ⑦ 일정
        │    Request  : houseId, title, scheduledAt
        │    Response : id, participantUserIds, joined
        │
        ├──▶ ⑧ 공지
        │    Request  : houseId, title, content
        │    Response : id, pinned, createdAt
        │
        └──▶ ⑨ 채팅
             Request  : houseId (기록 조회)
             Response : ChatMessage[]
                    ↓
             STOMP SUBSCRIBE /sub/house/{houseId}
             STOMP SEND      /pub/house/chat
```

---

# 권한 요약

| 등급 | 조건 | 해당 API |
| --- | --- | --- |
| 공개 | 토큰 없음 | House 목록, House 상세 |
| 공개(의도됐는지 불명확) | 인증 자체를 확인하지 않음 — `/api/houses/**`, `/api/shop/**` 가 `permitAll` | 퀘스트 조회·보상 수령, 상점 조회·구매 |
| 로그인 | 유효한 토큰 | 생성, 가입 신청, 탈퇴, 내 House, 추천 |
| 멤버 | 그 House의 APPROVED | 일정 전체, 공지 조회, 채팅 |
| 관리자 | LEADER / SUB_LEADER | 대기 목록, 승인, 거절, 강퇴, 공지 작성·고정·삭제 |
| 방장 | LEADER | House 수정, 역할 변경 |

# 상태 코드

| 코드 | 상황 |
| --- | --- |
| 400 | 필수 필드 누락, `role: LEADER` 지정, `scheduledAt`에 `Z`, 없는 퀘스트/상품, 미완료·중복 수령 퀘스트 |
| 401 | 토큰 없음 · 만료 |
| 403 | 멤버 아님 · 방장 아님 |
| 404 | 없는 House · 일정 · 공지 |
| 409 | 중복 신청, 정원 초과, 이미 승인, 이미 참가 |

퀘스트·상점 API는 `BusinessException`이 아니라 `IllegalArgumentException`/`IllegalStateException`을 던진다. 그래서 다른 API였다면 `404`·`409`였을 상황(없는 퀘스트, 없는 상품, 이미 수령한 보상)도 전부 `400`으로 내려간다.

# 프론트 목업과 다른 점

| 프론트 목업 | 백엔드 | 필요한 일 |
| --- | --- | --- |
| `OWNER / MANAGER / MEMBER` | `LEADER / SUB_LEADER / MEMBER` | 이름 매핑 |
| `type: SOCIAL/COMPETITIVE` + `visibility: PUBLIC/PRIVATE` | `type: PUBLIC/PRIVATE` 하나 | 백엔드 필드 추가 필요 |
| `game: "리그오브레전드"` | 없음 | 백엔드 필드 추가 필요 |
| 초대 보내기·수락·거절 | 없음 | API 자체가 없음 |
| 일정 참석/불참/미정 | 참가/미참가 | 상태값 축소 또는 확장 |
| `id: "house-summoners-rest"` | `id: 1` (Long) | 타입 변경 |
