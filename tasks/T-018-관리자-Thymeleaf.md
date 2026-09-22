# T-018 — 관리자 화면 2장 · 폼 처리

**담당:** BE-B · **브랜치:** `be-admin` · **예상:** D6~D7 · **선행:** T-005 · T-006 · T-009 · T-010

## [배경]

`resources/templates/`에 **레이아웃·프래그먼트·화면 2장이 이미 있다**.
컨트롤러(`admin/` 패키지)만 만들면 된다. **`admin/AdminLoginController`(GET `/admin/login` 한 줄)는 골격에 이미 있다** —
로그인 화면이 첫날부터 열리게 하기 위해서다. 검수·분쟁 컨트롤러를 그 옆에 추가한다.

```
templates/layout/base.html      공통 레이아웃 (th:fragment)
templates/fragments/nav.html    상단 내비게이션
templates/admin/login.html      관리자 로그인
templates/admin/products.html   검수 대기 목록 + 승인/반려 폼
templates/admin/disputes.html   분쟁 목록 + 증빙 다운로드 + 처리 폼
```

`SPEC.md` §7 — `/admin/**`은 **세션 + formLogin + CSRF on**. `SecurityConfig`에 이미 있다.

**핵심은 폼 처리 흐름이다** (`docs/04-화면설계서.md` ADM-002):

```
POST → @Valid + BindingResult → 실패: 같은 템플릿에 오류 재표시
                              → 성공: redirect (PRG 패턴)
```

PRG(Post-Redirect-Get)를 쓰는 이유: 성공 후 새로고침하면 같은 요청이 두 번 들어간다.

## [목표]

관리자가 상품을 검수하고 분쟁을 처리할 수 있다.

## [수용 기준]

- [ ] `/admin/login` 에서 시드 관리자(`admin@udt.test` / `Admin1234!`)로 로그인된다
- [ ] `/admin/products` 에 **검수대기 3건**이 보인다
- [ ] 승인 → 그 상품이 `/api/products` 목록에 **나타난다**
- [ ] 반려 사유를 **비우고** 제출 → **같은 화면에 오류 문구**가 뜬다 (redirect 아님)
- [ ] 반려 사유를 채워 제출 → redirect + 상태 `REJECTED` + 사유 저장
- [ ] `/admin/disputes` 에서 증빙 파일 다운로드 링크(`/admin/disputes/{id}/files/{fileId}`)가 동작한다
      — **시드 분쟁에는 파일이 없다.** 확인하려면 먼저 T-010/T-017로 증빙을 첨부한 분쟁을
      하나 만든 뒤 누른다. 파일 0개인 분쟁은 "첨부 없음"으로 보여야 한다
- [ ] 강제 환불 → 구매자 잔액 복구 · 상품 **`ON_SALE` 복귀** · 분쟁 `RESOLVED`
- [ ] 강제 확정 → 판매자 잔액 증가 · 상품 `SOLD` · 분쟁 `RESOLVED`
- [ ] **비로그인으로 `/admin/products` 접근 → 로그인 화면으로 이동** (JSON 401 아님)
- [ ] 일반 회원 계정으로 접근 → 403
- [ ] 관리자 메서드 하나에 `@PreAuthorize("hasRole('ADMIN')")` 가 붙어 있다

## [제약]

- 수정 허용: `admin/` 패키지 · `resources/templates/`
- **`admin/` 의 컨트롤러는 `@Controller`다** (`@RestController` 아님)
- **Service는 BE-C·BE-D 것을 호출만 한다** — 관리자용 Service를 따로 만들지 않는다
  (규칙이 두 벌이 되고, 상태 전이 단일 오너 원칙이 깨진다)
- 템플릿에 **엔티티를 직접 넘기지 않는다** — DTO만 (§8 B1)
- `<form th:action>` 을 쓴다 — CSRF 토큰이 자동 삽입된다

## [확인 명령]

```bash
node seams/check-api.mjs      # /admin 변경이 /api 를 깨뜨리지 않았는지
# 브라우저: http://localhost:8080/admin/products
```
