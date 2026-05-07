-- ============================================================
-- Migration 1: chat_messages table setup
-- ============================================================
ALTER TABLE chat_messages
  ALTER COLUMN message TYPE TEXT,
  ALTER COLUMN response TYPE TEXT;

ALTER TABLE chat_messages
  ADD COLUMN IF NOT EXISTS response_data TEXT,
  ADD COLUMN IF NOT EXISTS quick_replies TEXT;

-- ============================================================
-- Migration 2: Fix faq_documents embedding dimension
-- gemini-embedding-001 thực tế trả 768 dims, không phải 3072
-- ============================================================

-- Bước 1: Xóa dữ liệu cũ (nếu có embedding sai dimension)
TRUNCATE TABLE public.faq_documents RESTART IDENTITY;

-- Bước 2: Đổi cột embedding sang vector(3072)
ALTER TABLE public.faq_documents
  ALTER COLUMN embedding TYPE vector(3072)
  USING NULL::vector(3072);

-- Bước 3: Tạo index IVFFLAT để tìm kiếm nhanh hơn (tùy chọn, cần >= 100 rows)
-- CREATE INDEX IF NOT EXISTS faq_documents_embedding_idx
--   ON faq_documents USING ivfflat (embedding vector_cosine_ops)
--   WITH (lists = 5);

-- Verify
SELECT column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_name = 'faq_documents';
