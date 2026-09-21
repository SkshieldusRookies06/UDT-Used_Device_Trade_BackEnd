import { createServer } from "node:http";

const now = () => new Date().toLocaleString("sv", { timeZone: "Asia/Seoul" }).replace(" ", "T") + "+09:00";
const ok = (data, message) => JSON.stringify({ success: true, data, message, timestamp: now() });
const err = (statusCode, code, message) => JSON.stringify({ success: false, statusCode, code, message, timestamp: now() });

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,PATCH,DELETE,OPTIONS",
  "Access-Control-Allow-Headers": "Authorization,Content-Type",
  "Access-Control-Max-Age": "600",
};

const product = (id) => ({
  id: String(id), title: "맥북 에어 M2 13인치", priceKrw: 850000, conditionGrade: "A",
  status: "ON_SALE", categoryName: "노트북", sellerNickname: "판매왕",
  thumbnailUrl: `/api/products/${id}/images/31`, wishCount: 3, createdAt: now(),
});

const ME = { id: "2", email: "buyer1@udt.test", nickname: "구매자", role: "MEMBER", balanceKrw: 2000000 };

const txn = (id, status, extra = {}) => ({
  id: String(id), productId: "20", productTitle: "아이맥 24 M1",
  buyerId: "2", sellerId: "5", buyerNickname: "구매자", sellerNickname: "판매왕",
  amountKrw: 1290000, status, courier: null, trackingNo: null,
  createdAt: now(), confirmedAt: null, ...extra,
});

const TXNS = {
  1: txn(1, "PAID"),
  2: txn(2, "SHIPPING", { courier: "CJ대한통운", trackingNo: "123456789012" }),
  3: txn(3, "CONFIRMED", { courier: "CJ대한통운", trackingNo: "123456789012", confirmedAt: now() }),
  4: txn(4, "DISPUTED", { courier: "CJ대한통운", trackingNo: "123456789012" }),
};

const dispute = (txId) => ({
  id: "1", transactionId: String(txId), status: "OPEN",
  reason: "수령한 제품 액정에 멍이 있습니다.",
  files: [{ id: "1", originalName: "evidence.jpg" }], createdAt: now(),
});

// 1x1 투명 PNG — 목 단계에서 <img> 가 깨지지 않게 한다
const PIXEL = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==", "base64");

const emptyPage = { content: [], page: { number: 0, size: 12, totalElements: 0, totalPages: 0, first: true, last: true, numberOfElements: 0 } };
const pageOf = (content) => ({ content, page: { number: 0, size: 12, totalElements: content.length, totalPages: content.length ? 1 : 0, first: true, last: true, numberOfElements: content.length } });

const wished = new Set();

createServer(async (req, res) => {
  const url = new URL(req.url, "http://x");
  const p = url.pathname;
  const m = req.method;
  const send = (status, body) => { res.writeHead(status, { "Content-Type": "application/json", ...CORS }); res.end(body); };
  const auth = req.headers.authorization;
  const needAuth = () => {
    if (auth?.startsWith("Bearer ")) return false;
    send(401, err(401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다"));
    return true;
  };
  const body = async () => { let raw = ""; for await (const c of req) raw += c; return raw; };
  const json = async () => { try { return JSON.parse(await body() || "{}"); } catch { return {}; } };

  if (m === "OPTIONS") { res.writeHead(204, CORS); return res.end(); }

  // 파일 두 종 — 계약에 있는 seam 이므로 목도 응답한다
  if (/^\/api\/products\/\d+\/images\/\d+$/.test(p) && m === "GET") {
    res.writeHead(200, { "Content-Type": "image/png", ...CORS });
    return res.end(PIXEL);
  }
  if (/^\/api\/disputes\/\d+\/files\/\d+$/.test(p) && m === "GET") {
    if (needAuth()) return;
    res.writeHead(200, { "Content-Type": "image/png", "Content-Disposition": 'attachment; filename="evidence.jpg"', ...CORS });
    return res.end(PIXEL);
  }

  if (p === "/api/health") return send(200, ok({ status: "UP" }, "ok"));
  if (p === "/api/categories") return send(200, ok([{ id: "1", name: "노트북" }], "조회 완료"));

  if (p === "/api/products" && m === "GET") {
    const empty = url.searchParams.get("q") === "__none__";
    const size = Math.min(Number(url.searchParams.get("size") ?? 12), 100);
    const content = empty ? [] : [product(12)];
    return send(200, ok({
      content,
      page: { number: Number(url.searchParams.get("page") ?? 0), size, totalElements: content.length,
              totalPages: content.length ? 1 : 0, first: true, last: true, numberOfElements: content.length },
    }, "상품 목록 조회가 완료되었습니다"));
  }

  if (p === "/api/products" && m === "POST") {
    if (needAuth()) return;
    await body();
    return send(201, ok({ ...product(99), status: "INSPECTING", description: "설명", sellerId: "5", wished: false,
                          images: [{ id: "91", url: "/api/products/99/images/91", sortOrder: 0 },
                                   { id: "92", url: "/api/products/99/images/92", sortOrder: 1 }],
                          updatedAt: now() }, "상품이 등록되었습니다. 관리자 검수 후 판매중으로 전환됩니다"));
  }

  const wishM = p.match(/^\/api\/products\/(\d+)\/wishes$/);
  if (wishM && (m === "POST" || m === "DELETE")) {
    if (needAuth()) return;
    const id = wishM[1];
    if (m === "POST") {
      if (wished.has(id)) return send(409, err(409, "WISH_ALREADY_EXISTS", "이미 찜한 상품입니다"));
      wished.add(id);
      return send(201, ok({ productId: id, wished: true, wishCount: 4 }, "찜했습니다"));
    }
    if (!wished.has(id)) return send(404, err(404, "WISH_NOT_FOUND", "찜하지 않은 상품입니다"));
    wished.delete(id);
    return send(200, ok({ productId: id, wished: false, wishCount: 3 }, "찜을 해제했습니다"));
  }

  const buyM = p.match(/^\/api\/products\/(\d+)\/purchase$/);
  if (buyM && m === "POST") {
    if (needAuth()) return;
    if (buyM[1] === "13") return send(400, err(400, "INSUFFICIENT_BALANCE", "잔액이 부족합니다"));
    if (buyM[1] === "16") return send(409, err(409, "PRODUCT_NOT_ON_SALE", "판매 중인 상품이 아닙니다"));
    return send(201, ok(txn(1, "PAID"), "구매가 완료되었습니다"));
  }

  if (/^\/api\/products\/\d+$/.test(p) && m === "GET") {
    const id = p.split("/")[3];
    if (id === "999999999") return send(404, err(404, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"));
    return send(200, ok({ ...product(id), description: "설명", sellerId: "5", wished: wished.has(id),
                          images: [{ id: "31", url: `/api/products/${id}/images/31`, sortOrder: 0 }],
                          updatedAt: now() }, "조회 완료"));
  }
  if (/^\/api\/products\/[^/]+$/.test(p) && m === "GET") return send(400, err(400, "VALIDATION_ERROR", "입력값을 확인해 주세요"));

  const txM = p.match(/^\/api\/transactions\/(\d+)(\/[a-z]+)?$/);
  if (txM) {
    if (needAuth()) return;
    const t = TXNS[txM[1]];
    if (!t) return send(404, err(404, "TRANSACTION_NOT_FOUND", "거래를 찾을 수 없습니다"));
    const action = txM[2];
    if (!action && m === "GET") return send(200, ok({ ...t, dispute: t.status === "DISPUTED" ? dispute(t.id) : null }, "조회 완료"));
    if (action === "/shipping" && m === "PATCH") {
      if (t.status !== "PAID") return send(409, err(409, "INVALID_TRANSACTION_STATUS", "현재 거래 상태에서는 처리할 수 없습니다"));
      const b = await json();
      return send(200, ok({ ...t, status: "SHIPPING", courier: b.courier ?? "CJ대한통운", trackingNo: b.trackingNo ?? "123456789012" }, "송장이 등록되었습니다"));
    }
    if (action === "/confirm" && m === "PATCH") {
      if (t.status !== "SHIPPING") return send(409, err(409, "INVALID_TRANSACTION_STATUS", "현재 거래 상태에서는 처리할 수 없습니다"));
      return send(200, ok({ ...t, status: "CONFIRMED", confirmedAt: now() }, "구매가 확정되었습니다"));
    }
    if (action === "/disputes" && m === "POST") {
      if (!["PAID", "SHIPPING"].includes(t.status)) return send(409, err(409, "INVALID_TRANSACTION_STATUS", "현재 거래 상태에서는 처리할 수 없습니다"));
      await body();
      return send(201, ok(dispute(t.id), "분쟁이 접수되었습니다"));
    }
    return send(400, err(400, "VALIDATION_ERROR", "입력값을 확인해 주세요"));
  }

  if (p === "/api/auth/login" && m === "POST") {
    const { email, password } = await json();
    if (email === "buyer1@udt.test" && password === "Test1234!")
      return send(200, ok({ accessToken: "mock.jwt.token.value", user: { id: "2", nickname: "구매자", role: "MEMBER", balanceKrw: 2000000 } }, "로그인 성공"));
    return send(401, err(401, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다"));
  }

  if (p === "/api/auth/signup" && m === "POST") {
    const { email } = await json();
    if (email === "buyer1@udt.test") return send(409, err(409, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다"));
    return send(201, ok({ id: "9", email: email ?? "new@udt.test", nickname: "신규회원", role: "MEMBER", balanceKrw: 0 }, "회원가입이 완료되었습니다"));
  }

  if (p === "/api/me" || p.startsWith("/api/me/")) {
    if (needAuth()) return;
    if (p === "/api/me") return send(200, ok(ME, "조회 완료"));
    if (p === "/api/me/products") return send(200, ok(pageOf([product(12), { ...product(16), status: "INSPECTING" }]), "조회 완료"));
    if (p === "/api/me/wishes") return send(200, ok(pageOf([product(12)]), "조회 완료"));
    if (p === "/api/me/transactions") {
      const role = url.searchParams.get("role");
      if (role && !["buyer", "seller"].includes(role))
        return send(400, err(400, "VALIDATION_ERROR", "입력값을 확인해 주세요"));
      return send(200, ok(pageOf([TXNS[1], TXNS[2], TXNS[3], TXNS[4]]), "조회 완료"));
    }
    return send(200, ok(emptyPage, "조회 완료"));
  }

  send(404, err(404, "PRODUCT_NOT_FOUND", `목 서버가 구현하지 않은 경로입니다: ${m} ${p}`));
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
