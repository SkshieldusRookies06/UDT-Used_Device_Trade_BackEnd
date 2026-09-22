# 내 하루 루프 — 백엔드 (모든 명령은 리포 루트 `<repo>/`에서 시작)

> 먼저 `onboarding/팀원용-한장.md`의 다섯 줄을 읽는다.
> **아래 명령은 셋업을 실제로 통과한 사람이 자기가 친 명령을 그대로 붙여넣는다.**

## 0회차 (첫날 한 번만)

**OS별로 한 번씩 걸리는 것 — 내 것부터 확인**

| | Windows | macOS / Linux |
|---|---|---|
| 실행 | **IntelliJ ▶** (전원 공통). 터미널로 돌릴 때만 `mvnw.cmd` (앞에 `./` 없음 · 래퍼는 선택) | IntelliJ ▶. 터미널이면 `chmod +x mvnw` 후 `./mvnw` |
| MariaDB 포트 찾기 | `netstat -ano \| findstr LISTENING \| findstr :330` | `lsof -i :3306 -i :3307` |
| 8080 잡은 프로세스 죽이기 | `netstat -ano \| findstr :8080` → `taskkill /PID <pid> /F` | `lsof -ti :8080 \| xargs kill` |
| 줄바꿈 | `.gitattributes`가 LF로 고정한다 — 에디터가 CRLF로 바꾸면 diff가 전부 빨개진다 | — |
| JDK 여러 개 | `where java` 로 어느 JDK가 잡히는지 | `/usr/libexec/java_home -V` (mac) |


```
전제: JDK 17 이상 설치 — 확인: java -version
      IDE가 쓰는 JDK는 별개다. File → Project Structure → SDK 가 17 이상인지 한 번 더 확인
전제: Lombok 쓰므로 IntelliJ에서 Enable annotation processing 켠다
전제: MariaDB 기동 · 포트 확인 · DB/계정 생성 (SPEC §6.1)
      SELECT VERSION();                -- 10.x.x-MariaDB 인지 먼저 확인
      CREATE DATABASE udt DEFAULT CHARACTER SET utf8mb4;
      CREATE USER 'udt'@'localhost' IDENTIFIED BY 'udt';
      GRANT ALL PRIVILEGES ON udt.* TO 'udt'@'localhost';

git clone <URL>
IntelliJ → Open → 리포 폴더 선택 → pom.xml 을 Maven 프로젝트로 인식 (우하단 "Load Maven" 뜨면 클릭)
                                                   첫 로딩은 의존성 다운로드로 5분. 멈춘 게 아니다
Run/Debug Configurations → UdtApplication → Active profiles: local
손으로 만들 파일 없음 — application-local.yml 은 clone 에 딸려온다
```

> **실행·테스트는 IntelliJ가 표준이다.** 실행 = `UdtApplication` ▶ · 테스트 = `src/test` 우클릭 → Run 'All Tests'.
> Maven 래퍼(`mvnw`)는 리포에 **없다**(선택 · IDE 없이 빌드 확인이 필요해지면 그때 넣는다 — README "첫 빌드 전에").
> **게이트(`node seams/check-api.mjs`)만은 전원이 같은 명령으로** — 이건 IDE와 무관하다.

## 아침 (매일)

```
1) IntelliJ ▶ UdtApplication (Active profiles: local)
   확인: 콘솔 끝에 "Started UdtApplication in N seconds"
         그 위에 "Tomcat started on port 8080"
   확인: 터미널 2에서 node seams/check-api.mjs → "OK: ... 계약 준수"

   DB 리셋 = 터미널 1을 Ctrl+C 하고 1)을 다시
   (로컬은 ddl-auto: create + 시드가 기동마다 돈다 — 데이터가 이상하면 재기동이 곧 리셋)

2) git switch main && git pull && git switch -c be-<작업명>
```

## 작업

```
3) 오늘 티켓 tasks/T-###.md 를 연다. 지시서가 없으면 시작하지 않고 오너에게 (규약 4조)
4) 에이전트에 넣는 것은 셋뿐: 지시서 + SPEC.md 해당 절 + (수정이면) diff
```

## 끝 (매일)

```
5) IntelliJ 테스트 전체 실행 에러 0 · node seams/check-api.mjs OK
6) curl "http://localhost:8080/api/products?page=0&size=12" 출력을 SPEC.md §4.2 예시와 눈으로 대조
   게이트가 안 보는 것(정렬 순서·값의 말이 되는가)이 여기서 보인다
7) git diff 를 읽는다 → 커밋·push는 내가 한다 → 오너에게 머지 요청
8) worklog/<내이름>/D##.md 3줄 → 내가 커밋
```

---

# 막혔을 때 — 터미널에 뜬 문자열에서 찾는다

| 터미널 1에 뜬 것 | 뜻 · 먼저 볼 곳 |
|---|---|
| `Web server failed to start. Port 8080 was already in use.` | 어제 실행이 살아 있다. 찾아서 Ctrl+C |
| `Failed to configure a DataSource: 'url' attribute is not specified` | 프로파일이 안 붙었다. `-Dspring-boot.run.profiles=local` 확인 |
| `Communications link failure` / `Connection refused` | DB 서버가 안 떠 있다 |
| `Client does not support authentication protocol ... 'sha256_password'` (SQLState 08004) | **그 포트는 MariaDB가 아니라 MySQL이다.** MariaDB 확정 포트는 **3307** — 3306에 붙고 있으면 `DB_PORT`를 확인한다 (SPEC §6.1) |
| `GSS-API authentication exception` · `Krb5LoginModule` · `ErrorCode 1045 / SQLState 28000` | 계정이 없거나 다른 서버에 붙었다. SPEC §6.1의 `CREATE USER` 를 **MariaDB에 접속한 상태에서** 실행했는지 확인 |
| `Unable to determine Dialect without JDBC metadata` | 단독 원인이 아니다 — **위쪽 첫 WARN 줄**이 진짜 원인이다. 거기부터 읽는다 |
| `Unknown database 'udt'` | DB를 안 만들었다. 0회차 전제 줄의 CREATE DATABASE |
| `Access denied for user 'udt'@...` | 계정·권한을 안 만들었다. 0회차 전제 줄 |
| `Table 'udt.products' doesn't exist` (기동 직후) | 시드가 DDL보다 먼저 돌았다 — `defer-datasource-initialization: true` 확인 (SPEC §0) |
| 시드가 **에러 없이 안 돈다** | `spring.sql.init.mode`가 기본값(`embedded`)이다. local에서 `always`로 |
| `Duplicate entry ... for key 'PRIMARY'` (첫 POST에서) | `data.sql`이 id를 명시했다. **id를 지운다** (SPEC §6) |
| `Schema-validation: missing table` | `prod` 프로파일로 붙었다. 로컬은 `local` |
| `cannot find symbol ... getTitle()` | Lombok 애노테이션 처리가 IDE에서 꺼져 있다 — Enable annotation processing |
| `invalid source release: 17` | JDK가 낮다. JDK 17 이상으로 |
| `Unsupported class file major version` | JDK와 `pom.xml`의 `<java.version>`이 어긋났다 |
| `LazyInitializationException` | 컨트롤러에서 지연 로딩이 열렸다. **DTO 변환을 Service 안(@Transactional)에서** (SPEC §8 B1) |
| 상세 API가 500 또는 JSON이 무한히 길다 | 엔티티를 직접 반환했다. DTO로 (SPEC §0 응답 객체 행) |
| 목록이 갑자기 느리고 SQL이 수십 줄 | N+1이다. seller·category는 `fetch join`, images는 `default_batch_fetch_size` |
| `HHH90003004` (firstResult/maxResults 경고) | **컬렉션 fetch join + 페이징**을 같이 썼다. 전 테이블을 메모리로 올린다 — 컬렉션은 batch size로 |
| `Whitelabel Error Page` (브라우저에) | REST 경로면 `@RestController` 확인 · `/admin` 경로면 템플릿 파일명·`templates/` 경로 확인 |
| Security 넣자마자 **모든 API가 401** | permitAll 경로와 CORS를 **Security 체인 안에** 명시했는가 (SPEC §7) |
| `Using generated security password: ...` (기동 로그) | `SecurityFilterChain`은 있지만 `UserDetailsService`가 아직 없다. T-005에서 DB 사용자로 교체하면 이 줄이 사라진다. **그 전까지 `/admin` 로그인은 아이디 `user` + 이 생성 비밀번호**로 된다 |
| check-api가 **10개 중 8개 ok** (RED 2개) | RED는 `POST /api/auth/login (자격 오류)`와 `로그인 → 토큰 → /api/me` — 둘 다 `AuthController`가 없어서다(T-004·T-005). **8/10면 정상 진행 중**이고, T-005가 끝나면 10/10가 된다 |
| `required a single bean, but 2 were found: corsConfigurationSource, mvcHandlerMappingIntrospector` | Spring MVC가 만드는 `HandlerMappingIntrospector`도 `CorsConfigurationSource`다. 우리 빈에 `@Primary`가 빠지면 둘 중 못 고른다 | `CorsConfig`의 빈에 `@Primary` (스캐폴드에 이미 있음 — 지웠으면 복구) |
| `LazyInitializationException: could not initialize proxy` | `open-in-view: false`라 **트랜잭션 밖에서 연관 엔티티를 건드렸다.** 대개 Controller에서 DTO 변환을 했거나, Service 메서드에 `@Transactional`이 없다 | DTO 변환을 Service 안(`@Transactional`)으로 옮긴다. 규약 B2 |
| 테스트 실행이 컨텍스트 로딩에서 죽는다 | `@SpringBootTest`는 **MariaDB가 떠 있어야** 돈다 | 테스트 전에 DB를 켠다. 순수 로직 테스트는 `@SpringBootTest` 없이 짠다 |
| 프론트 콘솔에 `Unexpected token '<'` | `/api/**` 체인이 `/admin/**`보다 뒤에 있다. `@Order` 순서 (SPEC §7) |
| 관리자 폼 제출이 403 | CSRF 토큰. Thymeleaf `<form th:action>`을 쓰면 자동 삽입된다 |
| check-api가 `401 응답이 JSON이 아님` | 기본 302 리다이렉트다. `AuthenticationEntryPoint`에서 공통 봉투 JSON으로 (SPEC §7) |

**30분 넘으면 `<백엔드 오너 이름>`에게 말한다. 규약 4번이고, 잘못이 아니다.**

## 용어 여섯

- **seam** — 팀원 사이 경계. 우리 프로젝트에선 API 하나
- **계약** — `SPEC.md` §4. 어떤 요청에 어떤 JSON을 주는지의 합의
- **게이트** — 계약대로인지 기계가 판정하는 명령. `node seams/check-api.mjs`
- **봉투** — 모든 응답을 감싸는 `{success, data, message, timestamp}` (SPEC §0)
- **DTO** — 응답 전용 객체. 엔티티를 그대로 내보내면 DB 구조가 계약이 되어 버린다
- **diff** — 내가 바꾼 줄 목록. 머지 전에 내가 읽는다
