-- 실험 1: 좋아요 INSERT 한 번만 하고, 이 트랜잭션이 잡은 락을 본다 (FK 검사 켜짐: 기본값)
BEGIN;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9002);
SELECT object_name AS `table`, index_name, lock_type, lock_mode, lock_status, lock_data
  FROM performance_schema.data_locks
 WHERE thread_id = PS_CURRENT_THREAD_ID() ORDER BY object_name, lock_type;
ROLLBACK;

-- 대조군: 같은 INSERT를 FK 검사를 끄고 한다
SET SESSION foreign_key_checks = 0;
BEGIN;
INSERT INTO product_like (product_id, user_id) VALUES (9001, 9002);
SELECT object_name AS `table`, index_name, lock_type, lock_mode, lock_status, lock_data
  FROM performance_schema.data_locks
 WHERE thread_id = PS_CURRENT_THREAD_ID() ORDER BY object_name, lock_type;
ROLLBACK;
SET SESSION foreign_key_checks = 1;
