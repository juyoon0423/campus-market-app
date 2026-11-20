SELECT SLEEP(0.5);
BEGIN;
SELECT '06-update-first-b: step1' AS step, NOW(6) AS at;
UPDATE product SET like_count = like_count + 1 WHERE id = 9001;
SELECT SLEEP(1);
SELECT '06-update-first-b: step2' AS step, NOW(6) AS at;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9003);
SELECT '06-update-first-b: 성공' AS step, NOW(6) AS at;
COMMIT;
