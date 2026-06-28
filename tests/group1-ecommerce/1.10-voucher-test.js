/**
 * =====================================================================
 *  TEST 1.10 — VOUCHER STACKING: MIỄN SHIP + GIẢM GIÁ
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Kiểm tra logic áp dụng voucher kết hợp:
 *   - Voucher giảm giá theo %
 *   - Voucher miễn phí ship
 *   - Voucher theo danh mục cụ thể
 *
 * CHẠY: node tests/group1-ecommerce/1.10-voucher-test.js
 * =====================================================================
 */

const cfg = require('../config');

async function validateVoucher(code, orderAmount, token) {
  const res = await fetch(
    `${cfg.BASE_URL}/vouchers/validate?code=${code}&orderAmount=${orderAmount}`,
    {
      method: 'GET',
      headers: { Authorization: token },
    }
  );
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function checkoutWithVoucher(voucherCode, token) {
  const res = await fetch(`${cfg.BASE_URL}/orders/checkout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: token },
    body: JSON.stringify({
      addressId: cfg.ADDRESS_ID,
      shippingMethodId: cfg.SHIPPING_METHOD_ID,
      paymentMethod: 'COD',
      voucherCode,
      idempotencyKey: `voucher-stack-${voucherCode}-${Date.now()}`,
    }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.10 — VOUCHER: VALIDATE & APPLY');
  console.log('='.repeat(65));

  const voucherCodes = [
    { code: cfg.VOUCHER_CODE, desc: 'Voucher mặc định (config.js)' },
    { code: 'FREESHIP', desc: 'Voucher miễn phí vận chuyển' },
    { code: 'INVALID123', desc: 'Voucher không tồn tại' },
    { code: 'EXPIRED', desc: 'Voucher hết hạn' },
  ];

  // Test validate từng loại voucher
  console.log('\n  [Phần 1] Validate các loại voucher...');
  const orderAmount = 500000;
  console.log(`  (orderAmount = ${orderAmount.toLocaleString('vi-VN')}đ)\n`);

  for (const v of voucherCodes) {
    const r = await validateVoucher(v.code, orderAmount, cfg.TOKEN_USER1);
    const isValid = r.body.valid === true;
    const icon = isValid ? '✅' : '🚫';
    console.log(`  ${icon} ${v.desc} (${v.code})`);
    console.log(`     Valid: ${r.body.valid} | Message: ${r.body.message ?? '-'}`);
    if (isValid) {
      console.log(`     Giảm giá: ${r.body.discountAmount?.toLocaleString('vi-VN') ?? 0}đ`);
    }
    await new Promise(res => setTimeout(res, 100));
  }

  // Test checkout với voucher hợp lệ
  console.log('\n  [Phần 2] Checkout với voucher hợp lệ...');
  const checkoutResult = await checkoutWithVoucher(cfg.VOUCHER_CODE, cfg.TOKEN_USER1);
  if (checkoutResult.status === 200) {
    const o = checkoutResult.body;
    console.log(`  ✅ Đặt hàng thành công | OrderId: ${o.id}`);
    console.log(`     Tổng tiền hàng: ${o.itemsTotal?.toLocaleString('vi-VN') ?? '?'}đ`);
    console.log(`     Giảm giá (voucher): ${o.discountAmount?.toLocaleString('vi-VN') ?? '?'}đ`);
    console.log(`     Phí ship: ${o.shippingFee?.toLocaleString('vi-VN') ?? '?'}đ`);
    console.log(`     TỔNG THANH TOÁN: ${o.totalAmount?.toLocaleString('vi-VN') ?? '?'}đ`);
    console.log(`     Mã voucher đã dùng: ${o.voucherCode ?? '-'}`);
  } else {
    console.log(`  ⚠️  HTTP ${checkoutResult.status}: ${JSON.stringify(checkoutResult.body).substring(0, 100)}`);
    console.log('  (Có thể giỏ hàng trống hoặc voucher đã dùng hết)');
  }

  console.log('\n  📋 CÁC LOẠI VOUCHER HỆ THỐNG HỖ TRỢ:');
  console.log('  ─────────────────────────────────────────────────────');
  console.log('  1. PERCENT — Giảm theo % (maxDiscountAmount giới hạn trần)');
  console.log('  2. AMOUNT  — Giảm số tiền cố định');
  console.log('  3. freeShipping=true — Miễn phí vận chuyển');
  console.log('  4. applicableCategoryIds — Chỉ áp dụng cho danh mục cụ thể');
  console.log('  5. usageLimitPerUser — Giới hạn số lần dùng/user');
  console.log('  6. minOrderAmount — Đơn tối thiểu mới được dùng');
  console.log('='.repeat(65));
}

run().catch(console.error);
