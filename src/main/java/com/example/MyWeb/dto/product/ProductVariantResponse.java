package com.example.MyWeb.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String color;
    private String size;
    private Double price;

    private Integer stock;
    private String status;
}
