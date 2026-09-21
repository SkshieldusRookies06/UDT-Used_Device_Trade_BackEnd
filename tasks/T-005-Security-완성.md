# T-005 — Security 체인 완성 · 로그인 API

**담당:** BE-B · **브랜치:** `be-auth` · **예상:** D3 · **선행:** T-002 · T-003 · T-004 · T-004

## [배경]

`config/SecurityConfig`에 **체인 3개 골격이 이미 있다**(`// TODO(T-005)` 표시).
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

## [목표]

로그인하면 토큰이 나오고, 그 토큰으로 보호 엔드포인트를 통과한다.

## [수용 기준]

- [ ] `UserDetailsService` 구현 — 이메일로 회원 조회 · 비밀번호는 BCrypt 비교
- [ ] `SecurityConfig`의 `/api/**` 체인에 **JWT 필터 등록**
- [ ] `AuthController` · `AuthService` — signup · login · me
- [ ] 기동 로그에서 **`Using generated security password:` 줄이 사라진다**
- [ ] 관리자 컨트롤러용 `@PreAuthorize("hasRole('ADMIN')")` 가 동작한다 (T-018에서 사용)
- [ ] **`node seams/check-api.mjs` 가 9개 전부 ok** ← 이 티켓의 완료 신호

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
- **`/api/**` permitAll 목록을 넓히지 않는다** — `SPEC.md` §3.2 권한 레벨 표가 정본
- **비밀번호를 응답에 절대 담지 않는다** (DTO에 password 필드 금지)
- 로그인 실패는 401 `INVALID_CREDENTIALS` — 404나 400으로 내지 않는다
  (이메일 존재 여부가 드러나면 계정 열거가 가능해진다)

## [확인 명령]

```bash
node seams/check-api.mjs      # 9/9 ok 여야 한다
```
