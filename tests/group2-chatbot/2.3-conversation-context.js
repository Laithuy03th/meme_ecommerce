/**
 * =====================================================================
 *  TEST 2.3 — CHATBOT: FOLLOW-UP INTENT THEO NGỮ CẢNH
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chatbot phải nhớ ngữ cảnh cuộc hội thoại.
 *   VD: Hỏi "cho tôi xem áo hoodie" → "giá bao nhiêu?" chatbot phải
 *   hiểu là "giá áo hoodie bao nhiêu?" chứ không hỏi lại từ đầu.
 *
 * CƠ CHẾ BACKEND (ConversationContextService):
 *   - Lưu lastIntent, lastProductKeyword theo sessionId (in-memory)
 *   - detectIntentByRules() kiểm tra context khi message ngắn/mơ hồ
 *   - Follow-up phrases: "cái đó", "nó", "loại đó", "giá bao nhiêu"
 *
 * CHẠY: node tests/group2-chatbot/2.3-conversation-context.js
 * =====================================================================
 */

const cfg = require('../config');

async function chat(message, sessionId) {
  const res = await fetch(`${cfg.BASE_URL}/chatbot/message`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, sessionId, userId: null }),
  });
  const body = await res.json().catch(() => ({}));
  return body;
}

function printTurn(turn, question, answer) {
  console.log(`\n  [Lượt ${turn}] 👤 User: "${question}"`);
  console.log(`           🤖 Bot: "${(answer.response ?? '').substring(0, 150)}..."`);
  console.log(`           Intent: ${answer.intent ?? '?'} | Products: ${answer.data?.products?.length ?? 0}`);
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 2.3 — CHATBOT: FOLLOW-UP CONTEXT');
  console.log('='.repeat(65));

  // ─── Kịch bản 1: Follow-up về sản phẩm ─────────────────────────
  const sessionA = `ctx-test-A-${Date.now()}`;
  console.log(`\n  ═══ KỊCH BẢN 1: Hỏi áo hoodie → follow-up giá ═══`);
  console.log(`  SessionId: ${sessionA}`);

  const a1 = await chat('tôi muốn xem áo hoodie', sessionA);
  printTurn(1, 'tôi muốn xem áo hoodie', a1);

  await new Promise(r => setTimeout(r, 1500));

  const a2 = await chat('giá bao nhiêu vậy?', sessionA);
  printTurn(2, 'giá bao nhiêu vậy?', a2);
  const ctx1Ok = a2.intent === 'product' || (a2.response ?? '').toLowerCase().includes('giá');
  console.log(`           ✅ Follow-up đúng context: ${ctx1Ok ? 'CÓ' : '⚠️ KHÔNG (hỏi lại từ đầu)'}`);

  await new Promise(r => setTimeout(r, 1500));

  const a3 = await chat('có màu đen không?', sessionA);
  printTurn(3, 'có màu đen không?', a3);
  const ctx2Ok = a3.intent === 'product';
  console.log(`           ✅ Tiếp tục context áo hoodie: ${ctx2Ok ? 'CÓ' : '⚠️ KHÔNG'}`);

  // ─── Kịch bản 2: Chuyển topic ────────────────────────────────────
  const sessionB = `ctx-test-B-${Date.now()}`;
  console.log(`\n  ═══ KỊCH BẢN 2: Hỏi SP → hỏi chính sách → quay lại SP ═══`);
  console.log(`  SessionId: ${sessionB}`);

  const b1 = await chat('cho tôi xem váy đẹp', sessionB);
  printTurn(1, 'cho tôi xem váy đẹp', b1);
  await new Promise(r => setTimeout(r, 1500));

  const b2 = await chat('chính sách đổi trả như thế nào?', sessionB);
  printTurn(2, 'chính sách đổi trả như thế nào?', b2);
  console.log(`           ✅ Detect policy đúng: ${b2.intent === 'policy' ? 'CÓ' : '⚠️ KHÔNG'}`);
  await new Promise(r => setTimeout(r, 1500));

  const b3 = await chat('tôi muốn mua thêm giày', sessionB);
  printTurn(3, 'tôi muốn mua thêm giày', b3);
  console.log(`           ✅ Chuyển sang product intent: ${b3.intent === 'product' ? 'CÓ' : '⚠️ KHÔNG'}`);

  console.log('\n─'.repeat(65));
  console.log('  📋 CƠ CHẾ CONTEXT:');
  console.log('  ConversationContextService lưu per-session:');
  console.log('  - lastIntent: intent của lượt trước');
  console.log('  - lastProductKeyword: từ khóa sản phẩm cuối');
  console.log('  - conversationHistory: lịch sử gửi cho Gemini');
  console.log('  → Giúp chatbot hiểu "nó", "loại đó", "giá bao nhiêu" trong context');
  console.log('='.repeat(65));
}

run().catch(console.error);
