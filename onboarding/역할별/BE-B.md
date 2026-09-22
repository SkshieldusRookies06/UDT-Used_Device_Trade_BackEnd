# BE-B — 인증·관리자

> 이름: ____________   ·   GitHub: ____________
>
> **이 한 장이 내 2주 전부다.** 다른 문서는 여기서 필요할 때만 연다.
> 팀 공통 규칙은 [`../팀원용-한장.md`](../팀원용-한장.md),
> 날짜별 전체 순서는 [`전체로드맵.md`](../../docs/참고/전체로드맵.md).

---

## 1. 한 줄로

**로그인이 되면 게이트가 9/9가 된다.** 그게 D3의 첫 이정표고, 그때까지 팀 전체가 목 서버 위에서 산다.
그리고 **관리자 화면(Thymeleaf)은 나 혼자만 만든다** — 서버 렌더링 화면이 이 프로젝트에 있다는 걸 보여 주는 게 내 파트다.

---

## 2. 내 파일

### 내가 소유한다 (내가 결정한다)

| 경로 | 내용 |
|---|---|
| `security/` | `JwtTokenProvider` · JWT 필터 · `UserDetailsService` |
| `config/SecurityConfig.java` | **체인 3개** — `/api/**` 무상태 JWT · `/admin/**` 세션 formLogin · 그 외 |
| `controller/AuthController.java` · `service/AuthService.java` | 로그인·회원가입 |
| `dto/Auth*` | 인증 DTO |
| `admin/` · `resources/templates/` | **관리자 화면 2장 + 폼 처리 (나 단독)** |
| `application.yml`의 `app.jwt.*` | 내 키 블록만 |

### 절대 안 건드린다

- `entity/` — 필드가 필요하면 **BE-A에게 요청**한다
- 상품 · 거래 `Service` 내부 (BE-C · BE-D)
- `common/` — 에러 코드가 더 필요하면 BE-A에게 요청
- `seams/` — **게이트를 통과시키려고 게이트를 고치지 않는다.** 검사가 틀렸다고 판단되면 멈추고 보고

### 공용 — 만지기 전에 채널에 한 줄 올린다

| 파일 | 규칙 |
|---|---|
| `application.yml` | **BE-A 소유.** 내 `app.jwt.*` 블록만 추가하고 남의 키는 읽지도 고치지도 않는다. 추가 전 채널에 한 줄 |
| `SecurityConfig` | 내 소유지만 팀 전체에 영향이 크다. **`/api/**` permitAll 목록을 넓힐 때는 반드시 공지** |

> 남의 파일이 틀려 보여도 고치지 않는다. **오너에게 보고**한다.
> 고치면 그 버그의 책임이 나에게 넘어온다.

---

## 3. 내 계약 상대

| 나 | 상대 | 무엇에 대해 |
|---|---|---|
| **BE-B** | **FE-A** | 로그인·회원가입 · **토큰 만료 시 화면이 어떻게 되는가** |

이 짝이 합의하면 인증 계약이 확정된다. **합의는 `SPEC.md`에 커밋되기 전까지 무효다.**

> D1 회의에서 FE-A와 이것부터 정한다 — **토큰 만료 시간 · 재발급 여부 · 401을 받은 화면의 동작**.
> 여기가 안 정해지면 FE-A의 인터셉터를 쓸 수 없다.

---

## 4. 내 티켓 (순서대로)

| 티켓 | 언제 | 선행 | 끝났다는 증거 |
|---|---|---|---|
| [T-004 JWT 발급·검증](../../tasks/T-004-JWT-발급검증.md) | D1~D2 | T-001 | 테스트 전체 통과(IntelliJ) · 게이트 7/9 (login 관련 2개만 red)가 정상 |
| [T-005 Security 완성·로그인 API](../../tasks/T-005-Security-완성.md) | **D3** | T-004 | **게이트 9/9 ok** ★ |
| [T-018 관리자 Thymeleaf](../../tasks/T-018-관리자-Thymeleaf.md) | D6~D7 | T-005 | 브라우저에서 검수 승인이 실제로 된다 |

**T-005가 D3의 팀 이정표다.** 이게 green이 되는 순간 프론트가 목을 끄고 실서버로 옮긴다.

> **`[수용 기준]`이 비어 있는 티켓은 시작하지 않는다** — 완료 판정을 말로 하게 된다.

---

## 5. 내 티켓 — 하나씩 어떻게 하나

> 아래는 **티켓을 실제로 어떻게 하는가**다. 순서대로 하면 된다.
> 각 단계 끝의 `확인:` 을 통과해야 다음으로 간다.

### T-004 — JWT 발급·검증 (D1~D2)

`security/` 폴더가 **비어 있다.** 여기부터 만든다. `pom.xml`에 `jjwt 0.12.6`은 이미 있다.

**1) 먼저 `config/SecurityConfig.java`를 읽는다** (고치지는 않는다 — T-005에서 한다)
체인 3개 골격이 `// TODO(T-005)` 표시와 함께 이미 들어 있다. **무엇을 채워야 하는지 먼저 본다.**

**2) `JwtTokenProvider` 를 만든다**

```
security/JwtTokenProvider.java
  - secret:  application.yml 의 ${JWT_SECRET:...} 을 @Value 로 받는다 (하드코딩 금지)
  - generateToken(User)  →  subject=userId, claim에 role, 만료 = app.jwt.expiration-seconds
  - validateToken(String) →  boolean (예외를 밖으로 던지지 않는다)
  - getUserId(String)     →  Long
```
> **secret을 코드에 박지 않는다.** 박으면 커밋에 남고, 그건 실제 사고다.
> `app.jwt.*` 블록만 `application.yml`에 추가한다 — 다른 키는 건드리지 않는다.

**3) `JwtAuthenticationFilter` 를 만든다** (`OncePerRequestFilter` 상속)

```
1) Authorization 헤더에서 "Bearer " 뒤를 꺼낸다 — 없으면 그냥 다음 필터로
2) validateToken 실패해도 예외를 던지지 않는다 — 인증 없이 다음 필터로 보낸다
   (거부는 SecurityConfig의 entryPoint가 한다. 여기서 던지면 봉투를 못 씌운다)
3) 성공하면 UsernamePasswordAuthenticationToken 을 SecurityContext 에 넣는다
```

**4) 토큰 만료 동작을 FE-A와 확정한다 (D1 회의)**
- 만료 시간 몇 분인가
- 재발급(refresh)을 하는가 — **안 하는 쪽을 권한다.** 2주 프로젝트에 리프레시 토큰은 시간을 먹는다
- 401을 받은 화면이 무엇을 하는가 → 현재 프론트 인터셉터는 **로그아웃 후 `/login`으로 보낸다**

합의한 내용을 `SPEC.md` §1 OPEN에 적고 BE-A에게 개정을 요청한다. **적기 전까지 그 합의는 무효다.**

`확인:` 테스트 전체 통과(IntelliJ) · 게이트는 여전히 7/9 (login 관련 2개만 red · 아직 정상).

---

### T-005 — Security 완성·로그인 API (D3 · ★ 팀 이정표)

**1) `UserDetailsService` 를 만든다**

```
security/CustomUserDetailsService.java
  loadUserByUsername(email) → UserRepository.findByEmail → UserDetails
  없으면 UsernameNotFoundException
```

**2) `SecurityConfig`의 `// TODO(T-005)` 표시를 따라 체인 3개를 채운다**

| 체인 | `@Order` | 대상 | 방식 |
|---|---|---|---|
| API | 1 | `securityMatcher("/api/**")` | **무상태 JWT** · csrf disable · `SessionCreationPolicy.STATELESS` · 필터 등록 |
| 관리자 | 2 | `securityMatcher("/admin/**")` | **세션 formLogin** · csrf 유지 · `hasRole("ADMIN")` |
| 그 외 | 3 | 나머지 | 정적 리소스 |

> **`@Order` 순서가 뒤집히면 `/admin`이 JSON 401로 튕긴다.** API 체인이 먼저다.
> permitAll 목록: health · `/api/auth/**` · `GET /api/products/**` · `GET /api/categories` · 이미지.
> **이 목록을 넓히지 않는다** — `SPEC.md` **§4.1 seam 표의 "인증" 칼럼**이 정본이다.

**3) `AuthService` · `AuthController` 를 만든다**

```
POST /api/auth/signup   이메일 중복 검사 → BCrypt 해시 → User 저장 → 201
POST /api/auth/login    이메일 조회 → passwordEncoder.matches → 실패 시 401
GET  /api/me            SecurityContext 의 userId → UserResponse
```

**절대 하지 말 것 둘**
- **응답 DTO에 `password` 필드를 만들지 않는다.** 하나라도 새어 나가면 끝이다
- **로그인 실패를 404로 내지 않는다.** 404면 "이 이메일은 존재하지 않는다"가 드러나서
  계정을 하나씩 찍어 볼 수 있게 된다. **401 `INVALID_CREDENTIALS` 하나로 통일**한다

**4) 직접 확인한다**

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)
curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN" | jq
```

**5) 게이트를 돌린다**

```bash
node seams/check-api.mjs
```

`확인:` **9/9 ok.** 이 순간 채널에 올린다 — "게이트 9/9입니다. 프론트는 목을 꺼도 됩니다."
**이게 D3의 팀 이정표다.**

---

### T-018 — 관리자 Thymeleaf (D6~D7)

`templates/` 에 **5장이 이미 있다** — `layout/base.html` · `fragments/nav.html` ·
`admin/login.html` · `admin/products.html` · `admin/disputes.html`.
**화면을 새로 그리는 게 아니라 컨트롤러를 붙이는 티켓**이다.

**1) `admin/AdminProductController` 를 만든다**

```
GET  /admin/products              검수대기 목록 → Model → admin/products
POST /admin/products/{id}/approve 승인 → BR-01 → redirect:/admin/products
POST /admin/products/{id}/reject  반려(+사유) → BR-02 → redirect
```

**2) `admin/AdminDisputeController` 를 만든다**

```
GET  /admin/disputes                    분쟁 목록
GET  /admin/disputes/{id}/files/{fid}   증빙 다운로드 ← 오너는 BE-D(T-010). 내 템플릿은 링크만 건다
POST /admin/disputes/{id}/refund        BR-07 강제 환불
POST /admin/disputes/{id}/confirm       BR-08 강제 확정
```

**3) 상태를 직접 바꾸지 않는다 — 이게 이 티켓의 핵심이다**
**BR-01·02(검수 승인/반려)도, BR-07·08(강제 환불/확정)도 전부 `TransactionService`(BE-D)를 호출**한다.

```java
transactionService.approveInspection(productId);            // BR-01
transactionService.rejectInspection(productId, reason);     // BR-02
transactionService.forceRefund(transactionId);              // BR-07
transactionService.forceConfirm(transactionId);             // BR-08
```
BR-07·BR-08은 잔액과 상품 상태가 같이 움직인다.
여기서 직접 바꾸면 상태 변경 코드가 두 군데가 되고, D8에 "왜 상품이 판매중인데 거래가 있지"가 나온다.
필요한 메서드가 없으면 **BE-D에게 요청**한다.

**4) 폼은 POST + redirect로 끝낸다** (PRG)
새로고침하면 승인이 두 번 되는 사고를 막는다.

**5) 브라우저로 확인한다**

```
localhost:8080/admin  →  admin@udt.test / Admin1234!
검수대기 상품 하나를 승인 → 목록에서 사라짐 → /api/products 에 나타남
```

`확인:` 승인한 상품이 **React 쪽 목록(`localhost:5173`)에 실제로 뜬다.** 시연 ② 구간이 이것이다.

---

## 6. 내 10일

```
D1~D2  T-004 JWT 발급·검증
D3   ★ T-005 Security 완성 · 로그인 API → 게이트 9/9 (팀 첫 이정표)
D4   ★ 1차 통합 대응 — **인증이 가장 먼저 터진다. 이날은 다른 걸 잡지 않는다**
D5     T-018 준비 (화면 2장 초안 · 폼 흐름 정리)
D6~D7  T-018 관리자 화면 2장 + 폼 처리
D8   ★ 시연 ②⑥ 구간 담당 (검수 승인 · 강제 환불)
D9     발표 인증·권한 슬라이드 · 회고록
D10    리허설
```

---

## 7. 내가 쓰는 문서

| 문서 | 언제 | 내 몫 |
|---|---|---|
| `docs/03-REST-API설계서.md` §3 (인증·권한) | D4 | 이 절만 내가 채운다 (문서 오너는 BE-C) |
| `docs/참고/발표대본.md` | D9 | 내 파트 |
| `docs/참고/예상질문.md` | D9 | 내 질문 3개 |
| `docs/회고록/retro-<이름>.md` | D9 오전 | 전원 공통 |

> 문서는 **마지막 날에 몰아 쓰지 않는다.** 원본이 확정되는 날 그 자리에서 떨어뜨린다.
> 문서 머리에 `정본: SPEC.md §N · 커밋 <해시>`를 적는다.

---

## 8. 내 명령어 (복붙)

```bash
# 기동
IntelliJ ▶ UdtApplication 실행 (Active profiles: local)

# 내 완료 증명 — T-005 끝나면 9/9 여야 한다
node seams/check-api.mjs

# 로그인 직접 확인
curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq

# 토큰으로 보호 엔드포인트
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken)
curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN" | jq

# 관리자 화면 (T-018)
#   브라우저에서 localhost:8080/admin — 세션 로그인 · admin@udt.test / Admin1234!
```

---

## 9. 내가 막힐 곳 (내 역할 고유)

| 증상 | 원인 | 볼 곳 |
|---|---|---|
| 모든 요청이 401 | `SecurityConfig`가 없거나 `@Order`가 뒤집혔다 | `SPEC.md` §7 — `/api/**` 체인이 먼저다 |
| `Using generated security password` 로그 | 스타터는 있는데 설정이 없다 | T-005를 하면 사라진다 |
| 401인데 응답이 HTML | 인증 실패가 봉투를 안 탔다 | `jsonAuthenticationEntryPoint` — 봉투를 직접 써야 한다 |
| `/admin` 이 JSON 401로 튕긴다 | `/api/**` 체인이 `/admin`까지 먹었다 | `securityMatcher("/api/**")` 확인 |
| 로그인 실패가 404로 나간다 | 이메일 존재 여부가 드러난다 | **401 `INVALID_CREDENTIALS`로 통일** — 계정 열거 방지 |
| 토큰이 갑자기 다 무효 | secret이 매 기동마다 바뀐다 | `${JWT_SECRET:...}` 고정값 확인 |
| 관리자로 로그인했는데 `/admin`이 403 | `hasRole("ADMIN")`은 권한 문자열이 **`ROLE_ADMIN`**이어야 통과한다 | `UserDetailsService`에서 `ROLE_` 접두어를 붙인다 (`SimpleGrantedAuthority("ROLE_" + role)`) |
| `/admin/login`이 404 | `loginPage("/admin/login")`은 **그 페이지를 서버가 그려 줘야** 한다 | T-018에서 `@GetMapping("/admin/login")` → `admin/login` 템플릿. **그 전까지 404가 정상** |

---

## 10. 발표에서 내가 말하는 것

**인증·권한 슬라이드 (체인 3개)** — `03-REST-API설계서` §3

핵심 한 문장: **"같은 서버인데 `/api/**`는 무상태 JWT, `/admin/**`은 세션입니다. 필터 체인을 둘로 나눴고, 순서는 `@Order`로 고정했습니다."**

> 여기가 잘 나오는 질문 구간이다. "왜 둘로 나눴나"에 답할 수 있어야 한다 —
> 브라우저가 직접 폼을 쓰는 관리자 화면과, 토큰을 들고 오는 React는 인증 방식이 다르기 때문.

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
