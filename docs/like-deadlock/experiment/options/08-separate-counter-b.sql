SELECT SLEEP(0.5);
BEGIN;
SELECT '08-separate-counter-b: step1' AS step, NOW(6) AS at;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9003);
SELECT SLEEP(1);
SELECT '08-separate-counter-b: step2' AS step, NOW(6) AS at;
UPDATE product_like_count_exp SET like_count = like_count + 1 WHERE product_id = 9001;
SELECT '08-separate-counter-b: 성공' AS step, NOW(6) AS at;
COMMIT;
