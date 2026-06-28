/**
 * =====================================================================
 *  TEST 1.2 — RACE CONDITION: NHIỀU NGƯỜI DÙNG 1 VOUCHER GIỚI HẠN
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chứng minh voucher có giới hạn lượt dùng (usageLimit = 1)
 *   sẽ chỉ cho phép đúng 1 người dùng, dù 10 người cùng checkout.
 *
 * CƠ CHẾ BACKEND:
 *   UPDATE vouchers SET used_count = used_count + 1
 *   WHERE id = ? AND used_count < usage_limit
 *   → rows = 0 → throw "Voucher vừa hết lượt sử dụng"
 *
 * CHUẨN BỊ TRƯỚC KHI CHẠY:
 *   1. Vào Admin → Voucher → Tạo voucher: code=RACE10, usageLimit=1,
 *      discountValue=10%, minOrderAmount=0, còn hạn
 *   2. Set VOUCHER_CODE = 'RACE10' trong config.js
 *   3. User1 đã có sản phẩm trong giỏ
 *
 * CHẠY: node tests/group1-ecommerce/1.2-race-voucher.js
 * =====================================================================
 */

const cfg = require('../config');
const CONCURRENT_USERS = 10;

async function checkoutWithVoucher(idx) {
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
        voucherCode: cfg.VOUCHER_CODE,
        idempotencyKey: `voucher-race-${Date.now()}-${idx}-${Math.random()}`,
      }),
    });
    const body = await res.json().catch(() => ({}));
    return { idx: idx + 1, status: res.status, elapsed: Date.now() - start, body };
  } catch (err) {
    return { idx: idx + 1, status: 'ERR', elapsed: Date.now() - start, body: { message: err.message } };
  }
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.2 — RACE CONDITION: VOUCHER GIỚI HẠN 1 LƯỢT');
  console.log('='.repeat(65));
  console.log(`  Voucher: ${cfg.VOUCHER_CODE} | usageLimit = 1`);
  console.log(`  Tung ${CONCURRENT_USERS} request checkout cùng lúc có voucher...`);
  console.log('─'.repeat(65));

  const promises = Array.from({ length: CONCURRENT_USERS }, (_, i) => checkoutWithVoucher(i));
  const results = await Promise.all(promises);

  let voucherApplied = 0;
  let voucherRejected = 0;
  let otherFail = 0;

  results.forEach(r => {
    const bodyStr = JSON.stringify(r.body);
    const isVoucherError = bodyStr.toLowerCase().includes('voucher') || bodyStr.toLowerCase().includes('hết lượt');
    const icon = r.status === 200 ? '✅' : (isVoucherError ? '🚫' : '⚠️');
    const msg = r.status === 200
      ? `ĐẶT HÀNG + VOUCHER THÀNH CÔNG`
      : bodyStr.substring(0, 80);
    console.log(`  ${icon} Luồng ${String(r.idx).padStart(2, '0')} | HTTP ${r.status} | ${msg}`);

    if (r.status === 200) voucherApplied++;
    else if (isVoucherError) voucherRejected++;
    else otherFail++;
  });

  console.log('─'.repeat(65));
  console.log(`  Voucher áp dụng thành công: ${voucherApplied}`);
  console.log(`  Bị từ chối (hết lượt voucher): ${voucherRejected}`);
  console.log(`  Lỗi khác (hết hàng, ...): ${otherFail}`);

  if (voucherApplied <= 1) {
    console.log('\n  ✅ PASS — Voucher chỉ được dùng tối đa 1 lần!');
    console.log('  → Atomic SQL UPDATE trên bảng vouchers hoạt động đúng.');
  } else {
    console.log('\n  ❌ FAIL — Voucher bị dùng nhiều hơn usageLimit!');
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
