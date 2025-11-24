// src/main/java/com/example/MyWeb/dto/order/UpdateOrderStatusRequest.java
package com.example.MyWeb.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {

    @NotBlank
    private String status; // PENDING / PAID / SHIPPED / COMPLETED / CANCELED
}
