/**
 * =====================================================================
 *  TEST 2.1 — CHATBOT: HỎI SẢN PHẨM SHOP KHÔNG BÁN
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chatbot phải trả lời "shop không bán X" thay vì hallucinate
 *   hoặc gợi ý sản phẩm không liên quan khi hỏi về mặt hàng
 *   shop không kinh doanh.
 *
 * CƠ CHẾ BACKEND:
 *   1. Chatbot detect intent = "product"
 *   2. Query DB → products rỗng (không tìm thấy)
 *   3. Thử relax constraints → vẫn rỗng
 *   4. Lấy category list từ DB → trả lời "shop không bán X, hiện có: ..."
 *
 * CHẠY: node tests/group2-chatbot/2.1-unknown-product.js
 * =====================================================================
 */

const cfg = require('../config');
let sessionId = `test-unknown-${Date.now()}`;

async function chat(message, sessionId, userId = null) {
  const start = Date.now();
  const res = await fetch(`${cfg.BASE_URL}/chatbot/message`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, sessionId, userId }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, elapsed: Date.now() - start, body };
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 2.1 — CHATBOT: HỎI SẢN PHẨM SHOP KHÔNG BÁN');
  console.log('='.repeat(65));

  const unknownProducts = [
    'shop có bán thìa không?',
    'mua nồi chiên không dầu được không?',
    'tôi cần tìm máy tính laptop',
    'bán điện thoại Samsung không?',
    'shop có tủ lạnh không?',
  ];

  for (const msg of unknownProducts) {
    const r = await chat(msg, sessionId);
    const response = r.body.response ?? '';
    const intent = r.body.intent ?? '?';
    const products = r.body.data?.products ?? [];

    // Kiểm tra: phải KHÔNG trả về sản phẩm ngẫu nhiên
    const isCorrect = products.length === 0 || response.toLowerCase().includes('không bán') ||
      response.toLowerCase().includes('chưa kinh doanh') ||
      response.toLowerCase().includes('không có') ||
      response.toLowerCase().includes('chưa có');

    const icon = isCorrect ? '✅' : '⚠️';
    console.log(`\n  ${icon} Câu hỏi: "${msg}"`);
    console.log(`     Intent: ${intent} | Sản phẩm trả về: ${products.length} cái`);
    console.log(`     Phản hồi: "${response.substring(0, 120)}..."`);
    if (!isCorrect) {
      console.log('     ⚠️  Chatbot gợi ý sản phẩm không liên quan — cần kiểm tra lại!');
    }

    await new Promise(r => setTimeout(r, 1500)); // Tránh rate limit
  }

  console.log('\n─'.repeat(65));
  console.log('  📋 KẾT LUẬN:');
  console.log('  Chatbot phải nói rõ "shop không bán X" + liệt kê category thực tế');
  console.log('  → Không hallucinate, không gợi ý sản phẩm ngẫu nhiên');
  console.log('  → Dữ liệu category lấy động từ DB (admin thêm mới tự cập nhật)');
  console.log('='.repeat(65));
}

run().catch(console.error);
