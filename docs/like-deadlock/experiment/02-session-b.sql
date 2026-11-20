-- 세션 B (사용자 9003의 좋아요). A보다 0.5초 늦게 시작
SELECT SLEEP(0.5);
BEGIN;
SELECT 'B: INSERT product_like' AS step, NOW(6) AS at;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9003);
SELECT SLEEP(1);
SELECT 'B: UPDATE product' AS step, NOW(6) AS at;
UPDATE product SET like_count = like_count + 1, version = version + 1 WHERE id = 9001;
SELECT 'B: UPDATE 성공' AS step, NOW(6) AS at;
COMMIT;
