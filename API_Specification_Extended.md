# Crew API 명세서 (수정판)

> **작성 일시:** 2024-08-28  
> **상태:** 구현/미구현 분리 명시  
> **주의:** 프론트엔드는 새 명세 기준으로 구현 중이므로, 백엔드 구현 필수

---

## 📋 구현 상태 요약

| 기능 | 상태 | 비고 |
|------|------|------|
| **1단계: 기본 기능** | | |
| House XP 조회 | ✅ **구현됨** | GET /api/houses/{houseId}에 xp, hc 포함 |
| 주간 퀘스트 조회 | ⚠️ **부분 구현** | questType, currentCount, targetCount, isCompleted, weekStartDate 누락 |
| 퀘스트 진행도 업데이트 | ❌ **미구현** | PATCH API 없음 |
| 퀘스트 보상 수령 | ✅ **구현됨** | POST /api/houses/{houseId}/quests/{questId}/claim |
| 퀘스트 인증/보안 | ❌ **미구현** | @PreAuthorize, 멤버십 검증 없음 |
| **2단계: 재화 시스템** | | |
| 재화 조회 | ❌ **미구현** | GET /api/houses/{houseId}/currency 없음 |
| 재화 배치 조회 | ❌ **미구현** | POST /api/houses/currency/batch 없음 |
| 재화 차감 로직 | ❌ **미구현** | HC 차감/검증 미구현 |
| **3단계: 상점 기능** | | |
| 상점 아이템 조회 | ✅ **구현됨** | GET /api/shop/items (단순 List만) |
| 상점 아이템 상세 | ❌ **미구현** | GET /api/shop/items/{itemId} 없음 |
| 상점 안전한 구매 | ⚠️ **부분 구현** | HC 차감/검증/Lock 미구현 |
| 상점 구매 이력 | ❌ **미구현** | GET /api/shop/inventory 없음 |
| 아이템 적용 | ❌ **미구현** | POST /api/shop/inventory/{id}/apply 없음 |
| 아이템 해제 | ❌ **미구현** | DELETE /api/shop/inventory/{id}/apply 없음 |
| **4단계: 랭킹 시스템** | | |
| 전체 House 랭킹 | ❌ **미구현** | GET /api/houses/ranking 없음 |
| 개인 House 랭킹 | ❌ **미구현** | GET /api/houses/ranking/my 없음 |
| **5단계: 추천 및 매칭** | | |
| 플레이어 추천 | ❌ **미구현** | GET /api/users/recommendations/players 없음 |
| 이벤트 플레이어 | ❌ **미구현** | GET /api/events/{eventId}/players 없음 |
| 매칭 요청 | ❌ **미구현** | POST /api/users/matchmaking/request 없음 |
| **보안 기본** | | |
| 사용자 인증 | ❌ **미구현** | /api/houses/, /api/shop/ permitAll 상태 |
| 권한 검증 | ❌ **미구현** | 멤버십/사용자 검증 없음 |
| 동시성 제어 | ❌ **미구현** | Pessimistic Lock 없음 |

---

## 1. House XP (하우스 경험치)

### 개요
House XP는 House 단위의 경험치 시스템입니다. 주간 퀘스트 완료 시 보상으로 획득됩니다.

**상태:** ✅ **구현됨**

### 데이터 모델

#### House XP 필드
| 필드 | 타입 | 설명 | 기본값 |
|------|------|------|--------|
| id | Long | House ID | - |
| xp | Long | 누적 경험치 | 0L |
| hc | Long | 누적 재화 | 0L |

### API 엔드포인트

#### 1.1 House 정보 조회 (XP 포함)
```
GET /api/houses/{houseId}
```

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| houseId | Long | House ID |

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| viewerId | Long | O | 조회 요청자 ID |

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "House Name",
  "description": "House Description",
  "type": "PUBLIC",
  "leaderId": 100,
  "maxMembers": 20,
  "xp": 1200,
  "hc": 5000,
  "createdAt": "2024-08-28T00:00:00Z",
  "members": [
    {
      "userId": 100,
      "role": "LEADER",
      "status": "APPROVED"
    }
  ]
}
```

**Response Fields:**
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | House ID |
| xp | Long | 현재 누적 경험치 |
| hc | Long | 현재 누적 재화 |
| members | Array | House 멤버 목록 |

**Error Responses:**
- 404 NOT_FOUND: House를 찾을 수 없음

---

## 2. 주간 퀘스트 (Weekly Quest)

### 개요
House 멤버가 함께 완료할 수 있는 주간 퀘스트 시스템입니다. 매주 월요일 00:00에 자동 초기화되며, 완료 시 XP와 HC 보상을 획득합니다.

**상태:** ⚠️ **부분 구현** - 응답에 퀘스트 상세 정보 누락, 진행도 업데이트 API 없음

### 퀨스트 타입

| QuestType | 대상 | 목표 | XP 보상 | HC 보상 | 설명 |
|-----------|------|------|--------|--------|------|
| WIN_TOGETHER | House | 공동 승리 7회 | 200 | 0 | House 멤버들이 함께 7번 승리 |
| PLAY_TOGETHER | House | 2명 이상 함께 플레이 5회 | 150 | 0 | House 멤버 2명 이상이 함께 플레이 5회 |
| SCHEDULE_JOIN | House | 일정 참여 완료 3회 | 150 | 0 | House 일정에 참여하여 완료 3회 |
| DAILY_ACTIVE | House | 서로 다른 3일 함께 플레이 | 150 | 0 | 주간 서로 다른 3일 함께 플레이 |

### 데이터 모델

#### HouseQuest Entity
| 필드 | 타입 | 설명 | 기본값 |
|------|------|------|--------|
| id | Long | 퀘스트 ID | - |
| houseId | Long | House ID | - |
| questType | QuestType | 퀘스트 타입 | - |
| currentCount | Int | 현재 진행도 | 0 |
| isCompleted | Boolean | 완료 여부 | false |
| isRewardClaimed | Boolean | 보상 수령 여부 | false |
| weekStartDate | LocalDateTime | 주간 시작 날짜 (월요일 00:00) | - |

### API 엔드포인트

#### 2.1 주간 퀘스트 목록 조회

```
GET /api/houses/{houseId}/quests
```

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| houseId | Long | House ID |

**현재 구현된 Response (200 OK):**
```json
[
  {
    "questId": 1001,
    "rewardClaimed": false,
    "rewardXp": 200,
    "rewardHc": 0
  },
  {
    "questId": 1002,
    "rewardClaimed": false,
    "rewardXp": 150,
    "rewardHc": 0
  }
]
```

**현재 Response Fields:**
| 필드 | 타입 | 설명 |
|------|------|------|
| questId | Long | 퀘스트 ID |
| rewardClaimed | Boolean | 보상 수령 여부 |
| rewardXp | Long | XP 보상량 |
| rewardHc | Long | HC 보상량 |

**⚠️ 누락된 필드 (프론트 기대치):**
| 필드 | 타입 | 설명 |
|------|------|------|
| questType | String | ❌ 퀘스트 타입 (WIN_TOGETHER, PLAY_TOGETHER 등) |
| currentCount | Int | ❌ 현재 진행도 |
| targetCount | Int | ❌ 완료 목표 |
| isCompleted | Boolean | ❌ 완료 여부 |
| weekStartDate | LocalDateTime | ❌ 주간 시작 날짜 |

**Error Responses:**
- 404 NOT_FOUND: House를 찾을 수 없음
- 401 UNAUTHORIZED: 인증 필요 (현재는 permitAll)

**Notes:**
- 매주 월요일 00:00 (KST)에 모든 House의 퀘스트가 초기화됨
- **현재:** HC 보상이 모두 0으로 설정됨
- **보안 이슈:** 사용자 검증 없음 (누구나 접근 가능)

---

#### 2.2 퀘스트 진행도 업데이트 [미구현]

```
PATCH /api/houses/{houseId}/quests/{questId}/progress
```

**상태:** ❌ **미구현** - 현재 백엔드에 없음  
**필요:** 프론트에서 요청 시도 중 → 백엔드 구현 필수

**예상 Request:**
```json
{
  "increment": 1
}
```

**예상 Response (200 OK):**
```json
{
  "questId": 1001,
  "questType": "WIN_TOGETHER",
  "currentCount": 6,
  "targetCount": 7,
  "isCompleted": false,
  "isRewardClaimed": false,
  "rewardXp": 200,
  "rewardHc": 0
}
```

**예상 Error Responses:**
- 400 BAD_REQUEST: 유효하지 않은 increment 값
- 401 UNAUTHORIZED: 인증 필요
- 403 FORBIDDEN: House 멤버가 아님
- 404 NOT_FOUND: 퀘스트를 찾을 수 없음
- 409 CONFLICT: 이미 완료된 퀘스트

**구현 요구사항:**
```java
// QuestService에 추가
@Transactional
public void updateQuestProgress(Long houseId, Long questId, int increment) {
  HouseQuest quest = questRepository.findById(questId)
    .orElseThrow(() -> new IllegalArgumentException("퀘스트 없음"));
  quest.addProgress(increment);  // 기존 메서드 활용
}
```

---

#### 2.3 퀘스트 보상 수령

```
POST /api/houses/{houseId}/quests/{questId}/claim
```

**상태:** ✅ **구현됨** (보안/검증 미흡)

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| houseId | Long | House ID |
| questId | Long | 퀘스트 ID |

**Response (200 OK):**
```json
{
  // 실제 응답은 void
}
```

**⚠️ 현재 문제점:**
1. 응답 본문 없음 (클라이언트에 상태 확인 불가)
2. 사용자 검증 없음 (누구나 호출 가능)
3. House 멤버십 확인 없음 (권한 검증 없음)
4. 중복 수령 방지 체크만 있음

**Side Effects (실제 동작):**
- ✅ House의 xp에 rewardXp 만큼 추가됨
- ✅ House의 hc에 rewardHc 만큼 추가됨 (현재는 0)
- ✅ quest.isRewardClaimed = true 설정

**Error Responses:**
- 400 BAD_REQUEST: 완료되지 않은 퀘스트
- 400 BAD_REQUEST: 이미 보상 수령
- 404 NOT_FOUND: 퀘스트 또는 House 없음

**보안 개선 필요:**
```java
// 추가 필요한 검증
@PostMapping("/{questId}/claim")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> claimReward(
    @PathVariable Long houseId,
    @PathVariable Long questId,
    @AuthenticationPrincipal User user) {  // JWT에서 사용자 추출
  
  // 1. House 멤버 확인
  houseService.requireApprovedMember(houseId, user.getId());
  
  // 2. 보상 수령
  questService.claimQuestReward(houseId, questId);
  
  // 3. 응답 반환
  return ResponseEntity.ok(/* DTO with status */);
}
```

---

## 3. 재화 (HC - House Currency)

### 개요
HC(House Currency)는 House 단위의 재화입니다. 주간 퀘스트 완료 보상이나 상점 구매 등으로 관리됩니다.

**상태:** ❌ **미구현** - 전용 API 없음 (House.hc 필드는 존재)

### 데이터 모델

#### House HC 필드
| 필드 | 타입 | 설명 | 기본값 |
|------|------|------|--------|
| id | Long | House ID | - |
| hc | Long | House의 현재 HC 잔액 | 0L |

### API 엔드포인트

#### 3.1 House HC 잔액 조회 [미구현]

```
GET /api/houses/{houseId}/currency
```

**상태:** ❌ **미구현** - 현재 백엔드에 없음

**예상 Response (200 OK):**
```json
{
  "houseId": 1,
  "hc": 5000,
  "xp": 1200
}
```

**대체 방법 (현재):**
GET /api/houses/{houseId}를 호출하면 응답에 `hc` 필드 포함

```json
{
  "id": 1,
  "name": "...",
  "xp": 1200,
  "hc": 5000,  // ← House에 직접 포함
  "members": [...]
}
```

**구현 필요:**
```java
@GetMapping("/{houseId}/currency")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<CurrencyDto> getCurrency(
    @PathVariable Long houseId,
    @AuthenticationPrincipal User user) {
  House house = houseService.get(houseId, user.getId());
  return ResponseEntity.ok(new CurrencyDto(house.getId(), house.getHc(), house.getXp()));
}
```

---

#### 3.2 HC 잔액 조회 (다중) [미구현]

```
POST /api/houses/currency/batch
```

**상태:** ❌ **미구현** - 현재 백엔드에 없음

**예상 Request:**
```json
{
  "houseIds": [1, 2, 3]
}
```

**예상 Response (200 OK):**
```json
[
  {
    "houseId": 1,
    "hc": 5000,
    "xp": 1200
  },
  {
    "houseId": 2,
    "hc": 3500,
    "xp": 800
  }
]
```

**구현 필요:**
```java
@PostMapping("/currency/batch")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<List<CurrencyDto>> getCurrencyBatch(
    @RequestBody CurrencyBatchRequest req) {
  List<House> houses = houseRepository.findAllById(req.getHouseIds());
  return ResponseEntity.ok(
    houses.stream()
      .map(h -> new CurrencyDto(h.getId(), h.getHc(), h.getXp()))
      .toList()
  );
}
```

---

#### 3.3 HC 차감 [미구현 - 내부 로직만 있음]

```
PATCH /api/houses/{houseId}/currency/deduct
```

**상태:** ❌ **미구현** - 공개 API 없음  
**현재:** ShopService.buyItem() 내부에서 호출 예정 (주석 처리됨)

**현재 코드:**
```java
// ShopService.java
public void buyItem(Long userId, Long houseId, Long itemId) {
  ShopItem item = shopItemRepository.findById(itemId)
      .orElseThrow(() -> new IllegalArgumentException("상품이 없음"));

  // 유저/하우스의 HC 차감 확인 로직 - 구현됨
  // if (user.getHc() < item.getPriceHc()) 
  //   throw new IllegalStateException("HC가 부족합니다.");
  // user.deductHc(item.getPriceHc());

  inventoryRepository.save(new Inventory(userId, houseId, item));
}
```

**구현 필요사항:**
1. HC 차감 로직 활성화
2. 동시성 제어 (Pessimistic Lock)
3. 트랜잭션 처리

---

## 4. 상점 (Shop)

### 개요
상점은 House가 HC를 사용하여 아이템을 구매할 수 있는 시스템입니다. 구매한 아이템은 Inventory에 저장됩니다.

**상태:** ⚠️ **부분 구현** - 기본 구매만 가능, 대부분 기능 미흡

### 데이터 모델

#### ShopItem Entity
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 아이템 ID |
| name | String | 아이템 명 |
| category | ItemCategory | 카테고리 |
| priceHc | Int | HC 가격 (5의 배수) |
| imageUrl | String | 이미지 URL |

#### ItemCategory Enum
```
BORDER          # 테두리
TITLE           # 타이틀
BANNER          # 배너
THEME           # 테마
CHAT_SKIN       # 채팅 스킨
NICKNAME_DECO   # 닉네임 데코레이션
HOUSE_ICON      # 하우스 아이콘
```

### API 엔드포인트

#### 4.1 상점 아이템 목록 조회

```
GET /api/shop/items
```

**상태:** ✅ **구현됨** (필터/페이지네이션 미흡)

**현재 구현:**
- 모든 아이템 단순 List 반환
- 페이지네이션 미지원
- 카테고리 필터 미지원

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Gold Border",
    "category": "BORDER",
    "priceHc": 500,
    "imageUrl": "https://example.com/images/gold-border.png"
  },
  {
    "id": 2,
    "name": "Silver Title",
    "category": "TITLE",
    "priceHc": 300,
    "imageUrl": "https://example.com/images/silver-title.png"
  }
]
```

**⚠️ 보안 이슈:**
- 인증 없이 누구나 접근 가능 (permitAll)

**구현 개선 필요:**
```java
@GetMapping("/items")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Page<ShopItemDto>> getItems(
    @RequestParam(required = false) String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
  Pageable pageable = PageRequest.of(page, size);
  Page<ShopItem> items = category != null
    ? shopItemRepository.findByCategory(ItemCategory.valueOf(category), pageable)
    : shopItemRepository.findAll(pageable);
  return ResponseEntity.ok(items.map(ShopItemDto::from));
}
```

---

#### 4.2 상점 아이템 상세 조회 [미구현]

```
GET /api/shop/items/{itemId}
```

**상태:** ❌ **미구현** - 현재 백엔드에 없음

**예상 Response (200 OK):**
```json
{
  "id": 1,
  "name": "Gold Border",
  "category": "BORDER",
  "priceHc": 500,
  "imageUrl": "https://example.com/images/gold-border.png",
  "description": "고급 테두리 디자인"
}
```

**구현 필요:**
```java
@GetMapping("/items/{itemId}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ShopItemDto> getItem(@PathVariable Long itemId) {
  ShopItem item = shopItemRepository.findById(itemId)
    .orElseThrow(() -> new ResourceNotFoundException("아이템 없음"));
  return ResponseEntity.ok(ShopItemDto.from(item));
}
```

---

#### 4.3 아이템 구매

```
POST /api/shop/buy
```

**상태:** ⚠️ **부분 구현** - HC 차감/검증 미흡

**현재 구현 (RequestParam 방식):**
```
POST /api/shop/buy?userId=100&itemId=1&houseId=1
```

**현재 Response (200 OK):**
```json
// void - 응답 없음
```

**⚠️ 현재 문제점:**

| 문제 | 상태 | 설명 |
|------|------|------|
| **인증 없음** | ❌ | userId를 파라미터로 받음 (보안 위험) |
| **권한 검증 없음** | ❌ | House 멤버 확인 안함 |
| **HC 차감 미구현** | ❌ | 재화 실제 차감 안함 |
| **HC 잔액 확인 안함** | ❌ | 부족해도 구매 가능 |
| **중복 구매 방지 없음** | ❌ | 같은 아이템 여러번 구매 가능 |
| **응답 DTO 없음** | ❌ | 구매 결과 알 수 없음 |

**개선된 API 명세:**

```
POST /api/shop/buy
Content-Type: application/json

{
  "itemId": 1,
  "houseId": 1  // 옵션: House 전용 아이템인 경우만
}
```

**예상 Response (200 OK):**
```json
{
  "inventoryId": 5001,
  "itemId": 1,
  "itemName": "Gold Border",
  "category": "BORDER",
  "priceHc": 500,
  "purchasedAt": "2024-08-28T12:30:00Z",
  "message": "구매 완료"
}
```

**예상 Error Responses:**
- 401 UNAUTHORIZED: 인증 필요
- 403 FORBIDDEN: House 멤버가 아님
- 404 NOT_FOUND: 아이템 없음
- 409 CONFLICT: HC 부족

**필수 구현 사항:**
```java
@PostMapping("/buy")
@PreAuthorize("isAuthenticated()")
@Transactional
public ResponseEntity<ShopBuyDto> buyItem(
    @RequestBody ShopBuyRequest req,
    @AuthenticationPrincipal User user) {
  
  // 1. 아이템 조회
  ShopItem item = shopItemRepository.findById(req.getItemId())
    .orElseThrow(() -> new ResourceNotFoundException("아이템 없음"));
  
  // 2. House 멤버 확인 (필수)
  if (req.getHouseId() != null) {
    houseService.requireApprovedMember(req.getHouseId(), user.getId());
  }
  
  // 3. HC 잔액 확인 (필수)
  House house = houseRepository.findByIdWithLock(req.getHouseId())  // Pessimistic Lock
    .orElseThrow(() -> new ResourceNotFoundException("House 없음"));
  if (house.getHc() < item.getPriceHc()) {
    throw new InsufficientCurrencyException("HC 부족");
  }
  
  // 4. HC 차감 (필수)
  house.deductHc(item.getPriceHc());
  
  // 5. Inventory 저장
  Inventory inventory = inventoryRepository.save(
    new Inventory(user.getId(), req.getHouseId(), item)
  );
  
  return ResponseEntity.ok(ShopBuyDto.from(inventory));
}
```

---

#### 4.4 구매 이력 조회 [미구현]

```
GET /api/shop/inventory
```

**상태:** ❌ **미구현** - 현재 백엔드에 없음

**예상 Request:**
```
GET /api/shop/inventory?userId=100&houseId=1&category=BORDER
```

**예상 Response (200 OK):**
```json
[
  {
    "inventoryId": 5001,
    "itemId": 1,
    "itemName": "Gold Border",
    "category": "BORDER",
    "priceHc": 500,
    "imageUrl": "https://example.com/images/gold-border.png",
    "purchasedAt": "2024-08-28T12:30:00Z"
  }
]
```

**구현 필요:**
```java
@GetMapping("/inventory")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<List<InventoryDto>> getInventory(
    @RequestParam Long userId,
    @RequestParam(required = false) Long houseId,
    @RequestParam(required = false) String category,
    @AuthenticationPrincipal User user) {
  
  // 본인 inventory만 조회 가능
  if (!user.getId().equals(userId)) {
    throw new ForbiddenException("권한 없음");
  }
  
  List<Inventory> items = inventoryRepository.findByUserIdAndHouseIdAndCategory(
    userId, houseId, category
  );
  return ResponseEntity.ok(items.stream()
    .map(InventoryDto::from)
    .toList());
}
```

---

#### 4.4 안전한 구매 (Safe Purchase with Locking)

```
POST /api/shop/buy
Content-Type: application/json
```

**상태:** ⚠️ **부분 구현** - 기본 로직은 있으나 보안/동시성 제어 부족

**안전한 구매 구현 명세:**

이 API는 Pessimistic Lock을 사용하여 HC 차감 시 동시성 문제를 방지합니다.

**Request (개선된 형식):**
```json
{
  "itemId": 1,
  "houseId": 1
}
```

**Request Fields:**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| itemId | Long | O | 구매할 아이템 ID |
| houseId | Long | O | House ID |

**Response (200 OK):**
```json
{
  "inventoryId": 5001,
  "itemId": 1,
  "itemName": "Gold Border",
  "category": "BORDER",
  "priceHc": 500,
  "purchasedAt": "2024-08-28T12:30:00Z",
  "remainingHc": 4500,
  "message": "구매 완료"
}
```

**Response Fields:**
| 필드 | 타입 | 설명 |
|------|------|------|
| inventoryId | Long | Inventory ID (구매 이력 추적용) |
| itemId | Long | 구매한 아이템 ID |
| itemName | String | 아이템 이름 |
| category | String | 아이템 카테고리 |
| priceHc | Int | 차감된 HC 금액 |
| purchasedAt | LocalDateTime | 구매 시각 |
| remainingHc | Long | 구매 후 남은 HC |
| message | String | 성공 메시지 |

**Error Responses:**
- 401 UNAUTHORIZED: 인증 필요
- 403 FORBIDDEN: House 멤버가 아님
- 404 NOT_FOUND: 아이템 또는 House 없음
- 409 CONFLICT: HC 부족
- 409 CONFLICT: 동시 구매 중복 요청

**필수 구현 사항:**

```java
// 1. HouseRepository에 Pessimistic Lock 메서드 추가
@Repository
public interface HouseRepository extends JpaRepository<House, Long> {
  @Query("SELECT h FROM House h WHERE h.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<House> findByIdWithLock(@Param("id") Long id);
}

// 2. ShopService 개선
@Service
@Transactional
public class ShopService {
  
  @Transactional
  public ShopBuyDto buyItem(Long userId, Long houseId, Long itemId) {
    // 1. 아이템 조회 (Inventory 중복 확인)
    ShopItem item = shopItemRepository.findById(itemId)
      .orElseThrow(() -> new ResourceNotFoundException("아이템 없음"));
    
    // 2. House 잠금 획득 (동시성 제어)
    House house = houseRepository.findByIdWithLock(houseId)
      .orElseThrow(() -> new ResourceNotFoundException("House 없음"));
    
    // 3. HC 잔액 확인
    if (house.getHc() < item.getPriceHc()) {
      throw new InsufficientCurrencyException(
        String.format("HC 부족. 필요: %d, 보유: %d", 
          item.getPriceHc(), house.getHc())
      );
    }
    
    // 4. 중복 구매 확인 (같은 카테고리 중복 구매 방지)
    boolean alreadyOwned = inventoryRepository
      .existsByUserIdAndHouseIdAndItemCategory(
        userId, houseId, item.getCategory()
      );
    
    if (alreadyOwned && isUniquePerCategory(item.getCategory())) {
      throw new ConflictException(
        String.format("%s 카테고리는 1개만 소유 가능", item.getCategory())
      );
    }
    
    // 5. HC 차감 (원자적 연산)
    house.deductHc(item.getPriceHc());
    houseRepository.save(house);
    
    // 6. Inventory 저장
    Inventory inventory = new Inventory(userId, houseId, item);
    inventory = inventoryRepository.save(inventory);
    
    return ShopBuyDto.from(inventory, house.getHc());
  }
  
  private boolean isUniquePerCategory(ItemCategory category) {
    // 카테고리별 정책: BORDER, TITLE, BANNER는 1개만
    return category == ItemCategory.BORDER || 
           category == ItemCategory.TITLE || 
           category == ItemCategory.BANNER;
  }
}

// 3. Controller 구현
@RestController
@RequestMapping("/api/shop")
public class ShopController {
  
  @PostMapping("/buy")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ShopBuyDto> buyItem(
      @RequestBody ShopBuyRequest req,
      @AuthenticationPrincipal User user) {
    
    // House 멤버 검증
    houseService.requireApprovedMember(req.getHouseId(), user.getId());
    
    ShopBuyDto result = shopService.buyItem(
      user.getId(), req.getHouseId(), req.getItemId()
    );
    
    return ResponseEntity.ok(result);
  }
}

// 4. DTO 정의
@Data
public class ShopBuyRequest {
  @NotNull(message = "itemId 필수")
  private Long itemId;
  
  @NotNull(message = "houseId 필수")
  private Long houseId;
}

@Data
public class ShopBuyDto {
  private Long inventoryId;
  private Long itemId;
  private String itemName;
  private String category;
  private Integer priceHc;
  private LocalDateTime purchasedAt;
  private Long remainingHc;
  private String message;
  
  public static ShopBuyDto from(Inventory inventory, Long remainingHc) {
    ShopBuyDto dto = new ShopBuyDto();
    dto.inventoryId = inventory.getId();
    dto.itemId = inventory.getItem().getId();
    dto.itemName = inventory.getItem().getName();
    dto.category = inventory.getItem().getCategory().name();
    dto.priceHc = inventory.getItem().getPriceHc();
    dto.purchasedAt = LocalDateTime.now();
    dto.remainingHc = remainingHc;
    dto.message = "구매 완료";
    return dto;
  }
}
```

**동시성 제어 시나리오:**

```
시나리오: 같은 House에서 2명이 동시에 HC 500 아이템 구매 (보유: HC 600)

1. 요청 A, B 동시 도착
2. 요청 A: findByIdWithLock(houseId) → Lock 획득
3. 요청 B: findByIdWithLock(houseId) → 대기 (A가 Lock 해제할 때까지)
4. 요청 A: HC 600 - 500 = 100 남음 → 저장 → Lock 해제
5. 요청 B: Lock 획득 → HC 확인: 100 < 500 → InsufficientCurrencyException 발생
6. 결과: A만 구매 성공, B는 실패
```

---

#### 4.5 Inventory 조회

```
GET /api/shop/inventory
```

**상태:** ❌ **미구현** - 현재 백엔드에 없음

**API 명세:**

**Request:**
```
GET /api/shop/inventory?houseId=1&category=BORDER&page=0&size=20
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| houseId | Long | O | House ID |
| category | String | X | 필터: BORDER, TITLE, BANNER 등 |
| page | Int | X | 페이지 (기본: 0) |
| size | Int | X | 페이지 크기 (기본: 20) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "inventoryId": 5001,
      "itemId": 1,
      "itemName": "Gold Border",
      "category": "BORDER",
      "priceHc": 500,
      "imageUrl": "https://example.com/images/gold-border.png",
      "purchasedAt": "2024-08-28T12:30:00Z",
      "isApplied": true,
      "appliedAt": "2024-08-28T13:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

**Response Fields:**
| 필드 | 타입 | 설명 |
|------|------|------|
| inventoryId | Long | Inventory ID |
| itemId | Long | ShopItem ID |
| itemName | String | 아이템 이름 |
| category | String | 카테고리 |
| priceHc | Int | 구매 가격 |
| imageUrl | String | 이미지 URL |
| purchasedAt | LocalDateTime | 구매 시각 |
| isApplied | Boolean | 적용 여부 |
| appliedAt | LocalDateTime | 적용 시각 |

**구현 필요:**
```java
@GetMapping("/inventory")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Page<InventoryDto>> getInventory(
    @RequestParam Long houseId,
    @RequestParam(required = false) String category,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @AuthenticationPrincipal User user) {
  
  // House 멤버 검증
  houseService.requireApprovedMember(houseId, user.getId());
  
  Pageable pageable = PageRequest.of(page, size, Sort.by("purchasedAt").descending());
  
  Page<Inventory> items = category != null
    ? inventoryRepository.findByHouseIdAndItemCategory(
        houseId, ItemCategory.valueOf(category), pageable)
    : inventoryRepository.findByHouseId(houseId, pageable);
  
  return ResponseEntity.ok(items.map(InventoryDto::from));
}
```

---

#### 4.6 아이템 적용 및 해제

**상태:** ❌ **미구현** - 현재 백엔드에 없음

##### 4.6.1 아이템 적용

```
POST /api/shop/inventory/{inventoryId}/apply
```

**설명:**
구매한 아이템을 실제로 적용합니다. 카테고리별 적용 방식이 다릅니다.
- BORDER, TITLE, BANNER: House에 적용
- THEME: House 테마 변경
- CHAT_SKIN, NICKNAME_DECO: 사용자 프로필에 적용
- HOUSE_ICON: House 아이콘 변경

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| inventoryId | Long | Inventory ID |

**Response (200 OK):**
```json
{
  "inventoryId": 5001,
  "itemId": 1,
  "itemName": "Gold Border",
  "category": "BORDER",
  "isApplied": true,
  "appliedAt": "2024-08-28T13:00:00Z",
  "previousItemId": null,
  "message": "아이템이 적용되었습니다"
}
```

**Error Responses:**
- 401 UNAUTHORIZED: 인증 필요
- 403 FORBIDDEN: House 멤버가 아님
- 404 NOT_FOUND: Inventory 없음
- 409 CONFLICT: 이미 적용 중

**구현 필요:**
```java
@PostMapping("/inventory/{inventoryId}/apply")
@PreAuthorize("isAuthenticated()")
@Transactional
public ResponseEntity<ItemApplyDto> applyItem(
    @PathVariable Long inventoryId,
    @AuthenticationPrincipal User user) {
  
  Inventory inventory = inventoryRepository.findById(inventoryId)
    .orElseThrow(() -> new ResourceNotFoundException("Inventory 없음"));
  
  // House 멤버 검증
  houseService.requireApprovedMember(inventory.getHouseId(), user.getId());
  
  ShopItem item = inventory.getItem();
  House house = houseRepository.findById(inventory.getHouseId())
    .orElseThrow(() -> new ResourceNotFoundException("House 없음"));
  
  // 기존 적용 아이템 해제
  Long previousItemId = null;
  if (item.getCategory() == ItemCategory.BORDER) {
    previousItemId = house.getAppliedBorderId();
    house.setAppliedBorderId(item.getId());
  } else if (item.getCategory() == ItemCategory.TITLE) {
    previousItemId = house.getAppliedTitleId();
    house.setAppliedTitleId(item.getId());
  } else if (item.getCategory() == ItemCategory.BANNER) {
    previousItemId = house.getAppliedBannerId();
    house.setAppliedBannerId(item.getId());
  } else if (item.getCategory() == ItemCategory.THEME) {
    previousItemId = house.getAppliedThemeId();
    house.setAppliedThemeId(item.getId());
  }
  
  inventory.setApplied(true);
  inventory.setAppliedAt(LocalDateTime.now());
  
  houseRepository.save(house);
  inventoryRepository.save(inventory);
  
  return ResponseEntity.ok(ItemApplyDto.from(inventory, previousItemId));
}
```

##### 4.6.2 아이템 해제

```
DELETE /api/shop/inventory/{inventoryId}/apply
```

**Response (200 OK):**
```json
{
  "inventoryId": 5001,
  "itemId": 1,
  "itemName": "Gold Border",
  "category": "BORDER",
  "isApplied": false,
  "message": "아이템이 해제되었습니다"
}
```

**구현 필요:**
```java
@DeleteMapping("/inventory/{inventoryId}/apply")
@PreAuthorize("isAuthenticated()")
@Transactional
public ResponseEntity<ItemApplyDto> removeItem(
    @PathVariable Long inventoryId,
    @AuthenticationPrincipal User user) {
  
  Inventory inventory = inventoryRepository.findById(inventoryId)
    .orElseThrow(() -> new ResourceNotFoundException("Inventory 없음"));
  
  // House 멤버 검증
  houseService.requireApprovedMember(inventory.getHouseId(), user.getId());
  
  ShopItem item = inventory.getItem();
  House house = houseRepository.findById(inventory.getHouseId())
    .orElseThrow(() -> new ResourceNotFoundException("House 없음"));
  
  // 적용 아이템 제거
  if (item.getCategory() == ItemCategory.BORDER && 
      house.getAppliedBorderId() != null &&
      house.getAppliedBorderId().equals(item.getId())) {
    house.setAppliedBorderId(null);
  } else if (item.getCategory() == ItemCategory.TITLE && 
             house.getAppliedTitleId() != null &&
             house.getAppliedTitleId().equals(item.getId())) {
    house.setAppliedTitleId(null);
  }
  // ... 다른 카테고리도 동일하게 처리
  
  inventory.setApplied(false);
  inventory.setAppliedAt(null);
  
  houseRepository.save(house);
  inventoryRepository.save(inventory);
  
  return ResponseEntity.ok(ItemApplyDto.from(inventory, null));
}
```

---

## 2.4 퀘스트 인증 및 멤버십 보안

### 개요
기존 퀘스트 API의 보안 요구사항을 명시합니다.

**현재 상태:** ❌ **미구현** - 인증/권한 검증 없음

### 보안 요구사항

#### 인증 (Authentication)
모든 퀘스트 API는 JWT 기반 인증이 필수입니다.

**현재 문제점:**
```java
// ❌ 잘못된 방식 - 누구나 접근 가능
@GetMapping("/{houseId}/quests")
public List<HouseQuestResponseDto> getQuests(@PathVariable Long houseId) { }
```

**개선된 방식:**
```java
// ✅ 올바른 방식 - 인증된 사용자만 접근
@GetMapping("/{houseId}/quests")
@PreAuthorize("isAuthenticated()")
public List<HouseQuestResponseDto> getQuests(
    @PathVariable Long houseId,
    @AuthenticationPrincipal User user) { }
```

#### 권한 검증 (Authorization)
House 멤버가 아닌 사용자는 해당 House의 퀘스트에 접근할 수 없습니다.

**구현 요구사항:**
```java
@Service
public class HouseService {
  
  /**
   * House의 승인된 멤버만 접근 가능하도록 검증
   */
  public void requireApprovedMember(Long houseId, Long userId) {
    HouseMember member = houseMemberRepository
      .findByHouseIdAndUserId(houseId, userId)
      .orElseThrow(() -> new ForbiddenException("House 멤버가 아닙니다"));
    
    if (member.getStatus() != MemberStatus.APPROVED) {
      throw new ForbiddenException("승인되지 않은 멤버입니다");
    }
  }
}
```

#### 완성된 퀘스트 API 예제

```java
@RestController
@RequestMapping("/api/houses")
public class QuestController {
  
  // 1. 주간 퀘스트 조회 - 보안 적용
  @GetMapping("/{houseId}/quests")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<HouseQuestResponseDto>> getQuests(
      @PathVariable Long houseId,
      @AuthenticationPrincipal User user) {
    
    // House 멤버 검증
    houseService.requireApprovedMember(houseId, user.getId());
    
    List<HouseQuest> quests = questService.getWeeklyQuests(houseId);
    return ResponseEntity.ok(
      quests.stream()
        .map(q -> new HouseQuestResponseDto(
          q.getId(),
          q.getQuestType(),
          q.getCurrentCount(),
          q.getQuestType().getTargetCount(),
          q.isCompleted(),
          q.isRewardClaimed(),
          q.getQuestType().getRewardXp(),
          q.getQuestType().getRewardHc(),
          q.getWeekStartDate()
        ))
        .toList()
    );
  }
  
  // 2. 퀘스트 진행도 업데이트 - 보안 적용
  @PatchMapping("/{houseId}/quests/{questId}/progress")
  @PreAuthorize("isAuthenticated()")
  @Transactional
  public ResponseEntity<HouseQuestResponseDto> updateQuestProgress(
      @PathVariable Long houseId,
      @PathVariable Long questId,
      @RequestBody QuestProgressRequest req,
      @AuthenticationPrincipal User user) {
    
    // House 멤버 검증
    houseService.requireApprovedMember(houseId, user.getId());
    
    // 퀘스트 업데이트
    HouseQuest quest = questService.updateQuestProgress(questId, req.getIncrement());
    
    return ResponseEntity.ok(HouseQuestResponseDto.from(quest));
  }
  
  // 3. 퀘스트 보상 수령 - 보안 적용
  @PostMapping("/{houseId}/quests/{questId}/claim")
  @PreAuthorize("isAuthenticated()")
  @Transactional
  public ResponseEntity<QuestClaimResponseDto> claimReward(
      @PathVariable Long houseId,
      @PathVariable Long questId,
      @AuthenticationPrincipal User user) {
    
    // House 멤버 검증
    houseService.requireApprovedMember(houseId, user.getId());
    
    // 보상 수령
    questService.claimQuestReward(houseId, questId);
    
    HouseQuest quest = questRepository.findById(questId).orElseThrow();
    return ResponseEntity.ok(QuestClaimResponseDto.from(quest));
  }
}
```

**보안 필터 설정:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/houses/**").authenticated()
        .requestMatchers("/api/shop/**").authenticated()
        .requestMatchers("/api/users/**").authenticated()
        .anyRequest().permitAll()
      )
      .oauth2Login(/* config */)
      .build();
    
    return http.build();
  }
}
```

---

## 5. 랭킹 시스템 (Ranking)

### 개요
House를 XP, HC, 또는 기타 지표로 랭킹합니다. 전체 랭킹과 카테고리별 랭킹을 지원합니다.

**상태:** ❌ **미구현** - 전용 API 없음

### 데이터 모델

House 랭킹은 House의 xp, hc, memberCount를 기반으로 계산됩니다.

### API 엔드포인트

#### 5.1 전체 House 랭킹 조회

```
GET /api/houses/ranking
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| sortBy | String | X | xp (기본), hc, memberCount |
| period | String | X | all (기본), weekly, monthly |
| page | Int | X | 페이지 (기본: 0) |
| size | Int | X | 페이지 크기 (기본: 50) |

**Response (200 OK):**
```json
{
  "content": [
    {
      "rank": 1,
      "houseId": 1,
      "houseName": "Top House",
      "leaderId": 100,
      "leaderName": "Leader01",
      "totalXp": 125000,
      "totalHc": 50000,
      "memberCount": 15,
      "createdAt": "2024-01-15T00:00:00Z"
    },
    {
      "rank": 2,
      "houseId": 2,
      "houseName": "Great House",
      "leaderId": 200,
      "leaderName": "Leader02",
      "totalXp": 110000,
      "totalHc": 45000,
      "memberCount": 12,
      "createdAt": "2024-02-10T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 5000,
  "totalPages": 100,
  "hasNext": true
}
```

**Response Fields:**
| 필드 | 타입 | 설명 |
|------|------|------|
| rank | Int | 현재 순위 |
| houseId | Long | House ID |
| houseName | String | House 이름 |
| leaderId | Long | Leader User ID |
| leaderName | String | Leader 닉네임 |
| totalXp | Long | 누적 XP |
| totalHc | Long | 누적 HC |
| memberCount | Int | 멤버 수 |
| createdAt | LocalDateTime | House 생성일 |

**구현 필요:**
```java
@RestController
@RequestMapping("/api/houses")
public class RankingController {
  
  @GetMapping("/ranking")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<HouseRankingDto>> getRanking(
      @RequestParam(defaultValue = "xp") String sortBy,
      @RequestParam(defaultValue = "all") String period,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    Page<House> houses = rankingService.getRanking(sortBy, period, pageable);
    
    return ResponseEntity.ok(
      houses.map((house, index) -> 
        HouseRankingDto.from(house, (long)(page * size + index + 1))
      )
    );
  }
}

@Service
public class RankingService {
  
  public Page<House> getRanking(String sortBy, String period, Pageable pageable) {
    Sort sort = Sort.by(Sort.Direction.DESC,
      "xp".equals(sortBy) ? "xp" :
      "hc".equals(sortBy) ? "hc" :
      "memberCount"
    );
    
    Pageable sortedPageable = PageRequest.of(
      pageable.getPageNumber(), 
      pageable.getPageSize(), 
      sort
    );
    
    if ("weekly".equals(period)) {
      LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
      return houseRepository.findByCreatedAtAfterOrderByXpDesc(weekAgo, sortedPageable);
    } else if ("monthly".equals(period)) {
      LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
      return houseRepository.findByCreatedAtAfterOrderByXpDesc(monthAgo, sortedPageable);
    }
    
    return houseRepository.findAll(sortedPageable);
  }
}
```

#### 5.2 사용자의 House 랭킹 조회

```
GET /api/houses/ranking/my
```

**Response (200 OK):**
```json
{
  "rank": 150,
  "houseId": 50,
  "houseName": "My House",
  "totalXp": 8500,
  "totalHc": 3000,
  "percentile": 97.0,
  "message": "상위 3%에 위치합니다"
}
```

**구현 필요:**
```java
@GetMapping("/ranking/my")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<MyRankingDto> getMyRanking(
    @AuthenticationPrincipal User user) {
  
  // 사용자가 속한 House 조회
  House house = houseRepository.findByLeaderId(user.getId())
    .orElseThrow(() -> new ResourceNotFoundException("House 없음"));
  
  // 랭킹 계산
  MyRankingDto result = rankingService.getMyRanking(house.getId());
  
  return ResponseEntity.ok(result);
}
```

---

## 6. 사용자 추천 및 이벤트 연결 (People Recommendation)

### 개요
같은 관심사를 가진 플레이어를 추천하고, 함께 할 사람을 찾을 수 있는 기능입니다.

**상태:** ❌ **미구현** - 전용 API 없음

### 데이터 모델

추천 로직은 다음 요소를 기반으로 합니다:
- 같은 House에 속한 사용자
- 비슷한 레벨/경험치를 가진 사용자
- 같은 이벤트에 참여한 사용자
- 최근 플레이 활동이 있는 사용자

### API 엔드포인트

#### 6.1 추천 플레이어 조회

```
GET /api/users/recommendations/players
```

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| houseId | Long | O | House ID (같은 House의 추천 플레이어) |
| limit | Int | X | 추천 수 (기본: 10, 최대: 50) |

**Response (200 OK):**
```json
[
  {
    "userId": 200,
    "nickname": "Player01",
    "level": 25,
    "totalXp": 12000,
    "houseId": 1,
    "lastActiveAt": "2024-08-28T10:30:00Z",
    "matchRatio": 95.5,
    "reason": "최근 활동이 활발한 멤버"
  },
  {
    "userId": 300,
    "nickname": "Player02",
    "level": 24,
    "totalXp": 11500,
    "houseId": 1,
    "lastActiveAt": "2024-08-27T18:00:00Z",
    "matchRatio": 92.0,
    "reason": "비슷한 경험치 수준"
  }
]
```

**Response Fields:**
| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| nickname | String | 닉네임 |
| level | Int | 레벨 |
| totalXp | Long | 누적 경험치 |
| houseId | Long | 소속 House ID |
| lastActiveAt | LocalDateTime | 마지막 활동 시각 |
| matchRatio | Double | 매칭 적합도 (0-100) |
| reason | String | 추천 이유 |

**구현 필요:**
```java
@RestController
@RequestMapping("/api/users")
public class RecommendationController {
  
  @GetMapping("/recommendations/players")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<PlayerRecommendationDto>> getPlayerRecommendations(
      @RequestParam Long houseId,
      @RequestParam(defaultValue = "10") int limit,
      @AuthenticationPrincipal User user) {
    
    // House 멤버 검증
    houseService.requireApprovedMember(houseId, user.getId());
    
    // 추천 조회
    List<PlayerRecommendationDto> recommendations = 
      recommendationService.getPlayerRecommendations(houseId, user.getId(), limit);
    
    return ResponseEntity.ok(recommendations);
  }
}

@Service
public class RecommendationService {
  
  public List<PlayerRecommendationDto> getPlayerRecommendations(
      Long houseId, Long userId, int limit) {
    
    // 1. 현재 사용자 정보 조회
    User currentUser = userRepository.findById(userId).orElseThrow();
    
    // 2. 같은 House의 다른 멤버 조회
    List<HouseMember> members = houseMemberRepository
      .findByHouseIdAndUserIdNotAndStatusApproved(houseId, userId);
    
    // 3. 각 멤버의 매칭 점수 계산
    List<PlayerRecommendationDto> recommendations = members.stream()
      .map(member -> calculateMatchScore(currentUser, member.getUser()))
      .filter(dto -> dto.getMatchRatio() > 70.0)  // 70% 이상만
      .sorted((a, b) -> Double.compare(b.getMatchRatio(), a.getMatchRatio()))
      .limit(limit)
      .collect(Collectors.toList());
    
    return recommendations;
  }
  
  private PlayerRecommendationDto calculateMatchScore(User user1, User user2) {
    double score = 0.0;
    
    // XP 기반 매칭 (±20% 범위)
    double xpDiff = Math.abs(user1.getTotalXp() - user2.getTotalXp());
    double xpRange = Math.max(user1.getTotalXp(), user2.getTotalXp()) * 0.2;
    double xpScore = Math.max(0, 40 * (1 - xpDiff / xpRange));
    
    // 레벨 기반 매칭 (±2 레벨)
    int levelDiff = Math.abs(user1.getLevel() - user2.getLevel());
    double levelScore = Math.max(0, 30 * (1 - levelDiff / 2.0));
    
    // 활동도 기반 (최근 활동 기준)
    LocalDateTime userLastActive = user1.getLastActiveAt();
    LocalDateTime otherLastActive = user2.getLastActiveAt();
    long daysDiff = ChronoUnit.DAYS.between(otherLastActive, userLastActive);
    double activityScore = daysDiff <= 3 ? 30 : 0;
    
    score = xpScore + levelScore + activityScore;
    
    String reason = determinePriority(xpScore, levelScore, activityScore);
    
    return PlayerRecommendationDto.from(user2, score / 100.0 * 100, reason);
  }
  
  private String determinePriority(double xpScore, double levelScore, double activityScore) {
    if (activityScore > 20) return "최근 활동이 활발한 멤버";
    if (xpScore > levelScore) return "비슷한 경험치 수준";
    return "비슷한 레벨";
  }
}
```

#### 6.2 이벤트별 플레이어 조회

```
GET /api/events/{eventId}/players
```

**상태:** ❌ **미구현** - Event 시스템 연동 필요

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| role | String | X | 찾는 역할 (e.g., "TANK", "SUPPORT", "DPS") |
| limit | Int | X | 최대 인원 (기본: 5) |

**Response (200 OK):**
```json
{
  "eventId": 10001,
  "eventName": "Raid Battle",
  "requiredPlayers": [
    {
      "userId": 200,
      "nickname": "Warrior01",
      "level": 30,
      "role": "TANK",
      "status": "AVAILABLE"
    },
    {
      "userId": 300,
      "nickname": "Mage02",
      "level": 29,
      "role": "SUPPORT",
      "status": "IN_PROGRESS"
    }
  ]
}
```

**구현 필요:**
```java
@GetMapping("/events/{eventId}/players")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<EventPlayerDto> getEventPlayers(
    @PathVariable Long eventId,
    @RequestParam(required = false) String role,
    @RequestParam(defaultValue = "5") int limit) {
  
  // Event 조회
  Event event = eventRepository.findById(eventId)
    .orElseThrow(() -> new ResourceNotFoundException("Event 없음"));
  
  // Event에 참여 중인 플레이어 조회
  List<User> players = eventService.getEventPlayers(eventId, role, limit);
  
  return ResponseEntity.ok(EventPlayerDto.from(event, players));
}
```

#### 6.3 함께 할 사람 찾기 (매칭 요청)

```
POST /api/users/matchmaking/request
```

**Request:**
```json
{
  "houseId": 1,
  "eventId": 10001,
  "targetRole": "SUPPORT",
  "preferredLevel": 25,
  "maxPlayers": 3
}
```

**Response (201 CREATED):**
```json
{
  "requestId": "match_20240828_001",
  "status": "WAITING",
  "createdAt": "2024-08-28T12:00:00Z",
  "expiresAt": "2024-08-28T12:30:00Z",
  "candidates": [
    {
      "userId": 200,
      "nickname": "Player01",
      "level": 24,
      "matchScore": 95
    }
  ]
}
```

**구현 필요:**
```java
@PostMapping("/matchmaking/request")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<MatchmakingResponseDto> requestMatching(
    @RequestBody MatchmakingRequest req,
    @AuthenticationPrincipal User user) {
  
  // 1. House 멤버 검증
  houseService.requireApprovedMember(req.getHouseId(), user.getId());
  
  // 2. 매칭 요청 생성
  MatchmakingRequest request = matchmakingService.createRequest(req, user.getId());
  
  // 3. 후보자 검색 (비동기)
  CompletableFuture.runAsync(() -> 
    matchmakingService.findCandidates(request)
  );
  
  return ResponseEntity.status(201).body(
    MatchmakingResponseDto.from(request)
  );
}
```

---

## 요청/응답 공통 형식

### 에러 응답
```json
{
  "timestamp": "2024-08-28T12:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "INVALID_INPUT",
  "message": "유효하지 않은 입력입니다.",
  "path": "/api/houses/1/quests"
}
```

**Error Fields:**
| 필드 | 타입 | 설명 |
|------|------|------|
| timestamp | LocalDateTime | 에러 발생 시각 |
| status | Int | HTTP 상태 코드 |
| error | String | 에러 타입 |
| errorCode | String | 비즈니스 에러 코드 |
| message | String | 에러 메시지 |
| path | String | 요청 경로 |

### 공통 HTTP 상태 코드
| 코드 | 설명 |
|------|------|
| 200 | OK - 요청 성공 |
| 201 | CREATED - 리소스 생성 성공 |
| 400 | BAD_REQUEST - 유효하지 않은 요청 |
| 401 | UNAUTHORIZED - 인증 필요 |
| 403 | FORBIDDEN - 권한 없음 |
| 404 | NOT_FOUND - 리소스 없음 |
| 409 | CONFLICT - 충돌 (HC 부족 등) |
| 500 | INTERNAL_SERVER_ERROR - 서버 에러 |

---

---

## 📌 주요 구현 완료 및 TODO 체크리스트

### 필수 구현 순서

#### Phase 1: 보안 강화 (최우선)
```
[ ] SecurityConfig에서 /api/houses/, /api/shop/ 보호
    - 현재: permitAll
    - 변경: @PreAuthorize("isAuthenticated()")
    
[ ] JWT 기반 사용자 인증
    - @AuthenticationPrincipal User user 활용
    - userId를 RequestParam으로 받지 않기
    
[ ] House 멤버십 검증
    - houseService.requireApprovedMember(houseId, userId)
    - 모든 House 관련 API에 추가

[ ] 퀘스트 API 보안 (Section 2.4)
    - GET /api/houses/{houseId}/quests - @PreAuthorize 추가
    - PATCH /api/houses/{houseId}/quests/{questId}/progress - 멤버십 검증
    - POST /api/houses/{houseId}/quests/{questId}/claim - 멤버십 검증
```

#### Phase 2: 주간 퀘스트 완성
```
[ ] HouseQuestResponseDto 확장
    - questType, currentCount, targetCount 추가
    - isCompleted, weekStartDate 추가
    
[ ] PATCH /api/houses/{houseId}/quests/{questId}/progress 구현 (Section 2.2)
    - 진행도 업데이트 로직
    - 목표치 도달 시 isCompleted = true
    - 멤버십 검증 추가
    
[ ] POST /api/houses/{houseId}/quests/{questId}/claim 개선 (Section 2.3)
    - House 멤버십 검증 추가
    - 응답 DTO 작성
```

#### Phase 3: 재화(HC) 시스템
```
[ ] GET /api/houses/{houseId}/currency 구현 (Section 3.1)
    - House HC 정보 조회 (House GET에서 분리)
    
[ ] POST /api/houses/currency/batch 구현 (Section 3.2)
    - 다중 House의 HC 조회
    
[ ] HC 차감 로직 활성화 (Section 3.3)
    - ShopService.buyItem()에서 주석 풀기
    - Pessimistic Lock 추가 (@Lock(LockModeType.PESSIMISTIC_WRITE))
```

#### Phase 4: 상점 기능 완성
```
[ ] GET /api/shop/items 개선 (Section 4.1)
    - 페이지네이션 지원
    - 카테고리 필터 지원
    - 보안: @PreAuthorize 추가
    
[ ] GET /api/shop/items/{itemId} 구현 (Section 4.2)
    - 아이템 상세 조회
    
[ ] POST /api/shop/buy 개선 - 안전한 구매 (Section 4.4)
    - RequestBody 방식으로 변경 (userId 제거)
    - @AuthenticationPrincipal에서 사용자 추출
    - HC 잔액 확인 및 차감 로직
    - Pessimistic Lock으로 동시성 제어
    - 중복 구매 방지 로직
    - 응답 DTO 작성
    
[ ] GET /api/shop/inventory 구현 (Section 4.5)
    - 구매 이력 조회
    - House별 Inventory 조회
    - 카테고리 필터 지원
    - 페이지네이션 지원
    
[ ] POST /api/shop/inventory/{inventoryId}/apply 구현 (Section 4.6.1)
    - 아이템 적용 로직
    - 카테고리별 적용 방식 구현 (BORDER, TITLE, BANNER, THEME 등)
    - 기존 적용 아이템 자동 해제
    
[ ] DELETE /api/shop/inventory/{inventoryId}/apply 구현 (Section 4.6.2)
    - 아이템 해제 로직
```

#### Phase 5: 랭킹 시스템
```
[ ] GET /api/houses/ranking 구현 (Section 5.1)
    - 전체 House 랭킹 조회
    - 정렬: xp, hc, memberCount 지원
    - 기간 필터: all, weekly, monthly 지원
    - 페이지네이션 지원
    
[ ] GET /api/houses/ranking/my 구현 (Section 5.2)
    - 사용자 소속 House의 랭킹 조회
    - 백분위수 계산
```

#### Phase 6: 사용자 추천 및 매칭
```
[ ] GET /api/users/recommendations/players 구현 (Section 6.1)
    - 추천 플레이어 조회
    - 매칭 점수 계산 (XP, 레벨, 활동도 기반)
    
[ ] GET /api/events/{eventId}/players 구현 (Section 6.2)
    - 이벤트별 플레이어 조회
    - 역할별 필터링
    
[ ] POST /api/users/matchmaking/request 구현 (Section 6.3)
    - 매칭 요청 생성
    - 후보자 비동기 검색
    - 매칭 요청 만료 처리
```

#### Phase 7: 고급 기능
```
[ ] 중복 구매 방지
    - 같은 아이템 여러번 구매 방지 로직
    - ItemCategory별 정책 설정 (BORDER, TITLE, BANNER는 1개만)
    
[ ] Inventory 관리 개선
    - 소유 아이템 적용/해제 API 완성
    - 사용 중인 아이템 표시
    - 아이템 동기화
    
[ ] HC 보상 추가
    - QuestType별 HC 보상 설정 (현재는 0)
    - getRewardHc() 메서드 개선
```

---

## 🔒 보안 개선 필수사항

### 현재 상태
```
permitAll: /api/houses/**, /api/shop/**
→ 모든 정보가 공개됨, 누구나 구매/보상 수령 가능
```

### SecurityConfig 변경안
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/houses/**").authenticated()    // 변경
        .requestMatchers("/api/shop/**").authenticated()       // 변경
        .anyRequest().permitAll()
      )
      .oauth2Login(/* config */)
      .build();
    
    return http.build();
  }
}
```

---

## 🔄 동시성/원자성 고려사항

### House XP 업데이트
```
✅ @Transactional으로 보호됨
```

### HC 차감 (필수 개선)
```
❌ 현재: 동시성 제어 없음
✅ 개선안:

@Repository
public interface HouseRepository extends JpaRepository<House, Long> {
  @Query("SELECT h FROM House h WHERE h.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<House> findByIdWithLock(@Param("id") Long id);
}

@Transactional
public void buyItem(...) {
  House house = houseRepository.findByIdWithLock(houseId);  // Lock 획득
  if (house.getHc() < price) throw new InsufficientCurrencyException();
  house.deductHc(price);  // 원자적 차감
  // Commit 시 Lock 해제
}
```

---

## 💡 주의사항

1. **HC 가격 제약:**
   - ShopItem.priceHc는 5의 배수로만 설정
   - @Min(5), @Max(100000) 등 Validation 추가

2. **HC 보상 상태:**
   - 현재: QuestType별 HC 보상 = 0
   - 향후: 비즈니스 결정 후 업데이트 필요

3. **프론트엔드 동기화:**
   - 프론트는 새 명세 기준으로 구현 진행 중
   - 백엔드 미구현 API 호출 시 404 에러 발생
   - 동기화 필수

4. **로깅 & 모니터링:**
   - HC 차감/증가: 감사 로그 필수
   - 구매 내역: 데이터 무결성 확인용
   
---

## 📋 테스트 케이스

### 주간 퀨스트 (Section 2)
```
[ ] GET /api/houses/1/quests - 인증 필요 확인
[ ] GET /api/houses/1/quests - 비멤버 접근 차단 확인
[ ] GET /api/houses/1/quests - 응답 필드 확인 (questType, currentCount, targetCount 등)
[ ] PATCH /api/houses/1/quests/1001/progress (increment=1)
[ ] PATCH /api/houses/1/quests/1001/progress - 목표 달성 시 isCompleted=true 확인
[ ] POST /api/houses/1/quests/1001/claim - HC 충분 경우
[ ] POST /api/houses/1/quests/1001/claim - 이미 수령 시 실패
[ ] 비멤버 접근 차단 확인
```

### 재화 시스템 (Section 3)
```
[ ] GET /api/houses/1/currency - 개별 조회
[ ] POST /api/houses/currency/batch - 다중 조회
[ ] HC 차감 로직 활성화 확인
```

### 상점 기본 (Section 4.1-4.3)
```
[ ] GET /api/shop/items - 모든 아이템 조회
[ ] GET /api/shop/items?category=BORDER - 카테고리 필터
[ ] GET /api/shop/items?page=0&size=20 - 페이지네이션
[ ] GET /api/shop/items/1 - 아이템 상세 조회
```

### 상점 안전한 구매 (Section 4.4)
```
[ ] POST /api/shop/buy - HC 충분한 경우 성공
[ ] POST /api/shop/buy - HC 부족한 경우 409 응답
[ ] POST /api/shop/buy - 비멤버 접근 차단
[ ] POST /api/shop/buy - 동시 구매 동시성 제어 확인
  - House HC 100만큼, 50짜리 아이템 2개 동시 구매 → 1개만 성공
[ ] POST /api/shop/buy - 중복 구매 방지 확인
  - BORDER 카테고리 2개 구매 시도 → 2번째 실패
[ ] 응답 DTO 확인 (inventoryId, remainingHc 등)
```

### Inventory 관리 (Section 4.5-4.6)
```
[ ] GET /api/shop/inventory - 사용자의 구매 이력 조회
[ ] GET /api/shop/inventory?category=BORDER - 카테고리 필터
[ ] GET /api/shop/inventory?page=0&size=20 - 페이지네이션
[ ] POST /api/shop/inventory/1/apply - 아이템 적용
[ ] POST /api/shop/inventory/1/apply - 기존 적용 아이템 자동 해제
[ ] DELETE /api/shop/inventory/1/apply - 아이템 해제
[ ] 다른 사용자 inventory 접근 차단
```

### 랭킹 시스템 (Section 5)
```
[ ] GET /api/houses/ranking - 전체 House XP 랭킹
[ ] GET /api/houses/ranking?sortBy=hc - HC 기준 랭킹
[ ] GET /api/houses/ranking?sortBy=memberCount - 멤버 수 기준 랭킹
[ ] GET /api/houses/ranking?period=weekly - 주간 랭킹
[ ] GET /api/houses/ranking?period=monthly - 월간 랭킹
[ ] GET /api/houses/ranking?page=0&size=50 - 페이지네이션
[ ] GET /api/houses/ranking/my - 개인 House 랭킹 조회
[ ] 백분위수 계산 확인
```

### 사용자 추천 및 매칭 (Section 6)
```
[ ] GET /api/users/recommendations/players - 추천 플레이어 조회
[ ] GET /api/users/recommendations/players?limit=5 - limit 파라미터
[ ] 매칭 점수 계산 확인
[ ] 추천 이유 표시 확인
[ ] GET /api/events/1/players - 이벤트별 플레이어 조회
[ ] GET /api/events/1/players?role=SUPPORT - 역할별 필터링
[ ] POST /api/users/matchmaking/request - 매칭 요청 생성
[ ] 매칭 후보자 비동기 검색 확인
```

### 보안 및 동시성 (Cross-Cutting)
```
[ ] 인증되지 않은 사용자 401 응답 확인
[ ] 비멤버 사용자 403 응답 확인
[ ] HC 차감 원자성 (동시 구매 시나리오)
[ ] Pessimistic Lock 동시성 제어
[ ] 사용자 토큰 위변조 감지
[ ] Rate limiting (API 과다 호출 방지)
```
