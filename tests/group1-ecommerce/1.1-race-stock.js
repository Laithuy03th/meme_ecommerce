/**
 * =====================================================================
 *  TEST 1.1 — RACE CONDITION: NHIỀU NGƯỜI CÙNG MUA 1 SẢN PHẨM
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chứng minh hệ thống KHÔNG bao giờ bán quá số lượng tồn kho
 *   dù 10 người bấm mua cùng 1 mili-giây.
 *
 * CƠ CHẾ BACKEND:
 *   UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?
 *   → Nếu stock = 0, WHERE không thỏa → 0 rows affected → throw exception
 *   → Chỉ đúng 1 request thành công, 9 request còn lại nhận lỗi hết hàng.
 *
 * CHUẨN BỊ TRƯỚC KHI CHẠY:
 *   1. Vào Admin → Sản phẩm → Set tồn kho = 1 (hoặc VARIANT stock = 1)
 *   2. User1 phải đã thêm sản phẩm đó vào giỏ hàng
 *   3. Điền TOKEN_USER1, PRODUCT_ID/VARIANT_ID, ADDRESS_ID trong config.js
 *
 * CHẠY: node tests/group1-ecommerce/1.1-race-stock.js
 * =====================================================================
 */

const cfg = require('../config');
const CONCURRENT_USERS = 10;

async function checkout(idx) {
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
        // Mỗi request có idempotencyKey khác nhau → lách qua tầng Idempotency
        // để test đúng tầng Atomic SQL
        idempotencyKey: `race-test-${Date.now()}-${idx}-${Math.random()}`,
      }),
    });
    const text = await res.text();
    let body;
    try { body = JSON.parse(text); } catch (e) { body = text; }
    const elapsed = Date.now() - start;
    return { idx: idx + 1, status: res.status, elapsed, body };
  } catch (err) {
    return { idx: idx + 1, status: 'ERR', elapsed: Date.now() - start, body: err.message };
  }
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.1 — RACE CONDITION: MUA HÀNG ĐỒNG THỜI');
  console.log('='.repeat(65));
  console.log(`  Tung ${CONCURRENT_USERS} request cùng một lúc...`);
  console.log(`  Sản phẩm ID: ${cfg.PRODUCT_ID} | Tồn kho cần set = 1`);
  console.log('─'.repeat(65));

  // Tạo tất cả request nhưng CHƯA gửi
  const promises = Array.from({ length: CONCURRENT_USERS }, (_, i) => checkout(i));

  // Promise.all → gửi đồng loạt cùng 1 thời điểm
  const results = await Promise.all(promises);

  let successCount = 0;
  let failCount = 0;

  results.forEach(r => {
    const icon = r.status === 200 ? '✅' : '❌';
    const msg = r.status === 200
      ? `ĐẶT HÀNG THÀNH CÔNG (${r.elapsed}ms)`
      : `Từ chối: ${JSON.stringify(r.body).substring(0, 80)} (${r.elapsed}ms)`;
    console.log(`  ${icon} Luồng ${String(r.idx).padStart(2, '0')} | HTTP ${r.status} | ${msg}`);
    if (r.status === 200) successCount++;
    else failCount++;
  });

  console.log('─'.repeat(65));
  console.log(`  TỔNG KẾT: ${successCount} thành công / ${failCount} bị từ chối`);

  if (successCount === 1 && failCount === CONCURRENT_USERS - 1) {
    console.log('\n  ✅ PASS — Hệ thống xử lý đúng! Đúng 1 đơn được tạo.');
    console.log('  → Tầng Atomic SQL UPDATE hoạt động chính xác.');
    console.log('  → Tồn kho KHÔNG bị âm dù có nhiều request đồng thời.');
  } else if (successCount > 1) {
    console.log('\n  ❌ FAIL — Có nhiều hơn 1 đơn được tạo! Kiểm tra lại cơ chế lock.');
  } else if (successCount === 0) {
    console.log('\n  ⚠️  KHÔNG CÓ đơn nào thành công. Kiểm tra lại token/addressId/stock.');
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
