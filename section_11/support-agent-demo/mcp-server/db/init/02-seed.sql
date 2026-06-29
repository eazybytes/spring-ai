-- =============================================================================
-- Support Agent — demo / dummy data
--
-- The rows below are deliberately wired to the four sample emails so the agent
-- can be exercised end-to-end:
--   1. Sarah  — blender with a cracked jug, THIRD time -> goodwill refund.
--   2. (any)  — pre-sales "X200 on European voltage?" -> answer from products.
--   3. Priya  — "charged twice for order #4471"  -> duplicate payment, refund 1.
--   4. Rohan  — sarcastic, half-Hindi, two issues -> multilingual + multi-intent.
--
-- All dates are RELATIVE to CURDATE()/NOW() so the scenarios stay valid whenever
-- the container seeds a fresh volume — no hard-coded calendar dates.
-- =============================================================================
USE mydatabase;

-- ---------------------------------------------------------------------------
-- Customers
-- ---------------------------------------------------------------------------
INSERT INTO customers (id, full_name, email, phone, preferred_language, loyalty_tier, created_at) VALUES
  (1, 'Sarah Mitchell', 'sarah.mitchell@example.com', '+1-415-555-0142', 'en',    'GOLD',     TIMESTAMP(CURDATE() - INTERVAL 1216 DAY, '09:20:00')),
  (2, 'James Cooper',   'james.cooper@example.com',   '+44-7700-900145', 'en',    'STANDARD', TIMESTAMP(CURDATE() - INTERVAL 12 DAY,   '16:05:00')),
  (3, 'Priya Sharma',   'priya.sharma@example.com',   '+1-312-555-0188', 'en',    'SILVER',   TIMESTAMP(CURDATE() - INTERVAL 661 DAY,  '11:42:00')),
  (4, 'Rohan Verma',    'rohan.verma@example.com',    '+91-98200-12345', 'en+hi', 'STANDARD', TIMESTAMP(CURDATE() - INTERVAL 220 DAY,  '19:30:00'));

-- ---------------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------------
-- `specifications` is free-form JSON, so each product carries only the
-- attributes that make sense for it — voltage for appliances, pages for a
-- book, size for apparel. The catalog below deliberately mixes categories to
-- show the table is product-agnostic.
INSERT INTO products (id, sku, name, description, category, price, currency, specifications, warranty_months, stock_quantity) VALUES
  (1, 'BLND-300', 'AeroBlend 300 High-Speed Blender',
      '1200W high-speed blender with a 2L Tritan jug. Known issue: jug can crack under thermal shock.',
      'Kitchen', 129.99, 'USD',
      JSON_OBJECT('power','1200W','capacity','2L Tritan jug',
                  'voltage','120V / 60Hz — North America only (NOT dual voltage)'), 24, 37),
  (2, 'X200', 'VoltMaster X200 Travel Steam Iron',
      'Compact travel steam iron with auto dual-voltage switching, ideal for international trips.',
      'Appliances', 79.99, 'USD',
      JSON_OBJECT('type','Travel steam iron',
                  'voltage','100-240V, 50/60Hz auto dual-voltage — works on European 230V mains',
                  'note','EU plug adapter required'), 12, 120),
  (3, 'HMX-50', 'HushMix 50 Hand Mixer',
      '5-speed quiet hand mixer with stainless beaters.',
      'Kitchen', 49.99, 'USD',
      JSON_OBJECT('speeds',5,'voltage','120V / 60Hz — North America only'), 12, 64),
  (4, 'KSET-12', 'ChefPro 12-Piece Knife Set',
      'German stainless steel 12-piece knife block set.',
      'Kitchen', 199.99, 'USD',
      JSON_OBJECT('pieces',12,'material','German stainless steel'), 60, 18),
  (5, 'EKET-7', 'BrewWell 7-Cup Electric Kettle',
      'Stainless steel cordless electric kettle with auto shut-off.',
      'Kitchen', 39.99, 'USD',
      JSON_OBJECT('capacity','7 cups','material','Stainless steel',
                  'voltage','120V / 60Hz — North America only'), 24, 50),
  -- Non-electronic products: prove the schema holds any category cleanly.
  (6, 'BOOK-021', 'The Quiet Garden (Paperback)',
      'Bestselling literary novel, paperback edition.',
      'Books', 14.99, 'USD',
      JSON_OBJECT('format','Paperback','pages',328,'language','English','author','M. Iyer'), 0, 210),
  (7, 'YOGA-08', 'ZenFlow Yoga Mat',
      'Non-slip TPE yoga mat with carry strap.',
      'Fitness', 34.99, 'USD',
      JSON_OBJECT('material','TPE','thickness','6mm','dimensions','183x61cm','color','Teal'), 12, 95),
  (8, 'TSHIRT-M', 'CottonComfort Crew T-Shirt',
      '100% combed-cotton crew-neck t-shirt.',
      'Apparel', 19.99, 'USD',
      JSON_OBJECT('size','M','color','Navy','material','100% cotton','fit','Regular'), 0, 300);

-- ---------------------------------------------------------------------------
-- Orders
-- ---------------------------------------------------------------------------
INSERT INTO orders (id, order_number, customer_id, order_date, status, shipping_address, total_amount, currency) VALUES
  -- (1) Sarah's latest blender — delivered, well inside the 24-month warranty.
  (1, '4198', 1, CURDATE() - INTERVAL 22 DAY,  'DELIVERED', '88 Maple Ave, San Francisco, CA 94110, USA', 129.99, 'USD'),
  -- (3) Priya's knife set — the order she says was charged twice.
  (2, '4471', 3, CURDATE() - INTERVAL 9 DAY,   'PAID',      '512 Lakeshore Dr, Chicago, IL 60611, USA',  199.99, 'USD'),
  -- (4) Rohan's order — kettle + hand mixer, the basis for his two complaints.
  (3, '4502', 4, CURDATE() - INTERVAL 6 DAY,   'DELIVERED', 'A-14 Green Park, New Delhi 110016, India',    89.98, 'USD'),
  -- Sarah's earlier blender orders (the two PRIOR cracked-jug incidents).
  (4, '3801', 1, CURDATE() - INTERVAL 183 DAY, 'DELIVERED', '88 Maple Ave, San Francisco, CA 94110, USA', 129.99, 'USD'),
  (5, '4007', 1, CURDATE() - INTERVAL 101 DAY, 'DELIVERED', '88 Maple Ave, San Francisco, CA 94110, USA', 129.99, 'USD');

-- ---------------------------------------------------------------------------
-- Order line items
-- ---------------------------------------------------------------------------
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
  (1, 1, 1, 129.99),   -- order 4198: AeroBlend 300
  (2, 4, 1, 199.99),   -- order 4471: ChefPro knife set
  (3, 5, 1, 39.99),    -- order 4502: kettle
  (3, 3, 1, 49.99),    -- order 4502: hand mixer
  (4, 1, 1, 129.99),   -- order 3801: AeroBlend 300 (1st failure)
  (5, 1, 1, 129.99);   -- order 4007: AeroBlend 300 (2nd failure)

-- ---------------------------------------------------------------------------
-- Payments
-- Order 4471 (Priya) has TWO captured charges with the same amount, seconds
-- apart, different transaction refs -> the classic duplicate charge.
-- (transaction_ref values are opaque identifiers, intentionally left static.)
-- ---------------------------------------------------------------------------
INSERT INTO payments (order_id, amount, currency, payment_method, transaction_ref, status, charged_at) VALUES
  (1, 129.99, 'USD', 'CARD', 'TXN-20260520-0001', 'CAPTURED', TIMESTAMP(CURDATE() - INTERVAL 22 DAY,  '14:03:11')),
  (2, 199.99, 'USD', 'CARD', 'TXN-20260602-0188', 'CAPTURED', TIMESTAMP(CURDATE() - INTERVAL 9 DAY,   '10:15:42')),  -- charge #1
  (2, 199.99, 'USD', 'CARD', 'TXN-20260602-0189', 'CAPTURED', TIMESTAMP(CURDATE() - INTERVAL 9 DAY,   '10:15:48')),  -- charge #2 (duplicate)
  (3,  89.98, 'USD', 'UPI',  'TXN-20260605-7741', 'CAPTURED', TIMESTAMP(CURDATE() - INTERVAL 6 DAY,   '08:22:05')),
  (4, 129.99, 'USD', 'CARD', 'TXN-20251210-0044', 'REFUNDED', TIMESTAMP(CURDATE() - INTERVAL 183 DAY, '12:00:00')),
  (5, 129.99, 'USD', 'CARD', 'TXN-20260302-0091', 'REFUNDED', TIMESTAMP(CURDATE() - INTERVAL 101 DAY, '17:45:30'));

-- ---------------------------------------------------------------------------
-- Refunds — historical record of the two earlier blender replacements/refunds.
-- (The current 3rd-time refund and the 4471 duplicate refund are what the
--  agent is expected to CREATE at runtime, so they are intentionally absent.)
-- ---------------------------------------------------------------------------
INSERT INTO refunds (order_id, payment_id, amount, currency, reason, refund_type, status, created_at) VALUES
  (4, 5, 129.99, 'USD', 'Cracked jug on arrival — 1st incident, replacement issued', 'WARRANTY', 'PROCESSED', TIMESTAMP(CURDATE() - INTERVAL 175 DAY, '10:00:00')),
  (5, 6, 129.99, 'USD', 'Cracked jug again — 2nd incident, replacement issued',       'WARRANTY', 'PROCESSED', TIMESTAMP(CURDATE() - INTERVAL 94 DAY,  '14:30:00'));

-- ---------------------------------------------------------------------------
-- Support tickets — prior history so the agent can SEE this is a repeat.
-- ---------------------------------------------------------------------------
INSERT INTO support_tickets
  (customer_id, order_id, product_id, channel, subject, raw_message, detected_language, intent, sentiment, status, resolution, created_at, resolved_at) VALUES
  -- Sarah, incident #1
  (1, 4, 1, 'EMAIL', 'Blender jug arrived cracked',
     'Hi, my new AeroBlend blender arrived with a crack along the jug. Can you help?',
     'en', 'WARRANTY_CLAIM', 'NEGATIVE', 'RESOLVED',
     'Replacement unit shipped + full refund processed (goodwill).',
     TIMESTAMP(CURDATE() - INTERVAL 178 DAY, '09:12:00'), TIMESTAMP(CURDATE() - INTERVAL 175 DAY, '10:05:00')),
  -- Sarah, incident #2
  (1, 5, 1, 'EMAIL', 'Cracked jug AGAIN',
     'This is the second time the jug has cracked. Getting frustrated.',
     'en', 'WARRANTY_CLAIM', 'ANGRY', 'RESOLVED',
     'Second replacement shipped + refund processed. Flagged product quality issue.',
     TIMESTAMP(CURDATE() - INTERVAL 98 DAY, '18:40:00'), TIMESTAMP(CURDATE() - INTERVAL 94 DAY, '14:35:00')),
  -- Rohan, an earlier minor query (so he is a known customer)
  (4, NULL, NULL, 'EMAIL', 'Order tracking',
     'Where is my order? / mera order kahan hai?',
     'en+hi', 'GENERAL', 'NEUTRAL', 'RESOLVED',
     'Shared tracking link; delivered next day.',
     TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '11:00:00'), TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '11:20:00'));
