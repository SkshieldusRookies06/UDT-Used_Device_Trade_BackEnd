# React 컴포넌트 설계서 — UDT

## 문서 정보

| 항목 | 내용 |
|---|---|
| 프로젝트명 | UDT (Used Device Trade) |
| 문서 버전 | v0.1 |
| 작성일 | 2026-09-21 |
| 작성자 | (이름) |
| 최종 수정일 | 2026-09-21 |

> 정본: `../UDT-Used_Device_Trade-frontend/src/`(형제 리포) · `04-화면설계서.md`
> **D3에 골격을 작성하고, D8에 실제 코드에서 재발췌한다.**

---

## 0. 컴포넌트 설계 프로세스

### STEP 1 — 화면에서 사각형 그리기 (SCR-001 기준)

```
┌─ ProductListPage ────────────────────────┐
│ ┌─ 검색 폼 ──────────────────────────┐   │
│ │  input  +  Button                  │   │
│ └────────────────────────────────────┘   │
│ ┌─ 카드 그리드 ───────────────────────┐   │
│ │ ┌ProductCard┐ ┌ProductCard┐ …      │   │
│ │ │ StatusBadge│                      │   │
│ │ └───────────┘ └───────────┘         │   │
│ └────────────────────────────────────┘   │
│ ┌─ Pagination ───────────────────────┐   │
│ └────────────────────────────────────┘   │
└──────────────────────────────────────────┘
```

**분리 기준**: 두 화면 이상에서 반복되는가 · 독립적으로 이해되는가 · 커졌는가.
문서 기준은 100줄, 실제 추출 기준은 **200줄**을 넘을 때로 운영한다
(미리 쪼개면 오히려 추적이 어려워진다).

### STEP 2 — 컴포넌트 트리

```
App (RouterProvider)
└── Layout                          (Props: 없음 / 헤더 인라인 · authStore 구독)
    └── Outlet
        ├── ProductListPage         (State: data, loading, error / URL: q, page)
        │   ├── ProductCard         (Props: product)
        │   │   └── StatusBadge     (Props: status)
        │   ├── LoadingSpinner      (Props: label)
        │   └── Pagination          (Props: page, onChange)
        ├── ProductDetailPage       (State: product, loading, error / wishStore)
        ├── ProductNewPage          (State: form, files, submitting)
        ├── MyPage                  (State: tab, data)
        ├── TransactionDetailPage   (State: transaction, actionLoading)
        ├── LoginPage               (State: form / authStore 쓰기)
        ├── SignupPage              (State: form)
        └── NotFoundPage · ErrorPage  (라우트 `*` · errorElement)
```

보호 라우트 3개(`/products/new` · `/mypage` · `/transactions/:id`)는 `RequireAuth`(Props: children)가 감싼다.

### STEP 3 — 상태 위치 결정

| 상태 | 질문 | 결정 | 위치 |
|---|---|---|---|
| 검색어 `q` | 새로고침 후에도 유지되어야 하나? **예** | **URL 쿼리** | `useSearchParams` |
| 페이지 번호 | 뒤로가기로 돌아가야 하나? **예** | **URL 쿼리** | `useSearchParams` |
| 상품 목록 데이터 | 한 화면만 쓰나? 예 | 로컬 | `ProductListPage` |
| 로딩·에러 | 한 화면만 쓰나? 예 | 로컬 | 각 페이지 |
| 로그인 토큰·사용자 | 여러 화면이 쓰나? **예** · 새로고침 유지? **예** | **전역 + 영속화** | `authStore` |
| 찜한 상품 ID | 목록·상세·마이페이지 3화면이 공유 | **전역** | `wishStore` |
| 폼 입력값 | 그 화면만 | 로컬 | 각 페이지 |

> **검색 조건을 전역 상태에 두지 않은 이유:** URL에 두면 새로고침·뒤로가기·링크 공유가
> 공짜로 따라온다. 전역 상태에 두면 그 세 가지를 직접 구현해야 한다.

### STEP 4 — 문서 작성

아래 §1~§7.

---

## 1. 컴포넌트 개요 (대표: ProductListPage)

### 1.1 컴포넌트명
`ProductListPage` (`src/pages/ProductListPage/index.jsx`)

### 1.2 목적 및 역할
판매중인 상품을 검색 조건과 페이지에 따라 조회해 카드 그리드로 보여준다.
**네 가지 화면 상태를 모두 처리하는 기준 구현**이며, 나머지 화면은 이 구조를 따른다.

### 1.3 기능 요구사항
- 상품 목록 조회(페이징) · 상품명 검색 · 상세 이동
- 로딩·정상·빈 결과·에러 상태 표시 · 에러 시 재시도
- 검색어와 페이지를 URL에 반영

---

## 2. 컴포넌트 구조 설계

### 2.1 컴포넌트 분류

| 분류 | 컴포넌트 | 사용 화면 수 |
|---|---|---|
| 레이아웃 | `Layout` | 전체 |
| 공통 UI | `Button` | 7 |
| 공통 UI | `LoadingSpinner` | 4 |
| 공통 UI | `StatusBadge` | 4 |
| 도메인 | `ProductCard` | 2 (목록·마이페이지) |
| 라우팅 | `RequireAuth` | 3 |
| 화면 전용 | `Pagination` | 1 (목록) |

> **공통 컴포넌트의 기준은 "두 화면 이상에서 쓰이는가"다.** 한 화면만 쓰는 것은
> 그 화면 폴더 안에 둔다(`Pagination`).

### 2.2 컴포넌트 트리
§0 STEP 2 참조.

### 2.3 파일 구조

```
src/
├── App.jsx                    라우트 등록 + 오류 화면 + 404      공용
├── routes.js                  경로 문자열 상수                   공용
├── api/                       client.js · auth.js · products.js  공용
│                              transactions.js
├── store/                     authStore.js · wishStore.js        공용
├── components/                Button · Layout · LoadingSpinner   공용
│                              ProductCard · StatusBadge · RequireAuth
├── styles/                    tokens.css · global.css            공용
└── pages/
    ├── ProductListPage/       index.jsx · Pagination.jsx · *.module.css
    ├── ProductDetailPage/
    ├── ProductNewPage/
    ├── MyPage/
    ├── TransactionDetailPage/
    ├── LoginPage/ · SignupPage/
    └── NotFoundPage/ · ErrorPage/
```

**폴더가 곧 담당이다.** 화면 하나가 폴더 하나이고 담당자 한 명이다.
화면 전용 컴포넌트·스타일·훅은 전부 그 폴더 안에 둔다. `pages/` 밖은 모두 공용 파일이며
담당자가 한 명씩 지정되어 있다 — 7명이 동시에 작업해도 충돌 지점이 좁아진다.

---

## 3. Props 설계

모든 컴포넌트 파일에 `propTypes`를 선언한다.

### ProductCard

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `product.id` | string | Y | — | 상품 ID |
| `product.title` | string | Y | — | 상품명 |
| `product.priceKrw` | number | Y | — | 가격 |
| `product.status` | string | Y | — | 상태 |
| `product.categoryName` | string | N | — | |
| `product.sellerNickname` | string | N | — | |
| `product.thumbnailUrl` | string | N | `null` | 없으면 대체 표시 |

### Button

| 이름 | 타입 | 필수 | 기본값 |
|---|---|---|---|
| `children` | node | Y | — |
| `variant` | `"primary"\|"secondary"\|"danger"` | N | `"primary"` |
| `type` | `"button"\|"submit"` | N | `"button"` |
| `disabled` | bool | N | `false` |
| `onClick` | func | N | — |

### StatusBadge / LoadingSpinner / Pagination / RequireAuth

| 컴포넌트 | Props |
|---|---|
| `StatusBadge` | `status: string` (필수) |
| `LoadingSpinner` | `label: string` (기본 "불러오는 중…") |
| `Pagination` | `page: {number, totalPages, first, last}` · `onChange: func` |
| `RequireAuth` | `children: node` |

---

## 4. 상태 관리 설계

### 4.1 상태 위치 결정 근거
§0 STEP 3 표.

### 4.2 로컬 상태

| 화면 | 상태 |
|---|---|
| ProductListPage | `data` · `loading` · `error` · `keyword`(입력 중) · `reloadKey`(다시 시도용) |
| ProductDetailPage | `product` · `loading` · `error` |
| ProductNewPage | `form` · `files` · `submitting` · `fieldErrors` |
| MyPage | `data` · `loading` · `error` (탭은 URL `?tab=`) |
| TransactionDetailPage | `transaction` · `actionLoading` |

> `loading`·`error`를 화면마다 두는 이유: 화면 상태 계약의 4가지와 1:1로 대응시키기
> 위해서다. 둘 중 하나라도 없으면 "빈 결과"와 "로딩"이 같은 화면이 된다.

### 4.3 전역 상태 (Zustand) — 스토어 2개

**`authStore`** — 인증 상태 (새로고침 유지)

```js
{ accessToken, user, setAuth(token, user), setUser(user), clear() }
// persist 미들웨어로 localStorage에 저장 — 새로고침해도 로그인이 유지된다
```

구독 컴포넌트: `Layout`(헤더 표시) · `RequireAuth`(보호 라우트) · `LoginPage`(쓰기) ·
`api/client.js`(요청 헤더 · 401 처리)

**`wishStore`** — 찜한 상품 ID 목록

```js
{ wishedIds, setWishedIds(ids), add(id), remove(id), clear() }
```

구독 컴포넌트: `ProductListPage` · `ProductDetailPage` · `MyPage`

> **스토어를 2개로 제한한 이유:** 전역 상태는 담당자가 불분명한 공용 파일을 늘린다.
> 여러 화면이 실제로 공유하는 것만 올리고, 나머지는 URL과 로컬 상태로 해결했다.

---

## 5. API 연동 설계

### 5.1 사용하는 API 목록

| 화면 | 함수 | 엔드포인트 |
|---|---|---|
| SCR-001 | `fetchProducts(params)` | `GET /api/products` |
| SCR-002 | `fetchProduct(id)` · `addWish`/`removeWish` · `purchase` | `GET /api/products/{id}` 외 |
| SCR-003 | `fetchCategories()` · `createProduct(product, images)` | `POST /api/products` |
| SCR-004 | `fetchMyProducts` · `fetchMyTransactions` · `fetchMyWishes` | `/api/me/**` |
| SCR-005 | `fetchTransaction` · `registerShipping` · `confirmTransaction` · `openDispute` | `/api/transactions/**` |
| SCR-006/7 | `login` · `signup` · `fetchMe` | `/api/auth/**` · `/api/me` |

**컴포넌트에서 `axios`·`fetch`를 직접 호출하지 않는다.** 전부 `src/api/`를 거친다 —
계약이 바뀌면 고칠 곳이 한 곳이 된다.

### 5.2 Request / Response 예시

```js
const data = await fetchProducts({ q: "맥북", page: 0, size: 12 });
// data = { content: [...], page: { number, size, totalElements, ... } }
```

인터셉터가 공통 봉투를 벗기므로 **화면은 알맹이(`data`)만** 본다.

### 5.3 에러 처리 방식

```js
client.interceptors.response.use(
  (res) => (res.config?.responseType === "blob" ? res.data : res.data?.data),                      // 봉투를 한 곳에서 벗긴다
  (error) => {
    if (401 && !url.startsWith("/api/auth/")) {   // 로그인 API는 제외
      authStore.clear();
      location.assign("/login");
    }
    return Promise.reject({ code, message, fields, status });
  }
);
```

- 화면은 `error.message`를 그대로 보여준다 — **서버와 화면의 문구가 두 벌이 되지 않는다.**
- 검증 오류는 `error.fields[]`를 각 입력 아래에 표시한다.
- `/api/auth/**`를 401 처리에서 제외하지 않으면 로그인 실패가 로그인 화면 이동을
  유발해 무한 리다이렉트가 된다.

---

## 6. 컴포넌트 구현 골격

```jsx
export default function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const q = searchParams.get("q") ?? "";
  const page = Number(searchParams.get("page") ?? 0);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);   // 다시 시도 버튼이 +1

  useEffect(() => {
    let alive = true;                       // 언마운트 후 setState 방지
    setLoading(true); setError(null);
    fetchProducts({ q: q || undefined, page, size: 12 })
      .then((res) => alive && setData(res))
      .catch((e) => alive && setError(e))
      .finally(() => alive && setLoading(false));
    return () => { alive = false; };
  }, [q, page, reloadKey]);

  if (loading) return <LoadingSpinner />;
  if (error)   return <ErrorState message={error.message} onRetry={…} />;
  if (data.content.length === 0) return <EmptyState />;
  return <Grid items={data.content} />;
}
```

네 상태가 코드에서 그대로 네 갈래로 보인다. 나머지 화면도 같은 골격을 따른다.

`[D8] 각 화면 실제 구현 발췌로 교체`

---

## 7. 성능 최적화

| 항목 | 적용 |
|---|---|
| 라우트 단위 코드 분할 | `[D7] React.lazy + Suspense 적용 검토` |
| 이미지 | `object-fit: cover` + 고정 비율로 레이아웃 흔들림 방지 |
| 불필요한 리렌더 | 전역 상태를 2개로 제한해 구독 범위를 좁힘 |

---

## 8. 작성 체크리스트

- [x] 컴포넌트 트리가 그려져 있다
- [x] 상태 위치 결정 근거가 표로 정리되어 있다
- [x] 모든 컴포넌트에 `propTypes`가 선언되어 있다
- [x] 공통 컴포넌트의 사용 화면이 2개 이상이다
- [x] API 호출이 한 레이어에 모여 있다
- [x] 에러 처리 방식이 한 곳에 정의되어 있다
- [ ] 전 화면 구현 완료 후 재발췌 (D8)
