-- ============================================================
-- Migration: Thêm cột specifications cho bảng products
-- ============================================================
ALTER TABLE products ADD COLUMN IF NOT EXISTS specifications TEXT;

-- ============================================================
-- Seed Data: Thông số kỹ thuật cho từng sản phẩm
-- Dựa trên tên sản phẩm và category để điền thông số phù hợp
-- ============================================================

-- ===== ELECTRONICS =====

-- iPhone 15
UPDATE products SET specifications = '{
  "Màn hình": "6.1 inch Super Retina XDR OLED",
  "CPU": "Apple A16 Bionic",
  "RAM": "6GB",
  "Bộ nhớ trong": "128GB / 256GB / 512GB",
  "Camera sau": "48MP (chính) + 12MP (ultra wide)",
  "Camera trước": "12MP TrueDepth",
  "Pin": "3877 mAh, sạc nhanh 20W",
  "Hệ điều hành": "iOS 17",
  "Kết nối": "5G, Wi-Fi 6, Bluetooth 5.3, NFC",
  "Chất liệu": "Khung nhôm, mặt kính Ceramic Shield",
  "Kích thước": "147.6 x 71.5 x 7.8 mm",
  "Trọng lượng": "171g",
  "Màu sắc": "Pink, Yellow, Green, Blue, Black, White",
  "Chống nước": "IP68 (6m/30 phút)",
  "Bảo hành": "12 tháng chính hãng"
}'
WHERE LOWER(name) LIKE '%iphone 15%' AND category_id IN (SELECT id FROM categories WHERE slug = 'electronics');

-- iPhone 14
UPDATE products SET specifications = '{
  "Màn hình": "6.1 inch Super Retina XDR OLED",
  "CPU": "Apple A15 Bionic",
  "RAM": "6GB",
  "Bộ nhớ trong": "128GB / 256GB / 512GB",
  "Camera sau": "12MP (chính) + 12MP (ultra wide)",
  "Camera trước": "12MP TrueDepth",
  "Pin": "3279 mAh, sạc nhanh 20W",
  "Hệ điều hành": "iOS 16 (nâng cấp iOS 17)",
  "Kết nối": "5G, Wi-Fi 6, Bluetooth 5.3, NFC",
  "Chất liệu": "Khung nhôm, mặt kính Ceramic Shield",
  "Kích thước": "146.7 x 71.5 x 7.8 mm",
  "Trọng lượng": "172g",
  "Màu sắc": "Blue, Purple, Midnight, Starlight, Red",
  "Chống nước": "IP68 (6m/30 phút)",
  "Bảo hành": "12 tháng chính hãng"
}'
WHERE LOWER(name) LIKE '%iphone 14%' AND category_id IN (SELECT id FROM categories WHERE slug = 'electronics');

-- Galaxy S24 Ultra
UPDATE products SET specifications = '{
  "Màn hình": "6.8 inch Dynamic AMOLED 2X, 120Hz",
  "CPU": "Snapdragon 8 Gen 3",
  "RAM": "12GB",
  "Bộ nhớ trong": "256GB / 512GB / 1TB",
  "Camera sau": "200MP (chính) + 50MP (zoom) + 12MP (ultra wide) + 10MP (zoom 3x)",
  "Camera trước": "12MP",
  "Pin": "5000 mAh, sạc nhanh 45W",
  "Hệ điều hành": "Android 14, One UI 6.1",
  "Kết nối": "5G, Wi-Fi 7, Bluetooth 5.3, NFC, USB-C 3.2",
  "Chất liệu": "Khung titan, mặt kính Corning Gorilla Armor",
  "Kích thước": "162.3 x 79.0 x 8.6 mm",
  "Trọng lượng": "232g",
  "Màu sắc": "Titanium Black, Titanium Gray, Titanium Violet, Titanium Yellow",
  "Chống nước": "IP68 (2m/30 phút)",
  "S Pen": "Tích hợp",
  "Bảo hành": "12 tháng chính hãng"
}'
WHERE LOWER(name) LIKE '%galaxy s24 ultra%' AND category_id IN (SELECT id FROM categories WHERE slug = 'electronics');

-- Galaxy S23
UPDATE products SET specifications = '{
  "Màn hình": "6.1 inch Dynamic AMOLED 2X, 120Hz",
  "CPU": "Snapdragon 8 Gen 2",
  "RAM": "8GB",
  "Bộ nhớ trong": "128GB / 256GB",
  "Camera sau": "50MP (chính) + 12MP (ultra wide) + 10MP (zoom 3x)",
  "Camera trước": "12MP",
  "Pin": "3900 mAh, sạc nhanh 25W",
  "Hệ điều hành": "Android 13, One UI 5.1",
  "Kết nối": "5G, Wi-Fi 6E, Bluetooth 5.3, NFC, USB-C 3.2",
  "Chất liệu": "Khung nhôm Armor, mặt kính Corning Gorilla Glass Victus 2",
  "Kích thước": "146.3 x 70.9 x 7.6 mm",
  "Trọng lượng": "168g",
  "Màu sắc": "Phantom Black, Cream, Green, Lavender",
  "Chống nước": "IP68 (2m/30 phút)",
  "Bảo hành": "12 tháng chính hãng"
}'
WHERE LOWER(name) LIKE '%galaxy s23%' AND category_id IN (SELECT id FROM categories WHERE slug = 'electronics');

-- Đồng Hồ Thông Minh AMOLED
UPDATE products SET specifications = '{
  "Màn hình": "1.43 inch AMOLED tròn, độ sáng 466x466px",
  "CPU": "Processor tùy chỉnh",
  "Pin": "450 mAh, dùng 14 ngày",
  "GPS": "Tích hợp GPS + GLONASS",
  "Cảm biến": "Nhịp tim, SpO2, áp suất khí quyển, con quay hồi chuyển",
  "Chống nước": "5ATM (50m)",
  "Kết nối": "Bluetooth 5.3, Wi-Fi 2.4GHz",
  "Hệ điều hành": "Wear OS / HarmonyOS",
  "Theo dõi sức khỏe": "Nhịp tim 24/7, giấc ngủ, stress, nhiệt độ cơ thể",
  "Kích thước": "46mm",
  "Chất liệu dây": "Silicon mềm, có thể thay thế",
  "Tương thích": "Android 6.0+ / iOS 9.0+",
  "Bảo hành": "12 tháng"
}'
WHERE LOWER(name) LIKE '%đồng hồ thông minh%' AND LOWER(name) LIKE '%amoled%' AND category_id IN (SELECT id FROM categories WHERE slug = 'electronics');

-- Đồng Hồ Thể Thao GPS
UPDATE products SET specifications = '{
  "Màn hình": "1.3 inch MIP Transflective, luôn hiển thị",
  "Pin": "18 ngày chế độ smartwatch, 36h GPS",
  "GPS": "GPS + GLONASS + Galileo + BeiDou",
  "Cảm biến": "Nhịp tim quang học, SpO2, nhiệt độ da, altimeter",
  "Chống nước": "MIL-STD-810, 5ATM",
  "Kết nối": "Bluetooth 5.0, ANT+, Wi-Fi",
  "Môn thể thao": "Chạy bộ, bơi lội, đạp xe, leo núi, 30+ môn",
  "Kích thước": "45mm",
  "Chất liệu vỏ": "Nhựa polyme sợi thủy tinh",
  "Chất liệu dây": "Silicon",
  "Tương thích": "Android / iOS",
  "Bảo hành": "12 tháng"
}'
WHERE LOWER(name) LIKE '%đồng hồ thể thao%' AND LOWER(name) LIKE '%gps%' AND category_id IN (SELECT id FROM categories WHERE slug = 'electronics');


-- ===== BEAUTY =====

-- Kem Dưỡng Ẩm Hyaluronic
UPDATE products SET specifications = '{
  "Dung tích": "50ml",
  "Loại da phù hợp": "Mọi loại da, đặc biệt da khô và da hỗn hợp",
  "Thành phần chính": "Hyaluronic Acid 2%, Ceramide, Panthenol, Glycerin",
  "Công dụng": "Dưỡng ẩm sâu, phục hồi hàng rào bảo vệ da, làm mềm mịn da",
  "Hướng dẫn sử dụng": "Thoa đều lên mặt và cổ sáng-tối sau khi rửa mặt",
  "Tần suất": "2 lần/ngày",
  "Kết cấu": "Gel-cream nhẹ, thấm nhanh",
  "Hương": "Không hương",
  "Xuất xứ": "Hàn Quốc",
  "Hạn sử dụng": "36 tháng (chưa mở), 12 tháng (đã mở)",
  "Thành phần đặc biệt": "Không paraben, không cồn, không sulfate",
  "Bảo quản": "Nơi khô ráo, tránh ánh nắng trực tiếp"
}'
WHERE LOWER(name) LIKE '%kem dưỡng ẩm%' AND category_id IN (SELECT id FROM categories WHERE slug = 'beauty');

-- Kem Chống Lão Hóa Peptide
UPDATE products SET specifications = '{
  "Dung tích": "30ml",
  "Loại da phù hợp": "Da trưởng thành, da lão hóa, da khô",
  "Thành phần chính": "Matrixyl 3000, Argireline, Retinol 0.1%, Vitamin C 10%, Niacinamide",
  "Công dụng": "Giảm nếp nhăn, làm dày da, tăng collagen, làm đều màu da",
  "Hướng dẫn sử dụng": "Thoa lượng nhỏ lên mặt vào buổi tối sau serum",
  "Tần suất": "1 lần/ngày (tối)",
  "Kết cấu": "Cream đặc, giàu dưỡng chất",
  "Hương": "Nhẹ nhàng, tự nhiên",
  "Xuất xứ": "Pháp",
  "Hạn sử dụng": "24 tháng (chưa mở), 6 tháng (đã mở)",
  "Lưu ý": "Tránh vùng mắt, dùng kem chống nắng ban ngày",
  "Bảo quản": "Nhiệt độ phòng, tránh nhiệt"
}'
WHERE LOWER(name) LIKE '%kem chống lão hóa%' OR LOWER(name) LIKE '%peptide%' AND category_id IN (SELECT id FROM categories WHERE slug = 'beauty');

-- Kem Mắt Caffeine
UPDATE products SET specifications = '{
  "Dung tích": "15ml",
  "Loại da phù hợp": "Mọi loại da, vùng mắt nhạy cảm",
  "Thành phần chính": "Caffeine 5%, EGCG (chiết xuất trà xanh), Hyaluronic Acid, Vitamin K",
  "Công dụng": "Giảm quầng thâm, giảm bọng mắt, dưỡng ẩm vùng mắt, chống oxy hóa",
  "Hướng dẫn sử dụng": "Thoa nhẹ nhàng quanh vùng mắt sáng và tối bằng đầu ngón tay áp út",
  "Tần suất": "2 lần/ngày",
  "Kết cấu": "Gel nhẹ, thấm nhanh",
  "Hương": "Không hương",
  "Xuất xứ": "Anh Quốc",
  "Hạn sử dụng": "24 tháng (chưa mở), 12 tháng (đã mở)",
  "Thành phần đặc biệt": "Không paraben, không gluten, vegan",
  "Bảo quản": "Nơi mát, tránh nhiệt"
}'
WHERE LOWER(name) LIKE '%kem mắt%' AND category_id IN (SELECT id FROM categories WHERE slug = 'beauty');

-- Mặt Nạ Giấy Cấp Ẩm
UPDATE products SET specifications = '{
  "Dung tích": "25ml/miếng (hộp 10 miếng)",
  "Loại da phù hợp": "Mọi loại da, đặc biệt da khô và mất nước",
  "Thành phần chính": "Hyaluronic Acid, Aloe Vera, Centella Asiatica, Glycerin",
  "Công dụng": "Cấp ẩm tức thì, làm dịu da, sáng da, thu nhỏ lỗ chân lông",
  "Hướng dẫn sử dụng": "Đắp lên mặt sạch 15-20 phút, bỏ mặt nạ, massage nhẹ phần tinh chất còn lại",
  "Tần suất": "2-3 lần/tuần",
  "Chất liệu mặt nạ": "Vải tencel 100%",
  "Hương": "Không hương / nhẹ nhàng tự nhiên",
  "Xuất xứ": "Hàn Quốc",
  "Hạn sử dụng": "24 tháng",
  "Bảo quản": "Nơi thoáng mát, tránh ánh nắng"
}'
WHERE LOWER(name) LIKE '%mặt nạ giấy%' AND category_id IN (SELECT id FROM categories WHERE slug = 'beauty');

-- Mặt Nạ Đất Sét Làm Sạch
UPDATE products SET specifications = '{
  "Dung tích": "100ml",
  "Loại da phù hợp": "Da dầu, da hỗn hợp, da có lỗ chân lông to",
  "Thành phần chính": "Kaolin Clay, Bentonite Clay, Tea Tree Oil, Salicylic Acid 1%",
  "Công dụng": "Làm sạch lỗ chân lông, kiểm soát dầu, giảm mụn đầu đen, làm mịn da",
  "Hướng dẫn sử dụng": "Thoa đều lên da khô, để 10-15 phút, rửa sạch với nước ấm",
  "Tần suất": "1-2 lần/tuần",
  "Kết cấu": "Dạng paste đặc",
  "Hương": "Mint nhẹ",
  "Xuất xứ": "Mỹ",
  "Hạn sử dụng": "24 tháng (chưa mở), 12 tháng (đã mở)",
  "Lưu ý": "Tránh vùng mắt, không để quá lâu trên da nhạy cảm",
  "Bảo quản": "Đậy kín sau khi dùng"
}'
WHERE LOWER(name) LIKE '%mặt nạ đất sét%' AND category_id IN (SELECT id FROM categories WHERE slug = 'beauty');

-- Mặt Nạ Ngủ Phục Hồi
UPDATE products SET specifications = '{
  "Dung tích": "80ml",
  "Loại da phù hợp": "Mọi loại da, đặc biệt da khô và da căng thẳng",
  "Thành phần chính": "Madecassoside, Adenosine, Arbutin, Shea Butter, Vitamin E",
  "Công dụng": "Phục hồi da ban đêm, dưỡng ẩm suốt đêm, làm đều màu da, chống oxy hóa",
  "Hướng dẫn sử dụng": "Thoa một lớp mỏng lên mặt là bước cuối cùng trong quy trình chăm sóc da tối",
  "Tần suất": "Hàng đêm hoặc 2-3 lần/tuần",
  "Kết cấu": "Gel-cream mỏng nhẹ",
  "Hương": "Nhẹ nhàng, dễ chịu",
  "Xuất xứ": "Hàn Quốc",
  "Hạn sử dụng": "24 tháng (chưa mở), 12 tháng (đã mở)",
  "Bảo quản": "Nơi khô ráo, thoáng mát"
}'
WHERE LOWER(name) LIKE '%mặt nạ ngủ%' AND category_id IN (SELECT id FROM categories WHERE slug = 'beauty');


-- ===== FASHION =====

-- Áo Thun Cotton
UPDATE products SET specifications = '{
  "Chất liệu": "100% Cotton cao cấp",
  "Độ co giãn": "Không co giãn",
  "Độ dày": "180 GSM",
  "Phong cách": "Casual, Basic",
  "Cổ áo": "Cổ tròn / Cổ tim (tùy mẫu)",
  "Tay áo": "Tay ngắn",
  "Màu sắc": "Trắng, Đen, Xám, Navy, Hồng, Xanh lá",
  "Bảng size": "S (44-47cm), M (48-51cm), L (52-55cm), XL (56-59cm), XXL (60-63cm)",
  "Hướng dẫn giặt": "Máy giặt ≤30°C, giặt đảo mặt trong, không sấy máy",
  "Phù hợp với": "Đi chơi, ở nhà, tập gym nhẹ",
  "Mùa phù hợp": "Xuân, Hè, Thu",
  "Xuất xứ": "Việt Nam"
}'
WHERE LOWER(name) LIKE '%áo thun%' AND category_id IN (SELECT id FROM categories WHERE slug = 'fashion');

-- Áo Hoodie Zip Nỉ
UPDATE products SET specifications = '{
  "Chất liệu": "65% Cotton, 35% Polyester, lót nỉ mềm",
  "Độ co giãn": "Co giãn nhẹ 4 chiều",
  "Phong cách": "Streetwear, Casual, Athleisure",
  "Kiểu dáng": "Hoodie có dây kéo zip toàn thân",
  "Màu sắc": "Đen, Xám đậm, Xanh Navy, Nâu, Olive",
  "Bảng size": "S (ngực 84-88cm), M (89-93cm), L (94-98cm), XL (99-103cm), XXL (104-108cm)",
  "Hướng dẫn giặt": "Máy giặt ≤30°C, không dùng thuốc tẩy, phơi khô tự nhiên",
  "Đặc điểm": "Túi kangaroo phía trước, dây rút mũ điều chỉnh được",
  "Phù hợp với": "Đi chơi, thể thao nhẹ, mặc nhà",
  "Mùa phù hợp": "Thu, Đông",
  "Xuất xứ": "Việt Nam"
}'
WHERE LOWER(name) LIKE '%hoodie%' AND category_id IN (SELECT id FROM categories WHERE slug = 'fashion');

-- Áo Sơ Mi Oxford
UPDATE products SET specifications = '{
  "Chất liệu": "100% Oxford Cotton, dệt thoi",
  "Độ co giãn": "Không co giãn",
  "Phong cách": "Smart Casual, Business Casual",
  "Kiểu dáng": "Regular Fit, cổ button-down",
  "Màu sắc": "Trắng, Xanh nhạt, Xanh navy, Hồng pastel",
  "Bảng size": "S (cổ 37cm), M (cổ 39cm), L (cổ 41cm), XL (cổ 43cm), XXL (cổ 45cm)",
  "Hướng dẫn giặt": "Giặt tay hoặc máy nhẹ ≤30°C, ủi ở nhiệt độ trung bình",
  "Đặc điểm": "Cúc nhỏ tại cổ áo (button-down), túi ngực nhỏ",
  "Phù hợp với": "Đi làm, hẹn hò, sự kiện smart casual",
  "Mùa phù hợp": "Quanh năm",
  "Xuất xứ": "Việt Nam"
}'
WHERE LOWER(name) LIKE '%sơ mi%' AND category_id IN (SELECT id FROM categories WHERE slug = 'fashion');

-- Váy Midi Hoa
UPDATE products SET specifications = '{
  "Chất liệu": "100% Viscose (Lụa nhân tạo)",
  "Độ co giãn": "Không co giãn, dáng suôn rủ",
  "Phong cách": "Feminine, Boho, Elegant",
  "Kiểu dáng": "Váy midi dài qua gối 10-15cm, cổ vuông",
  "Màu sắc": "Hoa nhí nền đen, hoa nhí nền trắng, hoa tươi nền xanh",
  "Bảng size": "XS (60-64kg), S (54-58kg), M (58-63kg), L (63-68kg), XL (68-73kg)",
  "Chiều dài": "Khoảng 100-110cm từ eo xuống",
  "Hướng dẫn giặt": "Giặt tay nhẹ nhàng, không vắt mạnh, phơi bóng",
  "Đặc điểm": "Dây điều chỉnh eo, cúc sau lưng",
  "Phù hợp với": "Đi chơi, du lịch, dự tiệc nhẹ",
  "Mùa phù hợp": "Xuân, Hè",
  "Xuất xứ": "Việt Nam"
}'
WHERE LOWER(name) LIKE '%váy midi%' AND category_id IN (SELECT id FROM categories WHERE slug = 'fashion');

-- Váy Công Sở
UPDATE products SET specifications = '{
  "Chất liệu": "65% Polyester, 30% Viscose, 5% Elastane",
  "Độ co giãn": "Co giãn nhẹ, thoải mái di chuyển",
  "Phong cách": "Office, Formal, Smart",
  "Kiểu dáng": "Váy xòe tay lỡ, dài qua gối",
  "Màu sắc": "Đen, Trắng kem, Xanh navy, Xám",
  "Bảng size": "S (vòng ngực 80-84cm), M (84-88cm), L (88-92cm), XL (92-96cm)",
  "Chiều dài": "Khoảng 95-105cm từ vai xuống",
  "Hướng dẫn giặt": "Giặt máy nhẹ ≤30°C, ủi mặt trái",
  "Đặc điểm": "Cúc nút phía sau, dây đai eo, ly xếp nhẹ",
  "Phù hợp với": "Đi làm, họp, sự kiện công sở",
  "Mùa phù hợp": "Quanh năm",
  "Xuất xứ": "Việt Nam"
}'
WHERE LOWER(name) LIKE '%váy công sở%' AND category_id IN (SELECT id FROM categories WHERE slug = 'fashion');

-- Giày Sneaker / Giày Vans
UPDATE products SET specifications = '{
  "Chất liệu upper": "Canvas dày dặn (100% Cotton)",
  "Đế giày": "Cao su lưu hóa",
  "Lót trong": "Lót vải mềm, thấm hút tốt",
  "Phong cách": "Streetwear, Casual, Skate",
  "Chiều cao cổ": "Low-top (cổ thấp)",
  "Bảng size": "35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45",
  "Màu sắc": "Đen/Trắng, Trắng thuần, Navy/Trắng",
  "Hướng dẫn vệ sinh": "Dùng bàn chải mềm và xà phòng nhẹ, không cho vào máy giặt",
  "Phù hợp với": "Đi học, đi chơi, dạo phố, skate nhẹ",
  "Mùa phù hợp": "Quanh năm",
  "Xuất xứ": "Nhập khẩu"
}'
WHERE (LOWER(name) LIKE '%sneaker%' OR LOWER(name) LIKE '%giày%' OR LOWER(name) LIKE '%vans%') AND category_id IN (SELECT id FROM categories WHERE slug = 'fashion');


-- ===== HOME & LIVING =====

-- Ghế Công Thái Học
UPDATE products SET specifications = '{
  "Chất liệu lưng": "Lưới thoáng khí cao cấp (Mesh)",
  "Chất liệu đệm": "Bọt biển mật độ cao, phủ vải dệt",
  "Khung ghế": "Nhựa kỹ thuật cao cấp + thép",
  "Chân ghế": "5 chân nhựa PA, bánh xe silicone",
  "Tải trọng tối đa": "130kg",
  "Chiều cao ghế": "Điều chỉnh 42-52cm",
  "Chiều rộng ghế": "50cm",
  "Tựa đầu": "Điều chỉnh độ cao và góc",
  "Tay vịn": "3D (lên/xuống, trước/sau, xoay)",
  "Tựa lưng": "Điều chỉnh ngả 90°-135°",
  "Hỗ trợ thắt lưng": "Gối đỡ thắt lưng tích hợp, điều chỉnh",
  "Màu sắc": "Đen, Xám, Trắng",
  "Kích thước (LxRxC)": "65 x 68 x 115-125cm",
  "Trọng lượng ghế": "17kg",
  "Bảo hành": "24 tháng"
}'
WHERE LOWER(name) LIKE '%công thái học%' AND category_id IN (SELECT id FROM categories WHERE slug = 'home-living');

-- Ghế Gaming
UPDATE products SET specifications = '{
  "Chất liệu lưng/ngồi": "Da PU cao cấp (chống thấm, dễ vệ sinh)",
  "Đệm ngồi": "Bọt biển lạnh mật độ cao",
  "Khung ghế": "Thép carbon dày 3mm",
  "Chân ghế": "Kim loại mạ chrome, bánh xe PU",
  "Tải trọng tối đa": "150kg",
  "Chiều cao ghế": "Điều chỉnh 43-53cm",
  "Tay vịn": "4D (lên/xuống, trước/sau, xoay trái/phải)",
  "Tựa lưng": "Ngả 90°-180° (nằm hoàn toàn)",
  "Gối tựa đầu": "Có (tháo lắp được)",
  "Gối thắt lưng": "Có (tháo lắp được)",
  "Màu sắc": "Đen/Đỏ, Đen/Xanh, Trắng/Đen",
  "Kích thước (LxRxC)": "70 x 72 x 122-132cm",
  "Trọng lượng ghế": "22kg",
  "Bảo hành": "12 tháng"
}'
WHERE LOWER(name) LIKE '%ghế gaming%' AND category_id IN (SELECT id FROM categories WHERE slug = 'home-living');

-- Ghế Ăn Gỗ
UPDATE products SET specifications = '{
  "Chất liệu chân ghế": "Gỗ cao su tự nhiên, phủ sơn PU",
  "Chất liệu mặt ghế": "Gỗ MDF phủ Melamine",
  "Tải trọng tối đa": "100kg",
  "Kích thước (CxRxCao)": "42 x 42 x 82cm",
  "Chiều cao mặt ghế": "45cm",
  "Màu sắc": "Gỗ tự nhiên, Nâu cánh gián, Trắng",
  "Phong cách": "Scandinavian, Hiện đại",
  "Lắp ráp": "Có hướng dẫn kèm theo, cần lắp ráp",
  "Trọng lượng": "5.5kg",
  "Vệ sinh": "Lau bằng khăn ẩm, tránh để ướt lâu",
  "Xuất xứ": "Việt Nam",
  "Bảo hành": "12 tháng"
}'
WHERE LOWER(name) LIKE '%ghế ăn%' AND category_id IN (SELECT id FROM categories WHERE slug = 'home-living');

-- Bàn Làm Việc Gỗ
UPDATE products SET specifications = '{
  "Chất liệu mặt bàn": "Gỗ MDF 18mm phủ Melamine chống xước",
  "Chất liệu chân bàn": "Thép sơn tĩnh điện",
  "Kích thước (DxRxC)": "120 x 60 x 75cm",
  "Tải trọng mặt bàn": "50kg",
  "Màu sắc": "Gỗ tự nhiên/Chân đen, Trắng/Chân đen, Gỗ óc chó/Chân đen",
  "Khe quản lý dây": "Có, phía sau mặt bàn",
  "Móc treo tai nghe": "Có, 2 móc tháo lắp",
  "Lắp ráp": "Có hướng dẫn kèm theo",
  "Trọng lượng": "22kg",
  "Phong cách": "Hiện đại, tối giản",
  "Phù hợp với": "Làm việc, gaming, học tập",
  "Bảo hành": "12 tháng"
}'
WHERE LOWER(name) LIKE '%bàn làm việc%' AND category_id IN (SELECT id FROM categories WHERE slug = 'home-living');

-- Bàn Học Kệ Nhỏ
UPDATE products SET specifications = '{
  "Chất liệu mặt bàn": "Gỗ PB 15mm phủ Melamine",
  "Chất liệu chân bàn": "Thép carbon sơn tĩnh điện",
  "Kích thước bàn (DxRxC)": "100 x 50 x 75cm",
  "Kệ sách": "3 ngăn, kích thước mỗi ngăn 28 x 23cm",
  "Tải trọng": "Bàn 30kg, mỗi ngăn kệ 5kg",
  "Màu sắc": "Gỗ tự nhiên, Trắng, Xanh mint",
  "Lắp ráp": "Có hướng dẫn kèm theo",
  "Trọng lượng": "14kg",
  "Phong cách": "Đơn giản, tiết kiệm không gian",
  "Phù hợp với": "Học sinh, sinh viên, phòng nhỏ",
  "Bảo hành": "12 tháng",
  "Xuất xứ": "Việt Nam"
}'
WHERE LOWER(name) LIKE '%bàn học%' AND category_id IN (SELECT id FROM categories WHERE slug = 'home-living');

-- Bàn Sofa Tròn
UPDATE products SET specifications = '{
  "Chất liệu mặt bàn": "Gỗ MDF tròn phủ sơn PU bóng",
  "Chất liệu chân bàn": "Gỗ tự nhiên hoặc kim loại sơn tĩnh điện",
  "Đường kính mặt bàn": "60cm / 80cm (tùy lựa chọn)",
  "Chiều cao": "45cm",
  "Màu sắc": "Trắng, Đen, Walnut, Tự nhiên",
  "Tải trọng": "15kg",
  "Phong cách": "Scandinavian, Tối giản, Retro",
  "Lắp ráp": "Cần lắp ráp chân bàn, đơn giản",
  "Trọng lượng sản phẩm": "4.5kg",
  "Vệ sinh": "Lau sạch bằng khăn mềm, tránh hóa chất mạnh",
  "Bảo hành": "12 tháng",
  "Xuất xứ": "Việt Nam"
}'
WHERE LOWER(name) LIKE '%bàn sofa%' AND category_id IN (SELECT id FROM categories WHERE slug = 'home-living');

-- ============================================================
-- Verify: kiểm tra kết quả
-- ============================================================
SELECT
    p.name,
    c.slug AS category,
    CASE WHEN p.specifications IS NOT NULL THEN 'OK ✓' ELSE 'MISSING ✗' END AS specs_status,
    LEFT(p.specifications, 80) AS specs_preview
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.slug IN ('electronics', 'beauty', 'fashion', 'home-living')
AND p.status = 'ACTIVE'
ORDER BY c.slug, p.name;
