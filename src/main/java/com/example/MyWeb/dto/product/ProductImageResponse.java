package com.example.MyWeb.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageResponse {
    private Long id;
    private String imageUrl;
    private boolean thumbnail;
    private Integer sortOrder;
}
