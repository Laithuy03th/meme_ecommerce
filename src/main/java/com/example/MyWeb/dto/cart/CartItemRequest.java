package com.example.MyWeb.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequest {

    @NotNull
    private Long productId;

    private Long variantId; // có thể null

    @NotNull
    @Min(1)
    private Integer quantity;
}
