// dto/user/AddressResponse.java
package com.example.MyWeb.dto.user;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String ward;
    private String district;
    private String province;
    private String country;
    private boolean isDefault;
    private LocalDateTime createdAt;
}
