# T-006 — 상품 목록·상세 API 실구현

**담당:** BE-C · **브랜치:** `be-product-read` · **예상:** D1~D2 · **선행:** T-002

## [배경]

`controller/ProductController` · `CategoryController`는 지금 **하드코딩 껍데기**다
(`// TODO(T-006)` 표시). **URL과 응답 형태는 계약이므로 그대로 두고 본문만 바꾼다.**
이 껍데기 덕분에 프론트 3명이 이미 실서버 주소로 작업하고 있다 — 응답 형태를 바꾸면
그쪽이 즉시 깨진다.

계약 (`SPEC.md` §4.2):

```
GET /api/products?q=&categoryId=&page=0&size=12
  · 입력 전제: q는 원문 그대로 받는다 (트림·소문자화는 서버가 한다)
  · 정렬: createdAt 내림차순 고정
  · 필터: status = ON_SALE 인 상품만  ← ADR-06
  · 응답 data = {content:[…], page:{number,size,totalElements,totalPages,first,last,numberOfElements}}
```

`content[]` 원소 필드 — `id`(**문자열**) · `title` · `priceKrw` · `conditionGrade` ·
`status` · `categoryName` · `sellerNickname` · `thumbnailUrl`(**이미지 0장이면 `null`**) ·
`wishCount` · `createdAt`(**ISO8601 오프셋 포함**)

```
GET /api/products/{id}
  · data 는 객체(배열 아님). 위 필드 + description · sellerId · wished · images[] · updatedAt
  · images 는 0장이어도 [] — null 금지
  · 404 PRODUCT_NOT_FOUND · 400 VALIDATION_ERROR(형식 불량 id)
GET /api/categories → data 는 배열
```

## [목표]

껍데기를 Service·Repository 연결로 교체하고, 게이트가 계속 ok를 유지한다.

## [수용 기준]

- [ ] `ProductService` · `CategoryService` 생성 — **DTO 변환은 Service 안에서**
      (`@Transactional(readOnly = true)`)
- [ ] `ProductController`에서 `// TODO(T-006)` 주석 제거 · 하드코딩 제거
- [ ] 목록이 **시드 15건(ON_SALE)** 을 반환한다 (`totalElements: 15`)
- [ ] 검색 `?q=맥북` 이 제목 기준으로 걸러진다
- [ ] `?q=__none__` 이 **`content: []` · `totalElements: 0` · HTTP 200**
- [ ] `?size=101` 이 100으로 잘린다
- [ ] **검수대기·거래중 상품이 목록에 나오지 않는다**
- [ ] `node seams/check-api.mjs` 의 상품 관련 검사 4종이 ok

## [제약]

- 수정 허용: `controller/ProductController.java` · `CategoryController.java` ·
  `service/Product*` · `dto/Product*` · `repository/ProductRepository.java`
- **URL·응답 필드명·타입을 바꾸지 않는다.** 바꿔야 하면 `SPEC.md` §10 변경 규약을 먼저 탄다
- **Controller에서 Repository를 직접 부르지 않는다** (§8 B2)
- 엔티티를 직접 반환하지 않는다 — DTO만
- N+1 튜닝은 **T-019**에서 한다. 지금은 동작을 먼저 맞춘다

## [확인 명령]

```bash
node seams/check-api.mjs
curl -s "localhost:8080/api/products?page=0&size=12" | jq '.data.page'
curl -s "localhost:8080/api/products?q=__none__"     | jq '.data.content, .data.page.totalElements'
grep -rl "Repository" src/main/java --include='*Controller.java'   # 무출력
```
