// dto/user/UpdateProfileRequest.java
package com.example.MyWeb.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;

    @Pattern(regexp = "^[0-9+\\-()\\s]{6,20}$", message = "Invalid phone")
    private String phone;

    // MALE/FEMALE/OTHER (optional)
    private String gender;

    // ISO-8601: yyyy-MM-dd (optional)
    private String dateOfBirth;
}
