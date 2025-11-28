package com.example.MyWeb.config;

import com.example.MyWeb.model.*;
import com.example.MyWeb.model.enums.UserStatus;
import com.example.MyWeb.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final CategoryRepository categoryRepository;
        private final ProductRepository productRepository;
        private final ProductVariantRepository productVariantRepository;
        private final ProductImageRepository productImageRepository;
        private final ReviewRepository reviewRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                System.out.println("Seeding data with CURATED HIGH-QUALITY IMAGES...");

                // 1. Seed Roles
                Role adminRole = createRole("ADMIN", "Administrator");
                Role customerRole = createRole("CUSTOMER", "Customer");

                // 2. Seed Users
                createUser("admin@example.com", "123456", Set.of(adminRole));
                User user = createUser("user@example.com", "123456", Set.of(customerRole));
                createUser("jane@example.com", "123456", Set.of(customerRole));

                // 3. Seed Categories
                Map<String, Category> catMap = new HashMap<>();
                catMap.put("Electronics", createCategory("Electronics", "electronics",
                                "https://images.unsplash.com/photo-1550009158-9ebf69173e03?auto=format&fit=crop&q=80&w=500"));
                catMap.put("Fashion", createCategory("Fashion", "fashion",
                                "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&q=80&w=500"));
                catMap.put("Home & Living", createCategory("Home & Living", "home-living",
                                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&q=80&w=500"));
                catMap.put("Beauty", createCategory("Beauty", "beauty",
                                "https://images.unsplash.com/photo-1616683693504-3ea7e9ad6fec?auto=format&fit=crop&q=80&w=500"));

                // 4. Define Image Pools (Curated from Unsplash to match user requirements)
                Map<String, List<String>> imagePools = new HashMap<>();

                // Fashion - Dress
                imagePools.put("dress", Arrays.asList(
                                "https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1566174053879-31528523f8ae?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1612336307429-8a898d10e223?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1585487000160-6ebcfceb0d03?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1539008835657-9e8e9680c956?auto=format&fit=crop&q=80&w=800"));

                // Fashion - Shoes/Sneakers/Boots
                imagePools.put("shoes", Arrays.asList(
                                "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1600269452121-4f2416e55c28?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1543163521-1bf539c55dd2?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1608256246200-53e635b5b65f?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1560769629-975ec94e6a86?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&q=80&w=800"));

                // Fashion - T-Shirt/Jacket
                imagePools.put("clothes", Arrays.asList(
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1576871337622-98d48d1cf531?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1618354691373-d851c5c3a990?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&q=80&w=800"));

                // Electronics - Headphones/Audio
                imagePools.put("audio", Arrays.asList(
                                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1583394838336-acd977736f90?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&q=80&w=800"));

                // Electronics - Watch/Gadgets
                imagePools.put("gadgets", Arrays.asList(
                                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1551816230-ef5deaed4a26?auto=format&fit=crop&q=80&w=800"));

                // Beauty - Serum/Cream
                imagePools.put("skincare", Arrays.asList(
                                "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1611930022073-b7a4ba5fcccd?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1611930022288-5b9d33241b3f?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1616683693504-3ea7e9ad6fec?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?auto=format&fit=crop&q=80&w=800"));

                // Home - Furniture/Lamps
                imagePools.put("furniture", Arrays.asList(
                                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1505843490538-5133c6c7d0e1?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1592078615290-033ee584e267?auto=format&fit=crop&q=80&w=800"));

                // 5. Define Templates
                Map<String, List<BaseProduct>> templates = new HashMap<>();

                // Electronics
                templates.put("Electronics", Arrays.asList(
                                new BaseProduct("Wireless Headphones", "wireless-headphones", 299.99, "Electronics",
                                                "audio",
                                                List.of("Black", "Silver"), List.of()),
                                new BaseProduct("Smart Watch", "smart-watch", 199.50, "Electronics", "gadgets",
                                                List.of("Black", "White"), List.of())));

                // Fashion
                templates.put("Fashion", Arrays.asList(
                                new BaseProduct("Cotton T-Shirt", "cotton-tshirt", 29.99, "Fashion", "clothes",
                                                List.of("White", "Black"), List.of("S", "M", "L")),
                                new BaseProduct("Summer Dress", "summer-dress", 79.99, "Fashion", "dress",
                                                List.of("Red", "Floral"), List.of("S", "M", "L")),
                                new BaseProduct("Sneakers", "sneakers", 89.99, "Fashion", "shoes",
                                                List.of("White", "Black"), List.of("38", "39", "40"))));

                // Home
                templates.put("Home & Living", Arrays.asList(
                                new BaseProduct("Desk Lamp", "desk-lamp", 89.00, "Home & Living", "furniture",
                                                List.of("White", "Black"), List.of()),
                                new BaseProduct("Office Chair", "office-chair", 350.00, "Home & Living", "furniture",
                                                List.of("Black", "Grey"), List.of())));

                // Beauty
                templates.put("Beauty", Arrays.asList(
                                new BaseProduct("Face Serum", "face-serum", 45.00, "Beauty", "skincare",
                                                List.of(), List.of("30ml")),
                                new BaseProduct("Face Cream", "face-cream", 35.00, "Beauty", "skincare",
                                                List.of(), List.of("50g"))));

                // 6. Generate Products
                generateProductsForCategory(catMap.get("Fashion"), templates.get("Fashion"), 45, imagePools);
                generateProductsForCategory(catMap.get("Home & Living"), templates.get("Home & Living"), 45,
                                imagePools);
                generateProductsForCategory(catMap.get("Electronics"), templates.get("Electronics"), 45, imagePools);
                generateProductsForCategory(catMap.get("Beauty"), templates.get("Beauty"), 45, imagePools);

                System.out.println("Data seeding completed!");
        }

        private void generateProductsForCategory(Category category, List<BaseProduct> templates, int count,
                        Map<String, List<String>> imagePools) {
                Random rand = new Random();
                String[] adjectives = { "Premium", "Luxury", "Essential", "Modern", "Classic", "Ultra", "Pro", "Max",
                                "Sleek", "Durable" };

                for (int i = 1; i <= count; i++) {
                        BaseProduct template = templates.get(rand.nextInt(templates.size()));
                        String adj = adjectives[rand.nextInt(adjectives.length)];
                        String name = adj + " " + template.name + " " + i;
                        String slug = template.slug + "-" + category.getSlug() + "-" + i;
                        Double price = template.price + (rand.nextInt(50) - 20);

                        // Get image pool for this product type
                        List<String> pool = imagePools.get(template.imageType);

                        // Pick unique images from pool by cycling
                        String thumbnailUrl = pool.get(i % pool.size());

                        List<String> galleryImages = new ArrayList<>();
                        galleryImages.add(pool.get((i + 1) % pool.size()));
                        galleryImages.add(pool.get((i + 2) % pool.size()));
                        galleryImages.add(pool.get((i + 3) % pool.size()));

                        createProductFromTemplate(template, name, slug, price, category, thumbnailUrl, galleryImages);
                }
        }

        private Role createRole(String code, String name) {
                return roleRepository.findByCode(code)
                                .orElseGet(() -> roleRepository.save(Role.builder().code(code).name(name).build()));
        }

        private User createUser(String email, String password, Set<Role> roles) {
                return userRepository.findByEmail(email)
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .email(email)
                                                .passwordHash(passwordEncoder.encode(password))
                                                .roles(roles)
                                                .status(UserStatus.ACTIVE)
                                                .createdAt(LocalDateTime.now())
                                                .updatedAt(LocalDateTime.now())
                                                .build()));
        }

        private Category createCategory(String name, String slug, String imageUrl) {
                return categoryRepository.findBySlug(slug)
                                .map(existingCategory -> {
                                        existingCategory.setImageUrl(imageUrl);
                                        return categoryRepository.save(existingCategory);
                                })
                                .orElseGet(() -> categoryRepository.save(Category.builder()
                                                .name(name)
                                                .slug(slug)
                                                .description("Description for " + name)
                                                .imageUrl(imageUrl)
                                                .status("ACTIVE")
                                                .sortOrder(0)
                                                .build()));
        }

        private void createProductFromTemplate(BaseProduct template, String name, String slug, Double price,
                        Category category, String thumbnailUrl, List<String> galleryImages) {

                if (productRepository.existsBySlug(slug)) {
                        return;
                }

                Product p = Product.builder()
                                .name(name)
                                .slug(slug)
                                .category(category)
                                .basePrice(price)
                                .stockQuantity(100)
                                .thumbnailUrl(thumbnailUrl)
                                .shortDesc("Short description for " + name)
                                .longDesc("Long description for " + name + ". " + template.name
                                                + " is a great product.")
                                .status("ACTIVE")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                p = productRepository.save(p);

                // Variants
                if (!template.colors.isEmpty() || !template.sizes.isEmpty()) {
                        List<String> colors = template.colors.isEmpty() ? List.of("Default") : template.colors;
                        List<String> sizes = template.sizes.isEmpty() ? List.of("Standard") : template.sizes;

                        for (String color : colors) {
                                for (String size : sizes) {
                                        productVariantRepository.save(ProductVariant.builder()
                                                        .product(p)
                                                        .color(color.equals("Default") ? null : color)
                                                        .size(size.equals("Standard") ? null : size)
                                                        .sku(slug.toUpperCase() + "-"
                                                                        + (color.length() > 0 ? color.charAt(0) : "")
                                                                        + "-"
                                                                        + (size.length() > 0 ? size.charAt(0) : ""))
                                                        .price(price)
                                                        .stock(50)
                                                        .status("ACTIVE")
                                                        .createdAt(LocalDateTime.now())
                                                        .updatedAt(LocalDateTime.now())
                                                        .build());
                                }
                        }
                } else {
                        productVariantRepository.save(ProductVariant.builder()
                                        .product(p)
                                        .sku(slug.toUpperCase() + "-DEF")
                                        .price(price)
                                        .stock(100)
                                        .status("ACTIVE")
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build());
                }

                // Images (Gallery)
                int sortOrder = 0;
                for (String imgUrl : galleryImages) {
                        productImageRepository.save(ProductImage.builder()
                                        .product(p)
                                        .imageUrl(imgUrl)
                                        .sortOrder(sortOrder++)
                                        .build());
                }
        }

        @Data
        @Builder
        @RequiredArgsConstructor
        static class BaseProduct {
                final String name;
                final String slug;
                final Double price;
                final String categoryName;
                final String imageType; // Key for image pool
                final List<String> colors;
                final List<String> sizes;
        }
}
