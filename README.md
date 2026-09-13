# backend · 보관 (archived)

> **이 레포는 더 이상 사용하지 않습니다.**
> 여기 있던 서비스들은 각각 독립 레포로 분리되었고, 이후의 모든 변경은 분리된 레포에서 이루어집니다.
> 마지막 커밋 · 2026-09-01

GameHouse 초기 구현체. Gradle 멀티모듈 하나에 서비스 6개와 공용 라이브러리를 담고, Docker Compose 로 EC2 한 대에 올리던 구조였다.

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

실행 환경(Compose · Kubernetes · 관측)은 [infra](https://github.com/NexusOps-gamehouse/infra) 로,
프런트는 [frontend](https://github.com/NexusOps-gamehouse/frontend) 로 이미 분리되어 있었다.

---

## 2. 왜 나눴나

멀티모듈은 **코드만 나눈 상태**였다. 배포 단위는 여전히 하나였다.

- 한 서비스만 고쳐도 전체가 다시 빌드되고 함께 배포됐다
- 서비스마다 다른 속도로 릴리스할 수 없었다
- `settings.gradle` 이 모듈 7개를 전부 선언하므로, 한 서비스의 이미지를 만들 때도 Gradle 이 7개 프로젝트를 모두 읽어야 했다 (각 `Dockerfile` 이 다른 모듈의 `build.gradle` 까지 복사하는 이유)

EKS 로 옮기면서 서비스마다 이미지 · 파이프라인 · 릴리스 주기를 따로 갖게 하려면, 레포부터 나뉘어야 했다.

---

## 3. 어떻게 나눴나

`split/*` 브랜치가 그 작업의 흔적이다. 모듈별로 히스토리를 잘라내 각 레포의 초기 커밋으로 삼았다.

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

**각 레포는 이 레포의 커밋 히스토리를 이어받았다.** 서비스 레포에서 `git log` 를 끝까지 내려가면 `msa구조 변경`(2026-08-20)이 나오는데, 그게 이 레포의 커밋이다. 분리는 새 출발이 아니라 잘라내기였다.

`common` 만 성격이 다르다. 나머지가 독립 부트 앱이 된 것과 달리 common 은 라이브러리로 남아, GitHub Packages 에 `gg.duo:common` 으로 올려 6개 서비스가 의존한다.

---

## 4. 당시 구조

| | |
|---|---|
| 빌드 | Gradle 멀티모듈 (`settings.gradle` 에 7개 선언) |
| 런타임 | Spring Boot 3.3.5 · Java 17 |
| 이미지 | 모듈별 `Dockerfile` — 빌드 컨텍스트는 **레포 루트**, jlink 로 최소 JRE 조립 |
| 배포 | Docker Compose · EC2 1대 (infra 레포의 `docker-compose.yml`) |
| 서비스 간 통신 | RabbitMQ 이벤트 (`common` 의 record 계약) |
| CI | `ci-cd.yml` — 시크릿 스캔 → 서비스 6개 이미지 빌드 + 실기동 스모크 테스트 |

**로컬 포트** — user `8081` · post `8082` · chat `8083` · riot `8084` · match `8085` · crew `8086`

CI가 이미지를 빌드만 하지 않고 **실제로 띄워봤던** 이유는 jlink 때문이다. JRE 모듈을 손으로 골라 조립하므로, 하나가 빠져도 컴파일과 빌드는 전부 통과하고 실행할 때만 `NoClassDefFound` 가 난다. 서비스마다 쓰는 라이브러리(webflux / websocket / amqp)가 달라 필요한 JDK 모듈도 달랐고, 그래서 6개를 각각 띄워 확인했다.

`deploy` job 은 `if: false` 로 비활성 상태다. EKS 전환이 결정되면서 EC2 배포를 멈춘 시점의 모습 그대로다.

---
