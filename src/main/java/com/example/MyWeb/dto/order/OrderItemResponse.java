package com.example.MyWeb.dto.order;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private Long variantId;

    private String productName;
    private String productSlug;
    private String thumbnailUrl;

    private String color;
    private String size;

    private Double unitPrice;
    private Integer quantity;
    private Double totalPrice;
}
