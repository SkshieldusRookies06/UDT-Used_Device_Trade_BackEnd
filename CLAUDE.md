# UDT 백엔드 — 에이전트 컨텍스트

이 리포는 **허브**다: `SPEC.md`(계약 정본) · `seams/` · `mock/` · `docs/` · `worklog/`가 여기 산다.
프론트는 별도 리포(`UDT-Used_Device_Trade-frontend`, 형제 폴더).
**코드·다른 문서와 충돌하면 `SPEC.md`가 이긴다.**

구조: `src/main/java/com/rookies6/udt/` · `src/main/resources/` · `seams/` · `mock/` · `docs/` · `tasks/`

## 강제 규약
- 응답 키는 **camelCase** · id는 **문자열** · 날짜는 `OffsetDateTime`(오프셋 포함)
- 전 엔드포인트 **공통 봉투** — 에러는 `@RestControllerAdvice` 한 곳에서만
- **응답은 DTO만. 엔티티 직접 반환 금지.** DTO 변환은 Service 안에서
- Controller에서 Repository 직접 호출 금지 · Service에 웹 타입(`ResponseEntity`·`Model`) 금지
- 상태 전이는 `TransactionService`에서만 (SPEC §2.5)
- `/api/**`와 `/admin/**`은 Security 체인이 다르다 — `@Order` 순서 주의 (SPEC §7)

## 명령
```
실행     IntelliJ ▶ UdtApplication (Active profiles: local)   — Maven 래퍼 없음
테스트   IntelliJ src/test → Run 'All Tests'
게이트   node seams/check-api.mjs
목 서버  node mock/server.mjs
커밋     feat|fix|docs|refactor|chore|test(<범위>): 한 줄 [T-###]   (규약: docs/참고/GitHub규약.md)
```

## 금지
- **게이트(`seams/`)를 통과시키려고 게이트를 수정하지 않는다.** 검사가 틀렸다고 판단되면 멈추고 보고
- **commit·push·merge 금지** — 사람이 한다
- **전면 재작성 금지** — 최소 diff. 구조를 바꿔야 하면 멈추고 보고
- 공용 파일(`config/` · `common/` · `data.sql` · `pom.xml`)을 자기 파일에서 재정의하지 않는다
- **`worklog/`는 읽지 않는다.** 입력은 지시서 + SPEC 해당 절 + diff 셋뿐
- 계약을 바꿀 때 **프론트 리포를 대신 고치지 않는다** — SPEC·게이트·목만 고치고 보고 (SPEC §12)
- 코드 주석은 최소화 — 설명은 `README.md`·`docs/`에 둔다
