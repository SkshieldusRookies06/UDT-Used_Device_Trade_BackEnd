# BE-D — 거래·분쟁 (에스크로)

> 이름: ____________   ·   GitHub: ____________
>
> **이 한 장이 내 2주 전부다.** 다른 문서는 여기서 필요할 때만 연다.
> 팀 공통 규칙은 [`../팀원용-한장.md`](../팀원용-한장.md),
> 날짜별 전체 순서는 [`전체로드맵.md`](../../docs/참고/전체로드맵.md).

---

## 1. 한 줄로

**이 프로젝트가 존재하는 이유가 내 파트다.** "돈 보내고 물건을 못 받는" 문제를 상태 머신으로 푸는 것.
`TransactionService`의 상태 전이는 **나 단독 오너**다 — BR-01~08 여덟 개 전부, 분쟁 전이(`markDisputed`)까지. 아무도 여기를 못 건드린다.
분쟁 엔티티·증빙 파일은 BE-A(T-010)가 만들고 내 메서드를 호출한다 — **나는 상태 머신 하나에 집중한다.** 발표에서도 **가장 중요한 장(에스크로 상태 머신)** 을 내가 말한다.

---

## 2. 내 파일

### 내가 소유한다 (내가 결정한다)

| 경로 | 내용 |
|---|---|
| `service/TransactionService.java` | **상태 전이 단독 오너.** 상태를 바꾸는 코드는 전부 여기에만 있다 |
| `controller/TransactionController.java` | 구매 · 송장 · 구매확정 |
| `dto/Transaction*` | DTO — `TransactionResponse`는 BE-A(T-021)도 쓴다. **D2에 필드를 확정해 알려 준다** |
| `repository/TransactionRepository` | **D2 저녁에 BE-A에게서 인계받는다** |
| `src/test/**` 거래 테스트 · 게이트 거래 검사 3개 | **T-022** (게이트 파일은 BE-A 소유 — diff로 넘기거나 페어) |

### 절대 안 건드린다

- `entity/` — 필드가 필요하면 **BE-A에게 요청**한다
- **상품 등록·검색** (BE-C). 상품 조회가 필요하면 BE-C의 Service를 **호출**하고 고치지 않는다
- **Dispute·DisputeFile 생성, 증빙 파일 저장·다운로드** (BE-A T-010). 나는 `markDisputed()`로 전이만 준다
- `/api/me/*` 목록 (BE-A T-021)
- `security/` · `SecurityConfig` (BE-B)
- `common/` — 에러 코드가 더 필요하면 BE-A에게 요청

### 공용 — 만지기 전에 채널에 한 줄 올린다

특별히 공용 파일을 만질 일이 없는 역할이다. **그래서 가장 방해 없이 깊게 갈 수 있다.**
대신 내 상태 머신이 틀리면 시연 전체가 무너진다.

> 남의 파일이 틀려 보여도 고치지 않는다. **오너에게 보고**한다.
> 고치면 그 버그의 책임이 나에게 넘어온다.

---

## 3. 내 계약 상대

| 나 | 상대 | 무엇에 대해 |
|---|---|---|
| **BE-D** | **FE-C** | 거래 **상태별로 어떤 버튼이 보이는가** · 송장 · 구매확정 |
| BE-D | BE-A | `markDisputed(buyerId, txId)` 시그니처(D2) · `TransactionResponse` 필드(D2) · `approve/reject/forceRefund/forceConfirm` 시그니처(D6 전, BE-B도 쓴다) |

이 짝이 합의하면 거래 계약이 확정된다. **합의는 `SPEC.md`에 커밋되기 전까지 무효다.**

> D1 회의에서 FE-C와 정할 것 — **상태 × 역할(구매자/판매자) 별로 허용되는 액션 표**.
> FE-C의 화면이 이 표 그대로 버튼을 켜고 끈다. 표가 없으면 FE-C는 시작할 수 없다.

---

## 4. 내 티켓 (순서대로)

| 티켓 | 언제 | 선행 | 끝났다는 증거 |
|---|---|---|---|
| [T-009 거래 상태 머신](../../tasks/T-009-거래-상태머신.md) | D1~D3 | T-002·T-003 | **BR-01~BR-08 8개 규칙이 전부 테스트로 증명된다** |
| [T-003 공통 예외·봉투 검증](../../tasks/T-003-공통예외-검증.md) | **D4** | T-009 | 에러 4종이 봉투대로 · `common/` 수정은 BE-A에게 diff로 |
| [T-022 거래 게이트 확장·테스트](../../tasks/T-022-거래-게이트확장-테스트.md) | **D5~D6** | T-009 · T-021 | 게이트 **12/12**(실서버·목 양쪽) · 상태 머신 엣지 테스트 통과 |

> **T-009가 가장 무겁다.** 상태 전이 8개 규칙을 원자적으로 만드는 일이다. 그래서 분쟁 도메인은 BE-A에게 넘기고, 나는 D5~D6에 그 상태 머신을 테스트와 게이트로 **증명**하는 데 쓴다.

> **`[수용 기준]`이 비어 있는 티켓은 시작하지 않는다** — 완료 판정을 말로 하게 된다.

---

## 5. 내 티켓 — 하나씩 어떻게 하나

> 아래는 **티켓을 실제로 어떻게 하는가**다. 순서대로 하면 된다.
> 내 티켓은 2개뿐이지만 **T-009가 이 프로젝트에서 가장 무거운 작업**이다. 시간을 여기에 쓴다.

### T-009 — 거래 상태 머신 (D1~D3 · 이 서비스의 핵심)

**0) 시작 전에 `SPEC.md` §2.3·§2.4를 종이에 옮겨 적는다.**
상태 그림 2개와 BR-01~BR-08 표가 내 설계도다. **외울 때까지 본다.**

**1) D1 회의에서 FE-C에게 줄 표를 먼저 만든다 ← 코드보다 이게 먼저다**

| Transaction 상태 | 구매자가 할 수 있는 것 | 판매자가 할 수 있는 것 |
|---|---|---|
| `PAID` | 분쟁 신고 | 송장 입력 |
| `SHIPPING` | **구매확정** · 분쟁 신고 | — |
| `DISPUTED` | — | — (관리자만) |
| `CONFIRMED` · `REFUNDED` | — | — |

> **FE-C는 이 표가 없으면 T-017을 시작할 수 없다.** D1에 넘긴다.

**2) `TransactionService` 골격을 만든다 — 메서드 이름부터 정한다**

> **BR-01·BR-02도 내 몫이다.** 검수 승인/반려는 Product 상태를 바꾸므로 여기 있어야 한다.
> BE-B의 관리자 화면(T-018)이 이 둘을 호출한다 — D6 전까지 시그니처를 확정해 알려 준다.

```java
@Service
@RequiredArgsConstructor
public class TransactionService {
    @Transactional public void approveInspection(Long productId);                          // BR-01 (관리자)
    @Transactional public void rejectInspection(Long productId, String reason);            // BR-02 (관리자)
    @Transactional public TransactionResponse purchase(Long buyerId, Long productId);      // BR-03
    @Transactional public TransactionResponse registerShipping(Long sellerId, Long txId, ShippingRequest r); // BR-04
    @Transactional public TransactionResponse confirm(Long buyerId, Long txId);            // BR-05
    @Transactional public void markDisputed(Long buyerId, Long txId);                     // BR-06 전이만 — Dispute 생성은 BE-A(T-010)가 이 메서드를 호출
    @Transactional public TransactionResponse forceRefund(Long txId);                      // BR-07 (관리자)
    @Transactional public TransactionResponse forceConfirm(Long txId);                     // BR-08 (관리자)
}
```
> **모든 메서드에 `@Transactional`.** BR 한 줄이 한 트랜잭션이다.
> **여기 밖에서는 아무도 Transaction·Product 상태를 바꾸지 않는다** (§2.5). BE-B·BE-C는 이걸 호출만 한다.

**3) `purchase` (BR-03)를 만든다 — 가장 어렵다. 순서가 중요하다**

```
1) 상품 조회.  status != ON_SALE  → 409 PRODUCT_NOT_ON_SALE
2) product.seller.id == buyerId    → 400 SELF_PURCHASE_NOT_ALLOWED
3) buyer.balance < product.price    → 400 INSUFFICIENT_BALANCE
4) ── 여기부터 원자적으로 ──
   product.status = IN_TRADE
   buyer.balance -= price
   Transaction 저장 (status = PAID, amount = price)
```
> **판매자 잔액은 건드리지 않는다.** 이게 에스크로다. 대금은 구매확정(BR-05) 때 간다.

**동시성 — 이게 이 티켓의 진짜 난이도다.**
두 사람이 같은 상품을 동시에 누르면 둘 다 1번 검사를 통과할 수 있다. 조회 후 변경은 경합에 진다.
둘 중 하나로 막는다:

| 방법 | 어떻게 |
|---|---|
| **조건부 UPDATE** (권장·쉽다) | `UPDATE product SET status='IN_TRADE' WHERE id=? AND status='ON_SALE'` 의 **영향 행 수가 0이면** 이미 누가 샀다 → 409 |

조건부 UPDATE를 그대로 옮기면 이렇다 — `ProductRepository`(BE-C 소유)에 메서드 하나가 필요하니 **D2에 BE-C에게 요청**한다:

```java
// ProductRepository (BE-C에게 요청해서 넣는다)
@Modifying(clearAutomatically = true)
@Query("update Product p set p.status = :next where p.id = :id and p.status = :expected")
int transition(@Param("id") Long id,
               @Param("expected") ProductStatus expected,
               @Param("next") ProductStatus next);

// TransactionService.purchase() 안 — 검사 1~3 뒤에
int updated = productRepository.transition(productId, ProductStatus.ON_SALE, ProductStatus.IN_TRADE);
if (updated == 0) throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);   // 동시에 누가 먼저 샀다
buyer.withdraw(product.getPriceKrw());
transactionRepository.save(Transaction.paid(product, buyer, product.getPriceKrw()));   // 정적 팩토리는 내가 만든다
```
> `updated == 0` 한 줄이 동시성 방어의 전부다. 두 사람이 동시에 눌러도 DB가 한 줄만 바꾼다.
> 테스트 `같은_상품을_두_번_사면_두_번째가_실패한다`가 이걸 증명한다.
| 비관적 락 | `@Lock(PESSIMISTIC_WRITE)` 로 상품을 잠그고 조회 |

**4) `registerShipping` (BR-04)**

```
1) 호출자가 판매자인가?  아니면 403 TRANSACTION_FORBIDDEN
2) status != PAID        → 409 INVALID_TRANSACTION_STATUS
3) status = SHIPPING, courier·trackingNo 저장
```

**5) `confirm` (BR-05) — 돈이 움직이는 순간**

```
1) 호출자가 구매자인가?  아니면 403
2) status != SHIPPING    → 409
3) ── 원자적으로 ──
   transaction.status = CONFIRMED
   product.status     = SOLD
   seller.balance    += amount      ← 여기서 처음으로 판매자에게 돈이 간다
```

**6) 권한·상태 가드를 **모든** 메서드에 넣는다**
"호출자가 당사자인가" + "지금 상태에서 이 전이가 가능한가" 두 줄이 모든 메서드 앞에 있어야 한다.
빠진 메서드가 하나라도 있으면 남의 거래를 조작할 수 있다.

**7) 테스트로 증명한다 — 게이트가 아니라 테스트가 내 증거다**

```java
@Test void 판매중이_아니면_구매할_수_없다()
@Test void 본인_상품은_살_수_없다()
@Test void 잔액이_부족하면_구매가_실패하고_잔액이_그대로다()
@Test void 구매하면_구매자_잔액만_줄고_판매자_잔액은_그대로다()   // ← 에스크로의 증명
@Test void 구매확정하면_판매자_잔액이_늘고_상품이_SOLD가_된다()
@Test void 같은_상품을_두_번_사면_두_번째가_실패한다()             // ← 동시성
@Test void 남의_거래를_확정할_수_없다()
@Test void PAID_상태에서_바로_확정할_수_없다()
```

**8) 손으로도 한 번 돌려 본다**

```bash
BUYER=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)

curl -s localhost:8080/api/me -H "Authorization: Bearer $BUYER" | jq '.data.balanceKrw'   # 구매 전
curl -s -X POST localhost:8080/api/products/1/purchase -H "Authorization: Bearer $BUYER" | jq '.data.status'
curl -s localhost:8080/api/me -H "Authorization: Bearer $BUYER" | jq '.data.balanceKrw'   # 줄었는가
curl -s -X POST localhost:8080/api/products/1/purchase -H "Authorization: Bearer $BUYER" | jq '.code'  # 두 번째는 실패
```

`확인:` 테스트 8개 통과 · 위 curl에서 잔액이 정확히 가격만큼 줄고 두 번째 구매가 409.

---

### T-003 — 공통 예외·봉투 검증 (D4 · 통합일에 나온 에러 응답이 재료다)

`common/` 6개 파일이 이미 있다(BE-A 소유). **검증하고, 틀린 곳은 BE-A에게 diff로 넘기는 티켓**이다 — 내가 직접 고치지 않는다.

**1) `ErrorCode.java`를 `SPEC.md` §5 표와 한 줄씩 대조한다**

```bash
grep -cE '^\s+[A-Z_]+\(' src/main/java/com/rookies6/udt/common/ErrorCode.java
```
`확인:` 22개. 모자라면 §5 표에서 빠진 것을 채운다. **메시지 문구는 표 그대로** — 화면이 이 문구를 그대로 띄운다.

**2) `GlobalExceptionHandler`가 네 가지를 다 잡는지 본다**

| 잡는 것 | 내는 것 |
|---|---|
| `BusinessException` | 그 `ErrorCode`의 status·code·message |
| `MethodArgumentNotValidException` | 400 `VALIDATION_ERROR` + **`fields[]`** |
| `AccessDeniedException` | 403 |
| 나머지 `Exception` | 500 `INTERNAL_SERVER_ERROR` — **스택트레이스를 응답에 담지 않는다** |

**3) 절대 하지 말 것 하나**
`@JsonInclude(NON_NULL)`을 **전역으로 걸지 않는다.** `thumbnailUrl: null`이 응답에서 사라지면
프론트가 "키가 있는데 값이 null"로 분기할 수 없게 된다. 계약이 깨진다.

**4) 에러 형태를 직접 확인한다**

```bash
curl -s localhost:8080/api/products/999999999 | jq
# { "success": false, "statusCode": 404, "code": "PRODUCT_NOT_FOUND", "message": "...", "timestamp": "..." }
```

`확인:` `node seams/check-api.mjs` 의 에러 형태 검사가 ok.

---

### T-022 — 거래 게이트 확장 · 테스트 보강 (D5~D6)

**T-009를 "증명"하는 티켓이다.** 계약 표류를 기계가 잡게 하고, 상태 머신의 경계를 테스트로 닫는다.

**1) 게이트에 검사 3개를 추가한다** — `seams/check-api.mjs`는 BE-A 소유라 **BE-A 옆에서 같이 넣거나 diff로 넘긴다**

```
- POST /api/products/{id}/purchase       → 201 · data.buyerId·data.sellerId 가 문자열
- GET  /api/transactions/{id}            → status 가 5종 enum 중 하나 · amountKrw 숫자
- PATCH /api/transactions/{id}/confirm (PAID 상태 거래에) → 409 INVALID_TRANSACTION_STATUS
```
> 기존 9개 검사의 형태(`check()` · `errs.push`)를 그대로 따른다. **실서버와 목 서버 양쪽에서 12/12**여야 머지.
> 목이 12/12가 안 나오면 목이 계약을 안 따르는 것 — BE-A에게 보고.

**2) 상태 머신 엣지 테스트** — T-009의 8개 위에 얹는다

```java
@Test void 잔액이_정확히_가격과_같으면_구매되고_잔액이_0이_된다()
@Test void 같은_사용자가_두_상품을_연속_구매하면_잔액이_합만큼_준다()
@Test void 판매자가_자기_거래를_구매확정하면_403()
@Test void 환불된_상품은_다시_구매할_수_있다()          // REFUNDED → 상품 ON_SALE 복귀
@Test void 검수_승인하면_INSPECTING이_ON_SALE이_된다()  // BR-01 — BE-B T-018이 D6에 부른다
@Test void 강제_환불하면_잔액_상품_분쟁이_같이_바뀐다()  // BR-07 네 줄 원자성
```

**3) `./mvnw test` 는 MariaDB가 떠 있어야 돈다** — 저녁 게이트 전에 DB부터 켠다.

`확인:` 게이트 12/12 (실서버·목) · 테스트 전부 통과 · API 응답 형태는 한 글자도 안 바뀜.

---


## 6. 내 10일

```
D1~D3  T-009 거래 상태 머신 (이 프로젝트의 핵심 · 여기에 시간을 쓴다)
D4   ★ 1차 통합 대응 — FE-C와 상태별 액션을 맞춘다 + T-003 공통 예외·봉투 검증
D5~D6  T-022 거래 게이트 검사 3개(→12/12) · 상태 머신 테스트 보강
D7     마감 · 엣지 케이스 점검 (이중 구매 · 잔액 부족 · 본인 상품 구매)
D8   ★ 시연 ④⑤⑦ 구간 (송장 → 분쟁 → 구매확정)
D9     발표 **에스크로 상태 머신 — 가장 중요한 장** · 회고록
D10    리허설
```

---

## 7. 내가 쓰는 문서

| 문서 | 언제 | 어디서 파생 |
|---|---|---|
| `docs/01-도메인설계서.md` 상태 머신 절 | D2 | BE-A와 같이 — **상태표는 내가 채운다** |
| `docs/03-REST-API설계서.md` 거래·분쟁 절 | D4 | 내 엔드포인트 |
| `docs/참고/발표대본.md` 에스크로 장 | D9 | **분량이 가장 크다** |
| `docs/회고록/retro-<이름>.md` | D9 오전 | 전원 공통 |

> 문서는 **마지막 날에 몰아 쓰지 않는다.** 원본이 확정되는 날 그 자리에서 떨어뜨린다.
> 문서 머리에 `정본: SPEC.md §N · 커밋 <해시>`를 적는다.

---

## 8. 내 명령어 (복붙)

```bash
# 기동
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 내 완료 증명 — 상태 머신은 게이트가 아니라 테스트로 증명한다
./mvnw test
node seams/check-api.mjs

# 구매 → 송장 → 구매확정 (시연 ③④⑦)
BUYER=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)

curl -s -X POST localhost:8080/api/products/1/purchase -H "Authorization: Bearer $BUYER" | jq '.data.status'

# 같은 상품을 두 번 사 본다 — 두 번째는 반드시 실패해야 한다 (BR-03)
curl -s -X POST localhost:8080/api/products/1/purchase -H "Authorization: Bearer $BUYER" | jq '.code, .statusCode'

# 잔액이 실제로 움직였는지
curl -s localhost:8080/api/me -H "Authorization: Bearer $BUYER" | jq '.data.balanceKrw'
```

---

## 9. 내가 막힐 곳 (내 역할 고유)

| 증상 | 원인 | 볼 곳 |
|---|---|---|
| 동시에 두 번 누르면 둘 다 성공 | 원자성이 없다 | **`@Transactional` + 상품 상태 조건부 갱신.** 조회 후 변경은 경합에 진다 |
| 잔액이 음수가 된다 | 차감 전에 검사를 안 했다 | BR 규칙 — 차감과 검사가 같은 트랜잭션 안에 |
| 환불했는데 상품이 `SOLD`로 남는다 | 상태 전이가 한 쪽만 돌았다 | **거래 상태와 상품 상태는 같이 움직인다** (`SPEC.md` §2) |
| 상품 상태를 BE-C가 바꾸고 싶어 한다 | 그러면 상태 전이가 두 곳이 된다 | **거절한다.** 내 Service를 호출하게 한다 |
| 본인 상품을 본인이 산다 | 검사 누락 | BR 규칙 |
| 증빙 파일을 아무나 받는다 | 권한 검사 누락 | **거래 당사자 + 관리자만** |
| 상태가 늘어난다 | 상태를 추가하면 화면·문서·게이트가 같이 바뀐다 | **추가 전 `SPEC.md` §10 개정 절차** |

---

## 10. 발표에서 내가 말하는 것

**에스크로 상태 머신 + 트랜잭션 원자성** — `SPEC.md` §2.3·§2.4

**이게 발표에서 가장 중요한 장이다.** 다른 조와 갈리는 지점이 여기다.

말할 것 두 가지:
1. **상태 그림** — 상품 상태 × 거래 상태가 같이 움직인다
2. **원자성** — "동시에 두 번 구매하면 어떻게 되나"에 코드로 답한다

> 한 문장으로: **"구매확정 전까지 대금은 판매자에게 가지 않습니다. 분쟁이 나면 관리자가 증빙을 보고 되돌립니다."**

---

## 11. 매일 하는 것 (전원 공통 · 합쳐서 10분)

**아침 3문장 (스탠드업 · 15분 캡)**

```
1) 어제 머지한 것 — 커밋 해시 + 게이트 green/red 로 말한다 ("70% 됐어요" 금지)
2) 오늘 여는 티켓 하나
3) 막힌 것 — 30분 넘게 막혔으면 여기서 반드시 말한다
```

**하루 끝 (5분)**

```
1) 위 7번 게이트 명령 1회 → green
2) git diff 를 내가 읽는다. 한 화면(약 100줄)을 넘으면 머지하지 않고 티켓을 쪼갠다
3) 커밋·push는 내가 한다 → 오너에게 머지 요청
4) worklog/<내이름>/D##.md 에 3줄
   1) 머지: T-0XX · a1b2c3d · 게이트 OK
   2) 열었음: T-0XX
   3) 막힘: (있으면 한 줄 · 없으면 "없음")
```

**30분 규칙** — 30분 막히면 채널에 올린다. 질문은 팀 일정을 구하는 행위다.
답하는 쪽은 30분 안에 답하거나 "언제 보겠다"를 답한다.

---

## 12. 내가 지키는 네 줄

```
1. SPEC.md에 답이 없으면 추측하지 않는다 — 멈추고 오너에게 묻는다
2. "됐습니다"는 증거가 아니다 — 게이트 출력이 증거다
3. 위 2번 밖의 파일은 틀려 보여도 고치지 않는다 — 오너에게 보고
4. 계약에 대한 구두 합의는 SPEC.md에 커밋되기 전까지 무효
```

에이전트에 맡길 때는 **지시서(`tasks/T-###.md`) + SPEC 해당 절 + (수정이면) diff** 셋만 넣는다.
리포를 통째로 넣지 않는다. 커밋·push·머지는 사람이 한다.
