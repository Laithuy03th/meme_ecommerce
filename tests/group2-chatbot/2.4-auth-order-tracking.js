/**
 * =====================================================================
 *  TEST 2.4 — CHATBOT: THEO DÕI ĐƠN HÀNG (AUTH GUARD)
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chatbot phải yêu cầu đăng nhập khi user hỏi về đơn hàng
 *   mà chưa cung cấp userId. Sau khi đăng nhập (userId có giá trị)
 *   chatbot mới trả thông tin đơn hàng.
 *
 * CHẠY: node tests/group2-chatbot/2.4-auth-order-tracking.js
 * =====================================================================
 */

const cfg = require('../config');

async function chat(message, sessionId, userId = null) {
  const res = await fetch(`${cfg.BASE_URL}/chatbot/message`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, sessionId, userId }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 2.4 — CHATBOT: THEO DÕI ĐƠN HÀNG (AUTH GUARD)');
  console.log('='.repeat(65));

  const sessionId = `order-track-${Date.now()}`;
  const orderQuestions = [
    'đơn hàng của tôi đang ở đâu?',
    'cho tôi biết trạng thái đơn hàng',
    'tôi muốn kiểm tra đơn #123',
    'đơn hàng gần nhất của tôi',
  ];

  // Case 1: CHƯA đăng nhập (userId = null)
  console.log('\n  ═══ CASE 1: Hỏi đơn hàng khi CHƯA đăng nhập ═══');
  for (const q of orderQuestions.slice(0, 2)) {
    const r = await chat(q, sessionId, null);
    const response = r.body.response ?? '';
    const requiresAuth = r.body.requiresAuth === true ||
      response.toLowerCase().includes('đăng nhập') ||
      response.toLowerCase().includes('đăng ký') ||
      response.toLowerCase().includes('login');

    const icon = requiresAuth ? '✅' : '⚠️';
    console.log(`\n  ${icon} Câu hỏi: "${q}"`);
    console.log(`     Intent: ${r.body.intent ?? '?'} | requiresAuth: ${r.body.requiresAuth ?? false}`);
    console.log(`     Response: "${response.substring(0, 120)}"`);
    console.log(`     ${requiresAuth ? 'PASS — Chatbot yêu cầu đăng nhập đúng' : 'FAIL — Chatbot không yêu cầu đăng nhập!'}`);
    await new Promise(r => setTimeout(r, 1500));
  }

  // Case 2: ĐÃ đăng nhập (userId có giá trị)
  const loggedInUserId = 5; // Thay bằng userId thực tế trong DB
  console.log(`\n  ═══ CASE 2: Hỏi đơn hàng khi ĐÃ đăng nhập (userId=${loggedInUserId}) ═══`);

  for (const q of orderQuestions.slice(2)) {
    const r = await chat(q, sessionId, loggedInUserId);
    const response = r.body.response ?? '';
    const hasOrderInfo = r.body.data?.order != null || r.body.data?.orders != null ||
      response.toLowerCase().includes('đơn') ||
      response.toLowerCase().includes('order');

    const icon = hasOrderInfo ? '✅' : 'ℹ️';
    console.log(`\n  ${icon} Câu hỏi: "${q}"`);
    console.log(`     Intent: ${r.body.intent ?? '?'} | Order data: ${r.body.data?.order ? 'CÓ' : 'KHÔNG'}`);
    console.log(`     Response: "${response.substring(0, 120)}"`);
    await new Promise(r => setTimeout(r, 1500));
  }

  console.log('\n─'.repeat(65));
  console.log('  📋 CƠ CHẾ AUTH GUARD CHATBOT:');
  console.log('  - FE gửi userId (lấy từ JWT) kèm mỗi request chatbot');
  console.log('  - handleOrderTracking() kiểm tra: if (userId == null) → requiresAuth = true');
  console.log('  - Response chứa requiresAuth=true → FE hiện nút "Đăng nhập"');
  console.log('  - Sau khi đăng nhập → FE gửi lại message với userId thật');
  console.log('='.repeat(65));
}

run().catch(console.error);
