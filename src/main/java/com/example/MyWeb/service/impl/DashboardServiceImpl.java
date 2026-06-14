package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.admin.DashboardStatsResponse;
import com.example.MyWeb.model.enums.OrderStatus;
import com.example.MyWeb.repository.OrderRepository;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.example.MyWeb.repository.OrderItemRepository;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Revenue stats
        stats.setTotalRevenue(orderRepository.sumTotalAmountByStatus(OrderStatus.DELIVERED));
        stats.setTodayRevenue(calculateTodayRevenue());
        stats.setMonthRevenue(calculateMonthRevenue());

        // Order stats
        stats.setTotalOrders(orderRepository.count());
        stats.setPendingOrders(orderRepository.countByStatus(OrderStatus.PENDING));
        stats.setShippingOrders(orderRepository.countByStatus(OrderStatus.SHIPPED));
        stats.setCompletedOrders(orderRepository.countByStatus(OrderStatus.DELIVERED));
        stats.setCanceledOrders(orderRepository.countByStatus(OrderStatus.CANCELED));

        // Customer stats
        stats.setTotalCustomers(userRepository.count());
        stats.setNewCustomersThisMonth(calculateNewCustomersThisMonth());

        // Product stats
        stats.setTotalProducts(productRepository.count());
        stats.setLowStockProducts(calculateLowStockProducts());

        // Top selling products
        stats.setTopSellingProducts(getTopSellingProducts());

        return stats;
    }

    private Long calculateLowStockProducts() {
        int lowStockThreshold = 10;
        long lowStockSimpleProducts = productRepository.countByStockQuantityLessThanEqual(lowStockThreshold);

        return lowStockSimpleProducts;
    }

    private List<DashboardStatsResponse.TopProductDto> getTopSellingProducts() {
        List<Object[]> results = orderItemRepository.findTopSellingProducts();
        List<DashboardStatsResponse.TopProductDto> topProducts = new ArrayList<>();

        for (Object[] row : results) {
            DashboardStatsResponse.TopProductDto dto = DashboardStatsResponse.TopProductDto.builder()
                    .productId((Long) row[0])
                    .productName((String) row[1])
                    .totalSold((Long) row[3])
                    .revenue((Double) row[4])
                    .build();
            topProducts.add(dto);
        }

        return topProducts;
    }

    private Double calculateTodayRevenue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Double revenue = orderRepository.sumTotalAmountByStatusAndDateRange(
                OrderStatus.DELIVERED, startOfDay, endOfDay);
        return revenue != null ? revenue : 0.0;
    }

    private Double calculateMonthRevenue() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Double revenue = orderRepository.sumTotalAmountByStatusAndDateRange(
                OrderStatus.DELIVERED, startOfMonth, endOfMonth);
        return revenue != null ? revenue : 0.0;
    }

    private Long calculateNewCustomersThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();

        return userRepository.countByCreatedAtAfter(startOfMonth);
    }
}
