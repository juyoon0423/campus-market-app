BEGIN;
SELECT '07-no-counter-a: step1' AS step, NOW(6) AS at;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9002);
SELECT SLEEP(1);
SELECT '07-no-counter-a: step2' AS step, NOW(6) AS at;
SELECT COUNT(*) FROM product_like WHERE product_id = 9001;
SELECT '07-no-counter-a: 성공' AS step, NOW(6) AS at;
COMMIT;
