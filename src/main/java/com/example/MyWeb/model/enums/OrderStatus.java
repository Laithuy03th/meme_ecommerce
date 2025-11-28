package com.example.MyWeb.model.enums;

public enum OrderStatus {
    PENDING, // Đơn mới, chờ xác nhận
    CONFIRMED, // Admin đã xác nhận
    PACKED, // Đã đóng gói, sẵn sàng giao
    SHIPPED, // Đang vận chuyển
    DELIVERED, // Đã giao thành công
    CANCELED, // Đã hủy
    RETURN_REQUESTED, // User yêu cầu trả hàng
    RETURNED, // Đã trả hàng (Admin xác nhận)
    REFUNDED // Đã hoàn tiền
}
