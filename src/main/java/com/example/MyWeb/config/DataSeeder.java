package com.example.MyWeb.config;

import com.example.MyWeb.model.Role;
import com.example.MyWeb.model.ShippingMethod;
import com.example.MyWeb.model.User;
import com.example.MyWeb.model.enums.UserStatus;
import com.example.MyWeb.repository.RoleRepository;
import com.example.MyWeb.repository.ShippingMethodRepository;
import com.example.MyWeb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final ShippingMethodRepository shippingMethodRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) {
                System.out.println("=".repeat(70));
                System.out.println("🌱 SEEDING BASE DATA - ROLES, USERS, SHIPPING METHODS");
                System.out.println("=".repeat(70));

                // 1. Seed shipping methods
                seedShippingMethods();

                // 2. Seed roles
                Role adminRole = createRole("ADMIN", "Administrator");
                Role customerRole = createRole("CUSTOMER", "Customer");

                // 3. Seed users
                createUser("admin@example.com", "123456", Set.of(adminRole));
                createUser("user@example.com", "123456", Set.of(customerRole));
                createUser("jane@example.com", "123456", Set.of(customerRole));

                System.out.println("✅ BASE DATA SEEDED SUCCESSFULLY");
                System.out.println("=".repeat(70));
        }

        private void seedShippingMethods() {
                // Giữ logic đơn giản để tương thích với repository hiện tại của bạn
                if (shippingMethodRepository.count() > 0) {
                        System.out.println("   ⏭️  Shipping methods already exist, skipping...");
                        return;
                }

                System.out.println("   🚚 Seeding Shipping Methods...");

                shippingMethodRepository.save(ShippingMethod.builder()
                                .code("STANDARD")
                                .name("Giao Hàng Tiêu Chuẩn")
                                .description("Nhận hàng trong 3-5 ngày")
                                .baseFee(30_000.0)
                                .estimatedMinDays(3)
                                .estimatedMaxDays(5)
                                .isActive(true)
                                .sortOrder(1)
                                .iconUrl("https://cdn-icons-png.flaticon.com/512/709/709790.png")
                                .build());

                shippingMethodRepository.save(ShippingMethod.builder()
                                .code("EXPRESS")
                                .name("Giao Hàng Nhanh")
                                .description("Nhận hàng trong 1-2 ngày")
                                .baseFee(50_000.0)
                                .estimatedMinDays(1)
                                .estimatedMaxDays(2)
                                .isActive(true)
                                .sortOrder(2)
                                .iconUrl("https://cdn-icons-png.flaticon.com/512/263/263142.png")
                                .build());

                System.out.println("   ✅ Shipping methods seeded");
        }

        private Role createRole(String code, String name) {
                return roleRepository.findByCode(code)
                                .orElseGet(() -> {
                                        System.out.println("   👤 Creating role: " + code);
                                        return roleRepository.save(
                                                        Role.builder()
                                                                        .code(code)
                                                                        .name(name)
                                                                        .description(name)
                                                                        .build());
                                });
        }

        private User createUser(String email, String password, Set<Role> roles) {
                return userRepository.findByEmail(email)
                                .orElseGet(() -> {
                                        System.out.println("   👤 Creating user: " + email);
                                        return userRepository.save(
                                                        User.builder()
                                                                        .email(email)
                                                                        .passwordHash(passwordEncoder.encode(password))
                                                                        .roles(roles)
                                                                        .status(UserStatus.ACTIVE)
                                                                        .createdAt(LocalDateTime.now())
                                                                        .updatedAt(LocalDateTime.now())
                                                                        .build());
                                });
        }
}