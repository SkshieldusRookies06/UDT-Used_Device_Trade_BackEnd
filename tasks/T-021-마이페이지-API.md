# T-021 — 마이페이지 API 3개 (`/api/me/*`)

**담당:** BE-A · **브랜치:** `be-me` · **예상:** D3 · **선행:** T-002 · T-005(로그인 사용자 식별)

## [배경]

계약 (`SPEC.md` §4.1 · §4.9):

```
GET /api/me/products                     내가 등록한 상품 (검수대기·반려 포함 모든 상태) · ProductSummary 목록
GET /api/me/transactions?role=buyer|seller   내 거래 · Transaction 객체 목록 · role 없거나 다른 값 → 400 VALIDATION_ERROR
GET /api/me/wishes                       내 찜 · ProductSummary 목록
전부 MEMBER · 0-base 페이징 · {content[], page{}}
```

FE-C의 마이페이지 4탭(T-016)이 D1~D3에 이 셋을 본다. 상품·거래·찜에 걸쳐 있어 담당이 흩어지기 쉬운 것을
BE-A가 한 컨트롤러로 묶는다. 조회는 **신규 `repository/MeQueryRepository`(BE-A 소유)** 에 모은다 —
도메인 Repository(`ProductRepository`·`WishRepository`는 BE-C · `TransactionRepository`는 BE-D)에는
메서드를 끼워 넣지 않는다. DTO는 BE-C(`ProductSummaryResponse`)·BE-D(`TransactionResponse`)의 것을
**재사용**한다 — 새 DTO를 만들지 않는다.

## [목표]

로그인한 사용자가 자기 상품·거래·찜을 페이징으로 받는다.

## [수용 기준]

- [ ] `GET /api/me/products` — buyer1 계정으로 0건, seller1 계정으로 **22건**(전 상태)
- [ ] `GET /api/me/transactions?role=buyer` — buyer1 계정으로 **4건** · `role=seller` seller1 계정으로 4건
- [ ] `role` 없음 또는 `role=x` → 400 `VALIDATION_ERROR`
- [ ] `GET /api/me/wishes` — buyer1 계정으로 **2건**
- [ ] 비로그인 → 401 `AUTHENTICATION_REQUIRED`
- [ ] 응답의 `buyerId`·`sellerId`·`id`가 **문자열**
- [ ] Controller에 Repository 직접 주입 없음 (`MeService`를 거친다)

**판정 테스트** (`src/test/.../acceptance/me/` · 0절 표 번호와 1:1):

| # | 클래스 | 무엇을 보나 |
|---|---|---|
| ① | `MeProductsApiTest` | 시드 seller1 22건(전 상태) · buyer1 0건 · 내 것만 · 401 |
| ② | `MeTransactionsApiTest` | 시드 4건/4건 · `role` 누락·오류 400 · 당사자만 · id 문자열 · 401 |
| ③ | `MeWishesApiTest` | 시드 buyer1 2건 · 남의 찜 제외 · 401 |

> 시드 수치는 `AcceptanceSupport.seedUser("seller1@udt.test")`로 시드 계정을 집어 검사한다(읽기만 · 롤백).

## [제약]

- 수정 허용: `controller/MeController.java` · `service/MeService.java` · `repository/MeQueryRepository.java` (모두 신규)
- **fetch join은 단건 연관(`seller`·`category`)만.** 이미지(컬렉션)는 페이징과 함께 fetch join하면
  메모리 페이징이 된다 — 이미지 N+1은 T-019(BE-C)가 `default_batch_fetch_size`로 한 번에 잡는다
- 거래 목록 원소는 **`TransactionResponse.from(tx)`로만** 만든다 — 필드를 직접 조립하면 BE-D가
  DTO에 필드를 추가할 때 두 군데가 따로 놀게 된다
- BE-B의 `GET /api/me`(내 정보)는 건드리지 않는다 — 경로만 같은 접두어다
- DTO 변환은 Service 안(`@Transactional(readOnly = true)`)에서 — OSIV가 꺼져 있다
- `TransactionResponse`가 아직 없으면(T-009 진행 중) **BE-D에게 필드 확정을 요청**하고 그 전까지 거래 탭만 미룬다

## [확인 명령]

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)
curl -s "localhost:8080/api/me/transactions?role=buyer" -H "Authorization: Bearer $TOKEN" | jq '.data.page.totalElements'   # 4
curl -s "localhost:8080/api/me/transactions?role=x"     -H "Authorization: Bearer $TOKEN" | jq '.code'                     # VALIDATION_ERROR
curl -s "localhost:8080/api/me/wishes"                  -H "Authorization: Bearer $TOKEN" | jq '.data.page.totalElements'   # 2
grep -rl "Repository" src/main/java --include='MeController.java'   # 무출력
```
