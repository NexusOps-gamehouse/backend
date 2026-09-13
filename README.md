# backend · 보관 (archived)

GameHouse 백엔드의 **초기 구현체**입니다. Gradle 멀티모듈 하나에 서비스 6개를 담고 Docker Compose 로 EC2 한 대에 올리던 시절의 코드입니다.

> 현재 이 레포는 개발이 멈춰 있습니다. 서비스는 각각 독립 레포로 분리되었고, 지금은 EKS 위에서 GitOps 로 배포됩니다.
> **최신 코드를 찾고 있다면 아래 1번 표의 레포로 가시면 됩니다.**

이 레포를 남겨 둔 이유는 GameHouse가 **어떤 모습에서 출발해 어디로 갔는지**가 여기 남아 있기 때문입니다.

| | 이 레포 (2026-08) | 현재 (2026-09~) |
|---|---|---|
| 레포 수 | 1 | 서비스 6 + 공용 라이브러리 1 |
| 배포 단위 | 1 (전체가 함께) | 6 (서비스별 독립) |
| 실행 환경 | EC2 1대 · Docker Compose | EKS · Argo CD |
| 배포 방식 | GitHub Actions → SSM → EC2 | 이미지 태그 커밋 → Argo CD 동기화 |

---

## 1. 코드는 어디로 갔나

| 이 레포의 모듈 | 옮겨간 레포 | 맡은 일 |
|---|---|---|
| `user/` | [gamehouse-user](https://github.com/NexusOps-gamehouse/gamehouse-user) | 계정 · 프로필 · 친구 · 알림 |
| `post/` | [gamehouse-post](https://github.com/NexusOps-gamehouse/gamehouse-post) | 파티 모집글 · 신청 |
| `chat/` | [gamehouse-chat](https://github.com/NexusOps-gamehouse/gamehouse-chat) | 파티 채팅 |
| `match/` | [gamehouse-match](https://github.com/NexusOps-gamehouse/gamehouse-match) | Team Fit 매칭 · AI 설명 |
| `crew/` | [gamehouse-crew](https://github.com/NexusOps-gamehouse/gamehouse-crew) | House · 함께한 기록 · 상점 |
| `riot/` | [gamehouse-riot](https://github.com/NexusOps-gamehouse/gamehouse-riot) | Riot API 연동 |
| `common/` | [gamehouse-common](https://github.com/NexusOps-gamehouse/gamehouse-common) | 이벤트 계약 · 공용 설정 |

실행 환경은 [infra](https://github.com/NexusOps-gamehouse/infra), 프런트는 [frontend](https://github.com/NexusOps-gamehouse/frontend) 에 있습니다.

---

## 2. 여기서 어떤 구조였나

| | |
|---|---|
| 빌드 | Gradle 멀티모듈 — `settings.gradle` 에 7개 모듈 선언 |
| 런타임 | Spring Boot 3.3.5 · Java 17 |
| 서비스 간 통신 | RabbitMQ 이벤트. 계약은 `common` 의 Java record |
| 데이터 | PostgreSQL 한 인스턴스를 **서비스별 스키마·계정으로 분리** |
| 이미지 | 모듈별 `Dockerfile`, jlink 로 최소 JRE 조립 |
| 배포 | Docker Compose · EC2 1대 |

포트 — user `8081` · post `8082` · chat `8083` · riot `8084` · match `8085` · crew `8086`

**코드는 이미 서비스 단위로 갈라져 있었습니다.** 모듈 간 직접 호출 대신 이벤트를 쓰고, DB 계정을 분리해 남의 테이블을 읽지 못하게 막아 둔 상태였습니다. 그래서 나중에 레포를 나눌 때 코드를 다시 쓸 필요가 없었습니다.

바뀐 것은 **경계의 위치가 아니라 배포 단위**였습니다.

---

## 3. 왜 나눴나

멀티모듈은 코드만 나눈 상태였고, 배포는 여전히 하나였습니다.

- 한 서비스만 고쳐도 전체가 다시 빌드되고 함께 배포됨
- 서비스마다 다른 속도로 릴리스할 수 없음
- `settings.gradle` 이 모듈 7개를 전부 선언하므로, 한 서비스의 이미지를 만들 때도 Gradle 이 7개 프로젝트를 모두 읽어야 함

마지막 항목은 `Dockerfile` 에 그대로 흔적이 남아 있습니다. `user` 이미지 하나를 만드는 데도 나머지 6개 모듈의 `build.gradle` 을 복사해야 했습니다. 그러지 않으면 이렇게 죽습니다.

```
Configuring project ':user' without an existing directory is not allowed
```

EKS 로 옮기면서 서비스마다 이미지 · 파이프라인 · 릴리스 주기를 따로 갖게 하려면, 레포부터 나뉘어야 했습니다.

---

## 4. 어떻게 나눴나

`split/*` 브랜치가 그 작업의 흔적입니다. 모듈별로 히스토리를 잘라내 각 레포의 초기 커밋으로 삼았습니다.

```
backend (monorepo)
   ├─ split/user    ──> gamehouse-user
   ├─ split/post    ──> gamehouse-post
   ├─ split/chat    ──> gamehouse-chat
   ├─ split/match   ──> gamehouse-match
   ├─ split/crew    ──> gamehouse-crew
   ├─ split/riot    ──> gamehouse-riot
   └─ split/common  ──> gamehouse-common
```

**각 레포는 이 레포의 커밋 히스토리를 이어받았습니다.** 서비스 레포에서 `git log` 를 끝까지 내려가면 `msa구조 변경`(2026-08-20)이 나오는데, 그게 이 레포의 커밋입니다. 새로 만든 것이 아니라 잘라낸 것입니다.

`common` 만 성격이 다릅니다. 나머지가 독립 부트 앱이 된 것과 달리 common 은 라이브러리로 남아, GitHub Packages 에 `gg.duo:common` 으로 발행되고 6개 서비스가 의존합니다. 레포를 나눈 뒤 **이벤트 계약을 어디에 둘 것인가**에 대한 답입니다.

---
