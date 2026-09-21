# REST API 설계서 — UDT

## 문서 정보

| 항목 | 내용 |
|---|---|
| 프로젝트명 | UDT (Used Device Trade) |
| 문서 버전 | v0.1 |
| 작성일 | 2026-09-21 |
| 작성자 | (이름) |
| 최종 수정일 | 2026-09-21 |

> 정본: `SPEC.md` §4 · §5 · §7 · `common/ErrorCode.java` — 이 문서는 파생본이다.
> **D4(1차 통합)에 실물과 대조해 확정한다.**

---

## 1. API 설계 개요

### 1.1 설계 목적

프론트엔드와 백엔드가 **별도 리포·별도 담당자**로 나뉘어 있으므로, 두 팀 사이를 오가는
것은 HTTP 호출뿐이다. 이 문서는 그 호출의 계약이며 **코드보다 먼저 확정**되었다.
덕분에 백엔드 구현 전에도 프론트엔드가 실제와 같은 응답 형태로 개발을 시작할 수 있었다.

### 1.2 설계 원칙

| # | 원칙 |
|---|---|
| 1 | 모든 응답은 **동일한 봉투**를 쓴다 (성공·실패 구분 없이) |
| 2 | 에러는 **4xx/5xx 상태코드 + `success:false`**로 고정한다. `200 + 실패` 조합을 쓰지 않는다 |
| 3 | 응답은 **DTO만** 반환한다. 엔티티를 직접 반환하지 않는다 |
| 4 | JSON 키는 **camelCase** (DB 컬럼 표기는 프론트에 노출되지 않는다) |
| 5 | **식별자는 문자열**로 내린다 (JavaScript 정수 정밀도 문제 회피) |
| 6 | 날짜는 **ISO 8601 + 오프셋 포함** |
| 7 | 목록 필드는 비어도 **항상 배열**(`[]`)로 준다. `null`을 주지 않는다 |
| 8 | 에러 생성은 `@RestControllerAdvice` **한 곳**에서만 |

### 1.3 기술 스택

| 영역 | 기술 |
|---|---|
| 프레임워크 | Spring Boot 4.0.8 (Java 17) |
| 보안 | Spring Security · JWT (`jjwt` 0.12.6) · BCrypt |
| 데이터 | Spring Data JPA · MariaDB 10.11 |
| 직렬화 | Jackson (`time-zone: Asia/Seoul`) |
| 검증 | Bean Validation (`spring-boot-starter-validation`) |

---

## 2. API 공통 규칙

### 2.1 URL 설계 규칙

| 규칙 | 예시 |
|---|---|
| 리소스는 복수형 명사 | `/api/products` · `/api/transactions` |
| 계층 관계는 경로로 표현 | `/api/products/{id}/wishes` |
| 행위는 동사를 쓰지 않고 하위 리소스·상태로 표현 | `/api/transactions/{id}/confirm` (상태 전이) |
| 내 자원은 `/api/me` 하위로 | `/api/me/wishes` · `/api/me/transactions` |
| 소문자·하이픈 없음 (단일 단어 유지) | — |

### 2.2 HTTP 메서드 사용 규칙

| 메서드 | 용도 | 이 프로젝트의 예 |
|---|---|---|
| GET | 조회 (부작용 없음) | `GET /api/products` |
| POST | 생성 | `POST /api/products` · `POST /api/products/{id}/purchase` |
| PATCH | **부분 변경 · 상태 전이** | `PATCH /api/transactions/{id}/confirm` |
| DELETE | 삭제 | `DELETE /api/products/{id}/wishes` |

> PUT은 사용하지 않는다. 이 서비스의 변경은 전부 부분 변경이거나 상태 전이다.

### 2.3 HTTP 상태 코드 가이드

| 코드 | 사용 상황 |
|---|---|
| 200 | 조회·변경 성공 |
| 201 | 생성 성공 (회원가입 · 상품 등록 · 구매 요청 · 찜 추가 · 분쟁 접수) |
| 400 | 입력값 오류 · 비즈니스 제약 위반(잔액 부족·본인 상품 구매) |
| 401 | 인증 실패 또는 미인증 |
| 403 | 인증은 됐으나 권한 없음 (남의 거래 조작) |
| 404 | 자원 없음 |
| 409 | 상태 충돌 (판매중 아님 · 중복 찜 · 상태 전이 불가) |
| 500 | 서버 오류 |

### 2.4 공통 요청 헤더

| 헤더 | 값 | 필요 시점 |
|---|---|---|
| `Content-Type` | `application/json` | 요청 본문이 있을 때 |
| `Content-Type` | `multipart/form-data` | 파일 업로드 |
| `Authorization` | `Bearer <accessToken>` | 인증이 필요한 요청 |

### 2.5 공통 응답 형식

**성공 — 단일 객체**

```json
{
  "success": true,
  "data": { "id": "12", "title": "맥북 에어 M2 13인치" },
  "message": "상품 상세 조회가 완료되었습니다",
  "timestamp": "2026-09-22T14:03:00+09:00"
}
```

**성공 — 목록 (페이지네이션)**

```json
{
  "success": true,
  "data": {
    "content": [ { "id": "12", "title": "맥북 에어 M2 13인치" } ],
    "page": {
      "number": 0, "size": 12, "totalElements": 137, "totalPages": 12,
      "first": true, "last": false, "numberOfElements": 12
    }
  },
  "message": "상품 목록 조회가 완료되었습니다",
  "timestamp": "2026-09-22T14:03:00+09:00"
}
```

**실패**

```json
{
  "success": false,
  "statusCode": 409,
  "code": "PRODUCT_NOT_ON_SALE",
  "message": "판매 중인 상품이 아닙니다",
  "timestamp": "2026-09-22T14:03:00+09:00"
}
```

검증 실패 시에는 `fields` 배열이 추가된다.

```json
{ "success": false, "statusCode": 400, "code": "VALIDATION_ERROR",
   "message": "입력값을 확인해 주세요",
   "fields": [ { "name": "trackingNo", "message": "송장번호는 8~20자리 숫자입니다" } ],
   "timestamp": "…" }
```

> **페이지 번호는 0부터 시작한다.** 서버 응답을 그대로 쓰고, 화면에 보여줄 때만 +1 한다.
> 한쪽만 1-base로 바꾸면 첫 페이지가 빈 결과로 나온다.

---

## 3. 인증 및 권한 관리

### 3.1 인증 방식 — 진입점에 따라 둘

| 대상 | 방식 | 이유 |
|---|---|---|
| 사용자 API (`/api/**`) | **JWT** (`Authorization: Bearer`) | 프론트(5173)와 백엔드(8080)의 출처가 달라 쿠키 처리가 복잡해진다 |
| 관리자 화면 (`/admin/**`) | **세션 + 폼 로그인** | 폼 중심 화면이라 CSRF 토큰과 PRG 패턴이 프레임워크 기본으로 따라온다 |

**Security 필터 체인 3개** (`@Order`로 순서 고정 — API · 관리자 · 그 외)

```
@Order(1)  /api/**   stateless · JWT 필터 · CSRF off · CORS 적용 · 401은 JSON 봉투
@Order(2)  /admin/** 세션 · formLogin · CSRF on · 403은 HTML
@Order(3)  그 외      permitAll
```

> `/api/**` 체인이 먼저 매칭되어야 한다. 순서가 바뀌면 REST 요청이 로그인 HTML로
> 리다이렉트되어 프론트에서 JSON 파싱 오류가 난다.

**로그인 요청·응답**

```
POST /api/auth/login
{ "email": "buyer1@udt.test", "password": "Test1234!" }
```
```json
{ "success": true,
   "data": { "accessToken": "eyJhbGciOi…",
             "user": { "id": "3", "nickname": "구매자", "role": "MEMBER", "balanceKrw": 2000000 } },
   "message": "로그인되었습니다", "timestamp": "…" }
```

**토큰 만료 시 동작:** 401 응답 → 프론트 인터셉터가 인증 상태를 비우고 로그인 화면으로
이동 + 안내 문구. 단 `/api/auth/**` 응답은 이 처리에서 제외한다(로그인 실패도 401이라
무한 리다이렉트가 된다).

토큰 규격: 알고리즘 HS256 · sub=회원 ID · claims{role, nickname} · 만료 24시간(`app.jwt.expiration-seconds=86400`) · 재발급 없음

### 3.2 권한 레벨 정의

| 레벨 | 접근 가능 | 대상 엔드포인트 |
|---|---|---|
| 공개 | 누구나 | `GET /api/health` · `GET /api/categories` · `GET /api/products` · `GET /api/products/{id}` · 상품 이미지 · `POST /api/auth/**` |
| MEMBER | 로그인한 회원 | 상품 등록 · 찜 · 구매 · 내 정보 · 내 거래 |
| 당사자 | 해당 거래의 구매자 또는 판매자 | 거래 상세 · 배송 등록(판매자) · 구매확정(구매자) · 분쟁 접수(구매자) |
| ADMIN | 관리자 | `/admin/**` · 분쟁 증빙 다운로드 |

메서드 레벨 보안: 관리자 전용 처리에 `@PreAuthorize("hasRole('ADMIN')")` 적용.

---

## 4. 상세 API 명세

### 4.1 인증 API

| # | 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|---|
| 1 | POST | `/api/auth/signup` | 회원가입 | — |
| 2 | POST | `/api/auth/login` | 로그인 | — |
| 3 | GET | `/api/me` | 내 정보 | MEMBER |

**POST /api/auth/signup**

요청: `{ "email": "...", "password": "...", "nickname": "..." }`
성공 **201**: `data` = `{ "id", "email", "nickname" }`
에러: 409 `EMAIL_ALREADY_EXISTS` · 400 `VALIDATION_ERROR`

**GET /api/me**

성공 200: `data` = `{ "id", "email", "nickname", "role", "balanceKrw" }`
에러: 401 `AUTHENTICATION_REQUIRED`

### 4.2 상품 API

| # | 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|---|
| 1 | GET | `/api/categories` | 카테고리 목록 | — |
| 2 | GET | `/api/products` | 상품 목록 (검색·페이징) | — |
| 3 | GET | `/api/products/{id}` | 상품 상세 | — |
| 4 | POST | `/api/products` | 상품 등록 (multipart) | MEMBER |
| 5 | GET | `/api/products/{id}/images/{imageId}` | 상품 이미지 | — |
| 6 | POST | `/api/products/{id}/wishes` | 찜 추가 | MEMBER |
| 7 | DELETE | `/api/products/{id}/wishes` | 찜 해제 | MEMBER |

**GET /api/products**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `q` | string | N | — | 상품명 검색어. **원문 그대로** 전달(정규화는 서버가 한다) |
| `categoryId` | string | N | — | 카테고리 필터 |
| `page` | int | N | 0 | 페이지 번호 (**0-base**) |
| `size` | int | N | 12 | 페이지 크기 (최대 100) |

정렬: 등록일 내림차순 고정 · **판매중 상품만 조회된다**

응답 `data.content[]` 원소:

| 필드 | 타입 | 필수 | 형식 | 빈 값 |
|---|---|---|---|---|
| `id` | string | Y | 서버 발급 | — |
| `title` | string | Y | ≤100자 | — |
| `priceKrw` | number | Y | 원 단위 | — |
| `conditionGrade` | string | Y | `S`·`A`·`B`·`C` | — |
| `status` | string | Y | 상품 상태 | 목록은 항상 `ON_SALE` |
| `categoryName` | string | Y | — | — |
| `sellerNickname` | string | Y | — | — |
| `thumbnailUrl` | string | N | 경로 | **`null`** (이미지 0장) |
| `wishCount` | number | Y | — | — |
| `createdAt` | string | Y | ISO8601 오프셋 포함 | — |

에러: 400 `VALIDATION_ERROR` · 500 `INTERNAL_SERVER_ERROR`

**GET /api/products/{id}**

`data`는 **객체**(배열 아님). 위 필드 + `description` · `sellerId` · `wished`(boolean) ·
`images`(**배열 · 0장이면 `[]`**) · `updatedAt`.
`images[]` 원소: `{ "id", "url", "sortOrder" }`

에러: 404 `PRODUCT_NOT_FOUND` · **400 `VALIDATION_ERROR`(형식이 잘못된 id)**

**POST /api/products** (multipart)

파트: `product`(JSON) + `images`(파일 0~5개)
제약: jpg/png/webp · 각 5MB · 최대 5장 — **서버가 검증한다**
성공 **201**: 상세와 동일한 객체. `status`는 항상 `INSPECTING`
에러: 400 `VALIDATION_ERROR`(+`fields`) · 400 `FILE_TYPE_NOT_ALLOWED` · 400 `FILE_TOO_LARGE` · 401

> 등록 직후 목록에 보이지 않는 것이 정상이다(검수 대기). 화면에 안내 문구를 띄운다.

**POST / DELETE /api/products/{id}/wishes**

성공: 201 / 200 · `data` = `{ "productId", "wished", "wishCount" }`
에러: 409 `WISH_ALREADY_EXISTS` · 404 `WISH_NOT_FOUND` · 401

### 4.3 거래 API

| # | 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|---|
| 1 | POST | `/api/products/{id}/purchase` | 구매 요청 | MEMBER |
| 2 | GET | `/api/transactions/{id}` | 거래 상세 | 당사자 |
| 3 | GET | `/api/me/transactions?role=buyer\|seller` | 내 거래 목록 | MEMBER |
| 4 | PATCH | `/api/transactions/{id}/shipping` | 배송 정보 등록 | 판매자 |
| 5 | PATCH | `/api/transactions/{id}/confirm` | 구매 확정 | 구매자 |

**POST /api/products/{id}/purchase**

요청 본문 없음. 성공 **201**:

```json
{ "id": "7", "productId": "12", "productTitle": "맥북 에어 M2 13인치",
   "buyerId": "2", "sellerId": "5",
   "buyerNickname": "구매자", "sellerNickname": "판매왕", "amountKrw": 850000,
   "status": "PAID", "courier": null, "trackingNo": null,
   "createdAt": "2026-09-22T15:00:00+09:00", "confirmedAt": null }
```

> **`buyerId`·`sellerId`는 화면이 "내가 구매자인가 판매자인가"를 판정하는 근거다.**
> 거래 상세(SCR-005)의 상태별 액션 버튼이 여기에 걸려 있다. 닉네임은 중복될 수 있어 판정에 쓰지 않는다.

에러: 409 `PRODUCT_NOT_ON_SALE` · 400 `SELF_PURCHASE_NOT_ALLOWED` ·
400 `INSUFFICIENT_BALANCE` · 404 `PRODUCT_NOT_FOUND` · 401

> **원자성:** 상품 상태 변경 · 거래 생성 · 구매자 잔액 차감이 한 트랜잭션이다.

**PATCH /api/transactions/{id}/shipping**

요청: `{ "courier": "CJ대한통운", "trackingNo": "123456789012" }` (둘 다 필수 · 송장 8~20자리 숫자)
전제 상태: `PAID` · 판매자 본인만
에러: 400 `VALIDATION_ERROR`(+`fields`) · 403 `TRANSACTION_FORBIDDEN` · 409 `INVALID_TRANSACTION_STATUS`

**PATCH /api/transactions/{id}/confirm**

전제 상태: `SHIPPING` · 구매자 본인만
성공 200: `status: "CONFIRMED"` · `confirmedAt` 채워짐

> **원자성:** 거래 상태 · 상품 상태 · **판매자 잔액 증가**가 한 트랜잭션이다.
> 이 서비스에서 대금이 판매자에게 넘어가는 유일한 지점이다.

### 4.4 분쟁 API

| # | 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|---|
| 1 | POST | `/api/transactions/{id}/disputes` | 분쟁 접수 (multipart) | 구매자 |
| 2 | GET | `/api/disputes/{id}/files/{fileId}` | 증빙 파일 다운로드 (React) | 당사자 (JWT) |
| 3 | GET | `/admin/disputes/{id}/files/{fileId}` | 증빙 파일 다운로드 (관리자 화면) | ADMIN (세션) |

**POST /api/transactions/{id}/disputes**

파트: `dispute`(JSON `{ "reason": "..." }` 10~500자) + `files`(0~3개 · jpg/png/pdf · 각 5MB)
전제 상태: `PAID` 또는 `SHIPPING`
성공 **201**: `{ "id", "transactionId", "status", "reason", "files":[{"id","originalName"}], "createdAt" }`
에러: 409 `DISPUTE_ALREADY_EXISTS` · 409 `INVALID_TRANSACTION_STATUS` · 403 · 400

**GET /api/disputes/{id}/files/{fileId}** · **GET /admin/disputes/{id}/files/{fileId}**

응답: 파일 바이트 + **`Content-Disposition: attachment; filename="..."`**
에러: 403 `ACCESS_DENIED` · 404 `DISPUTE_NOT_FOUND`

> **경로가 둘인 이유** — `/api/**`는 무상태 JWT 체인이라 브라우저 주소창 이동으로는 인증되지 않는다.
> React는 `/api/...`를 `Authorization` 헤더와 함께 **blob으로** 받아 내려받고(`<a href>` 불가),
> 세션 인증인 관리자 화면(Thymeleaf)은 `/admin/...`을 **링크로** 누른다.
> 두 경로의 오너는 모두 BE-D이며 저장·권한 검사 코드는 한 벌을 공유한다.

### 4.5 기타

| 메서드 | 경로 | 응답 |
|---|---|---|
| GET | `/api/health` | `data` = `{ "status": "UP" }` |
| GET | `/api/me/wishes` | 상품 목록 형식 |
| GET | `/api/me/products` | 상품 목록 형식 (모든 상태 포함) |

---

## 5. 에러 코드 및 처리

### 5.1 표준 에러 코드

| 코드 | HTTP | 메시지 |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 입력값을 확인해 주세요 |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 |
| `AUTHENTICATION_REQUIRED` | 401 | 로그인이 필요합니다 |
| `ACCESS_DENIED` | 403 | 접근 권한이 없습니다 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 오류가 발생했습니다 |

### 5.2 비즈니스 에러 코드

| 코드 | HTTP | 메시지 | 위반 규칙 |
|---|---|---|---|
| `USER_NOT_FOUND` | 404 | 회원을 찾을 수 없습니다 | — |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일입니다 | BR-M001 |
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없습니다 | — |
| `PRODUCT_NOT_ON_SALE` | 409 | 판매 중인 상품이 아닙니다 | BR-T001 |
| `SELF_PURCHASE_NOT_ALLOWED` | 400 | 본인이 등록한 상품은 구매할 수 없습니다 | BR-T002 |
| `INSUFFICIENT_BALANCE` | 400 | 잔액이 부족합니다 | BR-T003 |
| `WISH_ALREADY_EXISTS` | 409 | 이미 찜한 상품입니다 | BR-W001 |
| `WISH_NOT_FOUND` | 404 | 찜하지 않은 상품입니다 | — |
| `TRANSACTION_NOT_FOUND` | 404 | 거래를 찾을 수 없습니다 | — |
| `TRANSACTION_FORBIDDEN` | 403 | 해당 거래의 당사자가 아닙니다 | BR-T004 |
| `INVALID_TRANSACTION_STATUS` | 409 | 현재 거래 상태에서는 처리할 수 없습니다 | BR-T004 · BR-D001 |
| `DISPUTE_ALREADY_EXISTS` | 409 | 이미 분쟁이 접수된 거래입니다 | BR-D002 |
| `DISPUTE_NOT_FOUND` | 404 | 분쟁을 찾을 수 없습니다 | — |
| `FILE_TYPE_NOT_ALLOWED` | 400 | 허용되지 않는 파일 형식입니다 | BR-P003 |
| `FILE_TOO_LARGE` | 400 | 파일 크기가 너무 큽니다 | BR-P003 |
| `FILE_COUNT_EXCEEDED` | 400 | 첨부 가능한 파일 수를 초과했습니다 | BR-P003 |
| `FILE_STORAGE_ERROR` | 500 | 파일을 저장하지 못했습니다 | — |

> 코드는 `common/ErrorCode.java` enum 하나에서만 나온다. 이 표와 enum의 항목 수가
> 다르면 표가 낡은 것이다.

### 5.3 예외 처리 구조

`BusinessException(ErrorCode)` → `@RestControllerAdvice` → 공통 에러 봉투.

Advice가 처리하는 스프링 예외:

| 예외 | → |
|---|---|
| `MethodArgumentNotValidException` | 400 `VALIDATION_ERROR` + `fields` |
| `HttpMessageNotReadableException` | 400 `VALIDATION_ERROR` |
| `MethodArgumentTypeMismatchException` | 400 `VALIDATION_ERROR` (형식 불량 id) |
| `MissingServletRequestParameterException` | 400 `VALIDATION_ERROR` |
| `NoResourceFoundException` | 404 |
| `Exception` | 500 `INTERNAL_SERVER_ERROR` |

---

## 6. API 검증

Swagger 대신 **계약 검증 스크립트**로 문서와 구현의 일치를 검사한다.

```
node seams/check-api.mjs [base_url]
```

검사 항목 9종 — 상태 코드 · 공통 봉투 · 필드 존재와 타입 · 값 범위 · 날짜 오프셋 ·
페이지 메타 · **빈 결과가 `[]`인지** · 에러 바디 형태 · 인증 동작.

같은 스크립트를 목 서버와 실서버 양쪽에 돌린다. 목 서버가 계약을 어기면 프론트가
처음부터 틀린 것을 만들게 되므로, 목도 이 검사를 통과해야 한다.

`[D4] 실행 결과 캡처 첨부`

---

## 7. 체크리스트

- [x] 모든 엔드포인트가 공통 응답 형식을 따른다
- [x] 에러 코드가 enum 한 곳에서 관리된다
- [x] 인증이 필요한 엔드포인트가 명시되어 있다
- [x] 페이지네이션 응답 구조가 정의되어 있다
- [x] 빈 값 표현(`null` vs `[]`)이 필드마다 정의되어 있다
- [x] 날짜 형식과 시간대가 고정되어 있다
- [ ] 전 엔드포인트 구현 완료 및 검증 스크립트 통과 (D8)
