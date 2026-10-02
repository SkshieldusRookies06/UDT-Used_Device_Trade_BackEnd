# 개인 회고록 — (이름)

> 파일명을 `retro-<이름>.md` 로 바꿔서 각자 작성한다. **1·2·3번은 리포에서 뽑고,
> 4·5번만 직접 쓴다.** D9 오전에 작성한다.

## 1. 맡은 것과 결과

> 생성 입력: `git log --author=<나> --oneline` · `tasks/T-*.md` 중 내 것 · `worklog/<나>/`

| 항목 | 값 |
|---|---|
| 담당 영역 | T-009 거래 상태 머신, T-003 공통 예외·봉투 검증, T-022 거래 게이트 확장·테스트 |
| 완료 티켓 수 | 3 |
| 머지 커밋 수 | 25 (merge 커밋 3개 제외) |
| 주요 산출물 | service/TransactionService.java<br>controller/TransactionController.java<br>dto/Transaction*<br>repository/TransactionRepository |

## 2. 배운 것 — 기술

> 입력: `worklog/<나>/D##.md` 의 3번 "막힘" 줄을 날짜순으로.
> 각 줄에 **원인 · 해결 · 다음에 먼저 볼 것** 세 가지를 붙인다.

| 날짜 | 막혔던 것 | 원인 | 해결 | 다음에 먼저 볼 것 |
|---|---|---|---|---|
| 9.22 | 구매해도 잔액이 안 빠짐 | `transition()`의 `@Modifying(clearAutomatically = true)`가 영속성 컨텍스트를 비워 `buyer.withdraw()`가 DB에 반영 안 됨 | `ProductRepository.java`에서 옵션 삭제 + `purchase()`에 `product.changeStatus(IN_TRADE)` 한 줄 추가 | 영속성 컨텍스트와 변경 감지(dirty checking) 원리부터 보기 |
| 9.22 | MariaDB 연결 실패로 앱·테스트가 전부 기동 안 됨 | 앱 기본 포트(3307)와 실제 MariaDB 포트(3306) 불일치 | `DB_PORT=3306` 환경변수를 실행 설정에 추가 | `${이름:기본값}` 환경변수 치환 문법 |
| 9.27 | `grep` 명령어가 전부 오류 | PowerShell에는 `grep`, 진짜 `curl`이 없음 | `grep` → `Select-String`, `curl` → `curl.exe`로 전환 | 지금 터미널이 PowerShell인지 Git Bash인지 먼저 확인 |
| 9.27 | 관리자가 강제 환불해도 분쟁이 OPEN으로 남음 | BR-07·08의 "Dispute → RESOLVED" 규칙이 코드에서 누락 | `resolveDispute()` 메서드를 만들어 `forceRefund`·`forceConfirm`에서 공통 호출 | BR 규칙표의 "몇 줄이 같이 바뀌어야 하는지"부터 세어보기 |
| 9.30 | 게이트가 13개 전부 응답 없음(RED) | 서버 자체가 안 떠 있는 상태에서 게이트 실행 | "전부 응답 없음"이면 코드가 아니라 기동 문제라는 걸 먼저 의심 | 게이트 스크립트 맨 아래 안내 문구부터 읽기 |

## 3. 배운 것 — 협업

> 입력: `OBSERVATIONS.md` 에서 내가 관련된 항목 2~3개

- github 브랜치 관리, 머지와 PR
- 백엔드 서비스 역할과, 에스크로 머신에 대해.

## 4. 아쉬운 것 · 다음에 다르게 할 것

> AI 프롬프트를 좀 더 잘 적고, 시간이 더 있었다면, 코드를 직접 작성해서 AI에게 맞는지 물어보고 싶다. 좀 더 구체적으로 코드를 뜯어보고 싶다.
> 기회가 있다면, 문서 작성과 프론트 (피그마를 이용한)을 팀원분에게 배워보고 싶다.

## 5. 소감 — 발표 1분 원고

> 저번 프로젝트에서 github에 대해 다 알게 되었다고 생각했는데, 팀프로젝트에서
> 프론트와 백엔드를 나누고, 제 역할까지 세세하게 나누다 보니 브랜치 관리도 쉽지 않았고 머지할때도 불안했던 것 같습니다.
> 그래도 재밌고 팀원들에게 배울점이 많았던 프로젝트였던 것 같습니다.