# BE-C — 상품·검색·성능

> 이름: ____________   ·   GitHub: ____________
>
> **이 한 장이 내 2주 전부다.** 다른 문서는 여기서 필요할 때만 연다.
> 팀 공통 규칙은 [`../팀원용-한장.md`](../팀원용-한장.md),
> 날짜별 전체 순서는 [`전체로드맵.md`](../../docs/참고/전체로드맵.md).

---

## 1. 한 줄로

**화면에 가장 많이 보이는 데이터가 내 것이다.** 목록·상세·등록·이미지·찜.
그리고 **N+1 튜닝(T-019)은 이 팀에서 유일하게 "성능을 다뤘다"를 증명하는 작업**이다 — 전후 쿼리 로그를 내가 찍는다.

---

## 2. 내 파일

### 내가 소유한다 (내가 결정한다)

| 경로 | 내용 |
|---|---|
| `controller/ProductController.java` · `CategoryController.java` | 상품·카테고리 |
| `controller/WishController.java` | 찜 토글 (목록 `/api/me/wishes`는 BE-A) |
| `service/Product*` · `service/WishService.java` | 서비스 |

| `dto/Product*` · `dto/Wish*` | DTO |
| `repository/ProductRepository` · `WishRepository` · `CategoryRepository` | **D2 저녁에 BE-A에게서 인계받는다** |

### 절대 안 건드린다

- `entity/` — 필드가 필요하면 **BE-A에게 요청**한다
- **거래 상태 변경** — `TransactionService`는 BE-D 단독이다. 상품 상태를 거래 흐름에서 바꿔야 하면 **BE-D에게 요청**
- `security/` · `SecurityConfig` (BE-B)
- `common/` — 에러 코드가 더 필요하면 BE-A에게 요청

### 공용 — 만지기 전에 채널에 한 줄 올린다

| 파일 | 규칙 |
|---|---|
| `service/FileStorageService.java` | **BE-A 소유.** 호출만. 검증 로직을 복제하지 않는다 |
| `application.yml` | **BE-A 소유.** T-019에서 `hibernate.default_batch_fetch_size` **한 줄만** 넣는다. BE-B의 `app.jwt.*`는 안 건드린다. 충돌 나면 main을 받아 그 줄만 다시 넣는다 |

> 남의 파일이 틀려 보여도 고치지 않는다. **오너에게 보고**한다.
> 고치면 그 버그의 책임이 나에게 넘어온다.

---

## 3. 내 계약 상대

| 나 | 상대 | 무엇에 대해 |
|---|---|---|
| **BE-C** | **FE-B** | 상품 목록·상세·등록 · 찜 · 이미지 |

이 짝이 합의하면 상품 계약이 확정된다. **합의는 `SPEC.md`에 커밋되기 전까지 무효다.**

> D1 회의에서 FE-B와 정할 것 — **검색 `q`가 제목만 보는지 설명까지 보는지**, 그리고 이미지 엔드포인트는 **이미 비로그인 허용으로 확정**돼 있다(`SPEC.md` §4.1 인증 칸 `—`).

---

## 4. 내 티켓 (순서대로)

| 티켓 | 언제 | 선행 | 끝났다는 증거 |
|---|---|---|---|
| [T-006 상품 목록·상세 API](../../tasks/T-006-상품-목록상세-API.md) | D1~D2 | T-002 | 게이트의 상품 검사 4개 통과 |
| [T-008 찜 토글](../../tasks/T-008-찜-토글.md) | D3 | T-006 | 찜 추가/해제가 두 번 눌러도 일관된다 |
| [T-007 상품 등록 연결](../../tasks/T-007-상품등록-이미지업로드.md) | D4 (반나절) | T-006 · **T-023(BE-A)** | 사진 3장 붙은 상품이 등록된다 — 파일 저장은 BE-A 서비스 호출 |
| [T-019 N+1 해결](../../tasks/T-019-N+1-튜닝.md) | **D7** | T-006 | **전후 쿼리 로그 캡처 2장** ★ |

> **`[수용 기준]`이 비어 있는 티켓은 시작하지 않는다** — 완료 판정을 말로 하게 된다.

---

## 5. 내 티켓 — 하나씩 어떻게 하나

> 아래는 **티켓을 실제로 어떻게 하는가**다. 순서대로 하면 된다.
> 각 단계 끝의 `확인:` 을 통과해야 다음으로 간다.

### T-006 — 상품 목록·상세 실구현 (D1~D2)

`ProductController`·`CategoryController`가 **하드코딩 껍데기로 이미 있다**(`// TODO(T-006)`).
덕분에 프론트 3명이 D1부터 실서버 주소로 작업한다. **URL과 응답 형태는 계약이니 그대로 두고 본문만 바꾼다.**

**1) 먼저 `SPEC.md` §4.2를 읽는다** — 응답 JSON이 필드 단위로 적혀 있다. 이게 내 목표물이다.

**2) `ProductRepository`에 조회 메서드를 만든다** (D2 저녁에 인계받는다)

> **D2에 BE-D가 메서드 하나를 요청해 온다** — 상품 상태를 조건부로 바꾸는 `transition(id, expected, next)`
> (`@Modifying @Query`). 구매 동시성 방어용이고 코드는 `BE-D.md` 5절에 있다. **내 파일이니 내가 넣는다.** 10분 일이다.

```java
Page<Product> findByStatusAndTitleContaining(ProductStatus status, String q, Pageable p);
// 카테고리 필터가 붙으면 Specification 이나 @Query 로 간다
```
> 검색 `q`가 **제목만** 보는지 설명까지 보는지 — **D1 회의에서 FE-B와 정하고 `SPEC.md` §4.2에 적는다.**
> 제목만 보는 쪽을 권한다. 설명까지 넣으면 인덱스가 안 먹고 D7 성능 작업이 커진다.

**3) `ProductService`를 만든다**

```
list(q, categoryId, page, size)  →  Page<Product>  →  PageResponse<ProductSummaryResponse>
detail(id)                       →  없으면 BusinessException(PRODUCT_NOT_FOUND)
```
**DTO 변환은 Service 안에서 한다.** Controller에서 하면 계층 규약(B2)이 깨진다.

**4) `ProductController` 껍데기의 본문을 Service 호출로 바꾼다**

```bash
grep -rl "Repository" src/main/java --include='*Controller.java'   # 무출력이어야 정상
```

**5) 계약 세 가지를 직접 확인한다**

```bash
# 페이징 — page/size 를 그대로 되돌려 주는가
curl -s "localhost:8080/api/products?page=0&size=12" | jq '.data.page'

# 빈 결과 — 에러가 아니라 빈 배열이어야 한다
curl -s "localhost:8080/api/products?q=__none__" | jq '.data.content, .data.page.totalElements'

# 없는 id — 404 + PRODUCT_NOT_FOUND
curl -s localhost:8080/api/products/999999999 | jq '.code, .statusCode'
```

**6) 하지 말 것 둘**
- **엔티티를 직접 반환하지 않는다.** 지연 로딩이 컨트롤러 밖에서 터지고, 비밀번호까지 새어 나갈 수 있다
- **N+1은 지금 고치지 않는다.** T-019가 그 일이다. **지금 미리 고치면 "개선 전" 로그를 못 찍는다** — 발표 자료가 날아간다

`확인:` 게이트의 상품 검사 4개 ok.

---

### T-007 — 상품 등록 · `FileStorageService` 연결 (D4 · 반나절)

**1) `SPEC.md` §4.4를 읽는다** — multipart 형태가 적혀 있다.
`product` 파트(JSON) + `images` 파트(파일 여러 개). **base64로 바꾸지 않는다.**

**2) 파일 저장은 내가 만들지 않는다** — BE-A의 `FileStorageService`(T-023)가 D3 저녁에 나온다.
`fileStorageService.store(file, "products")` 한 줄이 검증(형식·용량·이름·경로)을 다 하고 `StoredFile`을 돌려준다.
**여기서 다시 검증하지 않는다** — 그 예외(`FILE_TYPE_NOT_ALLOWED` 등)가 그대로 봉투로 나가게 둔다.
내가 하는 검사는 **개수(최대 5장 → `FILE_COUNT_EXCEEDED`)** 하나뿐이다.

**3) `ProductService.create()` 를 만든다**

```
1) 로그인 사용자 = seller
2) Product 저장  (status = INSPECTING  ← 등록 즉시 판매중이 아니다)
3) images 를 순회하며 fileStorageService.store(file, "products") → StoredFile → ProductImage 저장 (storedName·originalName·sortOrder)
4) ProductDetailResponse 반환
```
> **등록하면 `INSPECTING`이다.** 관리자 승인(BR-01)을 거쳐야 `ON_SALE`이 된다. 시연 ①②가 이것이다.

**5) 확인한다**

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"seller1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)

curl -s -X POST localhost:8080/api/products -H "Authorization: Bearer $TOKEN" \
  -F 'product={"title":"테스트","description":"설명","priceKrw":100000,"conditionGrade":"A","categoryId":"1"};type=application/json' \
  -F 'images=@a.jpg' -F 'images=@b.jpg' | jq '.data.status, (.data.images|length)'
```

`확인:` `"INSPECTING"` 과 `2` 가 나온다.

---

### T-008 — 찜 토글 (D5)

가장 작은 티켓이다. **멱등성 하나만 조심하면 된다.**

```
POST   /api/products/{id}/wishes   이미 있으면 409 WISH_ALREADY_EXISTS
DELETE /api/products/{id}/wishes   없으면 404 WISH_NOT_FOUND   ← 계약 확정. 협의 대상이 아니다
(`GET /api/me/wishes` · `/api/me/products` 목록은 BE-A의 T-021이 만든다 — 이 티켓은 토글만)
```

> `(userId, productId)` 에 **unique 제약**을 건다. 따닥 두 번 눌러도 두 줄이 안 생긴다.
> 프론트의 낙관적 UI가 이 제약을 믿고 동작한다.

`확인:` 같은 상품에 찜을 두 번 걸면 두 번째가 409.

---

### T-019 — N+1 해결 + 전후 캡처 (D7 · ★ 발표 자료)

**이 티켓은 순서가 전부다. 순서를 바꾸면 발표 자료가 안 나온다.**

**1) 먼저 쿼리 로그를 켠다** — `application.yml`에 임시로

```yaml
logging:
  level:
    org.hibernate.SQL: debug
```

**2) 개선 "전" 로그를 먼저 찍는다 ← 이걸 건너뛰면 끝이다**

```bash
curl -s "localhost:8080/api/products?page=0&size=12" > /dev/null
# 콘솔에 뜬 select 개수를 센다. 캡처한다 → docs/images/n1-before.png
```
> 상품 12개에 이미지·카테고리를 건마다 조회하면 **1 + 12 + 12 = 25개**쯤 나온다.

**3) 고친다 — 두 방법 중 하나**

| 방법 | 쓸 곳 | 주의 |
|---|---|---|
| `default_batch_fetch_size: 100` | **페이징이 있는 목록** ← 우리 경우 | `application.yml` 한 줄. **BE-B의 `app.jwt.*`는 건드리지 않는다** |
| `fetch join` | 페이징 없는 단건·소수 | **컬렉션 fetch join + 페이징은 같이 못 쓴다** (메모리에서 페이징해서 경고가 뜬다) |

**4) 개선 "후" 로그를 **같은 조건**으로 찍는다**

```bash
curl -s "localhost:8080/api/products?page=0&size=12" > /dev/null
# → docs/images/n1-after.png
```
> **같은 엔드포인트 · 같은 `size`** 여야 한다. 조건이 다르면 비교가 아니다.

**5) 숫자 두 개를 `docs/02-Entity설계서.md` §8에 적는다**
"목록 1회 호출: 25개 → 3개". 발표에서 이 문장을 말한다.

**6) 로그 레벨을 원래대로 돌리고 응답이 안 바뀌었는지 확인한다**

```bash
node seams/check-api.mjs   # 9/9 유지
```

`확인:` 캡처 2장 + 숫자 2개 + 게이트 9/9. **API 응답은 한 글자도 바뀌지 않았다.**

---

## 6. 내 10일

```
D1~D2  T-006 상품 목록·상세 실구현 (껍데기 교체)
D3     T-008 찜 토글
D4     T-007 상품 등록 — BE-A FileStorageService 연결 (반나절) + 통합 대응
D5     상품 마감
D4   ★ 1차 통합 대응 — FE-B와 응답 형태를 맞춘다
D6     마무리 · 03-REST-API설계서 확정
D7   ★ T-019 N+1 해결 + 전후 캡처 2장 (이건 미루면 증거가 안 남는다)
D8   ★ 시연 ①③ 구간 (상품 등록 · 검색→상세→찜→구매)
D9     발표 API 계약·병렬 개발 슬라이드 + 성능 슬라이드 · 회고록
D10    리허설
```

---

## 7. 내가 쓰는 문서

| 문서 | 언제 | 어디서 파생 |
|---|---|---|
| `docs/03-REST-API설계서.md` | D2 초안 · **D4 확정** | SPEC §4·§5 + `ErrorCode.java` |
| `docs/02-Entity설계서.md` §8 (N+1 전후) | **D7** | 쿼리 로그 캡처 — **내가 찍는다** |
| `docs/images/` 쿼리 로그 2장 | D7 | 같은 엔드포인트·같은 size로 전후 |
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

# 목록·페이징·빈 결과
curl -s "localhost:8080/api/products?page=0&size=12" | jq '.data.page'
curl -s "localhost:8080/api/products?q=__none__"     | jq '.data.content, .data.page.totalElements'

# 계층 규약 — 무출력이어야 정상
grep -rl "Repository" src/main/java --include='*Controller.java'

# 상품 등록 (T-007)
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"seller1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)
curl -s -X POST localhost:8080/api/products -H "Authorization: Bearer $TOKEN" \
  -F 'product={"title":"테스트","description":"설명","priceKrw":100000,"conditionGrade":"A","categoryId":"1"};type=application/json' \
  -F 'images=@a.jpg' -F 'images=@b.jpg' | jq '.data.status, (.data.images|length)'

# N+1 확인 (T-019) — application.yml 에 잠깐 켠다
#   logging.level.org.hibernate.SQL: debug
#   목록 1회 호출의 쿼리 개수를 전후로 센다
```

---

## 9. 내가 막힐 곳 (내 역할 고유)

| 증상 | 원인 | 볼 곳 |
|---|---|---|
| 목록 1회에 쿼리가 수십 개 | **N+1.** 이미지·카테고리를 건마다 조회 | T-019 — `default_batch_fetch_size` 또는 `fetch join` |
| `fetch join` + 페이징이 경고를 뱉는다 | 컬렉션 fetch join은 페이징과 같이 못 쓴다 | `default_batch_fetch_size` 쪽으로 간다 |
| 빈 검색 결과가 500 | `content: []`도 정상 응답이다 | 게이트의 빈 결과 검사 |
| `thumbnailUrl`이 응답에서 사라진다 | `@JsonInclude(NON_NULL)`을 전역으로 걸었다 | **걸지 않는다** — 프론트가 키 존재로 분기한다 |
| 파일 형식·용량·이름 문제 | 전부 BE-A `FileStorageService` 소관 | 내가 고치지 않는다 — BE-A에게 보고 |
| 상품 상태를 내가 바꿔야 할 것 같다 | 거래가 바꾸는 상태다 | **BE-D에게 요청** — `TransactionService` 단독 |

---

## 10. 발표에서 내가 말하는 것

**슬라이드 2개가 내 몫이다.**

1. **API 계약 발췌 + 병렬 개발 전략** — `03-REST-API설계서` §2.5
   > "프론트와 백엔드를 어떻게 동시에 진행했나"의 답이 이것이다 — **목 서버 + 계약 우선**.
2. **N+1 개선 전후 쿼리 로그** — `02-Entity설계서` §8
   > 숫자 두 개를 말한다. "목록 1회에 쿼리 N개 → M개". 캡처를 띄워 놓고 말한다.

> 2번은 **D7에 안 찍으면 발표할 게 없다.** 미루지 않는다.

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
