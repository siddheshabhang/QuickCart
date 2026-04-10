INSERT INTO product (id, name, price, description, stock, created_at, updated_at) VALUES
(1,  'Amul Milk',          60,  'Dairy milk - Mumbai supply',           25, NOW(), NOW()),
(2,  'Pav Bread',          30,  'Fresh pav from local bakery (Mumbai)', 20, NOW(), NOW()),
(3,  'Vada Pav Combo',     50,  'Street food classic from Mumbai',      40, NOW(), NOW()),
(4,  'Misal Pav',          80,  'Spicy Misal from Pune',                30, NOW(), NOW()),
(5,  'Poha',               40,  'Breakfast item popular in Maharashtra',35, NOW(), NOW()),
(6,  'Sabudana Khichdi',   70,  'Fasting special dish',                 20, NOW(), NOW()),
(7,  'Kolhapuri Masala',   120, 'Authentic Kolhapuri spice mix',        15, NOW(), NOW()),
(8,  'Alphonso Mangoes',   800, 'Ratnagiri Hapus Mango (1 dozen)',      10, NOW(), NOW()),
(9,  'Basmati Rice 5kg',   600, 'Premium rice',                         12, NOW(), NOW()),
(10, 'Eggs Tray',          150, '30 eggs tray',                         25, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset product sequence so new products don't conflict with seeded IDs
SELECT setval('product_id_seq', (SELECT MAX(id) FROM product));