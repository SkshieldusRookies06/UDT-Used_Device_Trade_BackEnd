-- 시드 · id를 명시하지 않는다 (시퀀스 충돌 → 첫 POST duplicate key)
-- 비밀번호: 회원 Test1234! / 관리자 Admin1234!  (BCrypt strength 10)
--
-- 잔액은 아래 거래 4건이 이미 반영된 '현재 잔액'이다 (SPEC §2.4 BR-03·BR-05).
--   buyer1  5,380,000 최초 → 거래 4건(1,290,000 + 890,000 + 520,000 + 680,000) 차감 → 2,000,000
--   seller1 2,000,000 최초 → 구매확정된 1건(520,000)만 입금       → 2,520,000
--   구매확정되지 않은 3건(2,860,000)은 에스크로에 묶여 있다 — 이게 이 서비스의 핵심이다
-- 상품 이미지는 시드에 넣지 않는다. 따라서 시드 상품의 thumbnailUrl 은 전부 null 이고,
-- 화면의 '이미지 없음' 경로가 첫날부터 실제로 검증된다. 실제 이미지는 T-007 등록으로 생긴다.

INSERT INTO users (email, password, nickname, role, balance_krw, created_at) VALUES
  ('admin@udt.test',  '$2a$10$7B30C.Z.4zQsuf7b9xrG3uwsv6exRv5C2CUBwDhXBIKVaTb7IJ3Uq',  '관리자',   'ADMIN',  0,       NOW()),
  ('seller1@udt.test','$2a$10$i01YpDmayKZMyujtk7a05edx5zza.eJGkemA.3JgCdyR3qtmdHSJO', '판매왕',   'MEMBER', 2520000, NOW()),
  ('buyer1@udt.test', '$2a$10$i01YpDmayKZMyujtk7a05edx5zza.eJGkemA.3JgCdyR3qtmdHSJO', '구매자',   'MEMBER', 2000000, NOW()),
  ('buyer2@udt.test', '$2a$10$i01YpDmayKZMyujtk7a05edx5zza.eJGkemA.3JgCdyR3qtmdHSJO', '알뜰구매', 'MEMBER', 300000,  NOW());

INSERT INTO categories (name) VALUES
  ('노트북'), ('스마트폰'), ('태블릿'), ('이어폰'), ('기타');

INSERT INTO products (seller_id, category_id, title, description, price_krw, condition_grade, status, created_at)
SELECT u.id, c.id, t.title, t.description, t.price_krw, t.condition_grade, t.status, NOW()
FROM (
  SELECT '맥북 에어 M2 13인치'        AS title, '2023년 구매, 배터리 사이클 120회' AS description, 850000 AS price_krw, 'A' AS condition_grade, 'ON_SALE'    AS status, '노트북'   AS cat UNION ALL
  SELECT '갤럭시북3 프로',              '액정 무상처, 충전기 포함',                   1100000,        'S',                  'ON_SALE',              '노트북'   UNION ALL
  SELECT 'LG 그램 16 2024',            '가벼움, 사무용으로만 사용',                    980000,        'A',                  'ON_SALE',              '노트북'   UNION ALL
  SELECT '아이폰 15 128GB',            '자급제, 애플케어 잔여',                        890000,        'S',                  'ON_SALE',              '스마트폰' UNION ALL
  SELECT '갤럭시 S24 256GB',           '케이스 끼고 사용, 잔기스',                      760000,        'A',                  'ON_SALE',              '스마트폰' UNION ALL
  SELECT '픽셀 8 프로',                 '직구 제품, 한글 지원',                          620000,        'B',                  'ON_SALE',              '스마트폰' UNION ALL
  SELECT '아이패드 프로 11 M2',        '펜슬 2세대 포함',                              950000,        'A',                  'ON_SALE',              '태블릿'   UNION ALL
  SELECT '갤럭시탭 S9',                 '풀박스',                                        700000,        'S',                  'ON_SALE',              '태블릿'   UNION ALL
  SELECT '에어팟 프로 2세대',           '2024년 구매',                                   180000,        'A',                  'ON_SALE',              '이어폰'   UNION ALL
  SELECT '버즈3 프로',                  '미개봉',                                        190000,        'S',                  'ON_SALE',              '이어폰'   UNION ALL
  SELECT '소니 WH-1000XM5',            '헤드폰, 파우치 포함',                           320000,        'A',                  'ON_SALE',              '이어폰'   UNION ALL
  SELECT '닌텐도 스위치 OLED',         '조이콘 쏠림 없음',                              280000,        'B',                  'ON_SALE',              '기타'     UNION ALL
  SELECT '애플워치 SE 2세대',          '밴드 2종 포함',                                 250000,        'A',                  'ON_SALE',              '기타'     UNION ALL
  SELECT '키크론 K8 프로',              '적축, 윤활 완료',                               95000,         'A',                  'ON_SALE',              '기타'     UNION ALL
  SELECT 'LG 울트라기어 27GP850',      '모니터, 무결점',                                350000,        'A',                  'ON_SALE',              '기타'     UNION ALL
  SELECT '맥북 프로 14 M3',            '검수 요청 중',                                 1890000,        'S',                  'INSPECTING',           '노트북'   UNION ALL
  SELECT '아이폰 14 프로',              '검수 요청 중',                                  790000,        'A',                  'INSPECTING',           '스마트폰' UNION ALL
  SELECT '갤럭시 워치6',                '검수 요청 중',                                  210000,        'B',                  'INSPECTING',           '기타'
) t
JOIN users u ON u.email = 'seller1@udt.test'
JOIN categories c ON c.name = t.cat;

-- 거래 4건을 위한 상품 4개 (거래 상태별 화면을 시드만으로 볼 수 있게 한다 · SCR-005)
INSERT INTO products (seller_id, category_id, title, description, price_krw, condition_grade, status, created_at)
SELECT u.id, c.id, t.title, t.description, t.price_krw, 'A', t.status, NOW()
FROM (
  SELECT '아이맥 24 M1'      AS title, '가상결제완료 상태 거래' AS description, 1290000 AS price_krw, 'IN_TRADE' AS status UNION ALL
  SELECT '델 XPS 13 2023',       '배송중 상태 거래',              890000,           'IN_TRADE'            UNION ALL
  SELECT '아이패드 미니 6',      '구매확정된 거래',               520000,           'SOLD'                UNION ALL
  SELECT '갤럭시 Z플립5',        '분쟁 진행 중인 거래',           680000,           'IN_TRADE'
) t
JOIN users u ON u.email = 'seller1@udt.test'
JOIN categories c ON c.name = '기타';

INSERT INTO transactions (product_id, buyer_id, amount_krw, status, courier, tracking_no, confirmed_at, created_at)
SELECT p.id, u.id, p.price_krw, 'PAID', NULL, NULL, NULL, NOW()
FROM products p JOIN users u ON u.email = 'buyer1@udt.test' WHERE p.title = '아이맥 24 M1';

INSERT INTO transactions (product_id, buyer_id, amount_krw, status, courier, tracking_no, confirmed_at, created_at)
SELECT p.id, u.id, p.price_krw, 'SHIPPING', 'CJ대한통운', '123456789012', NULL, NOW()
FROM products p JOIN users u ON u.email = 'buyer1@udt.test' WHERE p.title = '델 XPS 13 2023';

INSERT INTO transactions (product_id, buyer_id, amount_krw, status, courier, tracking_no, confirmed_at, created_at)
SELECT p.id, u.id, p.price_krw, 'CONFIRMED', 'CJ대한통운', '123456789013', NOW(), NOW()
FROM products p JOIN users u ON u.email = 'buyer1@udt.test' WHERE p.title = '아이패드 미니 6';

INSERT INTO transactions (product_id, buyer_id, amount_krw, status, courier, tracking_no, confirmed_at, created_at)
SELECT p.id, u.id, p.price_krw, 'DISPUTED', 'CJ대한통운', '123456789014', NULL, NOW()
FROM products p JOIN users u ON u.email = 'buyer1@udt.test' WHERE p.title = '갤럭시 Z플립5';

-- 분쟁 1건 (OPEN) — 관리자 화면(T-018)과 거래 상세 분쟁 표시(T-017)가 이걸 본다
INSERT INTO disputes (transaction_id, reporter_id, reason, status, admin_memo, resolved_at, created_at)
SELECT tx.id, tx.buyer_id, '수령한 제품 액정에 멍이 있습니다. 사진 첨부합니다.', 'OPEN', NULL, NULL, NOW()
FROM transactions tx JOIN products p ON p.id = tx.product_id
WHERE p.title = '갤럭시 Z플립5';

INSERT INTO wishes (user_id, product_id, created_at)
SELECT u.id, p.id, NOW()
FROM users u JOIN products p ON p.title IN ('맥북 에어 M2 13인치', '에어팟 프로 2세대')
WHERE u.email = 'buyer1@udt.test';
