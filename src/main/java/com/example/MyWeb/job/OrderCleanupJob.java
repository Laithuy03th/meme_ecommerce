package com.example.MyWeb.job;

import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.enums.OrderStatus;
import com.example.MyWeb.model.enums.PaymentMethod;
import com.example.MyWeb.model.enums.PaymentStatus;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupJob {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * Chạy định kỳ mỗi 5 phút (300000 ms) hoặc 1 phút (60000 ms)
     * Tìm các đơn hàng thanh toán VNPAY đang ở dạng PENDING mà chưa thanh toán sau 24h
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelUnpaidOnlineOrders() {
        // Hủy nếu đơn hàng được tạo quá 24h
        LocalDateTime timeLimit = LocalDateTime.now().minusHours(24);

        List<Order> unpaidOrders = orderRepository.findByStatusInAndPaymentMethodAndPaymentStatusNotAndCreatedAtBefore(
                java.util.Arrays.asList(OrderStatus.PENDING, OrderStatus.CONFIRMED),
                PaymentMethod.VNPAY,
                PaymentStatus.PAID,
                timeLimit
        );

        if (!unpaidOrders.isEmpty()) {
            log.info("Found {} unpaid VNPAY orders older than 24 hours. Canceling...", unpaidOrders.size());
            for (Order order : unpaidOrders) {
                try {
                    orderService.systemCancelOrder(order.getId(), "Quá hạn thanh toán 24h");
                    log.info("Successfully canceled unpaid order: {}", order.getId());
                } catch (Exception e) {
                    log.error("Failed to cancel unpaid order: {}", order.getId(), e);
                }
            }
        }
    }
}
