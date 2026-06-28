/**
 * =====================================================================
 *  TEST 1.8 — AUTH & CART: THÊM VÀO GIỎ KHI CHƯA LOGIN
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Kiểm tra luồng bảo mật:
 *   - API cần xác thực (giỏ hàng, đặt hàng) phải trả 401 nếu chưa login
 *   - Sau khi login với token hợp lệ thì thành công
 *
 * CHẠY: node tests/group1-ecommerce/1.8-auth-cart.js
 * =====================================================================
 */

const cfg = require('../config');

async function callApi(label, url, options) {
  const start = Date.now();
  try {
    const res = await fetch(url, options);
    const body = await res.json().catch(() => ({}));
    return { label, status: res.status, elapsed: Date.now() - start, body };
  } catch (err) {
    return { label, status: 'ERR', elapsed: Date.now() - start, body: { message: err.message } };
  }
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.8 — AUTH & CART SECURITY');
  console.log('='.repeat(65));

  const tests = [
    // ─── Không có token ───────────────────────────────────────────
    {
      label: '1. Xem giỏ hàng (không token)',
      url: `${cfg.BASE_URL}/cart`,
      options: { method: 'GET' },
      expectedStatus: 401,
    },
    {
      label: '2. Đặt hàng (không token)',
      url: `${cfg.BASE_URL}/orders/checkout`,
      options: {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ addressId: 1, shippingMethodId: 1, paymentMethod: 'COD' }),
      },
      expectedStatus: 401,
    },
    {
      label: '3. Xem đơn hàng (không token)',
      url: `${cfg.BASE_URL}/orders/my-orders`,
      options: { method: 'GET' },
      expectedStatus: 401,
    },
    // ─── Token sai / hết hạn ──────────────────────────────────────
    {
      label: '4. Xem giỏ hàng (token sai)',
      url: `${cfg.BASE_URL}/cart`,
      options: { method: 'GET', headers: { Authorization: 'Bearer invalid.token.here' } },
      expectedStatus: 401,
    },
    // ─── Token hợp lệ ─────────────────────────────────────────────
    {
      label: '5. Xem giỏ hàng (token hợp lệ)',
      url: `${cfg.BASE_URL}/cart`,
      options: { method: 'GET', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 200,
    },
    {
      label: '6. Xem đơn hàng (token hợp lệ)',
      url: `${cfg.BASE_URL}/orders/my-orders`,
      options: { method: 'GET', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 200,
    },
    // ─── User thường gọi API admin ────────────────────────────────
    {
      label: '7. Gọi API Admin Dashboard (user thường)',
      url: `${cfg.BASE_URL}/admin/dashboard`,
      options: { method: 'GET', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 403,
    },
    {
      label: '8. Xem tất cả đơn hàng - Admin API (user thường)',
      url: `${cfg.BASE_URL}/admin/orders`,
      options: { method: 'GET', headers: { Authorization: cfg.TOKEN_USER1 } },
      expectedStatus: 403,
    },
  ];

  let pass = 0;
  let fail = 0;

  for (const t of tests) {
    const r = await callApi(t.label, t.url, t.options);
    const ok = r.status === t.expectedStatus;
    const icon = ok ? '✅' : '❌';
    console.log(`  ${icon} ${t.label}`);
    console.log(`     Mong đợi: ${t.expectedStatus} | Nhận: ${r.status} | ${r.elapsed}ms`);
    if (!ok) {
      console.log(`     Body: ${JSON.stringify(r.body).substring(0, 80)}`);
      fail++;
    } else {
      pass++;
    }
    await new Promise(res => setTimeout(res, 100));
  }

  console.log('─'.repeat(65));
  console.log(`  KẾT QUẢ: ${pass} PASS / ${fail} FAIL`);
  if (fail === 0) {
    console.log('\n  ✅ PASS — Security layer hoạt động đúng!');
    console.log('  → JWT Filter chặn đúng các request không có/sai token.');
    console.log('  → Role-based access control (RBAC) hoạt động đúng.');
  } else {
    console.log('\n  ❌ Có lỗi trong cơ chế xác thực/phân quyền. Kiểm tra lại.');
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
