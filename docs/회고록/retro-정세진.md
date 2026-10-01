# 개인 회고록 — 세진 (BE-B · 인증·관리자)



## 1. 맡은 것과 결과

| 항목 | 값 |
|---|---|
| 담당 영역 | BE-B — 인증(JWT · Security 필터 체인 · 로그인 API) + 관리자 화면(Thymeleaf · 검수/분쟁 처리) |
| 완료 티켓 수 | 3개 — T-004 JWT 발급·검증 · T-005 Security 완성(회원가입·로그인·내 정보) · T-018 관리자 화면 2장 |
| 머지 커밋 수 | 14개 (머지 커밋 제외, 전부 `main` 반영 확인, 작업 초반에 이메일 계정이 달라서 기여도에는 두개의 계정으로 커밋이 되어있음) |
| 주요 산출물 | `JwtTokenProvider` · `JwtAuthenticationFilter` · `SecurityConfig` API 체인 필터 등록 · `CustomUserDetailsService`/`UserPrincipal` · `AuthController`/`AuthService` + DTO 5종(signup·login·me) · `AdminProductController`(검수 승인/반려) · `AdminDisputeController`(분쟁 목록·강제 환불/확정) · `AdminControllerExceptionHandler` · `templates/admin/products.html`·`disputes.html` |

## 2. 배운 것 — 기술

| 날짜 | 막혔던 것 | 원인 | 해결 | 다음에 먼저 볼 것 |
|---|---|---|---|---|
| 9/22 | 내 PC에서 DB 연결 실패 | MariaDB 포트가 PC마다 다름(공용 기본값 3307, 내 PC 3306) | `application-local.yml` 기본 포트를 3306으로 수정해 커밋 | 공용 설정 파일을 고치기 전에 `SELECT @@port;` 확인 → 환경변수 `DB_PORT`로 내 PC만 맞춘다 |
| 9/28 | 토큰을 보내도 모든 `/api/**` 요청이 401 | `JwtAuthenticationFilter`는 만들었지만 `apiFilterChain`에 등록하지 않음 | `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`로 등록 → `JwtFilterRegistrationTest` green | 필터·인터셉터는 "만든 것"과 "체인에 등록한 것"을 따로 확인한다 |
| 9/28 | 토큰 claim이 계약과 다름 | SPEC 토큰 규격은 `role · nickname`인데 `role`만 넣음 (검증표 A14) | 3-인자 `createToken(userId, role, nickname)` 오버로드 추가, 기존 2-인자는 위임으로 유지 | 구현 전에 SPEC의 토큰 규격 줄을 먼저 읽는다 |
| 9/28 | 관리자 분쟁 처리 코드가 실제 서비스와 안 맞음 | 존재하지 않는 `DisputeService.resolve()`·`DisputeDecision`을 추측으로 호출, 경로 변수도 `disputeId`로 잡음 | `TransactionService.forceRefund/forceConfirm` 직접 호출, 경로를 `transactionId`로 변경. 목록은 BE-A 확정 후 `DisputeService.findOpenDisputes()`로 교체 | 남의 Service를 부를 땐 SPEC·오너에게 메서드 시그니처부터 확인한다  |
| 9/28 | 관리자 화면 폼·링크가 컨트롤러와 어긋남 | 템플릿이 `d.id`로 폼을 보내고 증빙 링크가 `/api/...` 경로, `fid=f.id`에 `${}` 누락, 오류 메시지 표시 없음 | 폼 id를 `transactionId`로, 증빙 링크를 `/admin/disputes/.../files/...`로, flash 오류(`disputeError`) 표시 추가 | 컨트롤러를 바꾸면 그 화면 템플릿의 `th:action`·`th:href`를 같이 연다 |
| 9/28 | dev 머지 후 컴파일 오류 | `ErrorCode`에 없는 `getCode()`를 호출 (enum이라 `name()`이 코드) | `getCode()` → `name()` | 공용 클래스(`ErrorCode` 등)는 쓰기 전에 실제 필드를 열어 본다 |
| 9/30 | 서명 알고리즘이 SPEC과 다를 수 있음 | `signWith(key)`만 쓰면 jjwt가 키 길이로 알고리즘을 자동 선택 | `signWith(key, Jwts.SIG.HS256)`으로 고정 | 보안 설정은 라이브러리 기본값에 맡기지 않고 명시한다 |
| 10/1 | 직접 검사 방식을 수정 |String reason + isBlank() 직접 검사 방식   | @Valid RejectForm + BindingResult 방식으로 전환 | 서버 검증을 테스트하려면 input에 maxlength를 걸지 않아야 한다. |
## 2-1. 배운 것 — 보안


| 주제 | 내가 한 것 | 막는 위협 | 다음에 더 볼 것 |
|---|---|---|---|
| 비밀번호 저장 | 회원가입에서 `passwordEncoder.encode()`로 해시 저장, 로그인은 `matches()`로 비교 (`AuthService`) | DB 유출 시 평문 비밀번호 노출 | 비밀번호 정책(길이·조합)과 로그인 시도 횟수 제한 |
| 계정 열거 방지 | 이메일이 없을 때와 비밀번호가 틀릴 때 모두 401 `INVALID_CREDENTIALS`로 통일 | 응답 차이로 가입된 이메일을 알아내는 공격 | 응답 시간 차이(타이밍)까지 같게 만드는 방법 |
| 권한 상승 방지 | 회원가입 `role`은 요청값을 받지 않고 서버에서 `MEMBER`로 고정 | 요청 바디에 `"role":"ADMIN"`을 넣어 관리자로 가입 | DTO에 받지 않을 필드는 아예 두지 않는다 (Mass Assignment) |
| 토큰 검증 실패 처리 | 위조·만료 토큰은 예외를 잡아 `SecurityContext`를 비우고 인증 없이 통과, `role` claim이 없으면 인증 객체를 만들지 않음 (`JwtAuthenticationFilter`) | 깨진 토큰으로 인증 상태가 남는 것 | 실패 사유(만료/위조)를 로그로만 남기고 응답엔 노출하지 않는지 점검 |
| 권한 문자열 | 필터와 `UserPrincipal` 양쪽에서 권한을 `"ROLE_" + role`로 만듦 | `hasRole("ADMIN")`이 `ROLE_ADMIN`만 통과시켜 관리자가 403을 받거나, 접두어가 섞여 검사가 어긋나는 것 | 권한 문자열을 만드는 곳을 한 군데로 모은다 |
| 서명 알고리즘 고정 | `signWith(key, Jwts.SIG.HS256)`으로 명시 (9/30) | 라이브러리 기본값에 따라 알고리즘이 바뀌어 SPEC과 어긋나는 것 | 검증 쪽도 기대 알고리즘만 받는지 확인 (알고리즘 혼동 공격) |
| 관리자 기능 권한 | 관리자 컨트롤러의 강제 환불/확정·검수 처리에 `@PreAuthorize("hasRole('ADMIN')")`을 걸고, 일반 API를 거치지 않고 `TransactionService`를 직접 호출 (9/27 검증에서 로그인만 하면 일반 회원도 강제 환불 API를 부를 수 있던 구멍(A4)이 나와, 그 경로 대신 관리자 화면이 처리하도록 바꿈) | 일반 회원의 강제 환불 같은 수직 권한 우회 | "인증됨"과 "권한 있음"은 다르다 — 새 엔드포인트마다 누가 호출할 수 있는지 한 줄로 적는다 |

**한 줄 정리:** 보안 기능은 "만들었다"가 아니라 "연결되고 막힌다"를 확인해야 끝난다 — 내가 만든 JWT 필터도 체인에 등록하기 전까지는 아무것도 지키지 못했다(9/28).

## 3. 배운 것 — 협업

- **9/27 팀장 전수 검증 → 판정 테스트로 받은 할 일.** 내 티켓에 "필터 등록", "nickname claim" 두 줄과 판정 테스트 이름이 붙어 와서, 테스트가 green이면 끝인 상태로 일할 수 있었다. "됐습니다"보다 테스트 이름 하나가 소통 비용을 크게 줄였다.
- **관리자 화면은 남의 Service 위에 서는 화면이다.** 검수는 BE-D의 `TransactionService`, 분쟁 목록은 BE-A의 `DisputeService`를 호출한다. 확정 전에는 `TODO(T-018)`과 "서비스 연동 대기"를 커밋에 남기고 기다렸다가 교체했는데, 처음부터 오너에게 시그니처를 물었으면 refactor 커밋 하나를 줄일 수 있었다.
- **dev 통합 브랜치 + main은 팀장 PR.** 내 브랜치(`be-auth`·`be-admin`)에 dev를 자주 받아 오면서 충돌을 작게 유지했고, 머지 후 컴파일 오류처럼 통합에서만 드러나는 문제를 빨리 잡았다.

## 4. 아쉬운 것 · 다음에 다르게 할 것


worklog를 매일 3줄씩 남기지 못해서, 회고록을 쓸 때 막혔던 기억을 커밋 로그에서 거꾸로 찾아야 했다. 관리자 분쟁 컨트롤러를 SPEC과 오너 확인 없이 추측으로 먼저 짜는 바람에 같은 파일을 네 번 고쳤다. JWT 필터를 만들어 놓고 체인 등록을 빠뜨린 것처럼, "만들었다"와 "동작한다"를 구분하지 못한 순간이 있었다. 공용 설정 파일의 DB 포트를 내 PC에 맞춰 바꾼 것도 환경변수로 해결했어야 했다. 다음에는 구현 전에 SPEC 해당 절과 판정 테스트를 먼저 읽고, 하루 끝에 worklog를 남겨야겠다. 시연 피드백으로 관리자 화면 CSS를 수정하는것이 있었는데, 미리 만들때 관리자의 편의성도 생각했으면 어땠을까 싶다. 

## 5. 소감 — 발표 1분 원고


저는 인증과 관리자 화면을 맡아, 같은 서버에서 React용 `/api/**`는 JWT로, 관리자용 `/admin/**`은 세션으로 나눠 지키는 구조를 만들었습니다. 팀원분들이 소통도 잘되고 다들 능력도 좋아서, 수월하게 과정이 진행될 수 있었습니다. 
백엔드, 프론트엔드를 나눠서 나중에 합치는 방식으로 진행했는데 서류가 중요한것도 알수 있었습니다. 합쳐도 충돌이 거의 없었고, 이 정도 규모의 프로젝트는 혼자서도 해보고 싶어서, 시간 여유로울 때 만들어봐야겠다는 생각을 했습니다.
수업때 배운 내용들을 프로젝트로 진행해서, 복습 효과도 좋았고, 내용들이 익숙했습니다. 수업때 처음 배운 내용을 이해하는건 많이 어려웠지만 미니 프로젝트를 통해서, 직접 경험해보니 좋았습니다.
비전공에서 시작해 보안 엔지니어를 목표로 하는 입장에서, 필터 하나를 체인에 등록하지 않으면 모든 요청이 막힌다는 걸 직접 겪으며 보안은 설정 한 줄의 문제라는 걸 배웠습니다. 그리고 아직 많이 배우지 않았지만 다양한 보안관련 요소들이 있음을 알았습니다. 다음 프로젝트에서는 추측하기 전에 먼저 묻고, 기록을 매일 남기는 습관을 들여야겠습니다.
