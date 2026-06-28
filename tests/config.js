/**
 * =====================================================================
 *  MEME SHOP — CẤU HÌNH CHUNG CHO TẤT CẢ TEST
 * =====================================================================
 *
 *  ★ DỮ LIỆU ĐÃ ĐƯỢC CẬP NHẬT THEO DB THỰC TẾ (từ DB dump 2026-06-28)
 *
 * CÁCH SỬ DỤNG:
 *   1. Backend đang chạy tại port 8080
 *   2. Đăng nhập FE → mở DevTools (F12) → Application → Local Storage
 *      Tìm key "accessToken" → copy giá trị → dán vào TOKEN_USER1
 *      Thêm "Bearer " phía trước: 'Bearer eyJhbGci...'
 *   3. Chạy từng file test: node tests/group1-ecommerce/1.1-race-stock.js
 *
 * ─── THÔNG TIN DB THỰC TẾ ─────────────────────────────────────────────
 * User  id=5  : laithuy03th@gmail.com   (ROLE_CUSTOMER)
 * User  id=6  : selena03th@gmail.com    (ROLE_CUSTOMER)
 * User  id=1  : admin@example.com       (ROLE_ADMIN)
 *
 * Địa chỉ của User5:
 *   id=1 : KTX B10 ĐH BK, Hai Bà Trưng, Hà Nội  (NOT default)
 *   id=2 : KTX B10 ĐH BK, Hồ Chí Minh, Hà Nội   (DEFAULT ✓)
 *
 * Giỏ hàng User5 (cart_id=1, ACTIVE):
 *   - product_id=20, qty=1, price=990,000đ
 *   - product_id=17, qty=1, price=3,490,000đ
 *   - product_id=25, qty=1, price=150,000đ
 *
 * Categories:
 *   id=1 Electronics | id=2 Fashion | id=3 Home & Living | id=4 Beauty
 * =====================================================================
 */

module.exports = {
  BASE_URL: 'http://localhost:8080/api/v1',

  // ─── ★ PHẢI ĐIỀN TOKEN THẬT - Lấy từ DevTools sau khi đăng nhập ───
  // Mở F12 → Application → Local Storage → copy giá trị accessToken
  TOKEN_USER1: 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjUsInN1YiI6ImxhaXRodXkwM3RoQGdtYWlsLmNvbSIsImlhdCI6MTc3MjgxMjQ4OSwiZXhwIjoxNzcyODEzMzg5fQ.XUl7cpAy9ntIcfVQeI8Y7qrLMpvoCBqs2lU2_yh8_3szaHR4wHkHvAesM6o2-pqBdXGkPuq-ceVcazpjZWr7dA',
  TOKEN_USER2: 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjUsInN1YiI6ImxhaXRodXkwM3RoQGdtYWlsLmNvbSIsImlhdCI6MTc3MjgxMjQ4OSwiZXhwIjoxNzcyODEzMzg5fQ.XUl7cpAy9ntIcfVQeI8Y7qrLMpvoCBqs2lU2_yh8_3szaHR4wHkHvAesM6o2-pqBdXGkPuq-ceVcazpjZWr7dA',
  TOKEN_ADMIN: 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEsInN1YiI6ImFkbWluQGV4YW1wbGUuY29tIiwiaWF0IjoxNzgyNjMxNzgyLCJleHAiOjE3ODI2MzI2ODJ9.rv-ai6RfKfRLId7f-7s54c4ZtRDW1zDkh5Ct5amQlrHmrF9ebLn6zz1o-2Vo57PyE4vFvCRHI7sz1PpcholP2g',

  // ─── IDs thực tế từ DB ───────────────────────────────────────────────
  USER_ID: 8,    // laithuy03th@gmail.com
  USER_ID_2: 9,    // selena03th@gmail.com
  ADMIN_USER_ID: 1,    // admin@example.com

  ADDRESS_ID: 3,   // ★ Địa chỉ DEFAULT của User5 (dùng cho test checkout)
  ADDRESS_ID_ALT: 3,   // Địa chỉ phụ của User8

  // ─── Sản phẩm cho test Race Condition ───────────────────────────────
  // ★ Trước khi test 1.1: Vào Admin → Sản phẩm → Set stock_quantity = 1
  PRODUCT_ID: 25,  // SP giá 150,000đ – dùng cho test (đã có trong giỏ User5)
  PRODUCT_ID_ALT: 20,  // SP giá 990,000đ – backup nếu cần
  VARIANT_ID: null, // Products hiện tại không có variant → dùng null

  SHIPPING_METHOD_ID: 1, // Kiểm tra trong Admin → Phương thức vận chuyển

  // ─── Voucher test ────────────────────────────────────────────────────
  // ★ Tạo voucher mới trong Admin trước khi test 1.2:
  //   Mã: RACETEST | usageLimit=1 | discountType=PERCENT | value=10%
  VOUCHER_CODE: 'SALE60',

  // ─── Chatbot ─────────────────────────────────────────────────────────
  CHATBOT_SESSION_ID: 'test-session-' + Date.now(),
};
