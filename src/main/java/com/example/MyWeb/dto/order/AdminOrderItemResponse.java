// src/main/java/com/example/MyWeb/dto/order/AdminOrderItemResponse.java
package com.example.MyWeb.dto.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminOrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Double unitPrice;
    private Integer quantity;
    private Double lineTotal;
}
