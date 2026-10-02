# 개인 회고록 — 원종현

## 1. 맡은 것과 결과

| 항목 | 값 |
|---|---|
| 담당 영역 | 상품 목록·상세·검색·등록(이미지) · 찜 · 목록 성능(N+1) — `ProductController`·`CategoryController`·`WishController` / `ProductService`·`WishService`·`CategoryService` / `ProductRepository`·`WishRepository`·`CategoryRepository` / `dto/Product*`·`dto/Wish*`|
| 완료 티켓 수 | 4개 — T-006 목록·상세 · T-007 등록+이미지 · T-008 찜 토글 · T-019 N+1 (+ 다른 역할 지원 3건: T-009용 `transition()`, T-018용 `findInspectingProducts()` · 프론트 1건: 목록 카드 하트 찜 토글 — T-014 연장) |
| 머지 커밋 수 | 21개 |
| 주요 산출물  | 상품 API 6개(목록·상세·등록·카테고리·찜 추가/해제), 목록 1회 호출 쿼리 32 → 4, 판정 테스트 `acceptance/product` 22개 green, (프론트) 목록 카드 하트 찜 토글로 등록/해제 |

<details>
<summary>티켓별 커밋</summary>

| 티켓 | 커밋 |
|---|---|
| T-006 | 카테고리 조회 실구현 · 카테고리 주석 정리 · 상품 목록/상세 API 실구현 · import 복구 · 검색어 트림 및 소문자화 |
| T-007 | 상품 등록 API 구현 · 입력값 검증 · 이미지 저장 연결 · 카테고리 id 파싱 보강 · 판매자를 로그인 사용자로 연결 |
| T-008 | wish 서비스 로직 · wish controller 구현 · 찜 추가/해제를 로그인 사용자로 연결 · 상세 `wished`를 로그인 사용자 기준으로 계산 |
| T-009 (BE-D 지원) | 상품 상태 조건부 전이 메서드 추가 · `transition`의 `clearAutomatically` 옵션 제거 |
| T-019 | 상품 목록 N+1 해결 · `02-Entity설계서` N+1 기록 · 찜 수 집계 메서드 이름 수정 |
| T-018 (BE-B 지원) | 관리자 검수 목록용 메서드 추가 |
| T-014 연장 (프론트) | 목록 카드 하트로 찜 등록/해제 |

</details>

## 2. 배운 것 — 기술

| 날짜 | 막혔던 것 | 원인 | 해결 | 다음에 먼저 볼 것 |
|---|---|---|---|---|
| 9/22 | 검색 결과가 대소문자·앞뒤 공백에 따라 달라짐, `main` 머지 때 같은 검색 쿼리 줄 충돌 | 검색어를 정규화하지 않음, 다른 브랜치도 같은 줄을 수정함 | 서비스에서 `q.trim().toLowerCase()` + 쿼리에서 `LOWER(p.title)`로 비교, 빈 검색어는 `null`로 바꿔 `:q IS NULL OR ...`로 필터 생략, 충돌은 내 버전(`LOWER`) 채택 | 입력 정규화는 한 곳(Service)에서만 처리하기 |
| 9/22 | 찜을 빠르게 두 번 누르면 중복 저장될 수 있음 | `exists` 검사와 `save` 사이에 다른 요청이 끼어들 수 있음 | `wishes`의 `(user_id, product_id)` unique 제약에 맡기고 `DataIntegrityViolationException` → `409 WISH_ALREADY_EXISTS`, 해제할 찜이 없으면 `404 WISH_NOT_FOUND`  | 동시성은 애플리케이션 `if`가 아니라 DB 제약으로 막기 |
| 9/28 | 구매해도 구매자 잔액이 줄지 않음 | `transition()`의 `@Modifying(clearAutomatically = true)`가 영속성 컨텍스트를 비워 `buyer.withdraw()`의 변경 감지가 사라짐 | `clearAutomatically` 옵션 제거, BE-D의 `PurchaseBalanceTest`(실제 DB로 잔액 확인)로 검증 | `@Modifying` 옵션이 같은 트랜잭션의 다른 엔티티에 주는 영향 확인하기 |
| 9/28 | 등록 시 `conditionGrade:"X"`면 500, `priceKrw`가 없으면 NPE로 500 | 검증 애노테이션이 `title`에만 있었음 | DTO에 `@NotBlank`·`@Size`·`@NotNull`·`@Positive`·`@Pattern("S\|A\|B\|C")` 추가, `categoryId`는 `String` + `@Pattern("\\d+")` → 400 + `fields[]` | JSON → 객체 변환이 `@Valid`보다 먼저라서 `fields`는 Bean Validation 실패일 때만 채워짐 |
| 9/28 | 이미지 저장 중 실패하면 디스크에 파일이 남음 | `@Transactional`은 DB만 롤백하고 디스크 파일은 되돌리지 않음 | 저장한 `storedName`을 모아 두고 `catch(RuntimeException)`에서 `fileStorageService.delete()` 후 다시 던짐, 빈 파트는 `isEmpty()`로 거른 뒤 5장 제한 검사 | 트랜잭션이 되돌려 주는 것과 아닌 것(파일·외부 API)을 먼저 구분하기 |
| 9/28 | 상품 목록 1회 호출에 쿼리 32개 | 목록 1 + count 1 + 카테고리 5 + 판매자 1 + 이미지 12 + 찜 수 12(카테고리·판매자는 1차 캐시 덕분에 종류 수만큼만) | 단계별로 측정하며 적용, ① to-one `JOIN FETCH` → 26, ② 이미지 `default_batch_fetch_size: 100` → 15, ③ 찜 수 `GROUP BY` 집계 1회 → 4, 찜 0건 상품은 `getOrDefault(id, 0)` | 고치기 전에 먼저 세기, 컬렉션은 `fetch join` + 페이징 대신 batch size로 (메모리 페이징 `HHH90003004` 방지) |
| 9/28 | 페이지를 넘기면 같은 상품이 중복되거나 빠짐 | 시드 상품의 `createdAt`이 모두 같아 `ORDER BY createdAt`만으로는 순서가 정해지지 않음 | `ORDER BY p.createdAt DESC, p.id DESC` 보조 정렬 추가 | 페이징 정렬에는 항상 유일한 키로 tie-break |
| 9/29 | 시드 상품 `createdAt`이 API에서 약 +9시간으로 보여 목록 순서가 꼬임 | 시드 `data.sql`의 `NOW()`는 Auditing을 거치지 않고 DB 세션 시간대로 저장되는데, Hibernate는 다른 시간대로 해석함 | BE-A에 보고 → 공용 설정에서 시간대 통일, `hibernate.jdbc.time_zone` + JDBC URL `connectionTimeZone=+09:00&forceConnectionTimeZoneToSession=true` | 앱과 DB가 같은 시간대를 쓰는지 먼저 확인하기 |
| 10/01 | `product` 파트 없이 등록 요청하면 400이 아니라 500 (API 직접 호출 때만) | `GlobalExceptionHandler`의 400 목록에 `MissingServletRequestPartException`이 없음, 이름이 비슷한 `MissingServletRequestParameterException`과 상속 관계가 아니라 `Exception` 핸들러로 넘어감 | 공용 파일이라 직접 고치지 않고 BE-A에 보고 (분쟁 신청도 같은 구조임을 함께 전달), BE-A가 400 목록에 추가 → 400 `VALIDATION_ERROR` | 새 입력 경로를 만들면 값을 아예 안 보냈을 때도 확인하기 |
| 10/01 | (프론트) 목록 카드 하트를 눌러도 찜이 안 되고 상세로 이동 | 카드 전체가 `<Link>`이고 하트는 그 안의 `<span>`이라 클릭이 `<Link>`까지 전달됨 (이벤트 버블링) | 하트를 `<button type="button">`으로 바꾸고 `preventDefault()`·`stopPropagation()` 호출 | 클릭 요소 안에 클릭 요소를 넣을 땐 이벤트가 어디까지 전달되는지 확인하기 |

## 3. 배운 것 — 협업

- 계약(`SPEC.md`)이 먼저 있으니 병렬 개발이 가능해짐
    - 내 컨트롤러가 첫날부터 하드코딩 껍데기로 같은 URL·같은 응답 형태를 내줘서 프론트엔드 팀원이 기다리지 않고 화면을 만들 수 있었다. 
    - 개발이 끝난 뒤 목록 카드 하트 동작 문제가 나왔을 때는 1안·2안을 비교하고, 백엔드 변경 없이 기존 계약만으로 된다는 걸 확인한 뒤 직접 구현했다.  
  

- 다른 팀원 파트의 파일은 바로 고치지 않고 오너에게 보고한 후 작업하면 충돌이 적어짐  
    - `MeService`의 찜 수 N+1문제(BE-A), 시드 시간 +9h(BE-A), multipart 누락 500(BE-A)을 고치지 않고 오너에게 먼저 알렸다.
    - 다른 팀원의 파트(`components/`)를 고칠 때 또한 미리 보고 후 작업을 진행하였고, PR 본문에도 수정 허용 범위 밖의 파일을 수정했다는 것을 한번 더 알렸다. 
    - BE-D가 추가해달라고 요청한 `transition()`메서드는 내 파일 안에서 구현해야하는 메서드여서 내가 넣었는데, 검수 없이 요청 코드를 그대로 옮기면서 붙어 온 옵션(`clearAutomatically`)이 구매자가 상품을 구매해도 잔액이 안빠지는 오류를 만들었다. 
    - 남이 요청한 코드라도 내 파일에 들어갈 때 한 번 더 검수가 필요하다.


## 4. 아쉬운 것 · 다음에 다르게 할 것

- 프로젝트 기간이 짧아서 조장님이 초기 문서와 초기 필요 엔티티 등을 미리 설계해오셨다. 덕분에 프로젝트가 빨리 진행되기는 했지만, 
AI의 도움을 많이 받을 수 밖에 없었다.  
다음에는 시간적 여유가 있다면 구조 설계, 문서 작성에 같이 직접 관여하면서 내가 스스로 코드를 작성해가면서 AI에 대한 의존을 좀 더 줄여보고 싶다.  
- 지금은 N+1 문제를 일부 기능에만 적용했는데, 다음에는 이번 경험을 토대로 기능 전반의 쿼리를 개선해보면 좋을 듯하다. 

## 5. 소감

저는 상품 조회·등록·찜과 목록 속도 개선을 맡은 원종현입니다.  
짧은 기간임에도 불구하고 API 약속을 먼저 정하고 역할을 나눠서, 각자 맡은 부분에 집중하면서도 하나의 서비스로 맞춰지는 과정을 직접 경험할 수 있었습니다.  
가장 어려웠던 건 상품 목록을 한 번 불러올 때 DB 요청이 32번이나 나가는 문제였는데,  
로그로 하나씩 세어 원인을 나누고 단계별로 고쳐 4번까지 줄이면서 JPA가 언제 쿼리를 보내는지 배웠습니다.  
협업에서는 다른 팀원이 맡은 코드에 문제가 보이면 직접 고치기보다 담당자에게 먼저 알려 충돌을 줄였습니다.  
또 AI의 도움을 받아 작성한 코드도 처음부터 다시 따라가며 공부하면서 기존에 배웠던 이론들도 복습할 수 있는 계기가 되었습니다.  
비록 백엔드 부분을 맡았지만, 이번 웹 프로젝트를 통해 백엔드, 프론트엔드가 각각 어떻게 동작하고, 어떻게 연계되는지 직접 알아볼 수 있었던 좋은 시간이었던 것 같습니다.  
프로젝트가 잘 진행될 수 있게 함께해 주신 팀원분들께도 감사합니다.
