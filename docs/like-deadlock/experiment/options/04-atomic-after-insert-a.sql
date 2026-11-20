BEGIN;
SELECT '04-atomic-after-insert-a: step1' AS step, NOW(6) AS at;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9002);
SELECT SLEEP(1);
SELECT '04-atomic-after-insert-a: step2' AS step, NOW(6) AS at;
UPDATE product SET like_count = like_count + 1 WHERE id = 9001;
SELECT '04-atomic-after-insert-a: 성공' AS step, NOW(6) AS at;
COMMIT;
