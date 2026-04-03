INSERT INTO product (id, name, price, description, stock) VALUES
(1, 'Amul Milk', 60, 'Dairy milk - Mumbai supply', 25),
(2, 'Pav Bread', 30, 'Fresh pav from local bakery (Mumbai)', 20),
(3, 'Vada Pav Combo', 50, 'Street food classic from Mumbai', 40),
(4, 'Misal Pav', 80, 'Spicy Misal from Pune', 30),
(5, 'Poha', 40, 'Breakfast item popular in Maharashtra', 35),
(6, 'Sabudana Khichdi', 70, 'Fasting special dish', 20),
(7, 'Kolhapuri Masala', 120, 'Authentic Kolhapuri spice mix', 15),
(8, 'Alphonso Mangoes', 800, 'Ratnagiri Hapus Mango (1 dozen)', 10),
(9, 'Basmati Rice 5kg', 600, 'Premium rice', 12),
(10, 'Eggs Tray', 150, '30 eggs tray', 25)
ON CONFLICT (id) DO NOTHING;