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
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                System.out.println("=".repeat(70));
                System.out.println("🌱 SEEDING DATABASE - 80 PRODUCTS FIXED (VNĐ Pricing)");
                System.out.println("=".repeat(70));

                // 1. Seed Roles
                Role adminRole = createRole("ADMIN", "Administrator");
                Role customerRole = createRole("CUSTOMER", "Customer");

                // 2. Seed Users
                createUser("admin@example.com", "123456", Set.of(adminRole));
                createUser("user@example.com", "123456", Set.of(customerRole));
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

                // 4. Image Pools
                Map<String, List<String>> imagePools = createImagePools();

                // 5. Product Templates (VNĐ Pricing)
                Map<String, List<BaseProduct>> templates = createProductTemplates();

                // 6. Product Descriptions
                Map<String, ProductDescriptions> descriptions = createDescriptions();

                // 7. Generate Products (FIXED: Only if not exists)
                System.out.println("\n📦 Checking & Generating products...");
                long totalProducts = productRepository.count();

                if (totalProducts >= 80) {
                        System.out.println("   ⏭️  SKIPPED: Already have " + totalProducts + " products (target: 80)");
                        System.out.println("   💡 To reseed: DELETE FROM products; then restart app");
                } else {
                        generateProductsForCategory(catMap.get("Fashion"), templates.get("Fashion"), 20, imagePools,
                                        descriptions);
                        generateProductsForCategory(catMap.get("Beauty"), templates.get("Beauty"), 20, imagePools,
                                        descriptions);
                        generateProductsForCategory(catMap.get("Electronics"), templates.get("Electronics"), 20,
                                        imagePools, descriptions);
                        generateProductsForCategory(catMap.get("Home & Living"), templates.get("Home & Living"), 20,
                                        imagePools, descriptions);
                }

                System.out.println("\n" + "=".repeat(70));
                System.out.println("✅ DATA SEEDING COMPLETED!");
                System.out.println("   - Total products: " + productRepository.count());
                System.out.println("=".repeat(70) + "\n");
        }

        // ==================== DESCRIPTIONS ====================

        private Map<String, ProductDescriptions> createDescriptions() {
                Map<String, ProductDescriptions> desc = new HashMap<>();

                // Fashion - Dress
                desc.put("dress", new ProductDescriptions(
                                "Váy thời trang cao cấp, chất liệu thoáng mát, phù hợp mọi dịp",
                                "Váy được thiết kế với chất liệu vải cao cấp, thoáng mát và co giãn nhẹ. Kiểu dáng hiện đại, phù hợp cho cả đi làm lẫn dự tiệc. Form dáng ôm vừa phải tôn dáng người mặc. Có thể kết hợp với giày cao gót hoặc sandals để tạo nên phong cách thanh lịch và nữ tính."));

                // Fashion - Tops
                desc.put("tops", new ProductDescriptions(
                                "Áo thời trang năng động, chất cotton mềm mại, dễ phối đồ",
                                "Áo được làm từ chất liệu cotton 100% cao cấp, thấm hút mồ hôi tốt và thoáng khí. Thiết kế basic nhưng không kém phần thời trang, dễ dàng phối cùng quần jeans, chân váy hay quần short. Form áo vừa vặn, không bai giãn sau nhiều lần giặt. Phù hợp cho cả nam và nữ."));

                // Fashion - Shoes
                desc.put("shoes", new ProductDescriptions(
                                "Giày thể thao/dép cao cấp, êm ái, chống trơn trượt",
                                "Giày được thiết kế với đế cao su chống trơn, độ bám cao và độ đàn hồi tốt. Phần upper làm từ chất liệu thoáng khí, giúp chân luôn khô ráo. Lót giày êm ái, hỗ trợ tối đa khi vận động. Kiểu dáng thời trang, phù hợp cả đi học, đi chơi hay tập thể thao. Có nhiều màu sắc và size để lựa chọn."));

                // Beauty - Cream
                desc.put("cream", new ProductDescriptions(
                                "Kem dưỡng da chuyên sâu, cấp ẩm 24h, chiết xuất thiên nhiên",
                                "Kem dưỡng da với công thức đặc biệt từ các chiết xuất thiên nhiên, giúp cấp ẩm sâu và duy trì độ ẩm suốt 24 giờ. Kết cấu kem mịn, thấm nhanh không gây nhờn rít. Chứa Vitamin E, Hyaluronic Acid và các dưỡng chất giúp làm mờ nếp nhăn, cải thiện độ đàn hồi và dưỡng trắng da tự nhiên. Phù hợp cho mọi loại da."));

                // Beauty - Mask
                desc.put("mask", new ProductDescriptions(
                                "Mặt nạ dưỡng da cao cấp, làm sạch sâu, se khít lỗ chân lông",
                                "Mặt nạ với công thức đặc biệt giúp làm sạch sâu lỗ chân lông, loại bỏ bụi bẩn và bã nhờn tích tụ. Chứa các khoáng chất tự nhiên giúp se khít lỗ chân lông, cải thiện kết cấu da. Sử dụng 2-3 lần/tuần để đạt hiệu quả tối nhất. Sau khi sử dụng da trở nên mịn màng, sạch sâu và tươi sáng hơn. An toàn cho mọi loại da, kể cả da nhạy cảm."));

                // Electronics - Smartwatch
                desc.put("smartwatch", new ProductDescriptions(
                                "Đồng hồ thông minh đa năng, theo dõi sức khỏe 24/7, GPS tích hợp",
                                "Đồng hồ thông minh với màn hình AMOLED sắc nét, theo dõi nhịp tim, SpO2, giấc ngủ và hơn 100 chế độ thể thao. Tích hợp GPS, hỗ trợ cuộc gọi Bluetooth, nhận thông báo từ smartphone. Pin sử dụng liên tục đến 7 ngày. Chống nước IP68, phù hợp bơi lội và các hoạt động ngoài trời. Tương thích iOS và Android."));

                // Electronics - iPhone
                desc.put("iphone", new ProductDescriptions(
                                "iPhone chính hãng VN/A, chip A-series mạnh mẽ, camera Pro Max",
                                "iPhone với chip A-series thế hệ mới nhất, hiệu năng vượt trội cho mọi tác vụ từ gaming đến chỉnh sửa video 4K. Camera Pro với cảm biến lớn, chụp đêm xuất sắc, quay video Cinematic Mode. Màn hình Super Retina XDR 120Hz mượt mà. Pin sử dụng cả ngày, hỗ trợ sạc nhanh và sạc không dây MagSafe. Bảo hành chính hãng 12 tháng tại Việt Nam."));

                // Electronics - Samsung
                desc.put("samsung", new ProductDescriptions(
                                "Samsung Galaxy chính hãng, màn hình Dynamic AMOLED 2X, camera 200MP",
                                "Samsung Galaxy với màn hình Dynamic AMOLED 2X siêu mượt 120Hz, độ phân giải QHD+. Camera chính 200MP với AI xử lý ảnh thông minh, zoom quang học 10x. Chip Snapdragon/Exynos mạnh mẽ, RAM lớn đa nhiệm mượt mà. Pin khủng 5000mAh, sạc siêu nhanh 45W. Tích hợp S Pen (với dòng Ultra), chống nước IP68. Bảo hành chính hãng 12 tháng."));

                // Home - Table
                desc.put("table", new ProductDescriptions(
                                "Bàn làm việc/học tập hiện đại, gỗ công nghiệp cao cấp, chống trầy",
                                "Bàn được làm từ gỗ công nghiệp MDF phủ melamine chống trầy xước, chống thấm nước. Khung chân thép sơn tĩnh điện chắc chắn, chịu lực tốt. Thiết kế tối giản, hiện đại phù hợp với mọi không gian. Mặt bàn rộng rãi, đủ chỗ để laptop, sách vở và đồ dùng học tập. Lắp ráp đơn giản, dễ dàng di chuyển."));

                // Home - Chair
                desc.put("chair", new ProductDescriptions(
                                "Ghế văn phòng/gaming ergonomic, đệm cao cấp, nâng hạ linh hoạt",
                                "Ghế thiết kế ergonomic theo tiêu chuẩn quốc tế, hỗ trợ tối đa cột sống và giảm đau lưng. Đệm ngồi bọc da PU cao cấp, đệm foam dày dặn êm ái. Tay ghế có thể điều chỉnh độ cao, tựa lưng ngả 135 độ. Chân ghế 5 cánh bằng nhựa ABS chắc chắn với bánh xe di chuyển êm mượt. Trụ nâng hạ khí nén an toàn, chịu tải đến 120kg."));

                return desc;
        }

        // ==================== IMAGE POOLS (unchanged) ====================

        private Map<String, List<String>> createImagePools() {
                Map<String, List<String>> pools = new HashMap<>();

                pools.put("dress", Arrays.asList(
                                "https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1566174053879-31528523f8ae?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1612336307429-8a898d10e223?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1585487000160-6ebcfceb0d03?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1539008835657-9e8e9680c956?auto=format&fit=crop&q=80&w=800"));

                pools.put("tops", Arrays.asList(
                                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1576871337622-98d48d1cf531?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1618354691373-d851c5c3a990?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?auto=format&fit=crop&q=80&w=800"));

                pools.put("shoes", Arrays.asList(
                                "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1600269452121-4f2416e55c28?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1543163521-1bf539c55dd2?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1608256246200-53e635b5b65f?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1560769629-975ec94e6a86?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&q=80&w=800"));

                pools.put("cream", Arrays.asList(
                                "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1611930022073-b7a4ba5fcccd?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1611930022288-5b9d33241b3f?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1616683693504-3ea7e9ad6fec?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1570194065650-d99fb4bedf0a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1571781926291-c477ebfd024b?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1556229010-6c3f2c9ca5f8?auto=format&fit=crop&q=80&w=800"));

                pools.put("mask", Arrays.asList(
                                "https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1612817288484-6f916006741a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1596755389378-c31d21fd1273?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1609587312208-cea54be969e7?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1598440947615-a5f6768e0b6f?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1598662779094-110c2bad80b5?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1570554886111-e80fcca6a029?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1571781926291-c477ebfd024b?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1596755389378-c31d21fd1273?auto=format&fit=crop&q=80&w=800"));

                pools.put("smartwatch", Arrays.asList(
                                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1551816230-ef5deaed4a26?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1617625802912-cde586faf331?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1544117519-31a4b719223d?auto=format&fit=crop&q=80&w=800"));

                pools.put("iphone", Arrays.asList(
                                "https://images.unsplash.com/photo-1592286927505-4595ae6f431f?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1591337676887-a217a6970a8a?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1611472173362-3f53dbd65d80?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1632633728024-e1fd4e56f3d6?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1603921326210-6edd2d60ca68?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1616348436168-de43ad0db179?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?auto=format&fit=crop&q=80&w=800"));

                pools.put("samsung", Arrays.asList(
                                "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1585060544812-6b45742d762f?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1610792516307-ea5acd9c3b00?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1574944985070-8f3ebc6b79d2?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1598965675045-323d8f5935e3?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&q=80&w=800"));

                pools.put("table", Arrays.asList(
                                "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1595515106969-1ce29566ff1c?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1542744095-291d1f67b221?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1551298370-9d3d53740c72?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1509365465985-25d11c17e812?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1595428774223-ef52624120d2?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&q=80&w=800"));

                pools.put("chair", Arrays.asList(
                                "https://images.unsplash.com/photo-1524758631624-e2822e304c36?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1592078615290-033ee584e267?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1505843490538-5133c6c7d0e1?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1519947486511-46149fa0a254?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&q=80&w=800",
                                "https://images.unsplash.com/photo-1598300056393-4aac492f4344?auto=format&fit=crop&q=80&w=800"));

                return pools;
        }

        // ==================== PRODUCT TEMPLATES (VNĐ PRICING) ====================

        private Map<String, List<BaseProduct>> createProductTemplates() {
                Map<String, List<BaseProduct>> templates = new HashMap<>();

                // Price ranges in VNĐ:
                // < 400k, 400k-2M, 2M-5M, 5M-20M, > 20M

                templates.put("Fashion", Arrays.asList(
                                // Váy - Range: 400k-2M
                                new BaseProduct("Váy Dạ Hội", "summer-dress", 1_200_000.0, "Fashion", "dress", null,
                                                List.of("Đỏ", "Đen", "Trắng", "Hoa"), List.of("S", "M", "L", "XL")),
                                new BaseProduct("Váy Công Sở", "evening-dress", 650_000.0, "Fashion", "dress", null,
                                                List.of("Đen", "Xanh Navy", "Nâu"), List.of("S", "M", "L")),
                                new BaseProduct("Váy Dự Tiệc", "casual-dress", 890_000.0, "Fashion", "dress", null,
                                                List.of("Xanh", "Hồng", "Vàng"), List.of("S", "M", "L", "XL")),

                                // Áo - Range: < 400k
                                new BaseProduct("Áo Thun Cotton", "cotton-tshirt", 199_000.0, "Fashion", "tops", null,
                                                List.of("Trắng", "Đen", "Xám"), List.of("S", "M", "L", "XL")),
                                new BaseProduct("Áo Hoodie", "hoodie", 350_000.0, "Fashion", "tops", null,
                                                List.of("Đen", "Xanh Navy", "Xám"), List.of("M", "L", "XL")),
                                new BaseProduct("Áo Sơ Mi", "blouse", 280_000.0, "Fashion", "tops", null,
                                                List.of("Trắng", "Hồng", "Xanh"), List.of("S", "M", "L")),

                                // Giày - Range: 400k-2M
                                new BaseProduct("Giày Thể Thao", "sneakers", 850_000.0, "Fashion", "shoes", null,
                                                List.of("Trắng", "Đen", "Xám"), List.of("38", "39", "40", "41", "42")),
                                new BaseProduct("Giày Boots", "boots", 1_100_000.0, "Fashion", "shoes", null,
                                                List.of("Đen", "Nâu"), List.of("38", "39", "40", "41"))));

                templates.put("Beauty", Arrays.asList(
                                // Kem - Range: < 400k
                                new BaseProduct("Kem Dưỡng Da", "face-cream", 250_000.0, "Beauty", "cream", null,
                                                List.of(), List.of("50g", "100g")),
                                new BaseProduct("Kem Dưỡng Ẩm", "moisturizer", 320_000.0, "Beauty", "cream", null,
                                                List.of(), List.of("30ml", "50ml")),
                                new BaseProduct("Kem Ban Đêm", "night-cream", 380_000.0, "Beauty", "cream", null,
                                                List.of(), List.of("50g")),
                                new BaseProduct("Kem Chống Lão Hóa", "anti-aging-cream", 450_000.0, "Beauty", "cream",
                                                null,
                                                List.of(), List.of("30g", "50g")),
                                new BaseProduct("Kem Mắt", "eye-cream", 290_000.0, "Beauty", "cream", null,
                                                List.of(), List.of("15g")),

                                // Mặt nạ - Range: < 400k
                                new BaseProduct("Mặt Nạ Giấy", "sheet-mask", 35_000.0, "Beauty", "mask", null,
                                                List.of(), List.of("1 miếng", "Hộp 10 miếng")),
                                new BaseProduct("Mặt Nạ Đất Sét", "clay-mask", 180_000.0, "Beauty", "mask", null,
                                                List.of(), List.of("50g", "100g")),
                                new BaseProduct("Mặt Nạ Ngủ", "sleeping-mask", 240_000.0, "Beauty", "mask", null,
                                                List.of(), List.of("80g")),
                                new BaseProduct("Mặt Nạ Lột", "peel-off-mask", 150_000.0, "Beauty", "mask", null,
                                                List.of(), List.of("60g")),
                                new BaseProduct("Mặt Nạ Cấp Ẩm", "hydrating-mask", 210_000.0, "Beauty", "mask", null,
                                                List.of(), List.of("50g"))));

                templates.put("Electronics", Arrays.asList(
                                // Smart Watch - Range: 2M-5M (NO BRAND!)
                                new BaseProduct("Đồng Hồ Thông Minh", "smart-watch-pro", 3_500_000.0, "Electronics",
                                                "smartwatch", null, // ✅ No brand
                                                List.of("Đen", "Bạc", "Vàng"), List.of()),
                                new BaseProduct("Đồng Hồ Thể Thao", "fitness-watch", 2_200_000.0, "Electronics",
                                                "smartwatch", null, // ✅ No brand
                                                List.of("Đen", "Trắng", "Xanh"), List.of()),
                                new BaseProduct("Đồng Hồ Thông Minh Pro", "sport-watch", 2_800_000.0, "Electronics",
                                                "smartwatch", null, // ✅ No brand
                                                List.of("Đen", "Cam", "Xanh Lá"), List.of()),

                                // iPhone - Range: 5M-20M & > 20M
                                new BaseProduct("iPhone 15 Pro Max", "iphone-15-pro", 28_990_000.0, "Electronics",
                                                "iphone", "Apple",
                                                List.of("Đen", "Trắng", "Xanh", "Tím"),
                                                List.of("128GB", "256GB", "512GB", "1TB")),
                                new BaseProduct("iPhone 14 Pro", "iphone-14", 19_990_000.0, "Electronics", "iphone",
                                                "Apple",
                                                List.of("Đen", "Trắng", "Xanh"), List.of("128GB", "256GB", "512GB")),
                                new BaseProduct("iPhone 13", "iphone-13", 14_990_000.0, "Electronics", "iphone",
                                                "Apple",
                                                List.of("Đen", "Trắng", "Hồng"), List.of("128GB", "256GB")),

                                // Samsung - Range: 5M-20M & > 20M
                                new BaseProduct("Galaxy S24 Ultra", "galaxy-s24-ultra", 32_990_000.0, "Electronics",
                                                "samsung", "Samsung",
                                                List.of("Đen", "Xám", "Tím"), List.of("256GB", "512GB", "1TB")),
                                new BaseProduct("Galaxy S23", "galaxy-s23", 18_990_000.0, "Electronics", "samsung",
                                                "Samsung",
                                                List.of("Đen", "Trắng", "Xanh Lá"), List.of("128GB", "256GB")),
                                new BaseProduct("Galaxy Z Fold", "galaxy-fold", 41_990_000.0, "Electronics", "samsung",
                                                "Samsung",
                                                List.of("Đen", "Bạc"), List.of("512GB", "1TB"))));

                templates.put("Home & Living", Arrays.asList(
                                // Bàn - Range: 400k-2M
                                new BaseProduct("Bàn Làm Việc", "office-desk", 1_200_000.0, "Home & Living", "table",
                                                null,
                                                List.of("Trắng", "Đen", "Gỗ"), List.of()),
                                new BaseProduct("Bàn Học", "study-desk", 980_000.0, "Home & Living", "table", null,
                                                List.of("Trắng", "Đen", "Xám"), List.of()),
                                new BaseProduct("Bàn Ăn", "dining-table", 2_500_000.0, "Home & Living", "table", null,
                                                List.of("Gỗ", "Đen", "Trắng"), List.of()),
                                new BaseProduct("Bàn Sofa", "coffee-table", 750_000.0, "Home & Living", "table", null,
                                                List.of("Gỗ", "Kính", "Kim Loại"), List.of()),
                                new BaseProduct("Bàn Phụ", "side-table", 450_000.0, "Home & Living", "table", null,
                                                List.of("Trắng", "Đen", "Gỗ"), List.of()),

                                // Ghế - Range: 400k-2M
                                new BaseProduct("Ghế Văn Phòng", "office-chair", 1_500_000.0, "Home & Living", "chair",
                                                null,
                                                List.of("Đen", "Xám", "Xanh"), List.of()),
                                new BaseProduct("Ghế Ăn", "dining-chair", 650_000.0, "Home & Living", "chair", null,
                                                List.of("Trắng", "Đen", "Gỗ"), List.of()),
                                new BaseProduct("Ghế Sofa", "lounge-chair", 2_200_000.0, "Home & Living", "chair", null,
                                                List.of("Xám", "Be", "Xanh Navy"), List.of()),
                                new BaseProduct("Ghế Gaming", "gaming-chair", 1_800_000.0, "Home & Living", "chair",
                                                null,
                                                List.of("Đen Đỏ", "Đen Xanh", "Đen Trắng"), List.of()),
                                new BaseProduct("Ghế Bar", "bar-stool", 380_000.0, "Home & Living", "chair", null,
                                                List.of("Đen", "Trắng", "Chrome"), List.of())));

                return templates;
        }

        // ==================== GENERATE PRODUCTS ====================

        private void generateProductsForCategory(Category category, List<BaseProduct> templates, int targetCount,
                        Map<String, List<String>> imagePools, Map<String, ProductDescriptions> descriptions) {

                long existing = productRepository.countByCategory(category);

                if (existing >= targetCount) {
                        System.out.println("   ⏭️  " + category.getName() + ": Already has " + existing
                                        + " products (skipping)");
                        return;
                }

                int toGenerate = (int) (targetCount - existing);
                System.out.println("   📦 " + category.getName() + ": Generating " + toGenerate + " products...");

                Random rand = new Random();
                String[] adjectives = { "Cao Cấp", "Sang Trọng", "Thiết Yếu", "Hiện Đại", "Cổ Điển",
                                "Thời Trang", "Đẳng Cấp", "Phong Cách", "Tối Giản", "Premium" };

                for (int i = 1; i <= toGenerate; i++) {
                        BaseProduct template = templates.get(rand.nextInt(templates.size()));
                        String adj = adjectives[rand.nextInt(adjectives.length)];
                        String name = adj + " " + template.name + " #" + i;
                        String slug = template.slug + "-" + category.getSlug() + "-" + i;

                        // Price variation: ±10% from base
                        double variation = 1.0 + (rand.nextDouble() * 0.2 - 0.1); // 0.9 to 1.1
                        Double price = (double) (Math.round(template.price * variation / 1000) * 1000); // Round to
                                                                                                        // nearest 1000

                        List<String> pool = imagePools.get(template.imageType);
                        if (pool == null || pool.isEmpty()) {
                                System.err.println("      ⚠️  No images for: " + template.imageType);
                                continue;
                        }

                        String thumbnailUrl = pool.get(i % pool.size());
                        List<String> galleryImages = new ArrayList<>();
                        for (int j = 1; j <= 3; j++) {
                                galleryImages.add(pool.get((i + j) % pool.size()));
                        }

                        ProductDescriptions desc = descriptions.get(template.imageType);
                        createProductFromTemplate(template, name, slug, price, category, thumbnailUrl,
                                        galleryImages, desc);
                }

                System.out.println("      ✅ Done: " + toGenerate + " products added to " + category.getName());
        }

        private void createProductFromTemplate(BaseProduct template, String name, String slug, Double price,
                        Category category, String thumbnailUrl, List<String> galleryImages, ProductDescriptions desc) {

                if (productRepository.existsBySlug(slug)) {
                        return;
                }

                Product p = Product.builder()
                                .name(name)
                                .slug(slug)
                                .category(category)
                                .basePrice(price)
                                .brand(template.brand) // Brand cho Electronics, null for others
                                .stockQuantity(100)
                                .thumbnailUrl(thumbnailUrl)
                                .shortDesc(desc != null ? desc.shortDesc : "Sản phẩm chất lượng cao")
                                .longDesc(desc != null ? desc.longDesc : "Mô tả chi tiết sản phẩm " + name)
                                .status("ACTIVE")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                p = productRepository.save(p);

                // Variants
                if (!template.colors.isEmpty() || !template.sizes.isEmpty()) {
                        List<String> colors = template.colors.isEmpty() ? List.of("Mặc định") : template.colors;
                        List<String> sizes = template.sizes.isEmpty() ? List.of("Standard") : template.sizes;

                        for (String color : colors) {
                                for (String size : sizes) {
                                        productVariantRepository.save(ProductVariant.builder()
                                                        .product(p)
                                                        .color(color.equals("Mặc định") ? null : color)
                                                        .size(size.equals("Standard") ? null : size)
                                                        .sku(slug.toUpperCase() + "-" + color.substring(0, 1) + "-"
                                                                        + size.substring(0, 1))
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

                // Gallery images
                int sortOrder = 0;
                for (String imgUrl : galleryImages) {
                        productImageRepository.save(ProductImage.builder()
                                        .product(p)
                                        .imageUrl(imgUrl)
                                        .sortOrder(sortOrder++)
                                        .build());
                }
        }

        private Role createRole(String code, String name) {
                return roleRepository.findByCode(code)
                                .orElseGet(() -> roleRepository.save(Role.builder().code(code).name(name).build()));
        }

        private User createUser(String email, String password, Set<Role> roles) {
                return userRepository.findByEmail(email)
                                .orElseGet(() -> {
                                        User u = userRepository.save(User.builder()
                                                        .email(email)
                                                        .passwordHash(passwordEncoder.encode(password))
                                                        .roles(roles)
                                                        .status(UserStatus.ACTIVE)
                                                        .createdAt(LocalDateTime.now())
                                                        .updatedAt(LocalDateTime.now())
                                                        .build());

                                        // Create CustomerProfile for CUSTOMER users
                                        if (roles.stream().anyMatch(r -> "CUSTOMER".equals(r.getCode()))) {
                                                // This will be handled by UserService/AuthService
                                        }
                                        return u;
                                });
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
                                                .description("Danh mục " + name)
                                                .imageUrl(imageUrl)
                                                .status("ACTIVE")
                                                .sortOrder(0)
                                                .build()));
        }

        // Helper classes
        @Data
        @Builder
        @RequiredArgsConstructor
        static class BaseProduct {
                final String name;
                final String slug;
                final Double price;
                final String categoryName;
                final String imageType;
                final String brand; // Thương hiệu
                final List<String> colors;
                final List<String> sizes;
        }

        @Data
        @RequiredArgsConstructor
        static class ProductDescriptions {
                final String shortDesc;
                final String longDesc;
        }
}
