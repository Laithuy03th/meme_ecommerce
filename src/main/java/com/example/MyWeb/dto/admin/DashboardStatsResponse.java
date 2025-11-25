package com.example.MyWeb.dto.admin;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    // Revenue stats
    private Double totalRevenue;
    private Double todayRevenue;
    private Double monthRevenue;

    // Order stats
    private Long totalOrders;
    private Long pendingOrders;
    private Long shippingOrders;
    private Long completedOrders;
    private Long canceledOrders;

    // Customer stats
    private Long totalCustomers;
    private Long newCustomersThisMonth;

    // Product stats
    private Long totalProducts;
    private Long lowStockProducts;

    // Top selling products
    private List<TopProductDto> topSellingProducts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProductDto {
        private Long productId;
        private String productName;
        private Long totalSold;
        private Double revenue;
    }
}
