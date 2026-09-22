# UDT — 백엔드 (메인 리포)

> **이 프로젝트는 리포 2개로 구성됩니다.**
> 프론트엔드: [UDT-Used_Device_Trade-frontend](https://github.com/<조직>/UDT-Used_Device_Trade-frontend) ← 링크를 채워 주세요
> **계약 정본 `SPEC.md`와 제출 문서 `docs/`는 이 리포에 있습니다.**

중고 거래의 "돈 보내고 물건을 못 받는" 문제를, **관리자 검수 → 가상 에스크로 →
구매확정 전 정산 보류 → 분쟁 시 관리자 개입**이라는 거래 상태 프로세스로 해결하는 웹 서비스.

SK Shielders Rookies 6기 웹 팀 프로젝트 (7명 · 2주)

> ### 처음 팀 프로젝트를 한다면 여기부터
> **[`docs/참고/전체로드맵.md`](docs/참고/전체로드맵.md)** — D1부터 제출까지 10일이 날짜별로,
> "오늘 누가 무엇을 하고, 끝났는지 어떻게 확인하는가"까지 적혀 있다.
> 티켓(`tasks/`)이 **어떻게**라면, 로드맵은 **언제·어떤 순서로·지금 어디쯤인지**다.
>
> ### 내 역할 한 장 (1인 1파일 — 자기 것만 읽으면 된다)
> [`BE-A`](onboarding/역할별/BE-A.md) 리포·도메인·마이페이지·분쟁(팀장) ·
> [`BE-B`](onboarding/역할별/BE-B.md) 인증·관리자 ·
> [`BE-C`](onboarding/역할별/BE-C.md) 상품·성능 ·
> [`BE-D`](onboarding/역할별/BE-D.md) 거래 상태 머신

---

## 클론 위치 (계약 · SPEC.md §12)

```
<작업폴더>/
├── UDT-Used_Device_Trade-backend/     ← 지금 이 리포
└── UDT-Used_Device_Trade-frontend/    ← 형제 폴더로 클론. 프론트의 npm run gate 가 여기를 찾는다
```

## 실행

**DB는 MariaDB를 쓴다** — 드라이버도 `mariadb-java-client`다(SPEC ADR-11).
**한 PC에 MySQL과 MariaDB가 같이 깔려 있으면 3306을 MySQL이 차지하는 일이 흔하다.**
연결 전에 반드시 아래 0번을 먼저 한다.

```bash
# 0-a. 지금 붙은 서버가 MariaDB가 맞는지 (최초 1회 · SPEC §6.1)
mysql -u root -p -e "SELECT VERSION(); SELECT @@port;"
#   10.x.x-MariaDB → OK
#   8.x.x          → MySQL이다. MariaDB 포트를 찾아 아래 -P 와 DB_PORT 에 넣는다
#   Windows 포트 찾기: netstat -ano | findstr LISTENING | findstr :330
#   (MySQL이 깔린 PC는 MariaDB가 3307을 잡는다 — 그래서 확정값이 3307이다)

# 0-b. DB·계정 생성 (MariaDB에 접속한 상태에서 · 포트가 3306이 아니면 -P 3307 식으로)
mysql -u root -p -e "CREATE DATABASE udt DEFAULT CHARACTER SET utf8mb4;
  CREATE USER 'udt'@'localhost' IDENTIFIED BY 'udt';
  GRANT ALL PRIVILEGES ON udt.* TO 'udt'@'localhost'; FLUSH PRIVILEGES;"

# 1. 기동 — IntelliJ에서 프로젝트를 연 뒤 (스키마 생성 + 시드가 함께 돈다)
#    Run/Debug Configurations → UdtApplication → Active profiles: local → ▶
#    포트가 3307이 아니면 같은 화면의 VM options 에  -DDB_PORT=3306  (확정값은 3307 · SPEC §0)
#    IDE 없이 돌릴 때만:  mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local  (래퍼는 선택 · 아래 "첫 빌드 전에")

# 2. 계약 게이트
node seams/check-api.mjs

# (Spring이 아직 안 섰을 때) D1 땜빵 목 서버
node mock/server.mjs
```

### 시연 계정 (시드 · `src/main/resources/data.sql`)

| 역할 | 이메일 | 비밀번호 | 잔액 |
|---|---|---|---|
| 관리자 | `admin@udt.test` | `Admin1234!` | — |
| 판매자 | `seller1@udt.test` | `Test1234!` | 252만원 (구매확정 1건 입금 반영) |
| 구매자 | `buyer1@udt.test` | `Test1234!` | 200만원 |
| 구매자(잔액부족 시연용) | `buyer2@udt.test` | `Test1234!` | 30만원 |

관리자 화면: **http://localhost:8080/admin/products**

---

## 구조

```
UDT-Used_Device_Trade-backend/
├── SPEC.md                 계약 정본 — 코드·문서와 어긋나면 이 파일이 이긴다
├── CLAUDE.md               에이전트 컨텍스트
├── OBSERVATIONS.md         마찰 기록 (날짜·현상·비어 있던 장치 · 사람 이름 없이)
├── seams/check-api.mjs     계약 게이트 (프론트도 npm run gate 로 이 파일을 실행한다)
├── mock/server.mjs         D1 땜빵 목 서버
├── docs/                   제출 문서 — 프론트 담당자도 여기에 커밋한다
├── worklog/<이름>/D##.md   전원의 하루 3줄 일지
├── tasks/                  be-* 티켓 (fe-* 티켓은 프론트 리포)
├── onboarding/             팀원용 한 장 · backend.md · 역할별/BE-A~D.md
├── seed-images/            시연용 사진을 넣는 곳 (README만 커밋)
├── uploads/                업로드 저장소 (gitignore)
├── pom.xml
└── src/main/
    ├── java/com/rookies6/udt/
    │   ├── common/         ErrorCode · BusinessException · Advice · ApiResponse   BE-A
    │   ├── config/         CorsConfig · JpaAuditingConfig(BE-A) · SecurityConfig(BE-B)
    │   ├── entity/         8개 + enum 5                                          BE-A
    │   ├── repository/     8개                                                   BE-A → 리소스 오너
    │   ├── controller/     Health · Category · Product(껍데기)  → Auth(BE-B) · Transaction(BE-D) · Me·Dispute(BE-A)가 추가
    │   ├── dto/            리소스 접두어가 오너
    │   ├── security/       (아직 없음 — T-004 BE-B가 만든다)
    │   ├── service/        (아직 없음 — 각 오너가 만든다)
    │   └── admin/          (아직 없음 — T-018 BE-B가 만든다)
    └── resources/
        ├── application.yml · application-local.yml · application-prod.yml · data.sql
        └── templates/      layout/base · fragments/nav · admin/login·products·disputes
```

**패키지는 계층으로, 사람은 리소스 접두어로 나눈다.** `Product*` 다섯 파일이 한 사람 것이어야
혼자 끝까지 만들고 발표에서 설명할 수 있다. B1~B3 검사가 `*Controller.java`·`*Service.java`
**파일 이름**을 전제로 하므로 이름 규칙을 어기면 검사가 조용히 무동작한다.

---

## 무엇을 언제 하나

| 보는 것 | 문서 |
|---|---|
| **10일 전체 순서 · 날짜별 배정 · 저녁 체크포인트** | [`docs/참고/전체로드맵.md`](docs/참고/전체로드맵.md) |
| **내 역할만 — 소유 파일 · 내 티켓을 단계별로 어떻게 · 내 함정 · 내 발표** | `onboarding/역할별/BE-*.md` |
| 지금 잡을 티켓 (수용 기준까지) | [`tasks/README.md`](tasks/README.md) |
| 브랜치 · 커밋 · PR · 머지 · 충돌 (두 리포 공통) | [`docs/참고/GitHub규약.md`](docs/참고/GitHub규약.md) |

프론트 티켓은 프론트 리포 `tasks/`에. 아래는 D1 요약이고, D2 이후는 로드맵에 있다.

| 담당 | D1 티켓 |
|---|---|
| BE-A | T-001 리포 셋업·전원 첫 기동 → T-002 엔티티·시드 확정 |
| BE-B | T-004 JWT 발급·검증 |
| BE-C | T-006 상품 목록·상세 실구현 |
| BE-D | T-009 거래 상태 머신 |

**T-001이 모두의 선행이다.** 오전에 먼저 끝낸다.

---

## 제출 문서 (`docs/`)

**목차: [`docs/README.md`](docs/README.md)**

### 필수 제출 문서 5종

| # | 과제 항목 | 파일 | 상태 |
|---|---|---|---|
| 1 | 도메인 설계서 | `docs/01-도메인설계서.md` | 작성 완료 |
| 2 | Entity 설계서 | `docs/02-Entity설계서.md` | **N+1 캡처(D7) 대기** |
| 3 | REST API 설계서 | `docs/03-REST-API설계서.md` | **D4 실물 대조 후 확정** |
| 4 | 화면 설계서 | `docs/04-화면설계서.md` | **와이어프레임(D3)·캡처(D8) 대기** |
| 5 | React 컴포넌트와 Props 설계서 | `docs/05-React컴포넌트와Props설계서.md` | **D8 실물 재발췌** |

### 그 외

| 항목 | 위치 | 상태 |
|---|---|---|
| 개인 회고록 7명분 | `docs/회고록/` (양식 포함) | D9 |
| 발표 대본 | `docs/참고/발표대본.md` | 슬라이드는 D9 |
| 기획서 · 아키텍처 · 역할분담 · 예상질문 | `docs/참고/` | 작성 완료 |
| 일일 보고 | `docs/참고/일일보고/` (양식 포함) | 매일 |

**`[D#]` 표시가 있는 칸은 그 시점의 실제 산출물로 채운다.** 지금 채우면 없는 캡처와
측정하지 않은 수치를 적게 된다.

---

## 브랜치·커밋

```
main                정본
 └── be-<작업명>    작업 브랜치

커밋   feat|fix|docs|refactor|chore(<범위>): 한 줄 [T-###]
       예) feat(product): 상품 목록 검색·페이징 API [T-006]
```

**계약을 바꿀 때는 티켓 번호로 두 리포의 커밋을 짝 짓는다** — `SPEC.md` §12.
**backend를 먼저 머지한다.**

> 위는 요약이다. **브랜치 → 커밋 → PR → 머지 → 충돌** 전체 규칙과 PR 템플릿 사용법은
> [`docs/참고/GitHub규약.md`](docs/참고/GitHub규약.md) — `main`에는 PR 없이 들어가지 않는다.

---

## 아직 비어 있는 것

| 오너 | 채울 것 |
|---|---|
| BE-A | 엔티티 검토·보완 · 시드 조정 · **`MeController/Service`(T-021)** · **`DisputeController/Service` · 증빙 파일(T-010)** |
| BE-B | `security/`(JwtTokenProvider·필터) · `AuthController/Service` · `UserDetailsService` · `admin/` — **`SecurityConfig` 체인 3개는 골격이 있다**(`// TODO(T-005)`) |
| BE-C | `ProductService`·`WishService` · 이미지 업로드 · 검색·페이징 · **N+1 튜닝** |
| BE-D | **`TransactionService`(상태 전이 단독 오너 · BR-01~08 + `markDisputed`)** · `TransactionController` · 거래 테스트·게이트 확장(T-022) |

> **기동 직후 `check-api`는 9개 중 7개가 ok다.** RED 2개(`POST /api/auth/login (자격 오류)` · `로그인 → 토큰 → /api/me`)는
> 둘 다 `AuthController`가 없어서이고, T-005가 들어가면 9/9가 된다. 7개보다 적으면 다른 문제다.
>
> `ProductController`·`CategoryController`는 **하드코딩 껍데기**다(`// TODO(T-006)` 표시).
> URL과 응답 형태는 계약이므로 그대로 두고 **본문만** Service 연결로 바꾼다.
> 이 껍데기 덕분에 D1부터 프론트 3명이 실서버 주소로 작업할 수 있다.

## 첫 빌드 전에

1. **실행은 IntelliJ가 표준이다.** 폴더를 열면 `pom.xml`을 Maven 프로젝트로 인식한다(안 되면 `pom.xml` 우클릭 → Add as Maven Project).
   **Maven 래퍼(`mvnw`·`.mvn/`)는 일부러 없다** — 필요해지면(IDE 없이 터미널 빌드 확인 · 평가자 빌드) 수업 리포
   `SpringBoot4_Basic_Project`의 `mvnw`·`mvnw.cmd`·**`.mvn/` 폴더째** 복사하면 된다. `.mvn/wrapper/`가 빠지면
   `Cannot start maven from wrapper`가 난다. D8~D9 여유 있을 때 넣어도 늦지 않다.
2. **Spring Boot `4.0.8` / Java 17**로 잡아 뒀다(수업과 동일). 다르면 `pom.xml`의 parent
   `<version>` 한 줄만 바꾼다. Lombok을 쓰므로 IntelliJ **Enable annotation processing**을 켠다.
3. **컴파일·기동은 검증됐다** — 2026-09-22 Windows · JDK 24 · IntelliJ · MariaDB 10.11.18(3307)에서
   `Started UdtApplication` · 테이블 8개 생성 · 게이트 7/9 확인. 첫 실행 때 걸린 것은 `CorsConfigurationSource` 빈 중복 하나였고
   `@Primary`로 잡았다(`onboarding/backend.md` 에러 색인).
