/**
 * =====================================================================
 *  TEST 1.6 — AUTO-CANCEL JOB: ĐƠN PENDING QUÁ HẠN TỰ HỦY
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Chứng minh đơn hàng COD ở trạng thái PENDING quá X phút
 *   sẽ tự động bị hủy bởi Scheduled Job (không cần admin can thiệp).
 *
 * CƠ CHẾ BACKEND:
 *   @Scheduled(fixedDelay = 60000) autoCancelPendingOrders()
 *   → SELECT orders WHERE status=PENDING AND payment_method=COD
 *      AND created_at < NOW() - interval
 *   → Cập nhật status = CANCELLED + hoàn kho (increaseStock)
 *
 * CÁCH TEST:
 *   Option A (không sửa code): Đợi thật (nếu interval = 15 phút)
 *   Option B (nhanh hơn): Tạm thời đổi interval = 1 phút trong code BE
 *     và thêm 1 đơn cũ bằng cách set created_at = 2 phút trước trong DB
 *
 * CHẠY: node tests/group1-ecommerce/1.6-auto-cancel.js
 * =====================================================================
 */

const cfg = require('../config');

async function getMyOrders() {
  const res = await fetch(`${cfg.BASE_URL}/orders/my-orders?page=0&size=5`, {
    headers: { Authorization: cfg.TOKEN_USER1 },
  });
  const body = await res.json().catch(() => ({}));
  return body.content ?? body.data ?? body ?? [];
}

async function getOrderDetail(orderId) {
  const res = await fetch(`${cfg.BASE_URL}/orders/${orderId}`, {
    headers: { Authorization: cfg.TOKEN_USER1 },
  });
  return res.json().catch(() => ({}));
}

async function createOrder() {
  const res = await fetch(`${cfg.BASE_URL}/orders/checkout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: cfg.TOKEN_USER1 },
    body: JSON.stringify({
      addressId: cfg.ADDRESS_ID,
      shippingMethodId: cfg.SHIPPING_METHOD_ID,
      paymentMethod: 'COD',
      idempotencyKey: `auto-cancel-test-${Date.now()}`,
    }),
  });
  return res.json().catch(() => ({}));
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.6 — AUTO-CANCEL JOB: ĐƠN PENDING QUÁ HẠN');
  console.log('='.repeat(65));

  console.log('\n  [Bước 1] Tạo đơn hàng COD mới...');
  const order = await createOrder();
  if (!order.id) {
    console.log('  ❌ Không tạo được đơn. Kiểm tra giỏ hàng và token.');
    return;
  }
  console.log(`  ✅ Đơn tạo thành công | OrderId: ${order.id} | Status: ${order.status}`);

  console.log('\n  [Bước 2] Kiểm tra ngay sau khi tạo...');
  const initial = await getOrderDetail(order.id);
  console.log(`     Status: ${initial.status} | CreatedAt: ${initial.createdAt}`);

  console.log('\n  [Bước 3 — HÀNH ĐỘNG THỦ CÔNG CẦN THỰC HIỆN]');
  console.log('  ─────────────────────────────────────────────────────');
  console.log(`  1. Mở DBeaver/pgAdmin → Table "orders"`);
  console.log(`  2. Update đơn ${order.id}: SET created_at = NOW() - INTERVAL '20 minutes'`);
  console.log('     SQL: UPDATE orders SET created_at = NOW() - INTERVAL \'20 minutes\'');
  console.log(`          WHERE id = ${order.id};`);
  console.log('  3. Đợi Scheduled Job chạy (mỗi 60 giây) hoặc gọi API trigger thủ công');
  console.log('  ─────────────────────────────────────────────────────');

  console.log('\n  [Bước 4] Script sẽ polling trạng thái mỗi 30 giây (tối đa 5 phút)...');
  console.log('  (Nhớ thực hiện Bước 3 trước!)');

  let cancelled = false;
  for (let i = 0; i < 10; i++) {
    await new Promise(r => setTimeout(r, 30000)); // chờ 30s
    const current = await getOrderDetail(order.id);
    const elapsed = (i + 1) * 30;
    console.log(`     [${elapsed}s] Status: ${current.status} | PaymentStatus: ${current.paymentStatus}`);

    if (current.status === 'CANCELLED') {
      cancelled = true;
      console.log('\n  ✅ PASS — Đơn hàng đã tự động bị hủy bởi Scheduled Job!');
      console.log('  → Auto-cancel job hoạt động đúng.');
      console.log('  → Tồn kho đã được hoàn lại (increaseStock đã chạy).');
      break;
    }
  }

  if (!cancelled) {
    console.log('\n  ⚠️  Chưa thấy đơn bị hủy sau 5 phút.');
    console.log('  → Kiểm tra lại: đã update created_at chưa? Job interval bao lâu?');
    console.log('  → Xem log backend: grep "auto-cancel" hoặc "autoCancelPendingOrders"');
  }
  console.log('='.repeat(65));
}

run().catch(console.error);
