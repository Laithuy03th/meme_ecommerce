// dto/user/AddressRequest.java
package com.example.MyWeb.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    @NotBlank
    private String fullName;
    @NotBlank
    private String phone;
    @NotBlank
    private String addressLine1;
    private String ward;

    private String district;
    private String province;
    private String country = "Vietnam";
    private String label;
    private String zipCode;
    private Boolean isDefault = false;
}
