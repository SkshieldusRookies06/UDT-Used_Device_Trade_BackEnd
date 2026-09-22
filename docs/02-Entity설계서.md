# Entity 설계서 (Table 설계서 포함) — UDT

## 문서 정보

| 항목 | 내용 |
|---|---|
| 프로젝트명 | UDT (Used Device Trade) |
| 문서 버전 | v0.1 |
| 작성일 | 2026-09-21 |
| 작성자 | (이름) |
| 최종 수정일 | 2026-09-21 |

> 정본: `src/main/java/com/rookies6/udt/entity/` · `application-local.yml` · `data.sql`
> 이 문서는 **코드에서 발췌**한다. D2 엔티티 확정 후 작성하고, D8에 재발췌한다.

---

## 1. Entity 설계 개요

### 1.1 설계 목적

거래 신뢰 프로세스를 데이터 모델로 표현한다. 특히 **한 상품에 거래는 하나**,
**한 거래에 분쟁은 하나**라는 업무 규칙을 DB 제약으로 강제해, 애플리케이션 버그가 있어도
데이터가 깨지지 않게 한다.

### 1.2 설계 원칙

| # | 원칙 | 이유 |
|---|---|---|
| 1 | **모든 연관관계는 `LAZY`** | 즉시 로딩은 의도치 않은 조인과 N+1을 부른다 |
| 2 | **응답은 DTO만. 엔티티를 직접 반환하지 않는다** | DB 구조가 API 계약이 되는 것을 막는다 |
| 3 | **DTO 변환은 Service 안(`@Transactional`)에서** | 컨트롤러에서 변환하면 지연 로딩이 트랜잭션 밖에서 열린다 |
| 4 | **업무 규칙은 DB 제약으로도 건다** | 고유 제약이 최후의 방어선이다 |
| 5 | 기본 생성자는 `PROTECTED`, 생성은 `@Builder`로 | 불완전한 객체 생성을 막는다 |

### 1.3 기술 스택

| 영역 | 값 |
|---|---|
| JPA 구현체 | Hibernate 7.2 (Spring Boot 4.0.8) |
| DB | MariaDB 10.11 · InnoDB · `utf8mb4` |
| 스키마 생성 | 로컬 `ddl-auto: create` + 초기 데이터 자동 실행 |
| 보조 | Lombok · Spring Data JPA Auditing |

---

## 2. Entity 목록 및 분류

### 2.1 Entity 분류 매트릭스

| Entity | 분류 | 역할 | 담당 |
|---|---|---|---|
| `User` | 핵심 | 회원 (판매자·구매자·관리자) | BE-A |
| `Category` | 보조 | 상품 분류 | BE-A |
| `Product` | 핵심 | 판매 상품 | BE-A |
| `ProductImage` | 보조 | 상품 사진 (1:N) | BE-A |
| `Wish` | **조인** | 회원↔상품 다대다 | BE-A |
| `Transaction` | 핵심 | 거래 (상품과 1:1) | BE-A |
| `Dispute` | 핵심 | 분쟁 (거래와 1:1) | BE-A |
| `DisputeFile` | 보조 | 증빙 파일 (1:N) | BE-A |

### 2.2 Entity 상속 구조

```
BaseEntity (@MappedSuperclass)
  └─ createdAt : OffsetDateTime  (@CreatedDate)
       ↑ 상속: User · Product · Wish · Transaction · Dispute
       ✗ 미상속: Category · ProductImage · DisputeFile (생성 시각이 업무상 무의미)
```

---

## 3. 공통 설계 규칙

### 3.1 네이밍 규칙

| 대상 | 규칙 | 예 |
|---|---|---|
| Entity 클래스 | 단수 PascalCase | `Product` |
| 테이블 | 복수 snake_case | `products` |
| 필드 | camelCase | `priceKrw` |
| 컬럼 | snake_case | `price_krw` |
| 고유 제약 | `uk_<테이블>_<컬럼>` | `uk_wishes_user_product` |
| 인덱스 | `idx_<테이블>_<컬럼>` | `idx_products_status_created` |

> 금액 필드는 **단위를 이름에 박는다**(`priceKrw`·`amountKrw`·`balanceKrw`).
> 단위가 이름에 없으면 원인지 만원인지로 통합일에 다툰다.

### 3.2 공통 어노테이션 규칙

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "…", uniqueConstraints = …, indexes = …)
public class X extends BaseEntity { … }
```

- `@Setter`를 쓰지 않는다. 상태 변경은 **의미 있는 메서드**로만 한다
  (`confirm()` · `registerShipping()` · `reject(reason)`).
- 컬렉션 필드는 `new ArrayList<>()`로 초기화해 `null`이 나갈 길을 막는다.

### 3.3 ID 생성 전략

`@GeneratedValue(strategy = GenerationType.IDENTITY)` — MariaDB `AUTO_INCREMENT`.

> **API 응답에서는 문자열로 내린다.** Java `Long`이 JavaScript 정수 한계를 넘으면
> 끝자리가 조용히 바뀐다.

---

## 4. 상세 Entity 설계

### 4.1 User

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | Long | PK, IDENTITY | |
| `email` | String(100) | not null, **unique** | 로그인 ID |
| `password` | String(100) | not null | BCrypt 해시 |
| `nickname` | String(30) | not null | |
| `role` | Role | not null, `EnumType.STRING` | MEMBER / ADMIN |
| `balanceKrw` | Long | not null | 가상 잔액 |
| `createdAt` | OffsetDateTime | not null (상속) | |

```java
public void withdraw(Long amount) { this.balanceKrw -= amount; }
public void deposit(Long amount)  { this.balanceKrw += amount; }
```

> 잔액 변경을 메서드로만 열어 두면, 변경 지점이 코드에서 검색으로 전부 찾아진다.

### 4.2 Product

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | Long | PK | |
| `seller` | User | **`@ManyToOne(LAZY)`** not null | 등록자 |
| `category` | Category | **`@ManyToOne(LAZY)`** not null | |
| `title` | String(100) | not null | |
| `description` | String(2000) | not null | |
| `priceKrw` | Long | not null | |
| `conditionGrade` | ConditionGrade | not null | S·A·B·C |
| `status` | ProductStatus | not null | 생성 시 `INSPECTING` |
| `rejectReason` | String(200) | nullable | 반려 사유 |
| `images` | List\<ProductImage\> | **`@OneToMany(mappedBy)`** | `cascade = ALL` · `orphanRemoval` · `@OrderBy("sortOrder asc")` |

```java
public void changeStatus(ProductStatus next) { this.status = next; }
public void reject(String reason) { this.status = ProductStatus.REJECTED; this.rejectReason = reason; }
public void addImage(ProductImage image) { images.add(image); image.assignTo(this); }
```

인덱스: `idx_products_status_created(status, created_at)` — 목록 조회는 항상
"판매중 + 최신순"이라 복합 인덱스가 그대로 쓰인다. `idx_products_title` — 검색용.

### 4.3 Transaction

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | Long | PK | |
| `product` | Product | **`@ManyToOne(LAZY)`** not null | 진행 중 거래 1건은 상품 상태 전이로 보장 (SPEC §2.4) |
| `buyer` | User | `@ManyToOne(LAZY)` not null | |
| `amountKrw` | Long | not null | 거래 시점 가격 (스냅샷) |
| `status` | TransactionStatus | not null | 생성 시 `PAID` |
| `courier` / `trackingNo` | String(30) | nullable | 배송 정보 |
| `confirmedAt` | OffsetDateTime | nullable | 구매확정 시각 |

> **`amountKrw`를 따로 저장하는 이유:** 상품 가격이 나중에 바뀌어도 이미 성립한 거래
> 금액은 변하면 안 된다. 참조가 아니라 **값을 복사**해 둔다.

```java
public void registerShipping(String courier, String trackingNo) { … status = SHIPPING; }
public void confirm(OffsetDateTime at) { status = CONFIRMED; confirmedAt = at; }
```

### 4.4 Wish (조인 엔티티)

| 필드 | 타입 | 제약 |
|---|---|---|
| `user` | User | `@ManyToOne(LAZY)` not null |
| `product` | Product | `@ManyToOne(LAZY)` not null |

**`uk_wishes_user_product(user_id, product_id)`** — 중복 찜을 DB가 막는다.

> `@ManyToMany`를 쓰지 않고 조인 엔티티로 푼 이유: 찜한 시각 같은 속성을 나중에 붙일 수
> 있고, 중간 테이블을 직접 조회·제약할 수 있다.

### 4.5 Dispute · DisputeFile

| Entity | 핵심 |
|---|---|
| `Dispute` | `@OneToOne` Transaction (**unique**) · reporter · reason(500) · status · adminMemo · resolvedAt · `@OneToMany` files |
| `DisputeFile` | `@ManyToOne` Dispute · storedName · originalName · sizeBytes |

---

## 5. Enum 타입 정의

| Enum | 값 | 전이 규칙 |
|---|---|---|
| `Role` | MEMBER · ADMIN | — |
| `ConditionGrade` | S · A · B · C | — |
| `ProductStatus` | INSPECTING · ON_SALE · IN_TRADE · SOLD · REJECTED | BR-P001·P002·BR-T001 |
| `TransactionStatus` | PAID · SHIPPING · CONFIRMED · DISPUTED · REFUNDED | BR-T004·BR-D001 |
| `DisputeStatus` | OPEN · RESOLVED | BR-D002 |

**모두 `@Enumerated(EnumType.STRING)`.** `ORDINAL`을 쓰면 나중에 값을 중간에 끼워 넣는
순간 기존 데이터의 의미가 통째로 바뀐다.

---

## 6. 연관관계 매핑 전략

### 6.1 매핑 규칙

| 규칙 | 적용 |
|---|---|
| 모든 연관관계는 `LAZY` | `@ManyToOne`의 기본값은 `EAGER`이므로 **명시**한다 |
| **양방향은 필요할 때만** | `Product → images`, `Dispute → files` 두 곳뿐 |
| 단방향이 기본 | `Wish → User/Product`, `Transaction → Product/Buyer`는 단방향 |

> 양방향을 남발하면 연관관계 주인 관리와 순환 참조 문제가 생긴다.
> **목록 화면이 그 컬렉션을 필요로 할 때만** 양방향으로 만들었다.

### 6.2 Cascade 옵션

| 관계 | Cascade | 이유 |
|---|---|---|
| Product → ProductImage | `ALL` + `orphanRemoval` | 사진은 상품 없이 존재할 수 없다 |
| Dispute → DisputeFile | `ALL` + `orphanRemoval` | 증빙은 분쟁에 종속된다 |
| 그 외 | **없음** | 상품을 지운다고 회원이 지워지면 안 된다 |

### 6.3 양방향 연관관계 관리

연관관계 편의 메서드를 부모 쪽에 둔다.

```java
public void addImage(ProductImage image) {
    images.add(image);
    image.assignTo(this);   // 양쪽을 한 번에 맞춘다
}
```

---

## 7. 감사(Auditing) 설정

```java
@Getter @MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
```

`@EnableJpaAuditing`을 `config/JpaAuditingConfig`에 둔다.

> 타입이 `LocalDateTime`이 아니라 **`OffsetDateTime`**인 이유: 전자는 직렬화에 오프셋이
> 없어 프론트가 브라우저 시간대로 제멋대로 해석한다.

---

## 8. 성능 최적화 전략

### 8.1 N+1 문제

**대상: `GET /api/products` (상품 목록)**

목록 12건을 조회하면 각 상품의 판매자·카테고리를 읽느라 추가 쿼리가 발생한다.

| 구분 | 방식 |
|---|---|
| to-one (seller · category) | `fetch join` 또는 `@EntityGraph` |
| 컬렉션 (images) | `hibernate.default_batch_fetch_size: 100` |

> **컬렉션에 `fetch join` + 페이징을 같이 쓰지 않는다.** 하이버네이트가 페이징을 메모리에서
> 처리해(`HHH90003004`) 전체 행을 다 읽어 온다.

`[D7] 개선 전 쿼리 로그 캡처 — 첨부`
`[D7] 개선 후 쿼리 로그 캡처 — 첨부`
`[D7] 쿼리 수: 개선 전 N건 → 개선 후 N건`

### 8.2 쿼리 최적화

- `spring.jpa.open-in-view: false` — 컨트롤러에서 지연 로딩이 열리는 것을 막는다.
  기본값(`true`)이면 로컬에서는 멀쩡하고 부하가 걸릴 때만 느려진다.
- 목록 정렬·필터 조합에 맞춘 복합 인덱스 `(status, created_at)`.

---

## 9. 검증 및 제약조건

### 9.1 Bean Validation

| 도메인 규칙 | 애노테이션 | 에러 코드 |
|---|---|---|
| 이메일 형식 | `@Email` `@NotBlank` | `VALIDATION_ERROR` |
| 비밀번호 길이 | `@Size(min=8)` | `VALIDATION_ERROR` |
| 상품명 필수·길이 | `@NotBlank` `@Size(max=100)` | `VALIDATION_ERROR` |
| 가격 양수 | `@Positive` | `VALIDATION_ERROR` |
| 송장번호 형식 | `@Pattern(regexp="\\d{8,20}")` | `VALIDATION_ERROR` |
| 분쟁 사유 길이 | `@Size(min=10, max=500)` | `VALIDATION_ERROR` |

> 검증은 **DTO에** 건다. 엔티티에 걸면 저장 직전에야 터진다.

### 9.2 데이터베이스 제약조건 (Table 설계서)

#### users

| 컬럼 | 타입 | NULL | 기본값 | 설명 |
|---|---|---|---|---|
| id | BIGINT AI | N | — | PK |
| email | VARCHAR(100) | N | — | `uk_users_email` |
| password | VARCHAR(100) | N | — | BCrypt 해시 |
| nickname | VARCHAR(30) | N | — | |
| role | ENUM | N | — | MEMBER/ADMIN |
| balance_krw | BIGINT | N | — | 가상 잔액 |
| created_at | DATETIME(6) | N | — | |

#### products

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| id | BIGINT AI | N | PK |
| seller_id | BIGINT | N | FK → users |
| category_id | BIGINT | N | FK → categories |
| title | VARCHAR(100) | N | |
| description | VARCHAR(2000) | N | |
| price_krw | BIGINT | N | |
| condition_grade | ENUM | N | S/A/B/C |
| status | ENUM | N | 5종 |
| reject_reason | VARCHAR(200) | Y | |
| created_at | DATETIME(6) | N | |
| updated_at | DATETIME(6) | N | `@LastModifiedDate` — Product에만 둔다(상세 응답 `updatedAt`) |

#### transactions

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| id | BIGINT AI | N | PK |
| product_id | BIGINT | N | FK → products · **`idx_transactions_product`** |
| buyer_id | BIGINT | N | FK → users |
| amount_krw | BIGINT | N | 거래 시점 금액 |
| status | ENUM | N | 5종 |
| courier / tracking_no | VARCHAR(30) | Y | |
| confirmed_at | DATETIME(6) | Y | |
| created_at | DATETIME(6) | N | |

#### categories

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| id | BIGINT AI | N | PK |
| name | VARCHAR(30) | N | `uk_categories_name` |

#### product_images

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| id | BIGINT AI | N | PK |
| product_id | BIGINT | N | FK → products (cascade ALL · orphanRemoval) |
| stored_name | VARCHAR(200) | N | 서버가 새로 만든 파일명 |
| original_name | VARCHAR(200) | N | 업로드 당시 이름 (표시용) |
| sort_order | INT | N | 0부터 · 0번이 썸네일 |

#### wishes

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| id | BIGINT AI | N | PK |
| user_id | BIGINT | N | FK → users |
| product_id | BIGINT | N | FK → products |
| created_at | DATETIME(6) | N | |

`uk_wishes_user_product(user_id, product_id)` — 같은 상품을 두 번 찜할 수 없다.

#### disputes

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| id | BIGINT AI | N | PK |
| transaction_id | BIGINT | N | FK → transactions · **`uk_disputes_transaction`** (거래당 분쟁 1건) |
| reporter_id | BIGINT | N | FK → users (신고한 구매자) |
| reason | VARCHAR(500) | N | |
| status | ENUM | N | OPEN / RESOLVED |
| admin_memo | VARCHAR(500) | Y | 관리자 처리 메모 |
| resolved_at | DATETIME(6) | Y | |
| created_at | DATETIME(6) | N | |

#### dispute_files

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| id | BIGINT AI | N | PK |
| dispute_id | BIGINT | N | FK → disputes (cascade ALL · orphanRemoval) |
| stored_name | VARCHAR(200) | N | |
| original_name | VARCHAR(200) | N | 다운로드 파일명 |
| size_bytes | BIGINT | N | |

#### 제약조건 요약

| 종류 | 이름 | 대상 | 업무 규칙 |
|---|---|---|---|
| UNIQUE | `uk_users_email` | users(email) | BR-M001 |
| UNIQUE | `uk_categories_name` | categories(name) | — |
| UNIQUE | **`uk_wishes_user_product`** | wishes(user_id, product_id) | BR-W001 |
| UNIQUE | **`uk_disputes_transaction`** | disputes(transaction_id) | BR-D002 |
| INDEX | `idx_products_status_created` | products(status, created_at) | 목록 조회 |
| INDEX | `idx_products_title` | products(title) | 검색 |
| INDEX | **`idx_transactions_product`** | transactions(product_id) | 상품별 거래 조회 (유니크 아님 · SPEC §2.4) |
| FK | 10개 | — | 참조 무결성 |

#### 초기 데이터 (`data.sql`)

| 테이블 | 건수 | 비고 |
|---|---|---|
| users | 4 | 관리자 1 · 회원 3 (**BCrypt 해시**) · seller1 252만원(구매확정 1건 입금 반영) · buyer1 200만원 · buyer2 30만원 |
| categories | 5 | 노트북·스마트폰·태블릿·이어폰·기타 |
| products | 22 | 판매중 15 · 검수대기 3 · 거래중 3 · 거래완료 1 |
| transactions | 4 | **결제완료·배송중·구매확정·분쟁 각 1** — 거래 상세 화면의 상태별 액션을 시드만으로 확인할 수 있게 한다 |
| disputes | 1 | OPEN — 관리자 분쟁 화면이 본다 |
| wishes | 2 | |

> **초기 데이터에 id를 명시하지 않는다.** 명시하면 AUTO_INCREMENT 시퀀스와 어긋나
> 시연 중 첫 등록에서 중복 키 오류가 난다.

---

## 10. 테스트 전략

- **계약 검증 스크립트**가 Repository·Service 동작을 API 레벨에서 간접 검증한다
  (`node seams/check-api.mjs` — 10개 검사).
- `[D7] @DataJpaTest — 고유 제약 위반 · 연관관계 매핑 확인 (여유 시)`

---

## 11. 성능 모니터링

```yaml
# application.yml
spring:
  jpa:
    open-in-view: false
    # default_batch_fetch_size 는 T-019(N+1 튜닝)에서 넣는다 — 미리 넣으면 '개선 전' 로그를 못 찍는다

# application-local.yml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.bind: trace
```

§8.1의 개선 전후 쿼리 로그는 이 설정으로 수집한다.
