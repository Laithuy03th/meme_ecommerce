/**
 * =====================================================================
 *  TEST 1.9 — REVIEW GUARD: CHỈ ĐƯỢC REVIEW KHI ĐÃ MUA & GIAO HÀNG
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chứng minh hệ thống ngăn user đánh giá sản phẩm khi chưa mua,
 *   hoặc khi đơn hàng chưa được giao thành công.
 *
 * LUỒNG KIỂM TRA:
 *   1. Thử review sản phẩm bất kỳ (chưa mua) → phải bị từ chối
 *   2. Thử review sản phẩm đã mua nhưng đơn PENDING → bị từ chối
 *   3. Review sản phẩm đã mua + đơn DELIVERED → thành công
 *
 * CHẠY: node tests/group1-ecommerce/1.9-review-guard.js
 * =====================================================================
 */

const cfg = require('../config');

async function submitReview(productId, rating, comment, token) {
  const res = await fetch(`${cfg.BASE_URL}/reviews`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: token },
    body: JSON.stringify({
      productId,
      rating,
      comment,
    }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function getMyOrders(token) {
  const res = await fetch(`${cfg.BASE_URL}/orders/my-orders?page=0&size=10`, {
    headers: { Authorization: token },
  });
  const body = await res.json().catch(() => ({}));
  return body.content ?? body.data ?? [];
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.9 — REVIEW GUARD: KIỂM TRA QUYỀN ĐÁNH GIÁ');
  console.log('='.repeat(65));

  // Case 1: Review sản phẩm ngẫu nhiên (chưa mua)
  const UNOWNED_PRODUCT = 9999; // ID sản phẩm không tồn tại / chưa mua
  console.log(`\n  [Case 1] Review sản phẩm chưa mua (ProductId=${UNOWNED_PRODUCT})...`);
  const r1 = await submitReview(UNOWNED_PRODUCT, 5, 'Test review', cfg.TOKEN_USER1);
  const icon1 = (r1.status === 400 || r1.status === 403 || r1.status === 404) ? '✅' : '❌';
  console.log(`  ${icon1} HTTP ${r1.status} | ${JSON.stringify(r1.body).substring(0, 100)}`);
  console.log(`  ${icon1} ${r1.status !== 200 ? 'PASS — Bị từ chối đúng' : 'FAIL — Review không nên thành công!'}`);

  // Case 2: Review sản phẩm đã mua (lấy từ đơn thực tế)
  console.log('\n  [Case 2] Lấy danh sách đơn hàng để tìm sản phẩm đã mua...');
  const orders = await getMyOrders(cfg.TOKEN_USER1);

  if (orders.length === 0) {
    console.log('  ⚠️  Không có đơn hàng nào. Cần đặt hàng trước.');
  } else {
    // Tìm đơn DELIVERED (nếu có)
    const deliveredOrder = orders.find(o => o.status === 'DELIVERED');
    const pendingOrder = orders.find(o => o.status === 'PENDING' || o.status === 'CONFIRMED');

    if (pendingOrder && pendingOrder.items && pendingOrder.items.length > 0) {
      const pid = pendingOrder.items[0].productId ?? pendingOrder.items[0].product?.id;
      console.log(`\n  [Case 2a] Review SP đã mua nhưng đơn PENDING (ProductId=${pid})...`);
      const r2 = await submitReview(pid, 5, 'Test pending order review', cfg.TOKEN_USER1);
      const icon2 = (r2.status !== 200) ? '✅' : '❌';
      console.log(`  ${icon2} HTTP ${r2.status} | ${r2.status !== 200 ? 'PASS — Bị từ chối (đơn chưa giao)' : 'FAIL — Review không nên thành công!'}`);
    }

    if (deliveredOrder && deliveredOrder.items && deliveredOrder.items.length > 0) {
      const pid = deliveredOrder.items[0].productId ?? deliveredOrder.items[0].product?.id;
      console.log(`\n  [Case 2b] Review SP đã mua + đơn DELIVERED (ProductId=${pid})...`);
      const r3 = await submitReview(pid, 5, 'Sản phẩm tốt, giao hàng nhanh!', cfg.TOKEN_USER1);
      const icon3 = (r3.status === 200 || r3.status === 201) ? '✅' : '⚠️';
      console.log(`  ${icon3} HTTP ${r3.status} | ${r3.status === 200 || r3.status === 201 ? 'PASS — Review thành công!' : JSON.stringify(r3.body).substring(0, 100)}`);
    } else {
      console.log('  ⚠️  Chưa có đơn DELIVERED. Cần admin chuyển 1 đơn sang DELIVERED để test Case 2b.');
    }
  }

  console.log('\n  📋 TÓM TẮT REVIEW GUARD:');
  console.log('  ─────────────────────────────────────────────────────');
  console.log('  Hệ thống kiểm tra trước khi cho phép review:');
  console.log('  1. User đã đăng nhập chưa? (JWT Token)');
  console.log('  2. User có đơn hàng chứa sản phẩm này không?');
  console.log('  3. Đơn hàng đó đã DELIVERED chưa?');
  console.log('  → Nếu bất kỳ điều kiện nào KHÔNG thỏa → 400/403 Forbidden');
  console.log('='.repeat(65));
}

run().catch(console.error);
