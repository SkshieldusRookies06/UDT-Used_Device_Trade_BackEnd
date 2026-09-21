import { createServer } from "node:http";

const now = () => new Date().toLocaleString("sv", { timeZone: "Asia/Seoul" }).replace(" ", "T") + "+09:00";
const ok = (data, message) => JSON.stringify({ success: true, data, message, timestamp: now() });
const err = (statusCode, code, message) => JSON.stringify({ success: false, statusCode, code, message, timestamp: now() });

const product = (id) => ({
  id: String(id), title: "맥북 에어 M2 13인치", priceKrw: 850000, conditionGrade: "A",
  status: "ON_SALE", categoryName: "노트북", sellerNickname: "판매왕",
  thumbnailUrl: `/api/products/${id}/images/31`, wishCount: 3, createdAt: now(),
});

createServer(async (req, res) => {
  const url = new URL(req.url, "http://x");
  const p = url.pathname;
  const send = (status, body) => { res.writeHead(status, { "Content-Type": "application/json" }); res.end(body); };
  const auth = req.headers.authorization;

  if (p === "/api/health") return send(200, ok({ status: "UP" }, "ok"));
  if (p === "/api/categories") return send(200, ok([{ id: "1", name: "노트북" }], "조회 완료"));

  if (p === "/api/products") {
    const empty = url.searchParams.get("q") === "__none__";
    const size = Math.min(Number(url.searchParams.get("size") ?? 12), 100);
    const content = empty ? [] : [product(12)];
    return send(200, ok({
      content,
      page: { number: Number(url.searchParams.get("page") ?? 0), size, totalElements: content.length,
              totalPages: content.length ? 1 : 0, first: true, last: true, numberOfElements: content.length },
    }, "상품 목록 조회가 완료되었습니다"));
  }

  if (/^\/api\/products\/\d+$/.test(p)) {
    const id = p.split("/")[3];
    if (id === "999999999") return send(404, err(404, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"));
    return send(200, ok({ ...product(id), description: "설명", sellerId: "5", wished: false,
                          images: [{ id: "31", url: `/api/products/${id}/images/31`, sortOrder: 0 }],
                          updatedAt: now() }, "조회 완료"));
  }
  if (/^\/api\/products\/[^/]+$/.test(p)) return send(400, err(400, "VALIDATION_ERROR", "입력값을 확인해 주세요"));

  if (p === "/api/auth/login" && req.method === "POST") {
    let raw = ""; for await (const c of req) raw += c;
    const { email, password } = JSON.parse(raw || "{}");
    if (email === "buyer1@udt.test" && password === "Test1234!")
      return send(200, ok({ accessToken: "mock.jwt.token.value", user: { id: "2", nickname: "구매자", role: "MEMBER", balanceKrw: 2000000 } }, "로그인 성공"));
    return send(401, err(401, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다"));
  }

  if (p === "/api/me" || p.startsWith("/api/me/")) {
    if (!auth?.startsWith("Bearer ")) return send(401, err(401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다"));
    if (p === "/api/me") return send(200, ok({ id: "2", email: "buyer1@udt.test", nickname: "구매자", role: "MEMBER", balanceKrw: 2000000 }, "조회 완료"));
    return send(200, ok({ content: [], page: { number: 0, size: 12, totalElements: 0, totalPages: 0, first: true, last: true, numberOfElements: 0 } }, "조회 완료"));
  }

  send(404, err(404, "INTERNAL_SERVER_ERROR", "없는 경로"));
}).listen(8080)
  .on("listening", () => console.log("목 서버 기동 — http://localhost:8080 (Ctrl+C 로 종료)"))
  .on("error", (e) => {
    if (e.code === "EADDRINUSE") {
      console.error("포트 8080이 이미 사용 중입니다.");
      console.error("어제 띄운 백엔드나 목 서버가 살아 있는지 확인하고 Ctrl+C 로 종료하세요.");
      process.exit(2);
    }
    throw e;
  });
