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

    // Giá hiệu lực: nếu variant.price != null thì dùng,
    // còn null thì FE có thể fallback sang basePrice của product
    private Double price;

    private Integer stock;
    private String status; // ACTIVE / INACTIVE...
}
