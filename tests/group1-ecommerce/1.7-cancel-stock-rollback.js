/**
 * =====================================================================
 *  TEST 1.7 — HỦY ĐƠN → HOÀN KHO (STOCK ROLLBACK)
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chứng minh khi user hủy đơn hàng, tồn kho sẽ được cộng lại.
 *   Đây là tính năng quan trọng để đảm bảo dữ liệu kho hàng chính xác.
 *
 * CƠ CHẾ BACKEND:
 *   cancelOrder() → stockService.increaseStock(productId, quantity)
 *   → SQL: UPDATE products SET stock = stock + ? WHERE id = ?
 *
 * LUỒNG TEST:
 *   1. Lấy số lượng tồn kho hiện tại
 *   2. Đặt hàng (số lượng = N) → kho giảm N
 *   3. Hủy đơn → kho tăng lại N
 *   4. Kiểm tra kho = giá trị ban đầu
 *
 * CHẠY: node tests/group1-ecommerce/1.7-cancel-stock-rollback.js
 * =====================================================================
 */

const cfg = require('../config');

async function getProductStock(productId) {
  const res = await fetch(`${cfg.BASE_URL}/products/${productId}`);
  const body = await res.json().catch(() => ({}));
  return body.stockQuantity ?? body.stock ?? null;
}

async function createOrder() {
  const res = await fetch(`${cfg.BASE_URL}/orders/checkout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: cfg.TOKEN_USER1 },
    body: JSON.stringify({
      addressId: cfg.ADDRESS_ID,
      shippingMethodId: cfg.SHIPPING_METHOD_ID,
      paymentMethod: 'COD',
      idempotencyKey: `cancel-rollback-${Date.now()}`,
    }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function cancelOrder(orderId) {
  // Thử endpoint cancel của user
  const res = await fetch(`${cfg.BASE_URL}/orders/${orderId}/cancel`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: cfg.TOKEN_USER1 },
    body: JSON.stringify({ reason: 'Test stock rollback' }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.7 — HỦY ĐƠN → HOÀN KHO (STOCK ROLLBACK)');
  console.log('='.repeat(65));

  // Bước 1: Lấy tồn kho trước
  console.log(`\n  [Bước 1] Lấy tồn kho hiện tại (ProductId=${cfg.PRODUCT_ID})...`);
  const stockBefore = await getProductStock(cfg.PRODUCT_ID);
  console.log(`     Tồn kho trước khi đặt hàng: ${stockBefore ?? '(không lấy được - xem API)'}`);

  // Bước 2: Đặt hàng
  console.log('\n  [Bước 2] Đặt hàng...');
  const orderResult = await createOrder();
  if (orderResult.status !== 200) {
    console.log(`  ❌ Không tạo được đơn: ${JSON.stringify(orderResult.body).substring(0, 100)}`);
    return;
  }
  const orderId = orderResult.body.id;
  console.log(`  ✅ Đơn tạo thành công | OrderId: ${orderId}`);

  await new Promise(r => setTimeout(r, 500));

  const stockAfterOrder = await getProductStock(cfg.PRODUCT_ID);
  console.log(`     Tồn kho sau khi đặt hàng: ${stockAfterOrder ?? '(không lấy được)'}`);

  // Bước 3: Hủy đơn
  console.log('\n  [Bước 3] Hủy đơn hàng...');
  const cancelResult = await cancelOrder(orderId);
  if (cancelResult.status === 200 || cancelResult.status === 204) {
    console.log(`  ✅ Hủy đơn thành công | Status: ${cancelResult.body.status ?? 'CANCELLED'}`);
  } else {
    console.log(`  ⚠️  Cancel response: HTTP ${cancelResult.status} | ${JSON.stringify(cancelResult.body).substring(0, 100)}`);
    console.log('  (Một số API cần ADMIN để hủy đơn — kiểm tra role)');
  }

  await new Promise(r => setTimeout(r, 1000));

  // Bước 4: Kiểm tra kho đã hoàn lại chưa
  console.log('\n  [Bước 4] Kiểm tra tồn kho sau khi hủy...');
  const stockAfterCancel = await getProductStock(cfg.PRODUCT_ID);
  console.log(`     Tồn kho sau khi hủy đơn: ${stockAfterCancel ?? '(không lấy được)'}`);

  console.log('\n─'.repeat(65));
  console.log(`  Trước đặt hàng:  ${stockBefore}`);
  console.log(`  Sau đặt hàng:    ${stockAfterOrder}`);
  console.log(`  Sau hủy đơn:     ${stockAfterCancel}`);

  if (stockBefore !== null && stockAfterCancel !== null && stockBefore === stockAfterCancel) {
    console.log('\n  ✅ PASS — Tồn kho được hoàn lại chính xác sau khi hủy!');
    console.log('  → increaseStock() đã chạy đúng khi cancelOrder().');
  } else if (stockBefore !== null && stockAfterCancel !== null) {
    console.log(`\n  ❌ FAIL — Tồn kho không khớp! Trước: ${stockBefore}, Sau hủy: ${stockAfterCancel}`);
  } else {
    console.log('\n  ⚠️  Không thể so sánh (API không trả stock). Kiểm tra DB thủ công.');
    console.log('  SQL: SELECT stock_quantity FROM products WHERE id = ' + cfg.PRODUCT_ID);
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
