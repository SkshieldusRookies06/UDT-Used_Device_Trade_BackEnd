# UDT — 백엔드 (메인 리포)

> **이 프로젝트는 리포 2개로 구성됩니다.**
> 프론트엔드: [UDT-Used_Device_Trade-frontend](https://github.com/<조직>/UDT-Used_Device_Trade-frontend) ← 링크를 채워 주세요
> **계약 정본 `SPEC.md`와 제출 문서 `docs/`는 이 리포에 있습니다.**

중고 거래의 "돈 보내고 물건을 못 받는" 문제를, **관리자 검수 → 가상 에스크로 →
구매확정 전 정산 보류 → 분쟁 시 관리자 개입**이라는 거래 상태 프로세스로 해결하는 웹 서비스.

SK Shielders Rookies 6기 웹 팀 프로젝트 (7명 · 2주)

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

# 포트가 3307이 아니면 기동할 때 DB_PORT 를 넘긴다 (확정값은 3307 · SPEC §0)
#   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="-DDB_PORT=3306"

# 1. 기동 — 스키마 생성 + 시드가 함께 돈다
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 2. 계약 게이트
node seams/check-api.mjs

# (Spring이 아직 안 섰을 때) D1 땜빵 목 서버
node mock/server.mjs
```

### 시연 계정 (시드 · `src/main/resources/data.sql`)

| 역할 | 이메일 | 비밀번호 | 잔액 |
|---|---|---|---|
| 관리자 | `admin@udt.test` | `Admin1234!` | — |
| 판매자 | `seller1@udt.test` | `Test1234!` | 200만원 |
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
├── docs/                   제출 문서 13종 — 프론트 오너도 여기에 커밋한다
├── worklog/<이름>/D##.md   전원의 하루 3줄 일지
├── tasks/                  be-* 티켓 (fe-* 티켓은 프론트 리포)
├── onboarding/             팀원용 한 장 · backend.md
├── pom.xml
└── src/main/
    ├── java/com/rookies6/udt/
    │   ├── common/         ErrorCode · BusinessException · Advice · ApiResponse   BE-A
    │   ├── config/         CorsConfig(BE-A) · SecurityConfig(BE-B)
    │   ├── entity/         9개 + enum 5                                          BE-A
    │   ├── repository/     8개                                                   BE-A → 리소스 오너
    │   ├── security/       JWT (비어 있음)                                        BE-B
    │   ├── controller/     Product·Category(BE-C) · Auth(BE-B) · Transaction·Dispute(BE-D)
    │   ├── service/        (비어 있음)
    │   ├── dto/            리소스 접두어가 오너
    │   └── admin/          Thymeleaf 컨트롤러 (비어 있음)                          BE-B
    └── resources/
        ├── application*.yml · data.sql
        └── templates/      layout · fragments · admin 2장
```

**패키지는 계층으로, 사람은 리소스 접두어로 나눈다.** `Product*` 다섯 파일이 한 사람 것이어야
혼자 끝까지 만들고 발표에서 설명할 수 있다. B1~B3 검사가 `*Controller.java`·`*Service.java`
**파일 이름**을 전제로 하므로 이름 규칙을 어기면 검사가 조용히 무동작한다.

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

---

## 아직 비어 있는 것

| 오너 | 채울 것 |
|---|---|
| BE-A | 엔티티 검토·보완 · 시드 조정 |
| BE-B | `security/`(JwtTokenProvider·필터) · `SecurityConfig` **체인 2개** · `AuthController/Service` · `admin/` |
| BE-C | `ProductService`·`WishService` · 이미지 업로드 · 검색·페이징 · **N+1 튜닝** |
| BE-D | **`TransactionService`(상태 전이 단독 오너)** · `DisputeService` · 증빙 업로드/다운로드 |

> `ProductController`·`CategoryController`는 **하드코딩 껍데기**다(`// TODO(T-006)` 표시).
> URL과 응답 형태는 계약이므로 그대로 두고 **본문만** Service 연결로 바꾼다.
> 이 껍데기 덕분에 D1부터 프론트 3명이 실서버 주소로 작업할 수 있다.

## 첫 빌드 전에

1. **Maven 래퍼가 없다.** 수업 리포(`SpringBoot4_Basic_Project`)의 `mvnw`·`mvnw.cmd`·`.mvn/`을
   이 리포 루트로 복사하거나, IntelliJ에서 Maven 프로젝트로 열면 된다.
2. **Spring Boot `4.0.8` / Java 17**로 잡아 뒀다(수업과 동일). 다르면 `pom.xml`의 parent
   `<version>` 한 줄만 바꾼다. Lombok을 쓰므로 IntelliJ **Enable annotation processing**을 켠다.
3. **백엔드 컴파일은 아직 검증되지 않았다** — 스캐폴드 작성 환경에서 Maven Central 접근이
   막혀 의존성을 못 받았다. `javac` 구문 검사만 통과한 상태다.
