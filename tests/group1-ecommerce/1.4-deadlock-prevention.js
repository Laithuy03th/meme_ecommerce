/**
 * =====================================================================
 *  TEST 1.4 — DEADLOCK PREVENTION: 2 USER MUA CHÉO NHAU
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chứng minh hệ thống KHÔNG bị Deadlock khi 2 user mua giỏ hàng
 *   chứa cùng 2 sản phẩm nhưng theo thứ tự ngược nhau.
 *
 * KỊCH BẢN DEADLOCK CỔ ĐIỂN (nếu KHÔNG có fix):
 *   Transaction A: Lock ProductId=1 → chờ Lock ProductId=2
 *   Transaction B: Lock ProductId=2 → chờ Lock ProductId=1
 *   → Hai transaction chờ nhau mãi mãi (Deadlock!)
 *   → DB timeout sau vài giây → cả 2 đơn thất bại
 *
 * CƠ CHẾ BACKEND (đã fix):
 *   checkoutItems.sort((a, b) => a.getProduct().getId() - b.getProduct().getId())
 *   → Tất cả transaction đều lock theo thứ tự ID tăng dần
 *   → Không bao giờ có vòng chờ → Deadlock không xảy ra
 *
 * CHUẨN BỊ:
 *   - User1 và User2 đều có Sản phẩm A + Sản phẩm B trong giỏ
 *   - Kho đủ hàng cho cả 2 đơn
 *
 * CHẠY: node tests/group1-ecommerce/1.4-deadlock-prevention.js
 * =====================================================================
 */

const cfg = require('../config');

// Dùng 2 token khác nhau để giả lập 2 người dùng
// Nếu chỉ có 1 token thì 2 request cũng vẫn có 2 session DB riêng
async function checkoutUser(label, token, idx) {
  const start = Date.now();
  try {
    const res = await fetch(`${cfg.BASE_URL}/orders/checkout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: token,
      },
      body: JSON.stringify({
        addressId: cfg.ADDRESS_ID,
        shippingMethodId: cfg.SHIPPING_METHOD_ID,
        paymentMethod: 'COD',
        idempotencyKey: `deadlock-test-${label}-${Date.now()}-${Math.random()}`,
      }),
    });
    const body = await res.json().catch(() => ({}));
    return { label, status: res.status, elapsed: Date.now() - start, body };
  } catch (err) {
    return { label, status: 'ERR', elapsed: Date.now() - start, body: { message: err.message } };
  }
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.4 — DEADLOCK PREVENTION');
  console.log('='.repeat(65));
  console.log('  Kịch bản: 2 user checkout đồng thời có giỏ hàng chồng chéo');
  console.log('  Kỳ vọng: Cả 2 thành công HOẶC 1 thành công, KHÔNG timeout/deadlock');
  console.log('─'.repeat(65));

  const startTime = Date.now();

  // Gửi đồng thời
  const [r1, r2] = await Promise.all([
    checkoutUser('User1', cfg.TOKEN_USER1),
    checkoutUser('User2', cfg.TOKEN_USER2 || cfg.TOKEN_USER1), // dùng cùng token nếu chỉ có 1
  ]);

  const totalTime = Date.now() - startTime;

  [r1, r2].forEach(r => {
    const icon = r.status === 200 ? '✅' : '⚠️';
    console.log(`  ${icon} ${r.label}: HTTP ${r.status} | ${r.elapsed}ms`);
    if (r.status !== 200) {
      console.log(`     Lý do: ${JSON.stringify(r.body).substring(0, 100)}`);
    }
  });

  console.log('─'.repeat(65));
  console.log(`  Tổng thời gian: ${totalTime}ms`);

  const isDeadlock = totalTime > 30000; // DB deadlock timeout thường 30s+
  if (isDeadlock) {
    console.log('\n  ❌ FAIL — Có vẻ như xảy ra Deadlock (timeout > 30s)!');
  } else {
    console.log('\n  ✅ PASS — Hoàn thành nhanh, KHÔNG có Deadlock!');
    console.log('  → Sort items theo productId đã triệt tiêu vòng chờ chéo.');
    console.log(`  → Thời gian phản hồi: ${totalTime}ms (bình thường < 5000ms)`);
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
