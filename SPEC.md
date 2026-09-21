# UDT — 중고 전자기기 안전거래 플랫폼 · SPEC

**v0.2 초안 (D1 회의 안건) · 2026-09-21 작성 · 확인: (전원 이름)**

> **이 파일의 정본은 `UDT-Used_Device_Trade-backend` 리포에만 있다.**
> 프론트 리포에는 복사하지 않는다 — 복사본은 반드시 낡는다(§12).

> **이 문서가 정본이다.** 코드·다른 문서와 어긋나면 이 문서가 이긴다.
> §0 확정값 표와 본문이 어긋나면 **§0 표가 이긴다.**
> `[초안 — D1 회의 안건]` 표시가 붙은 절은 팀 합의 전까지 확정이 아니다.

---

## §0 스택 확정값 표 (논쟁 없이 확정 · 이 표가 정본)

| 항목 | 확정값 |
|---|---|
| 프론트 | React 18 + Vite (`npm create vite@latest -- --template react`) · `package-lock.json` 커밋 · Node LTS |
| 백엔드 | Spring Boot 4.0.x + Java 17 · **Maven wrapper 커밋**(`./mvnw`로만 실행) |
| DB | **MariaDB 10.x** · 드라이버 `mariadb-java-client` · **각자 로컬** · DB명 `udt` · 계정 `udt/udt` · 포트 **3307**(다르면 `DB_PORT` 환경변수) · 공유 개발 DB 없음 (ADR-11) |
| 포트 | 프론트 5173 (`server: { port: 5173, strictPort: true }`) · 백엔드 8080 |
| JSON 키 | **camelCase** (Jackson 기본 — 변환 레이어 없음) |
| DB 컬럼 | snake_case (JPA 매핑이 흡수 · 프론트는 DB를 모른다) |
| 식별자 | **JSON에서 문자열** — DTO의 id를 `String`으로. 전역 Long 문자열화 금지 |
| 날짜 | `OffsetDateTime` → ISO 8601 · `spring.jackson.time-zone=Asia/Seoul` · **`LocalDateTime` 엔티티에서도 금지** |
| 금액 | `Long` · 원 단위 · 필드명 `priceKrw`·`amountKrw` |
| 응답 봉투 | 성공 `{"success":true,"data":…,"message":"…","timestamp":"…"}` · 목록은 `data.content[]` + `data.page{...}` · 에러 `{"success":false,"statusCode":404,"code":"…","message":"…","timestamp":"…"}` |
| 에러 처리 | `@RestControllerAdvice` **한 곳** (`common/GlobalExceptionHandler`) |
| 응답 객체 | **DTO만 · 엔티티 직접 반환 금지** · DTO 변환은 Service 안에서 |
| API 호출 | axios 인스턴스 1개(`src/api/client.js`) · `timeout: 10000` · 인터셉터가 **`res.data.data`** 반환 · `success:false`면 `code`·`message`로 reject · `/api/auth/**`는 401 처리 제외 |
| 라우팅 | `react-router-dom` · 경로 문자열은 `src/routes.js` 상수에서만 |
| 전역 상태 | **Zustand 스토어 2개까지** — `authStore`(토큰·사용자·`persist`) · `wishStore`(찜 id) · **필터·페이지는 URL 쿼리** |
| 폼 검증 | HTML 기본 검증 + 제출 직전 함수 하나 · 검증 라이브러리 금지 · **정본은 백엔드 400** |
| 화면 상태 | 화면마다 `loading`·`error` state — §3 화면 상태 계약의 네 상태와 1:1 |
| 스타일 | **CSS Modules**(`<컴포넌트>.module.css`) + `styles/tokens.css` 1파일 · 색·간격 리터럴 금지 |
| 반응형 | **뷰포트 3종** — 모바일 390 · 태블릿 768 · 데스크톱 1280 |
| 공용 컴포넌트 | 첫날 `Button`·`Layout`·`LoadingSpinner` · 파일마다 `propTypes` (`prop-types` 패키지) |
| 인증 전달 | `Authorization: Bearer <jwt>` + localStorage (`authStore` persist) |
| 관리자 | Thymeleaf SSR · 세션 + `formLogin` + CSRF on · Security 체인 2개(§7) |
| seam 게이트 | `node seams/check-api.mjs [base_url]` (리포 루트에서) |
| 프론트 env | `VITE_API_URL` **하나만** |
| 페이지 인덱스 | **0-base 그대로** · `max-page-size=100` · 프론트는 표시만 +1 |
| OSIV | `spring.jpa.open-in-view=false` (로컬부터) |
| 시드 | `spring.jpa.defer-datasource-initialization=true` · `spring.sql.init.mode=always`(local) / `never`(prod) · **`data.sql`은 id를 명시하지 않는다** |
| `ddl-auto` | local `create` (+시드 재실행) · prod `validate` |
| 파일 저장 | 로컬 디스크 `uploads/` (gitignore) · 이미지 최대 5장·5MB·jpg/png/webp |
| **배포** | **범위 외** — 2주 기간에서 기능 완성도·문서화를 우선했다. 시연은 로컬 실행 (ADR-07) |

---

## §1 정렬 문서

### 1. 한 줄 정의
중고 전자기기 거래에서 **"돈 보내고 물건을 못 받거나, 사진과 다른 물건을 받는"** 문제를,
관리자 검수 → 가상 에스크로 → 구매확정 전 정산 보류 → 분쟁 시 관리자 개입이라는
**거래 상태 프로세스 자체**로 해결하는 웹 서비스.

### 2. 완료 그림 (마지막 날 시연 클릭 경로) — **전원이 소리 내어 한 번 읽는다**

```
① 판매자 로그인 → 상품 등록(사진 3장 업로드) → "검수대기" 상태 확인
② 관리자 /admin/products 로그인 → 검수 승인 → 상품이 "판매중"으로 전환
③ 구매자 로그인 → 목록에서 검색 → 상세 → 찜 → 구매하기 → 잔액 차감 · 상태 "거래중"
④ 판매자 → 거래 상세에서 송장번호 입력 → "배송중"
⑤ 구매자 → 분쟁 신고(증빙 사진 첨부) → 상태 "분쟁"
⑥ 관리자 /admin/disputes → 증빙 파일 다운로드 → 강제 환불 → 구매자 잔액 복구 · 상품 "판매중" 복귀
⑦ (정상 경로) 다른 거래에서 구매확정 → 판매자 잔액 증가 · 상품 "거래완료"
```

### 3. 명시적 비범위
실시간 채팅 · 실제 PG 결제 · 소셜 로그인 · 이메일/알림 발송 · 비밀번호 재설정 ·
무한 스크롤 · 다크모드 · 썸네일 자동 생성 · 추천 알고리즘 · 관리자 통계 대시보드 ·
**배포**(ADR-07) · 관리자 권한 세분화(역할 2종으로 고정)

### 4. 데이터·외부 연동
**외부 API 없음.** 모든 데이터는 `data.sql` 시드 + 사용자 입력.
→ 이 프로젝트의 최대 위험(키 발급·요금제·응답 지연)이 구조적으로 0이다.
상품 이미지 샘플은 팀이 직접 찍거나 무료 이미지 5~10장을 `seed-images/`에 둔다.

### 5. 기술 스택 · 개발 환경
§0 표. 전원 확인: `java -version` · `node -v` · `(cd backend && ./mvnw -v)`의 Java version 줄 · MySQL 8 기동.
**JDK는 17 이상이면 된다**(`pom.xml`의 `<java.version>17</java.version>`은 컴파일 타깃이라 JDK 21·24에서도 빌드된다).
다만 팀원마다 메이저가 다르면 "제 로컬에선 됩니다"가 생기므로 D1에 하나로 맞춘다.

### 6. 역할·오너십
**7명 — 백엔드 4(BE-A~D) / 프론트 3(FE-A~C).** 조는 **둘**, 팀 층 seam은 **API 하나**.
상세는 `docs/03-역할분담.md`.

### 7. 화면 목록 (§3) — 사용자 7종 + 관리자 3종. **이 목록에 없는 화면은 만들지 않는다.**

### 8. 인증
**범위에 있음.** 이메일+비밀번호 1종 · BCrypt · JWT(사용자) / 세션 폼(관리자).
회원가입은 일반 회원만. 관리자 계정은 시드에 1개 고정, 가입 화면 없음.

### 9. 환경
배포 없음 · 로컬 완결 · 지원 브라우저 **Chrome 1종** · 시연은 **시연 PC 1대에서 프로세스 2개**(Vite dev + Spring).

### OPEN
- (확정) 팀 인원 **7명 — 백엔드 4 / 프론트 3**
- `OPEN: D1` 서비스명 `UDT` 확정 여부 (대안: 안심거래, 체크딜)
- `OPEN: D1 강사 확인` 개인 회고록의 제출 형식(제출물 목록에 있음 → 일단 만든다)

---

## §2 도메인 · 상태 머신

### 2.1 역할
| 역할 | 하는 일 |
|---|---|
| `MEMBER` | 판매자·구매자 겸용. 상품 등록·구매·찜·분쟁 신고 |
| `ADMIN` | 상품 검수 승인/반려 · 분쟁 강제 환불/확정. **관리자 화면(Thymeleaf)에서만 활동** |

### 2.2 엔티티 (9개)

| 엔티티 | 관계 | 비고 |
|---|---|---|
| `User` | — | email(unique) · password(BCrypt) · nickname · role · **balanceKrw**(가상 잔액) |
| `Category` | Product 1:N | 시드 고정 5행(노트북·스마트폰·태블릿·이어폰·기타) |
| `Product` | User N:1 · Category N:1 | status · priceKrw · conditionGrade |
| `ProductImage` | Product N:1 | **1:N** · sortOrder · 최대 5장 |
| `Wish` | User N:1 · Product N:1 | **N:M 조인 엔티티** · `unique(user_id, product_id)` |
| `Transaction` | Product **1:1** · buyer(User) N:1 | status · amountKrw · courier · trackingNo · `unique(product_id)` |
| `Dispute` | Transaction **1:1** | status · reason · adminMemo · resolvedAt |
| `DisputeFile` | Dispute N:1 | **1:N** · 증빙 파일(다운로드 대상) |
| `Review` | Transaction **1:1** · writer/target N:1 | **P2 — D7 여유 시.** rating 1~5 |

> `Review`는 핵심 거래 흐름(등록→검수→구매→확정→분쟁)에 필수가 아니다. **우선순위 P2** —
> D5 중간 점검에서 상위 기능이 모두 끝났을 때만 착수한다.

### 2.3 상태 머신 (이 서비스의 핵심)

**Product.status**
```
INSPECTING(검수대기) ──관리자 승인──▶ ON_SALE(판매중) ──구매요청──▶ IN_TRADE(거래중)
        │                                    ▲                          │
        └──관리자 반려──▶ REJECTED            └──강제환불(관리자)──────────┤
                                                                        └──구매확정──▶ SOLD
```

**Transaction.status**
```
PAID(가상결제완료) ──판매자 송장입력──▶ SHIPPING(배송중) ──구매자 확정──▶ CONFIRMED(구매확정)
     │                                      │
     └──────구매자 분쟁신고──────────────────┘
                    ▼
              DISPUTED(분쟁) ──관리자──▶ REFUNDED(강제환불) 또는 CONFIRMED(강제확정)
```

### 2.4 두 엔티티가 맞물리는 규칙 — **각 줄이 한 트랜잭션**

| # | 행위 | 같이 바뀌는 것 (원자적) |
|---|---|---|
| BR-01 | 관리자 검수 승인 | Product `INSPECTING → ON_SALE` |
| BR-02 | 관리자 검수 반려 | Product `INSPECTING → REJECTED` + 반려 사유 저장 |
| BR-03 | 구매 요청 | Product `ON_SALE → IN_TRADE` · Transaction 생성 `PAID` · **buyer.balance −= price** |
| BR-04 | 송장 입력 | Transaction `PAID → SHIPPING` + courier·trackingNo |
| BR-05 | 구매 확정 | Transaction `→ CONFIRMED` · Product `→ SOLD` · **seller.balance += amount** |
| BR-06 | 분쟁 신고 | Transaction `PAID|SHIPPING → DISPUTED` · Dispute 생성 + 증빙 파일 저장 |
| BR-07 | 관리자 강제 환불 | Transaction `→ REFUNDED` · Product `→ ON_SALE` · **buyer.balance += amount** · Dispute `→ RESOLVED` |
| BR-08 | 관리자 강제 확정 | Transaction `→ CONFIRMED` · Product `→ SOLD` · **seller.balance += amount** · Dispute `→ RESOLVED` |

> **BR-03과 BR-05가 "정산 보류"의 구현이다** — 구매자 잔액은 결제 즉시 빠지지만
> 판매자 잔액은 구매확정 전까지 들어오지 않는다. 그 사이의 돈이 에스크로다.
> 발표 "기술적 도전" 슬라이드가 이 두 줄이다.

### 2.5 상태 변경 코드는 한 곳에만 둔다 (MUST)

**`TransactionService`(오너 BE-D)의 메서드만** Transaction·Product 상태를 바꾼다.
분쟁·관리자 기능(BE-B)과 상품 기능(BE-C)은 그 메서드를 **호출만 한다.** 이 규칙이 없으면 상태 변경 코드가
세 군데로 흩어져 D8에 "왜 상품이 판매중인데 거래가 있지"가 나온다.

### 2.6 도메인 예외 (전부 `BusinessException` + `ErrorCode`)

| 상황 | HTTP | code |
|---|---|---|
| 판매중이 아닌 상품 구매 | 409 | `PRODUCT_NOT_ON_SALE` |
| 본인 상품 구매 | 400 | `SELF_PURCHASE_NOT_ALLOWED` |
| 잔액 부족 | 400 | `INSUFFICIENT_BALANCE` |
| 이미 찜한 상품 | 409 | `WISH_ALREADY_EXISTS` |
| 상태 전이 불가(예: PAID인데 확정) | 409 | `INVALID_TRANSACTION_STATUS` |
| 남의 거래 조작 | 403 | `TRANSACTION_FORBIDDEN` |
| 이미 분쟁 진행 중 | 409 | `DISPUTE_ALREADY_EXISTS` |

---

## §3 화면 목록 + 화면 상태 계약

### 3.1 사용자 화면 (React · 7종)

| ID | 경로 | 이름 | 인증 | 담당 |
|---|---|---|---|---|
| SCR-001 | `/` | 상품 목록 (검색·카테고리·페이징) | 공개 | FE-B |
| SCR-002 | `/products/:id` | 상품 상세 (이미지·찜·구매) | 공개(구매는 로그인) | FE-B |
| SCR-003 | `/products/new` | 상품 등록 (이미지 다중 업로드) | 로그인 | FE-B |
| SCR-004 | `/mypage` | 마이페이지 (판매/구매/찜 탭 · 잔액) | 로그인 | FE-C |
| SCR-005 | `/transactions/:id` | 거래 상세 (상태별 액션) | 로그인(당사자만) | FE-C |
| SCR-006 | `/login` | 로그인 | 공개 | FE-A |
| SCR-007 | `/signup` | 회원가입 | 공개 | FE-A |
| — | `*` | NotFound + 라우트 `errorElement` | — | FE-A |

### 3.2 관리자 화면 (Thymeleaf SSR · 3종 · 담당 BE-B)

> 화면과 폼은 BE-B가 만들되, **Service는 BE-C(상품)·BE-D(거래)의 것을 호출만 한다.**
> Service가 웹 타입을 모르므로(§8 B3) REST 컨트롤러와 Thymeleaf 컨트롤러가 같은 Service를 쓴다 —
> 하이브리드가 계층 분리의 살아 있는 증거가 되는 지점이다.

| 경로 | 이름 | 폼 |
|---|---|---|
| `/admin/login` | 관리자 로그인 (담당 BE-B) | `formLogin` |
| `/admin/products` | 검수 대기 목록 + 승인/반려 | **POST → 검증 → redirect (PRG)** |
| `/admin/disputes` | 분쟁 목록 + 증빙 다운로드 + 강제 환불/확정 | POST → redirect |

### 3.3 화면 상태 계약 (MUST · 화면마다 네 상태)

| 상태 | 언제 | 화면 |
|---|---|---|
| 로딩 | 요청 중 | `LoadingSpinner` · 제출 버튼 비활성(중복 제출 방지) |
| 정상 | 200 · 결과 있음 | 본 화면 |
| **빈 결과** | 200 · 0건 | "조건에 맞는 상품이 없습니다" + 조건 완화 안내. **버그가 아니라 정상 상태다** |
| 에러 | 4xx/5xx·네트워크 실패 | 서버 `message` 표시 + 재시도 버튼 |

목 서버는 **빈 결과와 에러를 일부러 낼 수 있게** 만든다 — 검색어 `__none__`이면 빈 배열.

### 3.4 보호 라우트
`/products/new` · `/mypage` · `/transactions/:id` → 미로그인 시 `/login`으로 이동.
**토큰 만료(401) 시 동작:** 인터셉터가 `authStore` 비우고 `/login`으로 이동 + "다시 로그인해 주세요" 토스트.
(`/api/auth/**` 응답은 이 처리에서 제외 — 로그인 실패도 401이라 무한 리다이렉트가 된다.)

### 3.5 와이어프레임
화면당 손그림 1장(사진) · 화면당 5분 캡 · 첫날. **고해상도 시안 금지.**
`docs/wireframes/SCR-00N.jpg` — 여기 그려진 데이터 요소가 §4 응답 필드의 출발점이다.

---

## §4 API 계약

**공통 봉투는 §0 표. 아래 엔드포인트마다 다시 적지 않는다.**
성공 = `4xx/5xx` 아님 + `success:true`. 에러 = `4xx/5xx` + `success:false`.
**`200 + success:false` 조합은 쓰지 않는다.**

### 4.1 seam 표 (전 엔드포인트)

| 엔드포인트 | 메서드 | 인증 | 생산 | 소비 |
|---|---|---|---|---|
| `/api/health` | GET | — | BE | 게이트 |
| `/api/auth/signup` | POST | — | BE-B | SCR-007 |
| `/api/auth/login` | POST | — | BE-B | SCR-006 |
| `/api/me` | GET | MEMBER | BE-B | SCR-004 |
| `/api/categories` | GET | — | BE-C | SCR-001·003 |
| `/api/products` | GET | — | BE-C | SCR-001 |
| `/api/products/{id}` | GET | — | BE-C | SCR-002 |
| `/api/products` | POST (multipart) | MEMBER | BE-C | SCR-003 |
| `/api/products/{id}/images/{imageId}` | GET | — | BE-C | SCR-001·002 |
| `/api/products/{id}/wishes` | POST / DELETE | MEMBER | BE-C | SCR-001·002 |
| `/api/me/wishes` | GET | MEMBER | BE-C | SCR-004 |
| `/api/me/products` | GET | MEMBER | BE-C | SCR-004 |
| `/api/me/transactions?role=buyer\|seller` | GET | MEMBER | BE-D | SCR-004 |
| `/api/products/{id}/purchase` | POST | MEMBER | BE-D | SCR-002 |
| `/api/transactions/{id}` | GET | 당사자 | BE-D | SCR-005 |
| `/api/transactions/{id}/shipping` | PATCH | 판매자 | BE-D | SCR-005 |
| `/api/transactions/{id}/confirm` | PATCH | 구매자 | BE-D | SCR-005 |
| `/api/transactions/{id}/disputes` | POST (multipart) | 구매자 | BE-D | SCR-005 |
| `/api/disputes/{id}/files/{fileId}` | GET | 당사자·ADMIN | BE-D | SCR-005 · 관리자 |

> 관리자 `/admin/**`은 **seam이 아니다** — 소비자가 브라우저 사람뿐이라 계약도 게이트도 없다.

### 4.2 `GET /api/products` (백엔드 → 프론트)

- 인증: 불필요
- 쿼리: `q`(str, 선택) · `categoryId`(str, 선택) · `page`(int, 기본 0 · **0-base**) · `size`(int, 기본 12, 최대 100)
- **입력 전제:** `q`는 **원문 그대로** 넘긴다. 트림·소문자화는 서버가 한다.
- 정렬: `createdAt` 내림차순 고정
- 기본 필터: **`status=ON_SALE`인 상품만** 나온다 (검수대기·거래중·판매완료는 목록에 없다)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "12",
        "title": "맥북 에어 M2 13인치",
        "priceKrw": 850000,
        "conditionGrade": "A",
        "status": "ON_SALE",
        "categoryName": "노트북",
        "sellerNickname": "판매왕",
        "thumbnailUrl": "/api/products/12/images/31",
        "wishCount": 3,
        "createdAt": "2026-09-22T14:03:00+09:00"
      }
    ],
    "page": {"number": 0, "size": 12, "totalElements": 137, "totalPages": 12,
             "first": true, "last": false, "numberOfElements": 12}
  },
  "message": "상품 목록 조회가 완료되었습니다",
  "timestamp": "2026-09-22T14:03:00+09:00"
}
```

| 필드 | 타입 | 필수 | 형식 | 빈 값 | 비고 |
|---|---|---|---|---|---|
| id | str | Y | 서버 발급 | — | 숫자처럼 보여도 문자열 |
| title | str | Y | ≤100자 | — | |
| priceKrw | int | Y | 원 단위 | — | |
| conditionGrade | str | Y | `S`·`A`·`B`·`C` | — | |
| status | str | Y | Product.status enum | — | 목록은 항상 `ON_SALE` |
| categoryName | str | Y | — | — | |
| sellerNickname | str | Y | — | — | |
| thumbnailUrl | str | N | 경로 | **null** | 이미지 0장이면 null |
| wishCount | int | Y | — | — | |
| createdAt | str | Y | ISO8601 **오프셋 포함** | — | |

에러: 400 `VALIDATION_ERROR`(size>100 등) · 500 `INTERNAL_SERVER_ERROR`

### 4.3 `GET /api/products/{id}`

성공 200 · `data`는 **객체**(배열 아님).
필드 = 4.2 + `description`(str) · `images`(**배열 · 0장이면 `[]` · 절대 null 아님**) ·
`sellerId`(str) · `wished`(bool · 비로그인이면 false) · `updatedAt`.
`images[]` 원소: `{"id":"31","url":"/api/products/12/images/31","sortOrder":0}`

에러: 404 `PRODUCT_NOT_FOUND` · **400 `VALIDATION_ERROR`(형식 불량 id — `@PathVariable Long`에 문자열이 오면 여기)**

### 4.4 `POST /api/products` (multipart)

- 인증: MEMBER
- `Content-Type: multipart/form-data` · 파트 `product`(JSON) + `images`(파일 0~5)
- 파일 제약: **jpg/png/webp · 각 5MB · 최대 5장** — 서버가 검증한다(프론트 검증은 왕복 절약용)
- 성공 **201** · `data`는 4.3과 같은 객체 · `status`는 항상 **`INSPECTING`**
- 에러: 400 `VALIDATION_ERROR`(+`fields[]`) · 400 `FILE_TYPE_NOT_ALLOWED` · 400 `FILE_TOO_LARGE` · 401

> **등록 직후 목록에 안 보이는 것이 정상이다**(검수 대기). 화면에 "관리자 검수 후 판매중으로
> 전환됩니다" 문구를 반드시 띄운다 — 없으면 시연에서 고장으로 보인다.

### 4.5 `POST /api/products/{id}/purchase`

- 인증: MEMBER (구매자)
- 요청 바디 없음
- 성공 **201** · `data` = Transaction 객체
```json
{"id":"7","productId":"12","productTitle":"맥북 에어 M2 13인치",
 "buyerNickname":"구매자","sellerNickname":"판매왕","amountKrw":850000,
 "status":"PAID","courier":null,"trackingNo":null,
 "createdAt":"2026-09-22T15:00:00+09:00","confirmedAt":null}
```
- 에러: 409 `PRODUCT_NOT_ON_SALE` · 400 `SELF_PURCHASE_NOT_ALLOWED` · 400 `INSUFFICIENT_BALANCE` · 401 · 404 `PRODUCT_NOT_FOUND`
- **원자성(BR-03):** Product 상태 · Transaction 생성 · buyer 잔액이 한 트랜잭션. 하나라도 실패하면 전부 롤백.

### 4.6 `PATCH /api/transactions/{id}/confirm`

- 인증: **구매자 본인만** (판매자·타인은 403 `TRANSACTION_FORBIDDEN`)
- 전제 상태: `SHIPPING`. 아니면 409 `INVALID_TRANSACTION_STATUS`
- 성공 200 · `data` = Transaction 객체(`status: "CONFIRMED"`, `confirmedAt` 채워짐)
- **원자성(BR-05):** Transaction·Product·seller 잔액 한 트랜잭션

### 4.7 `PATCH /api/transactions/{id}/shipping`

- 인증: **판매자 본인만** · 전제 상태 `PAID`
- 바디: `{"courier":"CJ대한통운","trackingNo":"123456789012"}` (둘 다 필수 · trackingNo 숫자 8~20자)
- 성공 200 · 에러 400 `VALIDATION_ERROR`(+`fields[]`) · 403 · 409

### 4.8 `POST /api/transactions/{id}/disputes` (multipart)

- 인증: **구매자 본인만** · 전제 상태 `PAID` 또는 `SHIPPING`
- 파트 `dispute`(JSON: `{"reason":"..."}` 10~500자) + `files`(0~3장 · jpg/png/pdf · 각 5MB)
- 성공 201 · `data` = `{"id":"3","transactionId":"7","status":"OPEN","reason":"...","files":[{"id":"9","originalName":"대화캡처.png"}],"createdAt":"..."}`
- 에러: 409 `DISPUTE_ALREADY_EXISTS` · 409 `INVALID_TRANSACTION_STATUS` · 403 · 400

### 4.9 나머지 엔드포인트 (얇은 계약)

| 엔드포인트 | 성공 | `data` | 주요 에러 |
|---|---|---|---|
| `GET /api/health` | 200 | `{"status":"UP"}` | — |
| `POST /api/auth/signup` | 201 | `{"id","email","nickname"}` | 409 `EMAIL_ALREADY_EXISTS` · 400 |
| `POST /api/auth/login` | 200 | `{"accessToken","user":{"id","nickname","role","balanceKrw"}}` | **401 `INVALID_CREDENTIALS`** |
| `GET /api/categories` | 200 | **배열** `[{"id","name"}]` (0건이면 `[]`) | — |
| `POST /api/products/{id}/wishes` | 201 | `{"productId","wished":true,"wishCount"}` | 409 `WISH_ALREADY_EXISTS` · 401 |
| `DELETE /api/products/{id}/wishes` | 200 | `{"productId","wished":false,"wishCount"}` | 404 `WISH_NOT_FOUND` · 401 |
| `GET /api/me` | 200 | `{"id","email","nickname","role","balanceKrw"}` | 401 |
| `GET /api/me/wishes` | 200 | `{content[],page{}}` (4.2 원소와 동일) | 401 |
| `GET /api/me/products` | 200 | `{content[],page{}}` · 모든 status 포함 | 401 |
| `GET /api/me/transactions?role=buyer\|seller` | 200 | `{content[],page{}}` (4.5 원소) | 401 · 400(role 값 오류) |
| `GET /api/transactions/{id}` | 200 | 4.5 원소 + `dispute`(객체 또는 **null**) | 403 · 404 |
| `GET /api/products/{id}/images/{imageId}` | 200 | 이미지 바이트(`Content-Type: image/*`) | 404 |
| `GET /api/disputes/{id}/files/{fileId}` | 200 | 파일 바이트 + **`Content-Disposition: attachment; filename="..."`** | 403 · 404 |

---

## §5 표준 에러 코드 표

바디는 §0 공통 에러 봉투. `code`는 `ErrorCode` enum 하나에서만 나온다.

| code | HTTP | 메시지(사용자에게 그대로 보인다) |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 입력값을 확인해 주세요 (+`fields[]`) |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 |
| `AUTHENTICATION_REQUIRED` | 401 | 로그인이 필요합니다 |
| `ACCESS_DENIED` | 403 | 접근 권한이 없습니다 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 오류가 발생했습니다 |
| `USER_NOT_FOUND` | 404 | 회원을 찾을 수 없습니다 |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일입니다 |
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없습니다 |
| `PRODUCT_NOT_ON_SALE` | 409 | 판매 중인 상품이 아닙니다 |
| `SELF_PURCHASE_NOT_ALLOWED` | 400 | 본인이 등록한 상품은 구매할 수 없습니다 |
| `INSUFFICIENT_BALANCE` | 400 | 잔액이 부족합니다 |
| `WISH_ALREADY_EXISTS` | 409 | 이미 찜한 상품입니다 |
| `WISH_NOT_FOUND` | 404 | 찜하지 않은 상품입니다 |
| `TRANSACTION_NOT_FOUND` | 404 | 거래를 찾을 수 없습니다 |
| `TRANSACTION_FORBIDDEN` | 403 | 해당 거래의 당사자가 아닙니다 |
| `INVALID_TRANSACTION_STATUS` | 409 | 현재 거래 상태에서는 처리할 수 없습니다 |
| `DISPUTE_ALREADY_EXISTS` | 409 | 이미 분쟁이 접수된 거래입니다 |
| `DISPUTE_NOT_FOUND` | 404 | 분쟁을 찾을 수 없습니다 |
| `FILE_TYPE_NOT_ALLOWED` | 400 | 허용되지 않는 파일 형식입니다 |
| `FILE_TOO_LARGE` | 400 | 파일 크기가 너무 큽니다 |
| `FILE_COUNT_EXCEEDED` | 400 | 첨부 가능한 파일 수를 초과했습니다 |

**Advice가 최소로 잡을 스프링 예외 일곱:**
400 — `MethodArgumentNotValidException` · `HttpMessageNotReadableException` ·
`MethodArgumentTypeMismatchException` · `MissingServletRequestParameterException` /
404 — `NoResourceFoundException` · 도메인 NotFound / 500 — `Exception`

---

## §6 DB 스키마 (오너는 백엔드 하나 · 프론트는 DB를 모른다)

| 테이블 | 주요 컬럼 | 제약 |
|---|---|---|
| `users` | id · email · password · nickname · role · balance_krw · created_at | `uk_users_email` |
| `categories` | id · name | `uk_categories_name` |
| `products` | id · seller_id(FK) · category_id(FK) · title · description · price_krw · condition_grade · status · reject_reason · created_at | `idx_products_status_created`(목록 정렬) · `idx_products_title`(검색) |
| `product_images` | id · product_id(FK) · stored_name · original_name · sort_order | |
| `wishes` | id · user_id(FK) · product_id(FK) · created_at | **`uk_wishes_user_product(user_id, product_id)`** |
| `transactions` | id · product_id(FK) · buyer_id(FK) · amount_krw · status · courier · tracking_no · created_at · confirmed_at | **`uk_transactions_product(product_id)`** |
| `disputes` | id · transaction_id(FK) · reporter_id(FK) · reason · status · admin_memo · created_at · resolved_at | **`uk_disputes_transaction(transaction_id)`** |
| `dispute_files` | id · dispute_id(FK) · stored_name · original_name · size_bytes | |
| `reviews` (P2) | id · transaction_id(FK) · writer_id · target_id · rating · content | `uk_reviews_transaction` |

### 6.1 DB 연결 전 확인 (MUST · 30초)

MariaDB 클라이언트로 서버에 붙어 아래 둘을 본다. **`VERSION()`에 `MariaDB`가 없으면
그 포트는 MariaDB가 아니다** — MySQL이 3306을 쓰고 있고 MariaDB는 다른 포트(3307 등)에 있다.

```sql
SELECT VERSION();   -- 10.x.x-MariaDB  → OK
                    -- 8.x.x           → MySQL이다. MariaDB 포트를 찾아 DB_PORT 로 넘긴다
SELECT @@port;      -- 이 값이 SPEC 확정값(3307)과 다르면 DB_PORT 로 넘긴다
```

**기본값이 3307인 이유:** MySQL이 이미 깔린 PC에서는 MariaDB 설치 관리자가 3306을 피해
3307을 잡는다. 팀 확인 결과 실제 포트가 3307이었다. **3306에 붙으면 MySQL에 연결되어
`sha256_password`(1차)나 `GSS-API ... 1045`(2차)로 끊긴다** — 둘 다 이 한 가지 원인이다.

포트를 못 찾으면 Windows에서:
`netstat -ano | findstr LISTENING | findstr :330` · 서비스 목록에서 `MariaDB` 항목 확인.

DB·계정 생성(**MariaDB에 접속한 상태에서**):

```sql
CREATE DATABASE udt DEFAULT CHARACTER SET utf8mb4;
CREATE USER 'udt'@'localhost' IDENTIFIED BY 'udt';
GRANT ALL PRIVILEGES ON udt.* TO 'udt'@'localhost';
FLUSH PRIVILEGES;
```

MariaDB의 비밀번호 계정은 기본이 `mysql_native_password`라 추가 설정이 필요 없다.
`ErrorCode 1045 / SQLState 28000`이 뜨면 이 계정이 없거나 다른 서버에 붙은 것이다.

- **시드 `data.sql`을 첫날 커밋한다.** 관리자 1 · 회원 3(잔액 200만원씩) · 카테고리 5 ·
  상품 20(ON_SALE 15 · INSPECTING 3 · SOLD 2) · 거래 3(PAID·SHIPPING·CONFIRMED 각 1) ·
  분쟁 1(OPEN). **id를 명시하지 않는다**(시퀀스 충돌 → 시연 중 첫 POST가 duplicate key).
- 비밀번호는 **BCrypt 해시**로 박는다(평문 시드는 로그인이 안 된다). 시연 계정 비밀번호는
  `README.md`에 적는다.
- 로컬 DB는 각자 소유 → `ddl-auto: create` + 시드 재실행이 곧 리셋. **공유 개발 DB 없음.**

---

## §7 Security 체인 2개 (하이브리드 · 이게 없으면 첫날 전부 401)

```
@Order(1)  /api/**   → stateless · JWT 필터 · CSRF off · CORS 적용
                       401은 AuthenticationEntryPoint에서 JSON 공통 봉투로
                       (기본값은 302 리다이렉트라 axios 인터셉터가 발동하지 않는다)
@Order(2)  /admin/** → 세션 · formLogin("/admin/login") · CSRF on · 403은 HTML
           그 외      → permitAll (정적 · /api/health · /api/products GET)
```

- `/api/**` 체인이 **먼저** 매칭되어야 한다. 순서가 바뀌면 REST 요청이 로그인 HTML로
  리다이렉트되어 프론트 콘솔에 `Unexpected token '<'`가 뜬다.
- `BCryptPasswordEncoder` 빈 1개 · 관리자 컨트롤러 메서드 하나에
  `@PreAuthorize("hasRole('ADMIN')")` + `@EnableMethodSecurity` (메서드 레벨 보안 증거)
- **CORS는 전역 설정 클래스 하나**에 `http://localhost:5173` 등록 ·
  `CORS_ALLOWED_ORIGINS` 환경변수에서 읽는다 · `@CrossOrigin` 남발 금지 ·
  **Security 체인에도 같은 허용을 넣는다.** `127.0.0.1:5173`은 다른 origin이니
  "주소창은 localhost로만" 규칙으로 간다.

---

## §8 최소 계층 규약 (기계 검사 가능한 3+3줄)

**백엔드**
```
B1. Controller는 검증 + Service 호출 + 응답 반환만. DTO 변환은 Service 안에서(@Transactional).
    검사: grep -rl "import .*\.entity\." src/main/java --include='*Controller.java'   (무출력)
B2. Controller에서 Repository 직접 호출 금지.
    검사: grep -rl "Repository" src/main/java --include='*Controller.java'            (무출력)
B3. Service에 웹 타입(HttpServletRequest·ResponseEntity·Model) 금지.
    검사: grep -rlE "ResponseEntity|HttpServletRequest|org.springframework.ui.Model" \
          src/main/java --include='*Service.java'                                     (무출력)
```
> B3이 하이브리드의 핵심이다 — Service가 웹을 모르므로 **REST 컨트롤러와 Thymeleaf 컨트롤러가
> 같은 Service를 부른다.** 발표 Q&A "MVC인데 View는 어디 있나"의 답이 이것이다:
> "서버는 계층형(Controller–Service–Repository). 사용자 View는 React가, 관리자 View는
> Thymeleaf가 맡고 **Service는 하나**다."

**프론트**
```
F1. 서버 통신은 src/api/에서만.
    검사: grep -rlE "axios|\bfetch\(" src/pages src/components   (프론트 리포에서 · 무출력)
F2. 화면 간 공유 상태는 URL 쿼리가 1순위(검색어·카테고리·페이지).
    Zustand는 인증 + 찜 두 개만(§0 표).
F3. 컴포넌트가 200줄을 넘으면 훅·유틸로 추출한다. 계층을 미리 만들지 않는다.
```

---

## §9 ADR (결정 기록 · Q&A의 답이 여기서 나온다)

| # | 결정 | 이유 |
|---|---|---|
| ADR-01 | 세션 대신 **JWT**(사용자 쪽) | 프론트가 5173, 백엔드가 8080으로 origin이 갈려 쿠키 `SameSite` 처리가 2주에 무겁다 |
| ADR-02 | 관리자는 **세션 + Thymeleaf 폼 인증** | 관리자 화면은 폼 중심이고 SPA가 필요 없다. 세션 폼 인증을 쓰면 CSRF 토큰과 PRG 패턴이 프레임워크 기본으로 따라온다 |
| ADR-03 | 결제 상태에 `결제대기`를 두지 않는다 | 가상 결제라 구매요청 = 즉시 `PAID`. 상태 하나를 줄여 상태 머신을 단순하게 |
| ADR-04 | **상태 변경은 `TransactionService`에서만** | 분쟁·관리자 기능이 각자 상태를 바꾸면 불일치가 난다 |
| ADR-05 | 정산을 별도 엔티티가 아니라 **`User.balanceKrw` 한 필드**로 | 에스크로의 본질(구매확정 전엔 판매자에게 안 들어감)을 상태 전이만으로 표현할 수 있다 |
| ADR-06 | 상품 목록은 **`ON_SALE`만** 노출 | 검수 프로세스가 서비스의 핵심이라 미검수 상품이 목록에 있으면 안 된다 |
| ADR-07 | **배포는 범위 외** | 2주 기간에서 배포 파이프라인보다 기능 완성도와 문서화를 우선했다. 재현성은 D8 시연 PC 통합 + README 실행 3줄로 확보한다 |
| ADR-08 | 에러 봉투에 템플릿에 없는 `success:false`·`code`를 얹는다 | 프론트 인터셉터가 `success` 하나로 분기하고, §5 에러 코드 표를 화면이 쓸 수 있다 |
| ADR-09 | 페이지 인덱스 **0-base 유지** | 봉투의 `page.number`가 0-base. 한쪽만 1-base로 바꾸면 첫 화면이 빈 결과가 된다. 변환은 프론트 표시 한 곳에서만 |
| ADR-10 | UI 라이브러리 없이 **CSS Modules + 디자인 토큰** | 화면이 7개로 적어 토큰 한 파일이면 일관성이 확보된다. 라이브러리 학습·커스터마이징 비용이 2주 안에 회수되지 않는다 |
| ADR-11 | DB는 **MariaDB**로 통일하고 드라이버도 `mariadb-java-client` | 엔진과 드라이버가 어긋나면 인증 단계에서 끊긴다 — MariaDB 드라이버는 MySQL 8의 `sha256_password`를 지원하지 않는다(`SQLState 08004`). **한 PC에 MySQL과 MariaDB가 같이 깔려 있으면 3306을 MySQL이 차지하는 일이 흔하므로, 첫 연결 전에 §6.1 확인 절차를 돌린다** |

---

## §10 변경 규약

계약 변경은 **seam 오너·소비자 합의 → 이 문서 개정 커밋 → 전원 알림** 순서다.
**코드 먼저 금지.** 변경 *요청*도 말로 하지 않는다 —
`[요청자] [엔드포인트·필드] [화면 근거] [원하는 기한]` 한 줄로 이슈에 남긴다.

계약 한 줄이 바뀌면 **네 곳이 함께 바뀐다:**
① `SPEC.md` ② `seams/check-api.mjs` ③ 목 응답 ④ 전원 알림.
개정 커밋 메시지에 네 항목 체크를 붙인다.

```
docs(spec): GET /api/products 에 wishCount 추가
- [x] SPEC.md §4.2
- [x] seams/check-api.mjs
- [x] 목 응답
- [x] 팀 채널 알림
```

**정본은 한 곳이다.** 필드명·규칙을 README·설계서에 복사하지 말고 `SPEC.md §N` 참조로 가리킨다.

---

## §11 소유권 판정 규칙 (통합에서 깨졌을 때)

- `check-api` **red** → **백엔드 소유.** 화면을 보지 않는다.
- `check-api` **green인데 화면이 깨짐** → **프론트 소유.** 서버 로그를 보지 않는다.
- **판정 사이에 30초:** DevTools Network → "Copy as cURL"로 재현해 출력을 §4 예시와 대조.
  curl ≠ 계약이면 백엔드, = 계약이면 프론트.
- **양쪽 다 계약대로인데 깨짐** → 버그가 아니라 **계약 결함**이다(정렬 순서·입력 전제·빈 값·시간대).
  seam 오너가 계약을 개정하고 검사를 보강한다.

소유가 정해지면 한 줄 더 간다 — **"왜 게이트가 이걸 못 잡았나."**
답은 검사 보강 또는 계약 보강. `OBSERVATIONS.md`에 **날짜·현상·비어 있던 장치**를 적는다.
**사람 이름은 적지 않는다.**

---

## §12 리포 구성 (리포 2개 운영 규약)

이 프로젝트는 리포가 **둘**이다. 모노리포가 아니므로 아래 다섯 줄이 계약의 일부다.

```
UDT-Used_Device_Trade-backend   ← 허브. 이 SPEC.md · seams/ · mock/ · docs/ · worklog/ 의 정본이 여기 산다
UDT-Used_Device_Trade-frontend  ← 프론트 코드. SPEC 값을 복사하지 않고 링크로 가리킨다
```

| 무엇 | 어디가 정본 | 반대쪽은 |
|---|---|---|
| `SPEC.md` (이 문서) | backend | README에서 **링크만** |
| `seams/check-api.mjs` | backend | `npm run gate` 런처가 backend의 파일을 실행 |
| `seams/smoke.md` | **frontend** (소비자가 오너다) | — |
| `mock/server.mjs` | backend | `npm run mock`이 backend의 파일을 실행 |
| `docs/` 제출 문서 13종 | backend | 프론트 오너도 backend 리포에 커밋한다 |
| `worklog/` | backend | 전원이 backend 리포에 쓴다 |
| `tasks/T-###.md` | 각자 자기 리포 (`be-*`는 backend · `fe-*`는 frontend) | — |

### 클론 위치 (MUST — 같은 상위 폴더에)

```
<작업폴더>/
├── UDT-Used_Device_Trade-backend/
└── UDT-Used_Device_Trade-frontend/
```

프론트의 `npm run gate`·`npm run mock`은 **상위 폴더에서 `seams/check-api.mjs`를 가진 형제
폴더를 자동으로 찾는다** — 폴더 이름은 바꿔도 된다. 두 리포를 같은 상위 폴더에 두는 것만
지키면 된다. 다른 배치를 쓰려면 경로를 알려준다:
`BACKEND_REPO=/path/to/backend npm run gate`.

### 계약 개정은 두 리포에 걸친다 — 짝을 번호로 맞춘다

모노리포라면 커밋 하나로 끝나는 일이 여기선 둘로 갈린다. **한쪽만 머지된 상태**가
리포 2개 운영의 유일한 고유 위험이고, 아래가 그 방어다.

```
1. backend 리포에서 SPEC.md + check-api.mjs + mock/server.mjs 를 한 커밋으로
   → 커밋 메시지: docs(spec): <변경> [T-042]
2. frontend 리포의 소비 코드도 같은 티켓 번호를 단다
   → 커밋 메시지: feat(product): <변경> [T-042]
3. 팀 채널에 한 줄 — "T-042 계약 개정: <엔드포인트> · 양쪽 머지 완료"
4. 3번이 올라오기 전까지 그 티켓은 열려 있는 것으로 본다
```

**backend를 먼저 머지한다.** 반대로 하면 프론트가 계약에 없는 응답을 기대하는 상태로
main에 들어간다.

### 제출

**`UDT-Used_Device_Trade-backend`를 메인 리포로 제출한다**(문서 13종이 여기 있다).
양쪽 README 최상단에 상대 리포 링크와 "이 프로젝트는 리포 2개로 구성" 한 줄을 넣는다.
