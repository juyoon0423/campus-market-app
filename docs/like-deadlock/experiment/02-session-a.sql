-- 세션 A (사용자 9002의 좋아요). 앱의 applyToggle과 같은 순서: INSERT product_like → UPDATE product
BEGIN;
SELECT 'A: INSERT product_like' AS step, NOW(6) AS at;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9002);
SELECT SLEEP(1);
SELECT 'A: UPDATE product (B가 S 락을 쥐고 있어 대기)' AS step, NOW(6) AS at;
UPDATE product SET like_count = like_count + 1, version = version + 1 WHERE id = 9001;
SELECT 'A: UPDATE 성공' AS step, NOW(6) AS at;
COMMIT;
