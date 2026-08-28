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
받기   SUBSCRIBE /topic/crew.houses.{houseId}
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
             STOMP SUBSCRIBE /topic/crew.houses.{houseId}
             STOMP SEND      /pub/house/chat
```

---

# 권한 요약

| 등급 | 조건 | 해당 API |
| --- | --- | --- |
| 공개 | 토큰 없음 | House 목록, House 상세 |
| 로그인 | 유효한 토큰 | 생성, 가입 신청, 탈퇴, 내 House, 추천 |
| 멤버 | 그 House의 APPROVED | 일정 전체, 공지 조회, 채팅 |
| 관리자 | LEADER / SUB_LEADER | 대기 목록, 승인, 거절, 강퇴, 공지 작성·고정·삭제 |
| 방장 | LEADER | House 수정, 역할 변경 |

# 상태 코드

| 코드 | 상황 |
| --- | --- |
| 400 | 필수 필드 누락, `role: LEADER` 지정, `scheduledAt`에 `Z` |
| 401 | 토큰 없음 · 만료 |
| 403 | 멤버 아님 · 방장 아님 |
| 404 | 없는 House · 일정 · 공지 |
| 409 | 중복 신청, 정원 초과, 이미 승인, 이미 참가 |

# 프론트 목업과 다른 점

| 프론트 목업 | 백엔드 | 필요한 일 |
| --- | --- | --- |
| `OWNER / MANAGER / MEMBER` | `LEADER / SUB_LEADER / MEMBER` | 이름 매핑 |
| `type: SOCIAL/COMPETITIVE` + `visibility: PUBLIC/PRIVATE` | `type: PUBLIC/PRIVATE` 하나 | 백엔드 필드 추가 필요 |
| `game: "리그오브레전드"` | 없음 | 백엔드 필드 추가 필요 |
| 초대 보내기·수락·거절 | 없음 | API 자체가 없음 |
| 일정 참석/불참/미정 | 참가/미참가 | 상태값 축소 또는 확장 |
| `id: "house-summoners-rest"` | `id: 1` (Long) | 타입 변경 |
