package com.example.MyWeb.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1-1 với User
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String gender; // MALE/FEMALE/OTHER...

    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String avatarUrl;
}
