/**
 * =====================================================================
 *  TEST 2.2 — CHATBOT: ẢNH SẢN PHẨM KHỚP VỚI GỢI Ý AI
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Khi Chatbot gợi ý "Áo Hoodie Zip Nỉ Mềm", ảnh hiển thị phải là
 *   ảnh của chính sản phẩm đó, KHÔNG phải tất cả sản phẩm trong DB.
 *
 * CƠ CHẾ BACKEND:
 *   Gemini response kèm marker: <!--IDS:15-->
 *   → Backend parse → chỉ trả products = [id:15]
 *   → Frontend render đúng 1 ảnh
 *
 * KIỂM TRA:
 *   Gửi câu hỏi gợi ý → xem products[] trong response
 *   → Đối chiếu với tên sản phẩm trong response text
 *
 * CHẠY: node tests/group2-chatbot/2.2-product-image-sync.js
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
  return { status: res.status, body };
}

function checkSync(responseText, products) {
  if (!products || products.length === 0) return { synced: false, reason: 'Không có sản phẩm' };

  // Kiểm tra xem tên sản phẩm trong products có xuất hiện trong text không
  const synced = products.every(p => {
    const name = (p.name ?? '').toLowerCase();
    // Lấy từ đầu của tên (bỏ từ cuối để match linh hoạt)
    const nameParts = name.split(' ').slice(0, 3).join(' ');
    return responseText.toLowerCase().includes(nameParts) ||
      responseText.toLowerCase().includes(p.name?.toLowerCase().substring(0, 10));
  });

  return { synced, products: products.map(p => p.name) };
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 2.2 — CHATBOT: ẢNH SẢN PHẨM KHỚP VỚI GỢI Ý');
  console.log('='.repeat(65));

  const sessionId = `sync-test-${Date.now()}`;
  const questions = [
    'gợi ý cho tôi 1 áo hoodie phù hợp cho sinh viên',
    'tôi muốn mua váy cho bạn gái',
    'cho tôi xem sản phẩm giá rẻ nhất',
    'tôi cần áo thun cotton mềm',
  ];

  let passCount = 0;
  let failCount = 0;

  for (const q of questions) {
    console.log(`\n  📩 Câu hỏi: "${q}"`);
    const r = await chat(q, sessionId);
    const response = r.body.response ?? '';
    const products = r.body.data?.products ?? [];
    const intent = r.body.intent ?? '?';

    console.log(`     Intent: ${intent} | Số ảnh trả về: ${products.length}`);

    if (intent === 'product' && products.length > 0) {
      const check = checkSync(response, products);
      const icon = check.synced ? '✅' : '⚠️';
      console.log(`     ${icon} Ảnh đồng bộ với text: ${check.synced}`);
      console.log(`     Sản phẩm trong ảnh: [${products.map(p => p.name).join(', ')}]`);
      console.log(`     Text đề cập: "${response.substring(0, 150)}..."`);

      // Thông tin quan trọng: marker có được parse không?
      const hasMarker = !r.body.rawResponse?.includes('<!--IDS:'); // marker đã bị strip
      if (check.synced) passCount++; else failCount++;
    } else if (intent === 'product' && products.length === 0) {
      console.log('     ⚠️  Intent=product nhưng không có sản phẩm trả về');
    } else {
      console.log(`     ℹ️  Intent không phải product → không cần kiểm tra ảnh`);
    }

    await new Promise(r => setTimeout(r, 2000));
  }

  console.log('\n─'.repeat(65));
  console.log(`  KẾT QUẢ: ${passCount} đồng bộ / ${failCount} không đồng bộ`);
  console.log('\n  📋 CƠ CHẾ MARKER:');
  console.log('  Gemini nhận list sản phẩm kèm [ID:xxx], sau đó gắn');
  console.log('  <!--IDS:12,15--> vào cuối response để chỉ định SP gợi ý.');
  console.log('  Backend parse → strip marker → chỉ trả ảnh SP đó.');
  console.log('='.repeat(65));
}

run().catch(console.error);
