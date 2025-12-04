package com.example.MyWeb.config;

import com.example.MyWeb.model.Role;
import com.example.MyWeb.model.User;
import com.example.MyWeb.model.enums.UserStatus;
import com.example.MyWeb.repository.RoleRepository;
import com.example.MyWeb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 0. Fix constraint for UserStatus (PostgreSQL specific)
            try {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check");
                jdbcTemplate.execute(
                        "ALTER TABLE users ADD CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'BLOCKED', 'INACTIVE'))");
            } catch (Exception e) {
                System.out.println("Warning: Could not update users_status_check constraint: " + e.getMessage());
            }

            // 1. Tạo role CUSTOMER nếu chưa có
            Role customer = roleRepository.findByCode("CUSTOMER")
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .code("CUSTOMER")
                                    .name("Customer")
                                    .description("Customer role")
                                    .build()));

            // 2. Tạo role ADMIN nếu chưa có
            Role adminRole = roleRepository.findByCode("ADMIN")
                    .orElseGet(() -> roleRepository.save(
                            Role.builder()
                                    .code("ADMIN")
                                    .name("Administrator")
                                    .description("Admin role")
                                    .build()));

            // 3. Tạo user admin nếu chưa có
            String adminEmail = "admin1@gmail.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .email(adminEmail)
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .status(UserStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .roles(Set.of(adminRole))
                        .build();
                userRepository.save(admin);
            }
        };
    }
}
