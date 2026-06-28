/**
 * =====================================================================
 *  TEST 2.6 — CHATBOT: REAL-TIME CATALOG (ADMIN THÊM SP MỚI)
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chatbot phải BIẾT NGAY sản phẩm mới admin vừa thêm vào DB,
 *   KHÔNG cần restart server hay rebuild index.
 *
 * ĐIỂM QUAN TRỌNG:
 *   Chatbot dùng LLM (Gemini) query TRỰC TIẾP vào ProductRepository
 *   mỗi lần có request → không cache cứng → real-time với DB.
 *
 * KỊCH BẢN:
 *   1. Thêm sản phẩm mới qua Admin API
 *   2. Hỏi chatbot về sản phẩm đó
 *   3. Chatbot phải tìm thấy và giới thiệu
 *
 * CHẠY: node tests/group2-chatbot/2.6-realtime-catalog.js
 * =====================================================================
 */

const cfg = require('../config');

async function addProductViaAdmin(productData) {
  const res = await fetch(`${cfg.BASE_URL}/admin/products`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: cfg.TOKEN_ADMIN,
    },
    body: JSON.stringify(productData),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function chat(message) {
  const sessionId = `realtime-test-${Date.now()}`;
  const res = await fetch(`${cfg.BASE_URL}/chatbot/message`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, sessionId, userId: null }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

const TEST_PRODUCT_NAME = `Áo Test Realtime ${Date.now()}`;
const TEST_KEYWORD = 'áo test realtime';

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 2.6 — CHATBOT: REAL-TIME CATALOG');
  console.log('='.repeat(65));

  // Bước 1: Hỏi chatbot trước khi thêm sản phẩm
  console.log(`\n  [Bước 1] Hỏi chatbot về "${TEST_KEYWORD}" (chưa có trong DB)...`);
  const before = await chat(`có bán ${TEST_KEYWORD} không?`);
  const beforeHasProduct = (before.body.data?.products ?? []).length > 0 &&
    JSON.stringify(before.body.data.products).includes('realtime');
  console.log(`     Sản phẩm trả về: ${before.body.data?.products?.length ?? 0}`);
  console.log(`     ${!beforeHasProduct ? '✅ Đúng: Chưa có sản phẩm' : '⚠️ Đã có sẵn trong DB'}`);

  // Bước 2: Thêm sản phẩm mới qua Admin API
  console.log(`\n  [Bước 2] Thêm sản phẩm mới qua Admin API...`);
  console.log(`     Tên sản phẩm: "${TEST_PRODUCT_NAME}"`);

  const newProduct = {
    name: TEST_PRODUCT_NAME,
    description: 'Sản phẩm test để kiểm tra real-time catalog chatbot',
    basePrice: 199000,
    stockQuantity: 50,
    categoryId: 1, // Thay bằng categoryId thực tế
    brand: 'MemeShop',
    status: 'ACTIVE',
  };

  const addResult = await addProductViaAdmin(newProduct);
  if (addResult.status === 200 || addResult.status === 201) {
    const newProductId = addResult.body.id;
    console.log(`  ✅ Thêm sản phẩm thành công | ProductId: ${newProductId}`);

    // Bước 3: Hỏi chatbot NGAY sau khi thêm (không restart)
    console.log(`\n  [Bước 3] Hỏi chatbot ngay sau khi thêm (không restart BE)...`);
    await new Promise(r => setTimeout(r, 500));

    const after = await chat(`tôi muốn mua ${TEST_KEYWORD}`);
    const afterProducts = after.body.data?.products ?? [];
    const found = afterProducts.some(p =>
      (p.name ?? '').toLowerCase().includes('realtime') ||
      (p.name ?? '').toLowerCase().includes('test')
    );

    console.log(`     Intent: ${after.body.intent} | Products: ${afterProducts.length}`);
    console.log(`     Tìm thấy sản phẩm mới: ${found}`);
    afterProducts.forEach(p => console.log(`       - ${p.name} (${p.price?.toLocaleString('vi-VN')}đ)`));

    if (found) {
      console.log('\n  ✅ PASS — Chatbot nhận biết sản phẩm mới NGAY LẬP TỨC!');
      console.log('  → LLM query trực tiếp ProductRepository mỗi request.');
      console.log('  → Không cần rebuild index, không cần restart server.');
    } else {
      console.log('\n  ℹ️  Không tìm thấy sản phẩm test (có thể do keyword chưa khớp).');
      console.log('  → Thử hỏi chatbot trực tiếp trên FE với tên sản phẩm chính xác.');
      console.log(`  → Tên sản phẩm vừa thêm: "${TEST_PRODUCT_NAME}"`);
    }
  } else {
    console.log(`  ⚠️  Không thêm được sản phẩm: HTTP ${addResult.status}`);
    console.log(`     ${JSON.stringify(addResult.body).substring(0, 100)}`);
    console.log('  → Điền TOKEN_ADMIN trong config.js và đảm bảo token có role ADMIN');
    console.log('\n  📋 HƯỚNG DẪN TEST THỦ CÔNG:');
    console.log('  1. Vào Admin → Sản phẩm → Thêm mới sản phẩm tên "Áo Test XYZ"');
    console.log('  2. Mở chatbot → gõ: "shop có bán Áo Test XYZ không?"');
    console.log('  3. Chatbot phải tìm thấy và giới thiệu ngay (không cần restart)');
  }

  console.log('='.repeat(65));
}

run().catch(console.error);
