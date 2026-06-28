/**
 * =====================================================================
 *  TEST 1.5 — VNPAY PAYMENT FLOW: KIỂM TRA CALLBACK & ORDER STATUS
 * =====================================================================
 *
 * MỤC ĐÍCH:
 *   Kiểm tra luồng thanh toán VNPAY:
 *   1. Tạo đơn VNPAY → nhận payment URL
 *   2. Kiểm tra đơn hàng ở trạng thái PENDING / UNPAID
 *   3. Giả lập callback VNPAY thành công (gọi IPN endpoint)
 *   4. Kiểm tra đơn chuyển sang PAID / CONFIRMED
 *
 * LƯU Ý:
 *   - Bước 3 trong thực tế do server VNPAY gọi vào
 *   - Test này chỉ kiểm tra luồng 1, 2, 4 (không giả lập callback thật)
 *   - Để demo VNPAY thật: dùng VNPAY Sandbox trên trình duyệt
 *
 * CHẠY: node tests/group1-ecommerce/1.5-vnpay-flow.js
 * =====================================================================
 */

const cfg = require('../config');

async function createVnpayOrder() {
  console.log('\n  [Bước 1] Tạo đơn hàng với phương thức VNPAY...');
  const res = await fetch(`${cfg.BASE_URL}/orders/checkout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: cfg.TOKEN_USER1 },
    body: JSON.stringify({
      addressId: cfg.ADDRESS_ID,
      shippingMethodId: cfg.SHIPPING_METHOD_ID,
      paymentMethod: 'VNPAY',
      idempotencyKey: `vnpay-test-${Date.now()}`,
    }),
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function getOrderDetail(orderId) {
  const res = await fetch(`${cfg.BASE_URL}/orders/${orderId}`, {
    headers: { Authorization: cfg.TOKEN_USER1 },
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function createVnpayPaymentUrl(orderId) {
  console.log('\n  [Bước 2] Lấy Payment URL từ VNPAY...');
  const res = await fetch(`${cfg.BASE_URL}/payments/vnpay/create?orderId=${orderId}`, {
    method: 'GET',
    headers: { Authorization: cfg.TOKEN_USER1 },
  });
  const body = await res.json().catch(() => ({}));
  return { status: res.status, body };
}

async function run() {
  console.log('='.repeat(65));
  console.log('  TEST 1.5 — VNPAY PAYMENT FLOW');
  console.log('='.repeat(65));

  // Bước 1: Tạo đơn VNPAY
  const orderResult = await createVnpayOrder();
  if (orderResult.status !== 200) {
    console.log(`  ❌ Không tạo được đơn VNPAY: ${JSON.stringify(orderResult.body).substring(0, 150)}`);
    console.log('  → Kiểm tra lại: giỏ hàng có sản phẩm chưa? TOKEN có đúng không?');
    return;
  }

  const orderId = orderResult.body.id;
  console.log(`  ✅ Đơn hàng tạo thành công | OrderId: ${orderId}`);
  console.log(`     Status: ${orderResult.body.status} | PaymentStatus: ${orderResult.body.paymentStatus}`);

  // Kiểm tra trạng thái ban đầu
  console.log('\n  [Bước 2] Kiểm tra trạng thái đơn sau khi tạo...');
  const detail = await getOrderDetail(orderId);
  const order = detail.body;
  console.log(`     Status: ${order.status} | PaymentStatus: ${order.paymentStatus}`);
  if (order.status === 'PENDING' && order.paymentStatus === 'UNPAID') {
    console.log('  ✅ Đúng: PENDING + UNPAID (chờ thanh toán)');
  }

  // Lấy URL thanh toán
  const payResult = await createVnpayPaymentUrl(orderId);
  if (payResult.status === 200 && payResult.body.paymentUrl) {
    console.log(`\n  ✅ Payment URL: ${payResult.body.paymentUrl.substring(0, 80)}...`);
    console.log('\n  📋 HƯỚNG DẪN DEMO THẬT TRƯỚC HỘI ĐỒNG:');
    console.log('  ─────────────────────────────────────────────────────');
    console.log('  1. Copy URL trên, mở trình duyệt, paste vào → mở trang VNPAY Sandbox');
    console.log('  2. Điền số thẻ: 9704198526191432198 | Tên: NGUYEN VAN A');
    console.log('     Ngày phát hành: 07/15 | OTP: 123456');
    console.log('  3. Sau khi thanh toán → VNPAY gọi IPN về BE → Order tự chuyển PAID');
    console.log('  4. Chạy lại getOrderDetail để kiểm tra Status = CONFIRMED/PAID');
    console.log('  ─────────────────────────────────────────────────────');
  } else {
    console.log(`  ⚠️  Không lấy được Payment URL: ${JSON.stringify(payResult.body).substring(0, 100)}`);
  }

  console.log('\n  [Bước 3 - Auto] Kiểm tra lại trạng thái sau 3 giây...');
  await new Promise(r => setTimeout(r, 3000));
  const final = await getOrderDetail(orderId);
  console.log(`     Status: ${final.body.status} | PaymentStatus: ${final.body.paymentStatus}`);
  console.log('\n  (Nếu chưa thanh toán thật thì vẫn UNPAID — bình thường)');
  console.log('='.repeat(65));
}

run().catch(console.error);
