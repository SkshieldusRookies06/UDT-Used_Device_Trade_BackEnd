# T-005 — Security 체인 완성 · 로그인 API

**담당:** BE-B · **브랜치:** `be-auth` · **예상:** D3 · **선행:** T-004

## [배경]

`config/SecurityConfig`에 **체인 3개 골격이 이미 있다**(`// TODO(T-005)` 표시 — 완료 후 9/29 제거).
JWT 필터와 `UserDetailsService`만 연결하면 된다.

```
@Order(1)  /api/**   stateless · CSRF off · CORS 적용 · 401은 JSON 봉투
@Order(2)  /admin/** 세션 · formLogin · CSRF on
@Order(3)  그 외      permitAll
```

**체인 순서가 중요하다.** `/api/**`가 뒤로 밀리면 REST 요청이 로그인 HTML로 리다이렉트되어
프론트 콘솔에 `Unexpected token '<'`가 뜬다.

계약 (`SPEC.md` §4.1):

```
POST /api/auth/signup   201  data = {id, email, nickname}       · 409 EMAIL_ALREADY_EXISTS
POST /api/auth/login    200  data = {accessToken, user:{id, nickname, role, balanceKrw}}
                             · 401 INVALID_CREDENTIALS
GET  /api/me            200  data = {id, email, nickname, role, balanceKrw} · 401
```

## [9/27 개정] 바뀐 것과 이유 — 먼저 할 두 가지

**A. `config/SecurityConfig.java` — `apiFilterChain`에 JWT 필터 등록** (이게 없으면 토큰을 보내도 항상 401)
- 메서드 인자에 `JwtTokenProvider jwtTokenProvider` 추가(`@Component`라 주입된다).
- `.exceptionHandling(...)` 다음에 `.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)`.
- import: `org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter` · `com.rookies6.udt.security.JwtAuthenticationFilter` · `JwtTokenProvider`.

**B. `security/JwtTokenProvider.java` — `createToken(Long userId, String role, String nickname)` 오버로드 추가**, `.claim("nickname", nickname)` 포함 (SPEC §1 토큰 규격: claims `role·nickname`). 기존 2-인자 `createToken(userId, role)`은 **지우지 말고** 3-인자에 `null`로 위임(인수 테스트가 쓴다). `AuthService.login()`은 세 인자로 부른다.

**완료 판정:** A → `acceptance/auth/JwtFilterRegistrationTest`(지금 첫 번째가 401로 red — 필터가 등록되면 401이 아니게 된다) · B → `JwtNicknameClaimTest` · 본 작업(signup·login·me) → `AuthApiTest` 5개 + 게이트 13/13.

**이미 들어간 것 (팀장 · 손대지 않는다)**
- `security/CurrentUser.java`(공용) — `id()`는 401을 던지고 `idOrNull()`은 Optional. 컨트롤러 6곳이 이걸로 로그인 사용자를 꺼낸다(T-009·T-007·T-008 개정). 필터가 principal에 **userId 문자열**을 넣으므로 A가 끝나면 전부 동작한다.
- 로그인 응답 `user.balanceKrw`는 **DB 현재값**이어야 한다 — 게이트 거래 검사가 구매 전후 `/api/me` 잔액 차이로 잔액 유실(T-009 개정 1번) 회귀를 본다.

## [목표]

로그인하면 토큰이 나오고, 그 토큰으로 보호 엔드포인트를 통과한다.

## [수용 기준]

- [ ] `UserDetailsService` 구현 — 이메일로 회원 조회 · 비밀번호는 BCrypt 비교
- [ ] `SecurityConfig`의 `/api/**` 체인에 **JWT 필터 등록**
- [ ] `AuthController` · `AuthService` — signup · login · me
- [ ] 기동 로그에서 **`Using generated security password:` 줄이 사라진다**
- [ ] 관리자 컨트롤러용 `@PreAuthorize("hasRole('ADMIN')")` 가 동작한다 (T-018에서 사용)
- [ ] **`node seams/check-api.mjs` 가 13개 전부 ok** ← 이 티켓의 완료 신호

```bash
# 시드 계정으로 왕복 확인
curl -s -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"buyer1@udt.test","password":"Test1234!"}' | jq -r .data.accessToken
# → 위 토큰으로
curl -s localhost:8080/api/me -H "Authorization: Bearer <토큰>" | jq
```

## [제약]

- 수정 허용: `security/` · `config/SecurityConfig.java` · `controller/AuthController.java` ·
  `service/AuthService.java` · `dto/Auth*`
- **`/api/**` permitAll 목록을 넓히지 않는다** — `SPEC.md` §4.1 seam 표의 "인증" 칼럼(=`03-REST-API설계서` §3.2 권한 레벨 표)이 정본
- **비밀번호를 응답에 절대 담지 않는다** (DTO에 password 필드 금지)
- 로그인 실패는 401 `INVALID_CREDENTIALS` — 404나 400으로 내지 않는다
  (이메일 존재 여부가 드러나면 계정 열거가 가능해진다)

## [확인 명령]

```bash
node seams/check-api.mjs      # 13/13 ok 여야 한다
```
