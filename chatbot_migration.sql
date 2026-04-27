-- =====================================================================
-- CHATBOT FIX MIGRATION SCRIPT (Phiên bản đã cập nhật)
-- Chạy từng bước theo thứ tự trong pgAdmin hoặc psql
-- Database: meme_ecommerce
-- =====================================================================

-- =====================================================================
-- BƯỚC 1: Mở rộng cột chat_messages sang TEXT
-- (Hibernate ddl-auto=update KHÔNG tự ALTER cột đang tồn tại)
-- Chạy 2 lệnh này TRƯỚC khi restart backend
-- =====================================================================

ALTER TABLE chat_messages ALTER COLUMN message TYPE TEXT;
ALTER TABLE chat_messages ALTER COLUMN response TYPE TEXT;

-- Verify:
-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'chat_messages' AND column_name IN ('message', 'response');

-- =====================================================================
-- BƯỚC 2: Xóa session cũ (lịch sử cũ có intent sai + response cụt)
-- =====================================================================

-- Xóa session cụ thể (thay session_id bằng giá trị thật):
-- DELETE FROM chat_messages WHERE session_id = '563b9020-08e9-4436-bd36-aaea5d42951b';

-- Hoặc xóa toàn bộ để test sạch (chỉ dùng khi test):
-- DELETE FROM chat_messages;

-- =====================================================================
-- KHÔNG CẦN SEED FAQ THỦ CÔNG!
-- FaqDataSeeder.java đã tự động seed 16 FAQ documents + tạo embedding
-- mỗi khi backend khởi động (nếu count < 16).
-- Sau khi restart backend, kiểm tra bằng:
-- SELECT id, category, title, (embedding IS NOT NULL) as has_embedding
-- FROM faq_documents ORDER BY id;
-- =====================================================================

-- =====================================================================
-- BƯỚC 3: Sau khi chạy BƯỚC 1 ở trên, restart backend.
-- FaqDataSeeder sẽ tự seed 16 documents và gọi Gemini embed cho mỗi doc.
-- (Quá trình này mất khoảng 32-40 giây do sleep 2s giữa mỗi doc x 16 docs)
-- Xem log backend: "RAG FAQ: Done! Seeded 16/16 documents successfully."
--
-- Sau đó trên FE, xóa session cũ:
-- localStorage.removeItem("chatbot_session_id");
-- =====================================================================
