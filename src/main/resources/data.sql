-- 시드 · id를 명시하지 않는다 (시퀀스 충돌 → 첫 POST duplicate key)
-- 비밀번호: 회원 Test1234! / 관리자 Admin1234!  (BCrypt strength 10)

INSERT INTO users (email, password, nickname, role, balance_krw, created_at) VALUES
  ('admin@udt.test',  '$2a$10$7B30C.Z.4zQsuf7b9xrG3uwsv6exRv5C2CUBwDhXBIKVaTb7IJ3Uq',  '관리자',   'ADMIN',  0,       NOW()),
  ('seller1@udt.test','$2a$10$i01YpDmayKZMyujtk7a05edx5zza.eJGkemA.3JgCdyR3qtmdHSJO', '판매왕',   'MEMBER', 2000000, NOW()),
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

INSERT INTO products (seller_id, category_id, title, description, price_krw, condition_grade, status, created_at)
SELECT u.id, c.id, '아이맥 24 M1', '거래 진행 중인 상품', 1290000, 'A', 'IN_TRADE', NOW()
FROM users u JOIN categories c ON c.name = '기타' WHERE u.email = 'seller1@udt.test';

INSERT INTO transactions (product_id, buyer_id, amount_krw, status, created_at)
SELECT p.id, u.id, p.price_krw, 'PAID', NOW()
FROM products p JOIN users u ON u.email = 'buyer1@udt.test'
WHERE p.title = '아이맥 24 M1';

INSERT INTO wishes (user_id, product_id, created_at)
SELECT u.id, p.id, NOW()
FROM users u JOIN products p ON p.title IN ('맥북 에어 M2 13인치', '에어팟 프로 2세대')
WHERE u.email = 'buyer1@udt.test';
