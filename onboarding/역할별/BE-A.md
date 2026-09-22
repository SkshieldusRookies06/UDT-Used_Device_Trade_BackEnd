# BE-A — 리포·도메인 (팀장)

> 이름: ____________   ·   GitHub: ____________
>
> **이 한 장이 내 2주 전부다.** 다른 문서는 여기서 필요할 때만 연다.
> 팀 공통 규칙은 [`../팀원용-한장.md`](../팀원용-한장.md),
> 날짜별 전체 순서는 [`전체로드맵.md`](../../docs/참고/전체로드맵.md).

---

## 1. 한 줄로

**D1~D2는 바닥을 깐다. D3부터는 내 기능이 있다.** D1 오전의 T-001이 전원의 선행이고, 엔티티·시드·계약(`SPEC.md`)이 내 것이다.
그 위에 **마이페이지 API 3개(T-021)** 와 **분쟁·증빙 파일(T-010)** 이 내 기능 도메인이다 — 발표에서 "증빙 파일 권한 검사"를 내가 말한다.

> 관리 업무도 산출물이다 — 리포 셋업·게이트·목 서버·시드·계약 개정은 전부 커밋 로그로 증명된다.
> 개인 기여가 안 보일까 걱정하지 않아도 된다. `git log --author`가 말해 준다.

---

## 2. 내 파일

### 내가 소유한다 (내가 결정한다)

| 경로 | 내용 |
|---|---|
| `SPEC.md` | **계약 정본.** 코드와 충돌하면 이게 이긴다 |
| `seams/check-api.mjs` | 계약 게이트 9개 검사 |
| `mock/server.mjs` | 프론트가 쓰는 목 서버 |
| `entity/` | 엔티티 8종 — **끝까지 나 단독. 인계 없음** |
| `common/` | `ErrorCode` · `BusinessException` · `ApiResponse` · `@RestControllerAdvice` |
| `config/CorsConfig.java` | CORS |
| `resources/data.sql` | 시드 |
| `pom.xml` · `mvnw` · `.mvn/` · `README.md` · `CLAUDE.md` | 리포 뼈대 |
| `application.yml` · `application-local.yml` | 설정 — **키 블록 단위로만 남에게 연다** |
| `repository/` | D1~D2만. **D2 저녁에 파일 단위로 넘긴다** (아래 3번) — `Dispute*Repository`는 내가 계속 |
| **`controller/MeController.java` · `service/MeService.java`** | **T-021** `/api/me/products·transactions·wishes` (신규) |
| **`service/FileStorageService.java` · `controller/ProductImageController.java`** | **T-023** 파일 저장·검증·서빙 — 상품(BE-C)·분쟁(나) 공용 · 보안 검사 한 벌 (신규) |
| **`controller/DisputeController.java` · `service/DisputeService.java` · `dto/Dispute*` · `admin/AdminDisputeFileController.java`** | **T-010** 분쟁 접수 · 증빙 업로드/다운로드 (신규) |

### 절대 안 건드린다

- 남의 `Service` · `Controller` **내부**
- **상태 전이** — 분쟁도 `DISPUTED`로 바꾸는 건 BE-D의 `TransactionService.markDisputed()`다. 나는 호출만
- `security/` · `SecurityConfig` (BE-B)
- `admin/` · `templates/` (BE-B)
- 프론트 리포 — 계약이 바뀌어도 **프론트를 대신 고치지 않는다.** SPEC·게이트·목만 고치고 알린다

### 공용 — 만지기 전에 채널에 한 줄 올린다

내가 오너라서 공지할 필요는 없지만, **`SPEC.md`를 고칠 때는 반드시 알린다.**
계약 개정은 네 개가 같이 움직인다 — `SPEC.md` · `seams/check-api.mjs` · `mock/server.mjs` · 팀 공지.
**하나라도 빠지면 개정이 아니다** (`SPEC.md` §10).

> 남의 파일이 틀려 보여도 고치지 않는다. **오너에게 보고**한다.
> 고치면 그 버그의 책임이 나에게 넘어온다.

---

## 3. 내 계약 상대

| 나 | 상대 | 무엇에 대해 |
|---|---|---|
| BE-A | **전원** | 계약(`SPEC.md`) 개정 승인 · 엔티티 필드 추가 요청 |
| **BE-A** | **FE-C** | 마이페이지 4탭 목록(`/api/me/*`) · 분쟁 신고 폼 · 증빙 파일 다운로드 |
| BE-A | BE-D | `markDisputed(buyerId, txId)` 시그니처 (D2에 받는다) · `TransactionResponse` DTO |

### D2 저녁 — Repository 인계

골격만 만들어 놓고 **파일 단위로 넘긴다.** 그 뒤로는 Service 오너가 자기 Repository를 소유한다.

| 파일 | 넘길 사람 |
|---|---|
| `ProductRepository` · `WishRepository` · `CategoryRepository` | **BE-C** |
| `TransactionRepository` | **BE-D** |
| `DisputeRepository` · `DisputeFileRepository` | 내가 계속 (T-010) |
| `UserRepository` | 내가 계속 (BE-B는 소비만) |

**엔티티는 인계하지 않는다.** 필드가 필요하다는 요청이 오면 내가 고친다.

---

## 4. 내 티켓 (순서대로)

| 티켓 | 언제 | 선행 | 끝났다는 증거 |
|---|---|---|---|
| [T-001 리포 셋업·첫 기동](../../tasks/T-001-리포셋업-첫기동.md) | **D1 오전** | — | 7명 전원이 자기 PC에서 기동 성공 |
| [T-002 엔티티·시드 확정](../../tasks/T-002-엔티티-시드-확정.md) | D2 | T-001 | `data.sql`이 오류 없이 돌고 시드가 조회된다 |
| [T-021 마이페이지 API 3개](../../tasks/T-021-마이페이지-API.md) | **D3** | T-002 · T-005 | buyer1로 거래 4건·찜 2건, `role=x`는 400 |
| [T-023 파일 저장·검증·서빙](../../tasks/T-023-파일저장-서빙.md) | **D3** | T-002 | 위장 파일 400 · `../` 경로 무효 · UUID 이름 · 이미지 GET 200 |
| [T-010 분쟁 접수·증빙 파일](../../tasks/T-010-분쟁-증빙파일.md) | **D5~D6** | T-009 · T-023 | 증빙 첨부 분쟁 → 관리자 다운로드 → 강제 환불이 돈다 · 제3자 다운로드 403 |

**T-001은 D1 오전에 끝낸다.** 여섯 명이 이것을 기다리고 있다.

> **`[수용 기준]`이 비어 있는 티켓은 시작하지 않는다** — 완료 판정을 말로 하게 된다.

---

## 5. 내 티켓 — 하나씩 어떻게 하나

> 아래는 **티켓을 실제로 어떻게 하는가**다. 순서대로 하면 된다.
> 각 단계 끝의 `확인:` 을 통과해야 다음으로 간다. 통과 못 하면 **다음으로 가지 않는다** —
> 여기서 넘어간 문제는 D4 통합에서 세 배가 되어 돌아온다.

### T-001 — 리포 셋업·전원 첫 기동 (D1 오전 · 이게 늦으면 6명이 논다)

**1) Maven 래퍼를 채운다** — 팀원이 IntelliJ로 실행해도 래퍼는 넣는다(평가자 빌드·IDE 문제 판정용)
스캐폴드에 `mvnw`가 없다. 수업 리포(`SpringBoot4_Basic_Project`)에서 가져온다.

```bash
cp -r <수업리포>/mvnw <수업리포>/mvnw.cmd <수업리포>/.mvn ./
chmod +x mvnw
```
> IntelliJ에서 Maven 프로젝트로 열면 래퍼 없이도 되지만, **팀원 7명이 같은 명령을 써야 하니 래퍼를 넣는다.**

`확인:` `./mvnw -v` 가 버전을 출력한다.

**2) DB가 진짜 MariaDB인지 확인한다 (건너뛰면 D1이 통째로 날아간다)**

```bash
mysql -u root -p -e "SELECT VERSION(); SELECT @@port;"
```
- `10.x.x-MariaDB` → OK
- `8.x.x` → **MySQL이다.** MariaDB 포트를 찾는다 (Windows: `netstat -ano | findstr LISTENING | findstr :330`)
- MySQL이 깔린 PC는 MariaDB가 **3307**을 잡는다. 확정값이 3307인 이유가 이것이다

**3) DB·계정을 만든다**

```bash
mysql -u root -p -P 3307 -e "CREATE DATABASE udt DEFAULT CHARACTER SET utf8mb4;
  CREATE USER 'udt'@'localhost' IDENTIFIED BY 'udt';
  GRANT ALL PRIVILEGES ON udt.* TO 'udt'@'localhost'; FLUSH PRIVILEGES;"
```

**4) 기동한다** — `ddl-auto: create` 라서 스키마가 생기고 `data.sql` 시드가 같이 돈다.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`확인:` 기동 로그에 `Started UdtApplication` · `localhost:8080/api/health` 가 응답한다.

**5) 게이트를 돌린다** (다른 터미널)

```bash
node seams/check-api.mjs
```

`확인:` **9개 중 7개 ok가 정상이다.** red 2개는 `POST /api/auth/login (자격 오류)`와 `로그인 → 토큰 → /api/me` — 둘 다 `AuthController`가 없어서다.
`AuthController`가 아직 없기 때문이고 BE-B의 T-004가 채운다. **전부 red면 다른 문제다** — DB 아니면 포트.

**6) push하고 README의 `<조직>` 자리를 채운다**
두 리포가 서로를 링크하는 게 리포 2개 구성에서 길을 잃지 않는 유일한 장치다.

**7) 팀원 6명을 붙인다 — 여기가 T-001의 진짜 내용이다**
한 명씩 위 2~5를 돌리게 하고, **막히면 옆에 붙어서 같이 본다.**
포트가 사람마다 다를 수 있으니 각자 `SELECT @@port;` 를 먼저 돌리게 한다.

`확인:` **7명 전원이 자기 PC에서 게이트 7/9를 봤다.** 이게 D1 오전의 완료 조건이다.

---

### T-002 — 엔티티·시드 확정 (D2)

엔티티 8종은 이미 있다. **새로 만드는 게 아니라 검토·보완하는 티켓**이다.

**1) 엔티티 8종을 순서대로 읽는다**
`User` → `Category` → `Product` → `ProductImage` → `Transaction` → `Dispute` → `DisputeFile` → `Wish`
읽으면서 `SPEC.md` §2.2와 필드가 맞는지 대조한다.

**2) 두 가지만 확인한다**

```bash
grep -rn "FetchType.LAZY" src/main/java/com/rookies6/udt/entity/ | wc -l   # 10 이상
grep -rn "@Setter" src/main/java/com/rookies6/udt/entity/                  # 무출력
```
- **모든 `@ManyToOne`은 LAZY다.** EAGER 하나가 N+1을 만들고 그게 BE-C의 T-019를 망친다
- **엔티티에 `@Setter`를 붙이지 않는다.** 아무 데서나 상태를 바꿀 수 있게 되고 §2.5가 무너진다.
  상태 변경은 의미 있는 이름의 메서드로만 (`product.markOnSale()` 식)

**3) 제약조건 이름을 바꾸지 않는다**
`02-Entity설계서.md`가 그 이름을 인용한다. 바꾸면 문서가 틀린다.

**4) 시드를 확인한다 — DB에서 직접 센다**

> `/api/products`로 확인하지 않는다. T-006 전까지 그 엔드포인트는 **하드코딩 껍데기**라 DB를 안 본다
> (`totalElements: 1`, id `"12"` 하나가 나오면 껍데기다 — 정상).

```bash
mysql -u udt -pudt -P 3307 udt -e "SELECT COUNT(*) FROM products; SELECT status, COUNT(*) FROM products GROUP BY status; SELECT COUNT(*) FROM transactions; SELECT COUNT(*) FROM disputes;"
```
`확인:` **22 / ON_SALE 15 · INSPECTING 3 · IN_TRADE 3 · SOLD 1 / 4 / 1**. T-006이 끝난 뒤에는 API로 `totalElements: 15`가 나온다.

**5) 그날 저녁에 `docs/02-Entity설계서.md` §4를 실제 코드에서 재발췌한다**
> 기억으로 쓰지 않는다. `entity/*.java`를 열어 놓고 옮긴다.

**6) Repository를 인계한다** — 채널에 한 줄 올린다.
"`ProductRepository`·`WishRepository`·`CategoryRepository` → BE-C, `TransactionRepository`·`DisputeRepository` → BE-D 넘깁니다."

`확인:` 게이트 7/9 유지 · 위 grep 두 개 통과 · 인계 공지 완료.

---

### T-021 — 마이페이지 API 3개 (D3)

FE-C의 4탭이 이걸 본다. **크기는 하루**고, 어려운 건 없다 — Repository는 내가 만든 거고 DTO는 남의 걸 재사용한다.

**1) `MeService` 를 만든다 — 조회 3개, 전부 `@Transactional(readOnly = true)`**

```
myProducts(userId, pageable)      → productRepository.findBySellerId(...)      → Page<ProductSummaryResponse>
myTransactions(userId, role, pg)  → role=buyer: findByBuyerId / role=seller: findByProductSellerId → Page<TransactionResponse>
myWishes(userId, pageable)        → wishRepository.findByUserId(...) → 상품으로 변환 → Page<ProductSummaryResponse>
```
> `role`이 `buyer`·`seller` 외면 `BusinessException(VALIDATION_ERROR)`. `TransactionResponse`가 아직 없으면(BE-D T-009 진행 중) 필드를 BE-D에게 받아 온다 — 계약은 `SPEC.md` §4.5.

**2) Repository에 조회 메서드를 넣는다** — `ProductRepository.findBySellerId`·`WishRepository.findByUserId`·`TransactionRepository.findByBuyerId`·`findByProductSellerId`.
D2 저녁에 인계하기 **전에** 넣어 두면 인계받는 사람 파일을 안 건드린다. 인계 후라면 BE-C·BE-D에게 한 줄 요청.

**3) `MeController`** — 경로 3개, `@AuthenticationPrincipal`로 userId, Service 호출만. **Repository 직접 주입 금지.**

**4) 확인**

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)
curl -s "localhost:8080/api/me/transactions?role=buyer" -H "Authorization: Bearer $TOKEN" | jq '.data.page.totalElements'   # 4
curl -s "localhost:8080/api/me/wishes" -H "Authorization: Bearer $TOKEN" | jq '.data.page.totalElements'                    # 2
curl -s "localhost:8080/api/me/transactions?role=x" -H "Authorization: Bearer $TOKEN" | jq '.code'                          # VALIDATION_ERROR
```
`확인:` 세 숫자가 맞고, `MeController`에 `Repository`가 없다. FE-C에게 "실서버 붙여도 됩니다" 공지.

---

### T-023 — 파일 저장·검증·서빙 (D3 · T-021과 같은 날 · 내 기능 ②)

**왜 내가 하나** — 파일은 상품 이미지(BE-C)와 분쟁 증빙(나) 둘이 쓰는데, 이 프로젝트에서 **보안상 가장 새기 쉬운 자리**다.
경로 조작(`../../`), 확장자 위장(`.txt`를 `.jpg`로), 남의 파일 열람. 한 사람이 한 벌로 만들고 둘이 호출만 하게 한다.

**1) `FileStorageService` — 메서드 셋**

```
store(MultipartFile file, String subdir) → StoredFile{storedName, originalName, sizeBytes}
load(String subdir, String storedName)   → Resource
contentType(String storedName)           → "image/jpeg" 등
```

**2) `store()` 안의 검사 순서 — 순서가 중요하다**

```
1) file.isEmpty() → 400 VALIDATION_ERROR
2) 용량 > 5MB → 400 FILE_TOO_LARGE
3) 확장자 화이트리스트 (subdir 별: products=jpg jpeg png webp · disputes=+pdf)
   → 아니면 400 FILE_TYPE_NOT_ALLOWED
4) 내용 검사 — 확장자만 믿지 않는다
   이미지: ImageIO.read() 가 null 이면 FILE_TYPE_NOT_ALLOWED  (또는 매직 바이트 4개 비교)
   pdf: 앞 4바이트가 %PDF
5) storedName = UUID.randomUUID() + "." + 확장자      ← 원본 이름은 여기 절대 안 들어간다
6) 저장 경로 = Path.of(uploadDir, subdir).resolve(storedName).normalize()
   → 그 경로가 uploadDir 아래인지 startsWith 로 확인. 아니면 FILE_STORAGE_ERROR
7) Files.copy → 실패 시 500 FILE_STORAGE_ERROR
```
> 6번이 경로 조작 방어다. 5번에서 이름을 UUID로 만들었으니 사실상 못 뚫지만, **두 겹으로 둔다.**

**3) `ProductImageController` — `GET /api/products/{id}/images/{imageId}`**

```
1) imageId 로 ProductImage 조회 → 없으면 404 PRODUCT_NOT_FOUND
2) image.product.id != id 이면 404   ← 다른 상품의 이미지를 URL로 긁는 걸 막는다
3) load() 한 Resource 를 Content-Type 붙여 반환 (Content-Disposition 없음 — 인라인 표시)
```
permitAll은 `SecurityConfig`에 이미 있다 (`/api/products/*/images/*`).

**4) 기동 시 `uploads/products`·`uploads/disputes` 를 만든다** — `@PostConstruct` 에서 `Files.createDirectories`.

**5) 확인**

```bash
cp README.md fake.jpg                    # 위장 파일
# T-007(BE-C)이 연결되기 전엔 단위 테스트로 store() 를 직접 호출해 검사한다
./mvnw test -Dtest=FileStorageServiceTest
ls uploads/products                      # UUID 이름만 보여야 한다
```

`확인:` 위장 파일 → `FILE_TYPE_NOT_ALLOWED` · `../` 이름 → `uploads/` 밖에 아무것도 없음 · 채널에 "`FileStorageService` 올렸습니다, `store(file, \"products\")` 쓰세요" 공지.
**이게 D3 저녁에 안 나오면 BE-C의 D4 T-007이 막힌다.**

---

### T-010 — 분쟁 접수·증빙 파일 (D5~D6 · 내 기능 ③)

> **분업** — `PAID|SHIPPING → DISPUTED` 전이는 BE-D의 `TransactionService.markDisputed(buyerId, txId)`다. 나는 `DisputeService.open()` 안에서
> 그걸 **호출**하고 Dispute·DisputeFile을 만든다. `open()`이 `@Transactional`이라 둘이 한 트랜잭션이다(§2.5).

**1) `DisputeService.open()` (BR-06의 내 몫)**

```
1) 호출자가 구매자인가?                    → 아니면 403
2) status 가 PAID 또는 SHIPPING 인가?      → 아니면 409 INVALID_TRANSACTION_STATUS
3) 이미 분쟁이 있는가?                     → 409 DISPUTE_ALREADY_EXISTS
4) ── 원자적으로 ──
   transactionService.markDisputed(buyerId, txId)   ← 전이는 BE-D 메서드
   Dispute 저장 (사유)
   증빙 파일들을 DisputeFile 로 저장 (내 FileStorageService · store(file, "disputes"))
```
> 파일 저장은 T-023에서 내가 만든 `FileStorageService`다. 분쟁용 허용 목록(jpg·png·pdf)은 subdir로 구분한다.

**2) 증빙 다운로드에 권한 검사를 반드시 넣는다**

```
GET /api/disputes/{id}/files/{fileId}
  허용: 거래 당사자(구매자·판매자) + 관리자
  그 외 → 403
```
> **여기가 이 프로젝트에서 가장 새기 쉬운 구멍이다.** 파일 id만 바꿔 가며 남의 증빙을 받을 수 있게 된다.

**3) 관리자용 다운로드 경로 — `GET /admin/disputes/{id}/files/{fileId}` (세션 인증)**

같은 저장·권한 코드로 한 매핑만 더 둔다. BE-B의 관리자 화면(T-018)이 이 링크를 건다.
`forceRefund`·`forceConfirm`(BR-07·08)은 **BE-D T-009** 것이다 — 내가 만들지 않는다.

<details><summary>참고 — BR-07·08 부작용 (BE-D 것)</summary>

```
BR-07 강제 환불
   transaction.status = REFUNDED
   product.status     = ON_SALE        ← 다시 팔 수 있게 돌아간다
   buyer.balance     += amount         ← 구매자 돈이 돌아온다
   dispute.status     = RESOLVED

BR-08 강제 확정
   transaction.status = CONFIRMED
   product.status     = SOLD
   seller.balance    += amount
   dispute.status     = RESOLVED
```
> **네 줄이 전부 한 트랜잭션이다.** 하나라도 빠지면 "환불됐는데 상품이 SOLD"가 남는다.
> 이 메서드는 BE-B의 관리자 화면(T-018)이 호출한다.
</details>

**4) 시연 ⑤⑥ 경로를 통으로 돌려 본다**

```
구매 → 송장 → 분쟁 신고(파일 첨부) → 관리자 화면에서 증빙 다운로드 → 강제 환불
→ 구매자 잔액이 원래대로 · 상품이 다시 판매중
```

`확인:` 위 경로가 끊기지 않고 돈다 · 환불 후 구매자 잔액이 **구매 전과 정확히 같다**.

---

## 6. 내 10일

```
D1   T-001 리포·환경 (모두의 선행 — 오전에 끝낸다)
D2   T-002 엔티티·시드 확정 → 그날 저녁 02-Entity설계서 재발췌 · Repository 인계
D3   T-021 마이페이지 API 3개  ← 내 기능 ①
D4 ★ 1차 통합 중재 — 계약 분쟁 판정은 내 몫 · 03-REST-API설계서 확정
D5~D6  T-010 분쟁 접수 · 증빙 업로드/다운로드  ← 내 기능 ②
D7   마감 · 설계서 1차 재발췌
D8 ★ 시연 PC 셋업 주도 · 전체 경로 1회
D9   발표 1~2번 슬라이드 · 회고록 · 20:00 기능 동결 선언
D10  리허설 진행 · 제출
```

---

## 7. 내가 쓰는 문서

| 문서 | 언제 | 어디서 파생 |
|---|---|---|
| `docs/01-도메인설계서.md` | D1 초안 · **D2 확정** | SPEC §1·§2·§5 + ADR |
| `docs/02-Entity설계서.md` | **D2 저녁** · D8 재발췌 | `entity/*.java` · `data.sql` |
| `docs/참고/기획서.md` | D1 저녁 | SPEC §1 + §3 |
| `docs/참고/아키텍처.md` | D1 저녁 | SPEC §7·§8 + 리포 트리 |
| `docs/참고/역할분담.md` | D1 저녁 | SPEC §1-6 |
| `README.md` (리포 루트) | D1 저녁 | SPEC §0 + 실행 3줄 |
| `docs/회고록/retro-<이름>.md` | D9 오전 | 전원 공통 |

> 문서는 **마지막 날에 몰아 쓰지 않는다.** 원본이 확정되는 날 그 자리에서 떨어뜨린다.
> 문서 머리에 `정본: SPEC.md §N · 커밋 <해시>`를 적는다.

---

## 8. 내 명령어 (복붙)

```bash
# 기동
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 내 완료 증명
node seams/check-api.mjs

# 엔티티 규약 확인 (T-002)
grep -rn "FetchType.LAZY" src/main/java/com/rookies6/udt/entity/ | wc -l   # 10 이상
grep -rn "@Setter" src/main/java/com/rookies6/udt/entity/                  # 무출력이어야 정상

# 계층 규약 확인 (전원분 · D4·D8에 돌린다)
grep -rl "Repository" src/main/java --include='*Controller.java'           # 무출력

# 목 서버 (프론트가 쓴다 · 내가 살려 둔다)
node mock/server.mjs
```

---

## 9. 내가 막힐 곳 (내 역할 고유)

| 증상 | 원인 | 볼 곳 |
|---|---|---|
| 팀원마다 DB가 안 붙는다 | **MariaDB 포트가 PC마다 다르다.** MySQL이 깔린 PC는 MariaDB가 3307 | `SPEC.md` §6.1 · 각자 `SELECT @@port;` 를 먼저 돌리게 한다 |
| `Using generated security password` | Security 스타터는 있는데 설정이 없다 | BE-B의 T-005 대기 중이면 정상 |
| 시드가 안 들어간다 | `defer-datasource-initialization` 누락 | `application.yml` |
| 게이트가 red인데 원인을 모르겠다 | 게이트는 **어느 검사가 깨졌는지 이름으로 말한다** | 그 이름을 `SPEC.md`에서 찾는다 |
| 내가 관리 업무로만 소진된다 | 실제로 잘 생기는 일이다 | D5·D6·D7에 기능을 잡지 말고 **지원에 쓴다.** 대신 D1~D3의 바닥 작업이 내 기여다 |

---

## 10. 발표에서 내가 말하는 것

**슬라이드 1~2번 (소개 2분 + 아키텍처)**

- 문제·해결 방식 · 팀/역할 — `01-도메인설계서` 1.1 · `참고/역할분담`
- 아키텍처 그림 — 브라우저 → React → API → DB, **View가 둘**

> Q&A 방어 중 내 몫: **"MVC라던데 View는 어디 있나"**
> → "서버는 계층형입니다. 사용자 View는 React, 관리자 View는 Thymeleaf. **Service는 하나**입니다."

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
