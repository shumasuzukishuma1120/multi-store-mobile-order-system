-- Initial data for local development and manual verification.
-- PostgreSQL 16 / Flyway migration.

-- Stores.
INSERT INTO stores (id, name)
VALUES
    (1, '東京駅前店'),
    (2, '新宿店');

-- Users. Passwords are managed outside the application database.
INSERT INTO users (id, store_id, cognito_user_id, role, user_name)
VALUES
    (1, NULL, 'dummy-admin-sub', 'ADMIN', 'admin'),
    (2, 1, 'dummy-manager-store1-sub', 'STORE_MANAGER', 'manager_store1'),
    (3, 1, 'dummy-staff-store1-sub', 'STORE_STAFF', 'staff_store1'),
    (4, 2, 'dummy-staff-store2-sub', 'STORE_STAFF', 'staff_store2');

-- Restaurant tables.
INSERT INTO restaurant_tables (id, store_id, table_number, status, qr_token)
VALUES
    (1, 1, 'T1', 'AVAILABLE', 'qr-token-store1-table1'),
    (2, 1, 'T2', 'OCCUPIED', 'qr-token-store1-table2'),
    (3, 1, 'T3', 'PAYMENT_WAITING', 'qr-token-store1-table3'),
    (4, 1, 'T4', 'CLEANING', 'qr-token-store1-table4'),
    (5, 2, 'T1', 'AVAILABLE', 'qr-token-store2-table1');

-- Menu categories.
INSERT INTO menu_categories (id, store_id, name)
VALUES
    (1, 1, 'フード'),
    (2, 1, 'ドリンク'),
    (3, 2, 'フード');

-- Menus. Prices are tax-included JPY amounts.
INSERT INTO menus (id, menu_category_id, store_id, name, price, status)
VALUES
    (1, 1, 1, 'ハンバーガー', 900, 'AVAILABLE'),
    (2, 1, 1, 'カレー', 1000, 'SOLD_OUT'),
    (3, 1, 1, 'パスタ', 1100, 'SUSPENDED'),
    (4, 2, 1, 'コーヒー', 400, 'AVAILABLE'),
    (5, 3, 2, 'ハンバーガー', 950, 'AVAILABLE');

-- Visit sessions for local development.
-- T2: active/orderable, T3: payment waiting, T4: already paid/closed.
INSERT INTO visit_sessions (
    id,
    store_id,
    table_id,
    status,
    visit_token,
    expires_at,
    started_at,
    ended_at
)
VALUES
    (1, 1, 2, 'ACTIVE', 'visit-token-store1-table2-active', CURRENT_TIMESTAMP + INTERVAL '5 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL),
    (2, 1, 3, 'ACTIVE', 'visit-token-store1-table3-payment-waiting', CURRENT_TIMESTAMP + INTERVAL '4 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL),
    (3, 1, 4, 'CLOSED', 'visit-token-store1-table4-paid', CURRENT_TIMESTAMP + INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '30 minutes');

-- Orders.
-- Session 1: active table order.
-- Session 2: payment waiting table with one payable order and one cancelled order.
-- Session 3: already paid order.
INSERT INTO orders (
    id,
    visit_session_id,
    store_id,
    table_id,
    status,
    total_amount
)
VALUES
    (1, 1, 1, 2, 'ORDERED', 1300),
    (2, 2, 1, 3, 'ORDERED', 1300),
    (3, 2, 1, 3, 'CANCELLED', 900),
    (4, 3, 1, 4, 'COMPLETED', 900);

-- Order items.
INSERT INTO order_items (
    id,
    order_id,
    menu_id,
    cooking_status,
    quantity,
    unit_price,
    subtotal_amount
)
VALUES
    -- order 1: hamburger + coffee
    (1, 1, 1, 'WAITING', 1, 900, 900),
    (2, 1, 4, 'WAITING', 1, 400, 400),

    -- order 2: hamburger + coffee
    (3, 2, 1, 'SERVED', 1, 900, 900),
    (4, 2, 4, 'SERVED', 1, 400, 400),

    -- order 3: cancelled hamburger
    (5, 3, 1, 'CANCELLED', 1, 900, 900),

    -- order 4: already paid hamburger
    (6, 4, 1, 'SERVED', 1, 900, 900);

-- Payments.
INSERT INTO payments (
    id,
    store_id,
    visit_session_id,
    amount,
    status,
    paid_at
)
VALUES
    (1, 1, 3, 900, 'PAID', CURRENT_TIMESTAMP - INTERVAL '30 minutes');

-- Advance identity sequences after explicit IDs.
SELECT setval(pg_get_serial_sequence('stores', 'id'), (SELECT MAX(id) FROM stores), true);
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users), true);
SELECT setval(pg_get_serial_sequence('restaurant_tables', 'id'), (SELECT MAX(id) FROM restaurant_tables), true);
SELECT setval(pg_get_serial_sequence('menu_categories', 'id'), (SELECT MAX(id) FROM menu_categories), true);
SELECT setval(pg_get_serial_sequence('menus', 'id'), (SELECT MAX(id) FROM menus), true);
SELECT setval(pg_get_serial_sequence('visit_sessions', 'id'), (SELECT MAX(id) FROM visit_sessions), true);
SELECT setval(pg_get_serial_sequence('orders', 'id'), (SELECT MAX(id) FROM orders), true);
SELECT setval(pg_get_serial_sequence('order_items', 'id'), (SELECT MAX(id) FROM order_items), true);
SELECT setval(pg_get_serial_sequence('payments', 'id'), (SELECT MAX(id) FROM payments), true);
