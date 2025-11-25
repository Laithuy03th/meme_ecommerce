# HƯỚNG DẪN KẾT NỐI FRONTEND (NEXT.JS) VỚI BACKEND

Dự án Backend đã được audit và nâng cấp để sẵn sàng 100% cho việc tích hợp Frontend. Dưới đây là các thông tin quan trọng bạn cần biết.

## 1. Cấu hình CORS & Static Resources
Backend đã được cấu hình để chấp nhận requests từ:
- `http://localhost:3000` (Next.js default)
- `http://localhost:3001`

**Static Resources:**
- Các file upload sẽ được lưu tại thư mục `uploads/` trong root dự án.
- URL truy cập: `http://localhost:8080/uploads/{filename}`

## 2. API Upload File (Mới)
Để Admin có thể upload ảnh sản phẩm, hãy sử dụng endpoint mới này:

- **Endpoint**: `POST /api/v1/files/upload`
- **Header**: `Authorization: Bearer <admin_token>`
- **Body**: `form-data`
  - `file`: (Chọn file ảnh)
- **Response**:
  ```json
  {
    "fileName": "uuid-generated-name.jpg",
    "fileUrl": "http://localhost:8080/uploads/uuid-generated-name.jpg"
  }
  ```

**Flow tạo sản phẩm bên FE:**
1. Admin chọn ảnh -> Gọi API Upload -> Nhận về `fileUrl`.
2. Gọi API `POST /api/v1/admin/products` với `thumbnailUrl` là `fileUrl` vừa nhận được.

## 3. Logic Tự động tạo Slug
- Khi tạo sản phẩm (`POST /api/v1/admin/products`), nếu bạn để trống trường `slug`, Backend sẽ **tự động** tạo slug từ `name`.
- Ví dụ: Name = "Áo Thun Mùa Hè" -> Slug = "ao-thun-mua-he".

## 4. Kiểm tra Tồn kho (Inventory)
- API `POST /api/v1/users/me/cart/items` (Thêm vào giỏ) hiện đã kiểm tra số lượng tồn kho của biến thể (`ProductVariant`).
- Nếu số lượng yêu cầu > tồn kho -> Trả về lỗi 500/400 với message "Not enough stock".

## 5. Các Lưu ý khác cho Frontend Dev
- **Token**: Lưu `accessToken` vào `localStorage` hoặc `cookies`. Gửi kèm header `Authorization: Bearer <token>` cho mọi request bảo mật.
- **Xử lý lỗi**: Backend trả về format chuẩn:
  ```json
  {
    "timestamp": "...",
    "status": 4xx,
    "error": "...",
    "message": "Chi tiết lỗi",
    "path": "..."
  }
  ```
- **Enum Values**: Chú ý các giá trị Enum (viết hoa) khi gửi request:
  - `paymentMethod`: `COD`, `VNPAY`, `MOMO`
  - `discountType`: `PERCENT`, `AMOUNT`

## 6. Checklist trước khi chạy
1. Đảm bảo thư mục `uploads` tồn tại ở root (Backend sẽ tự tạo nếu chưa có).
2. Chạy Backend: `mvn spring-boot:run`.
3. Chạy Frontend: `npm run dev`.

Chúc bạn tích hợp thành công! 🚀
