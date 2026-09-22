# T-004 — JWT 발급·검증

**담당:** BE-B · **브랜치:** `be-jwt` · **예상:** D1~D2 · **선행:** T-001

## [배경]

`SPEC.md` §0 인증 전달 · §7 Security 체인이 정본이다.

```
사용자 API   Authorization: Bearer <jwt>   (프론트 5173 / 백엔드 8080 으로 출처가 갈려
                                            쿠키 대신 토큰을 쓴다 — ADR-01)
관리자 화면  세션 + 폼 로그인 (T-005·T-018)
```

**토큰 규격 — D1 회의에서 확정할 것** (`SPEC.md` §3.1 OPEN 항목):

```
알고리즘   HS256
sub        회원 ID
claims     role · nickname
만료       24시간 (application.yml: app.jwt.expiration-seconds=86400)
secret     32바이트 이상 (app.jwt.secret · 환경변수로 주입 가능)
```

의존성은 이미 `pom.xml`에 있다 — `jjwt-api` · `jjwt-impl` · `jjwt-jackson` 0.12.6.

## [목표]

토큰을 발급·검증하는 컴포넌트와, 요청에서 토큰을 읽어 인증 객체를 세우는 필터를 만든다.

## [수용 기준]

- [ ] `security/JwtTokenProvider` — `createToken(userId, role, nickname)` · `validate(token)` ·
      `getUserId(token)` 제공
- [ ] `security/JwtAuthenticationFilter` — `OncePerRequestFilter` 상속 ·
      `Authorization: Bearer` 헤더에서 토큰을 읽어 `SecurityContext`에 인증 객체 설정
- [ ] **토큰이 없거나 잘못돼도 필터가 예외를 던지지 않는다** — 인증 없이 통과시키고,
      보호 경로면 `AuthenticationEntryPoint`가 401 JSON을 낸다
- [ ] 단위 확인: 토큰 발급 → 검증 → userId 추출이 왕복한다
- [ ] 만료된 토큰 · 서명이 다른 토큰 · 형식이 깨진 토큰 3종이 모두 `validate` 에서 false

## [제약]

- 수정 허용: `security/` · `application.yml`의 **`app.jwt.*` 블록만**
- **`application.yml`은 BE-A 소유 공용 파일이다** — 자기 키 블록만 추가하고 남의 키는
  읽지도 고치지도 않는다. 추가 전 팀 채널에 한 줄 공지 (`역할분담.md` §2 공용 파일 명단)
- **`SecurityConfig`는 T-005에서 건드린다** — 이 티켓은 필터를 만들기만 한다
- secret을 코드에 하드코딩하지 않는다 (`application.yml`의 `${JWT_SECRET:...}`)

## [확인 명령]

```bash
IntelliJ에서 src/test 우클릭 → Run 'All Tests'
IntelliJ ▶ UdtApplication 실행 (Active profiles: local)
node seams/check-api.mjs        # 이 시점엔 7/9 (login 관련 2개만 RED)가 정상
```
