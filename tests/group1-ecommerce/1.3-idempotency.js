/**
 * =====================================================================
 *  TEST 1.3 — IDEMPOTENCY: BẤM NÚT ĐẶT HÀNG NHIỀU LẦN DO LAG
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chứng minh nếu user bấm nút "Đặt hàng" nhiều lần với CÙNG
 *   idempotencyKey (FE tạo 1 UUID duy nhất mỗi lần vào trang checkout),
 *   hệ thống CHỈ tạo đúng 1 đơn hàng, các request sau trả về đơn cũ.
 *
 * CƠ CHẾ BACKEND:
 *   SELECT * FROM orders WHERE idempotency_key = ? AND user_id = ?
 *   → Nếu đã tồn tại → return đơn cũ NGAY (không tạo mới, không trừ kho)
 *
 * KHÁC BIỆT VỚI TEST 1.1:
 *   - Test 1.1: idempotencyKey KHÁC NHAU (test Atomic SQL)
 *   - Test 1.3: idempotencyKey GIỐNG NHAU (test Idempotency guard)
 *
 * CHUẨN BỊ: User1 có sản phẩm trong giỏ, kho đủ hàng
 * CHẠY: node tests/group1-ecommerce/1.3-idempotency.js
 * =====================================================================
 */

const cfg = require('../config');

// 1 key cố định — giả lập FE tạo 1 UUID rồi gửi lại nhiều lần
const FIXED_KEY = `idempotency-demo-${Date.now()}`;
const REPEAT_COUNT = 5;

async function checkout(attempt) {
  const start = Date.now();
  try {
    const res = await fetch(`${cfg.BASE_URL}/orders/checkout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: cfg.TOKEN_USER1,
      },
      body: JSON.stringify({
        addressId: cfg.ADDRESS_ID,
        shippingMethodId: cfg.SHIPPING_METHOD_ID,
        paymentMethod: 'COD',
        idempotencyKey: FIXED_KEY, // ← GIỐNG NHAU cho mọi request
      }),
    });
    const body = await res.json().catch(() => ({}));
    return { attempt, status: res.status, orderId: body.id ?? body.orderId ?? '?', elapsed: Date.now() - start };
  } catch (err) {
    return { attempt, status: 'ERR', orderId: null, elapsed: Date.now() - start };
  }
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.3 — IDEMPOTENCY: BẤM NÚT NHIỀU LẦN DO LAG');
  console.log('='.repeat(65));
  console.log(`  idempotencyKey cố định: ${FIXED_KEY}`);
  console.log(`  Gửi ${REPEAT_COUNT} request với key giống nhau (tuần tự)...`);
  console.log('─'.repeat(65));

  const results = [];
  for (let i = 0; i < REPEAT_COUNT; i++) {
    // Gửi tuần tự (không đồng thời) để giả lập user bấm lại sau vài giây
    const r = await checkout(i + 1);
    results.push(r);
    console.log(`  Lần ${r.attempt}: HTTP ${r.status} | OrderId=${r.orderId} | ${r.elapsed}ms`);
    await new Promise(res => setTimeout(res, 200)); // delay 200ms giữa các lần
  }

  // Kiểm tra tất cả response có cùng orderId không
  const orderIds = results.filter(r => r.orderId && r.orderId !== '?').map(r => r.orderId);
  const uniqueOrderIds = new Set(orderIds);

  console.log('─'.repeat(65));
  console.log(`  OrderIds nhận được: [${[...uniqueOrderIds].join(', ')}]`);
  console.log(`  Số đơn hàng DUY NHẤT được tạo: ${uniqueOrderIds.size}`);

  if (uniqueOrderIds.size <= 1 && orderIds.length === REPEAT_COUNT) {
    console.log('\n  ✅ PASS — Đúng 1 đơn hàng được tạo dù bấm liên tục!');
    console.log('  → Idempotency Key guard hoạt động đúng.');
    console.log('  → Request 2-5 nhận lại đơn hàng cũ, không tạo đơn mới.');
  } else {
    console.log('\n  ❌ FAIL hoặc ⚠️  Kiểm tra lại cấu hình token/addressId.');
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
