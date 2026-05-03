ALTER TABLE chat_messages
  ALTER COLUMN message TYPE TEXT,
  ALTER COLUMN response TYPE TEXT;

ALTER TABLE chat_messages
  ADD COLUMN IF NOT EXISTS response_data TEXT,
  ADD COLUMN IF NOT EXISTS quick_replies TEXT;

TRUNCATE TABLE public.faq_documents RESTART IDENTITY;

ALTER TABLE public.faq_documents
ALTER COLUMN embedding TYPE vector(3072);
