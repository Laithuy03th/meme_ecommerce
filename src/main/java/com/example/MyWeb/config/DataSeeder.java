package com.example.MyWeb.config;

import com.example.MyWeb.model.Category;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.repository.CategoryRepository;
import com.example.MyWeb.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // Nếu đã có sản phẩm rồi thì không seed nữa để tránh trùng slug
        if (productRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // =========================
        // 1) TẠO 3 CATEGORY CHÍNH
        // =========================

        Category dresses = Category.builder()
                .name("Dresses")
                .slug("dresses") // UNIQUE
                .description("Các mẫu váy nữ")
                .status("ACTIVE")
                .sortOrder(1)
                .build();

        Category clothes = Category.builder()
                .name("Clothes")
                .slug("clothes") // áo, quần...
                .description("Áo, quần, áo khoác cho nữ")
                .status("ACTIVE")
                .sortOrder(2)
                .build();

        Category shoes = Category.builder()
                .name("Shoes")
                .slug("shoes")
                .description("Giày, sandal cho nữ")
                .status("ACTIVE")
                .sortOrder(3)
                .build();

        dresses = categoryRepository.save(dresses);
        clothes = categoryRepository.save(clothes);
        shoes = categoryRepository.save(shoes);

        // =========================
        // 2) TẠO PRODUCT DEMO
        // =========================

        // Váy
        Product dress1 = Product.builder()
                .name("Elegant Summer Dress")
                .slug("elegant-summer-dress")
                .shortDesc("Váy mùa hè thanh lịch, chất liệu thoáng mát.")
                .longDesc("Váy mùa hè thanh lịch, chất liệu cotton pha, " +
                        "phù hợp đi chơi, dạo phố, dự tiệc nhẹ.")
                .category(dresses)
                .basePrice(499000.0)
                .thumbnailUrl("https://via.placeholder.com/400x400?text=Summer+Dress")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Product dress2 = Product.builder()
                .name("Black Party Dress")
                .slug("black-party-dress")
                .shortDesc("Váy đen dự tiệc sang trọng.")
                .longDesc("Váy đen dự tiệc form ôm, tôn dáng, chất liệu cao cấp.")
                .category(dresses)
                .basePrice(699000.0)
                .thumbnailUrl("https://via.placeholder.com/400x400?text=Party+Dress")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Áo / Clothes
        Product top1 = Product.builder()
                .name("Basic White T-Shirt")
                .slug("basic-white-tshirt")
                .shortDesc("Áo thun trắng basic, dễ phối đồ.")
                .longDesc("Áo thun trắng unisex, chất liệu cotton thoáng mát, dễ phối với mọi loại quần/váy.")
                .category(clothes)
                .basePrice(199000.0)
                .thumbnailUrl("https://via.placeholder.com/400x400?text=White+Tee")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Product top2 = Product.builder()
                .name("Oversized Pink Hoodie")
                .slug("oversized-pink-hoodie")
                .shortDesc("Hoodie form rộng màu hồng.")
                .longDesc("Hoodie form rộng, nỉ bông ấm áp, phong cách Hàn Quốc.")
                .category(clothes)
                .basePrice(399000.0)
                .thumbnailUrl("https://via.placeholder.com/400x400?text=Pink+Hoodie")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Giày
        Product shoes1 = Product.builder()
                .name("Casual White Sneakers")
                .slug("casual-white-sneakers")
                .shortDesc("Giày sneaker trắng, phong cách trẻ trung.")
                .longDesc("Giày sneaker trắng, đế cao su, dễ phối nhiều kiểu outfit hằng ngày.")
                .category(shoes)
                .basePrice(599000.0)
                .thumbnailUrl("https://via.placeholder.com/400x400?text=White+Sneakers")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Product shoes2 = Product.builder()
                .name("Black High Heels")
                .slug("black-high-heels")
                .shortDesc("Giày cao gót đen thanh lịch.")
                .longDesc("Giày cao gót đen 7cm, phù hợp đi làm và dự tiệc.")
                .category(shoes)
                .basePrice(649000.0)
                .thumbnailUrl("https://via.placeholder.com/400x400?text=High+Heels")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        productRepository.save(dress1);
        productRepository.save(dress2);
        productRepository.save(top1);
        productRepository.save(top2);
        productRepository.save(shoes1);
        productRepository.save(shoes2);
    }
}
