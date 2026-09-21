// UDT seam 게이트 — 실행: node seams/check-api.mjs [base_url]   (리포 루트에서)
// 계약 정본: SPEC.md §4. 계약이 바뀌면 이 파일도 같은 커밋에서 바뀐다(SPEC.md §10).

const BASE = process.argv[2] ?? "http://localhost:8080";
const report = [];

async function call(path, opt = {}) {
  try {
    const r = await fetch(BASE + path, { signal: AbortSignal.timeout(5000), ...opt });
    let body;
    try { body = await r.json(); } catch { body = undefined; }
    return { status: r.status, headers: r.headers, body, down: false };
  } catch (e) {
    return { status: 0, body: undefined, down: true, msg: e.message };
  }
}

function envelope(body, errs) {
  if (body.success !== true) errs.push(`success가 true가 아님: ${JSON.stringify(body.success)}`);
  for (const k of ["data", "message", "timestamp"]) if (!(k in body)) errs.push(`봉투 필드 없음: ${k}`);
  return body.data;
}

function errorShape(body) {
  return body && body.success === false
    && typeof body.code === "string" && typeof body.statusCode === "number";
}

function checkPage(pg, errs, expectedNumber) {
  for (const k of ["number", "size", "totalElements", "totalPages", "first", "last", "numberOfElements"])
    if (!(k in pg)) errs.push(`data.page 필드 없음: ${k}`);
  if (pg.number !== expectedNumber)
    errs.push(`page.number 에코 ${JSON.stringify(pg.number)} (요청 ${expectedNumber} — 어느 쪽이 1-base로 바꿨는지 확인 · SPEC ADR-09)`);
  if (typeof pg.totalElements !== "number")
    errs.push(`totalElements 타입 ${typeof pg.totalElements} (계약: number)`);
}

function checkFields(obj, spec, errs, label) {
  for (const [k, t] of spec) {
    if (!(k in obj)) errs.push(`${label} 필드 없음: ${k}`);
    else if (obj[k] === null) errs.push(`${label} 필수 필드가 null: ${k}`);
    else if (typeof obj[k] !== t) errs.push(`${label} 타입 불일치 ${k}: ${typeof obj[k]} (계약: ${t})`);
  }
}

function checkIsoOffset(v, errs, label) {
  if (v === undefined || v === null) return;
  if (Number.isNaN(Date.parse(v))) errs.push(`${label} 파싱 불가: ${v}`);
  else if (!/(Z|[+-]\d{2}:\d{2})$/.test(v)) errs.push(`${label}에 오프셋 없음: ${v} (SPEC §0 날짜 행)`);
}

// ── GET /api/health ──────────────────────────────────────────────────────
{
  const errs = [];
  const r = await call("/api/health");
  if (r.down) errs.push(`서버 응답 없음: ${r.msg}`);
  else if (r.status !== 200) errs.push(`상태 ${r.status} (계약: 200)`);
  else if (r.body?.data?.status !== "UP" && r.body?.status !== "UP")
    errs.push(`status가 UP이 아님: ${JSON.stringify(r.body)}`);
  report.push(["GET /api/health", errs]);
}

// ── GET /api/categories ──────────────────────────────────────────────────
{
  const errs = [];
  const r = await call("/api/categories");
  if (r.down) errs.push(`서버 응답 없음: ${r.msg}`);
  else if (r.body === undefined) errs.push(`JSON 아님 (HTML? base URL·포트 확인) 상태 ${r.status}`);
  else if (r.status !== 200) errs.push(`상태 ${r.status} (계약: 200)`);
  else {
    const d = envelope(r.body, errs);
    if (!Array.isArray(d)) errs.push(`data가 배열이 아님: ${JSON.stringify(d)} (0건이어도 [])`);
    else if (d.length === 0) errs.push("카테고리가 0건 — 시드가 안 돌았다");
    else checkFields(d[0], [["id", "string"], ["name", "string"]], errs, "category");
  }
  report.push(["GET /api/categories", errs]);
}

// ── GET /api/products ────────────────────────────────────────────────────
{
  const errs = [];
  const r = await call("/api/products?page=0&size=12");
  if (r.down) errs.push(`서버 응답 없음: ${r.msg}`);
  else if (r.body === undefined) errs.push(`JSON 아님 (HTML? base URL·포트 확인) 상태 ${r.status}`);
  else if (r.status !== 200) errs.push(`상태 ${r.status} (계약: 200)`);
  else {
    const d = envelope(r.body, errs) ?? {};
    if (!Array.isArray(d.content)) errs.push("data.content가 배열이 아님 (빈 결과도 []) ");
    else if (d.content.length === 0) errs.push("content가 빈 배열 — 시드가 없거나 ON_SALE 상품이 없다");
    else {
      const it = d.content[0];
      checkFields(it, [
        ["id", "string"], ["title", "string"], ["priceKrw", "number"],
        ["conditionGrade", "string"], ["status", "string"], ["categoryName", "string"],
        ["sellerNickname", "string"], ["wishCount", "number"], ["createdAt", "string"],
      ], errs, "product");
      if (it.status !== "ON_SALE")
        errs.push(`목록에 ON_SALE 아닌 상품이 있음: ${it.status} (SPEC ADR-06)`);
      if (!["S", "A", "B", "C"].includes(it.conditionGrade))
        errs.push(`conditionGrade 값 밖: ${it.conditionGrade}`);
      if (!("thumbnailUrl" in it)) errs.push("thumbnailUrl 키 없음 (이미지 0장이면 null로 존재해야 한다)");
      checkIsoOffset(it.createdAt, errs, "createdAt");
    }
    checkPage(d.page ?? {}, errs, 0);
  }
  report.push(["GET /api/products", errs]);
}

// ── GET /api/products (빈 결과도 계약이다) ────────────────────────────────
{
  const errs = [];
  const r = await call("/api/products?q=__none__&page=0&size=12");
  if (r.down) errs.push(`서버 응답 없음: ${r.msg}`);
  else if (r.status !== 200) errs.push(`상태 ${r.status} (계약: 빈 결과도 200 · success:true)`);
  else {
    const d = envelope(r.body, errs) ?? {};
    if (!Array.isArray(d.content) || d.content.length !== 0)
      errs.push(`빈 결과가 []가 아님: ${JSON.stringify(d.content)}`);
    if (d.page?.totalElements !== 0)
      errs.push(`빈 결과의 totalElements가 0이 아님: ${JSON.stringify(d.page?.totalElements)}`);
  }
  report.push(["GET /api/products (빈 결과)", errs]);
}

// ── GET /api/products?size=101 (상한 검증) ────────────────────────────────
{
  const errs = [];
  const r = await call("/api/products?page=0&size=101");
  if (r.down) errs.push(`서버 응답 없음: ${r.msg}`);
  else if (r.status === 200) {
    const d = envelope(r.body, errs) ?? {};
    if ((d.page?.size ?? 0) > 100) errs.push(`size 상한 미적용: page.size=${d.page?.size} (계약: 최대 100)`);
  } else if (r.status !== 400) errs.push(`상태 ${r.status} (계약: 400 또는 size를 100으로 절단한 200)`);
  report.push(["GET /api/products (size 상한)", errs]);
}

// ── GET /api/products/{id} 상세 + 에러 ────────────────────────────────────
{
  const errs = [];
  const list = await call("/api/products?page=0&size=1");
  const firstId = list.body?.data?.content?.[0]?.id;

  if (firstId) {
    const r = await call(`/api/products/${firstId}`);
    if (r.status !== 200) errs.push(`상세 상태 ${r.status} (계약: 200)`);
    else {
      const d = envelope(r.body, errs) ?? {};
      if (Array.isArray(d)) errs.push("단일 조회의 data가 배열 (계약: 객체)");
      checkFields(d, [["id", "string"], ["title", "string"], ["description", "string"],
                      ["sellerId", "string"], ["wished", "boolean"]], errs, "productDetail");
      if (!Array.isArray(d.images)) errs.push(`images가 배열이 아님: ${JSON.stringify(d.images)} (0장이면 [])`);
      checkIsoOffset(d.createdAt, errs, "createdAt");
    }
  } else errs.push("상세 검사 건너뜀 — 목록이 비어 있다");

  let r = await call("/api/products/999999999");
  if (r.down) errs.push(`서버 응답 없음: ${r.msg}`);
  else {
    if (r.status !== 404) errs.push(`없는 id 상태 ${r.status} (계약: 404)`);
    if (!errorShape(r.body)) errs.push(`에러 바디가 공통 봉투 아님: ${JSON.stringify(r.body)} — @RestControllerAdvice 확인`);
    else {
      if (r.body.code !== "PRODUCT_NOT_FOUND") errs.push(`code ${r.body.code} (계약: PRODUCT_NOT_FOUND)`);
      if (r.body.statusCode !== r.status) errs.push(`statusCode(${r.body.statusCode}) ≠ HTTP 상태(${r.status})`);
    }
  }

  r = await call("/api/products/not-a-valid-id");
  if (!r.down) {
    if (r.status !== 400) errs.push(`형식 불량 id 상태 ${r.status} (계약: 400 VALIDATION_ERROR)`);
    if (!errorShape(r.body)) errs.push(`400 바디가 공통 봉투 아님: ${JSON.stringify(r.body)} — TypeMismatch를 Advice가 잡는가`);
  }
  report.push(["GET /api/products/{id} + 에러", errs]);
}

// ── POST /api/auth/login (자격 오류) ──────────────────────────────────────
{
  const errs = [];
  const r = await call("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "no-such-user@example.com", password: "wrong-password" }),
  });
  if (r.down) errs.push(`서버 응답 없음: ${r.msg}`);
  else {
    if (r.status !== 401) errs.push(`상태 ${r.status} (계약: 401 INVALID_CREDENTIALS)`);
    if (!errorShape(r.body))
      errs.push(`401 바디가 공통 봉투 아님: ${JSON.stringify(r.body)} — AuthenticationEntryPoint가 JSON을 내는가 (SPEC §7)`);
    else if (r.body.code !== "INVALID_CREDENTIALS") errs.push(`code ${r.body.code} (계약: INVALID_CREDENTIALS)`);
  }
  report.push(["POST /api/auth/login (자격 오류)", errs]);
}

// ── 보호 엔드포인트 무토큰 접근 ───────────────────────────────────────────
{
  const errs = [];
  for (const path of ["/api/me", "/api/me/transactions?role=buyer"]) {
    const r = await call(path);
    if (r.down) { errs.push(`서버 응답 없음: ${r.msg}`); break; }
    if (r.status !== 401) errs.push(`${path} 무토큰 상태 ${r.status} (계약: 401)`);
    if (r.body === undefined) errs.push(`${path} 401 응답이 JSON이 아님 — 로그인 HTML로 리다이렉트됐을 수 있다 (체인 순서 @Order 확인)`);
    else if (!errorShape(r.body)) errs.push(`${path} 401 바디가 공통 봉투 아님: ${JSON.stringify(r.body)}`);
  }
  report.push(["보호 엔드포인트 401", errs]);
}

// ── 로그인 성공 → 토큰 → 보호 엔드포인트 통과 (시드 계정) ──────────────────
{
  const errs = [];
  const SEED_EMAIL = process.env.SEED_EMAIL ?? "buyer1@udt.test";
  const SEED_PW = process.env.SEED_PW ?? "Test1234!";
  const login = await call("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: SEED_EMAIL, password: SEED_PW }),
  });
  if (login.down) errs.push(`서버 응답 없음: ${login.msg}`);
  else if (login.status !== 200) {
    errs.push(login.status === 404
      ? `로그인 엔드포인트가 없다 (404) — AuthController 미구현. T-005 전까지는 이 RED가 정상`
      : `시드 계정 로그인 실패 상태 ${login.status} — 시드 비밀번호가 BCrypt 해시인가 (SPEC §6) · 계정은 SEED_EMAIL/SEED_PW 환경변수로 바꾼다`);
  } else {
    const d = envelope(login.body, errs) ?? {};
    if (typeof d.accessToken !== "string" || d.accessToken.length < 10)
      errs.push(`accessToken 없음 또는 짧음: ${JSON.stringify(d.accessToken)}`);
    else {
      const me = await call("/api/me", { headers: { Authorization: `Bearer ${d.accessToken}` } });
      if (me.status !== 200) errs.push(`토큰으로 /api/me 접근 실패 상태 ${me.status} (계약: 200)`);
      else {
        const u = envelope(me.body, errs) ?? {};
        checkFields(u, [["id", "string"], ["email", "string"], ["nickname", "string"],
                        ["role", "string"], ["balanceKrw", "number"]], errs, "me");
      }
    }
  }
  report.push(["로그인 → 토큰 → /api/me", errs]);
}

// ── 출력 ─────────────────────────────────────────────────────────────────
const failed = report.filter(([, e]) => e.length);
for (const [name, errs] of report)
  console.log(errs.length ? `RED  ${name}\n  - ${errs.join("\n  - ")}` : `ok   ${name}`);

if (failed.length) {
  const allDown = report.every(([, e]) => e.some((m) => m.startsWith("서버 응답 없음")));
  console.log(allDown
    ? "\n→ 전부 '응답 없음'이면 코드가 아니라 기동·포트 문제다. 백엔드가 8080에 떠 있는지 먼저 본다."
    : "\n→ red는 백엔드 소유(SPEC §11). 판정 전 DevTools의 Copy as cURL로 같은 요청을 재현해 SPEC.md §4 예시와 대조한다.");
  process.exit(1);
}
console.log(`\nOK: ${BASE} 계약 준수`);
