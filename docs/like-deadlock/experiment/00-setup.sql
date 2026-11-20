DELETE FROM product_like WHERE product_id = 9001;
DELETE FROM product WHERE id = 9001;
DELETE FROM users WHERE id IN (9001, 9002, 9003);
INSERT INTO users (id, department, email, name, password, status, student_id, trust_score) VALUES
 (9001, 'd', 'seller@exp.test', 'seller', 'x', 'ACTIVE', 'S9001', 0),
 (9002, 'd', 'liker-a@exp.test', 'likerA', 'x', 'ACTIVE', 'S9002', 0),
 (9003, 'd', 'liker-b@exp.test', 'likerB', 'x', 'ACTIVE', 'S9003', 0);
INSERT INTO product (id, category, like_count, price, title, view_count, user_id, status, version)
 VALUES (9001, 'c', 0, 1000, 'exp product', 0, 9001, 'SELLING', 0);
