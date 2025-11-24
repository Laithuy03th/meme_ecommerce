package com.example.MyWeb.dto.cart;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long id;
    private Double totalAmount;
    private Integer totalItems;

    private List<CartItemResponse> items;
}
