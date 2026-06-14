package com.example.MyWeb.listener;

import com.example.MyWeb.event.OrderStatusChangedEvent;
import com.example.MyWeb.model.Order;
import com.example.MyWeb.model.enums.OrderStatus;
import com.example.MyWeb.service.AdminNotificationService;
import com.example.MyWeb.service.NotificationService;
import com.example.MyWeb.model.enums.AdminNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final NotificationService notificationService;
    private final AdminNotificationService adminNotificationService;

    @EventListener
    @Async
    public void handleOrderStatusChange(OrderStatusChangedEvent event) {
        Order order = event.getOrder();
        OrderStatus newStatus = event.getNewStatus();

        String title = "";
        String message = "";
        String type = "";

        switch (newStatus) {
            case PENDING:
                if (event.getOldStatus() == null) {
                    title = "Đặt hàng thành công";
                    message = "Đơn hàng #" + order.getId() + " của bạn đã được đặt thành công.";
                    type = "order_created";

                    adminNotificationService.createNotification(
                            "Đơn hàng mới",
                            "Khách hàng " + order.getUser().getEmail() + " vừa đặt đơn hàng #" + order.getId(),
                            AdminNotificationType.ORDER_CREATED,
                            "ORDER",
                            order.getId(),
                            "/orders/" + order.getId(),
                            order.getUser().getEmail());
                }
                break;
            case CONFIRMED:
                title = "Đơn hàng đã được xác nhận";
                message = "Đơn hàng #" + order.getId() + " của bạn đã được shop xác nhận và đang chuẩn bị hàng.";
                type = "order_confirmed";
                break;
            case SHIPPED:
                title = "Đơn hàng đang được giao";
                message = "Đơn hàng #" + order.getId() + " đang được giao đến bạn. Vui lòng chú ý điện thoại.";
                type = "order_shipping";
                break;
            case DELIVERED:
                title = "Giao hàng thành công";
                message = "Đơn hàng #" + order.getId() + " đã được giao thành công.";
                type = "order_completed";
                break;
            case CANCELED:
                title = "Đơn hàng đã bị hủy";
                message = "Đơn hàng #" + order.getId() + " đã bị hủy.";
                type = "order_cancelled";
                break;
            default:
                return;
        }

        if (!title.isEmpty()) {
            notificationService.createNotification(order.getUser(), title, message, type, order.getId());
        }
    }
}
