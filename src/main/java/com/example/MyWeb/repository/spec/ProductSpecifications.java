package com.example.MyWeb.repository.spec;

import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA Specification cho product search/filter.
 *
 * Tầng 1: Tìm theo products (name, brand, shortDesc, longDesc, sku)
 *           và categories (name, slug)
 * Tầng 2: Tìm thêm theo product_variants (color, size, sku)
 *
 * Ví dụ hoạt động:
 *   keyword=iphone        → match product.name
 *   keyword=samsung đen   → match product.name + variant.color
 *   keyword=256gb         → match variant.sku
 *   keyword=áo size m     → match product.name + variant.size
 */
public class ProductSpecifications {

    private ProductSpecifications() {} // Utility class

    public static Specification<Product> search(
            String keyword,
            String categorySlug,
            Double minPrice,
            Double maxPrice,
            String brand,
            Double minRating
    ) {
        return (root, query, cb) -> {
            // QUAN TRỌNG: distinct để tránh duplicate khi LEFT JOIN variant
            // (1 sản phẩm có nhiều variant sẽ xuất hiện nhiều lần nếu không có distinct)
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            // ── Chỉ lấy sản phẩm ACTIVE ─────────────────────────────────
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            // ── JOIN category (INNER — sản phẩm bắt buộc có category) ───
            Join<Object, Object> categoryJoin = root.join("category", JoinType.INNER);

            // ── JOIN variants (LEFT — sản phẩm không có variant vẫn xuất hiện) ──
            Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);

            // ── Filter: Category slug ────────────────────────────────────
            if (categorySlug != null && !categorySlug.isBlank()) {
                predicates.add(cb.equal(
                        categoryJoin.get("slug"),
                        categorySlug.trim()
                ));
            }

            // ── Filter: Khoảng giá ───────────────────────────────────────
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }

            // ── Filter: Brand (exact match, case-insensitive) ────────────
            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(cb.coalesce(root.<String>get("brand"), "")),
                        "%" + brand.trim().toLowerCase(Locale.ROOT) + "%"
                ));
            }

            // ── Filter: Rating tối thiểu ─────────────────────────────────
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        cb.coalesce(root.<Double>get("averageRating"), 0.0),
                        minRating
                ));
            }

            // ── Keyword search (Tầng 1 + Tầng 2) ────────────────────────
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

                // Tầng 1: Products fields
                Predicate byName      = cb.like(cb.lower(root.get("name")), kw);
                Predicate byBrand     = cb.like(cb.lower(cb.coalesce(root.<String>get("brand"), "")), kw);
                Predicate byShortDesc = cb.like(cb.lower(cb.coalesce(root.<String>get("shortDesc"), "")), kw);
                Predicate byLongDesc  = cb.like(cb.lower(cb.coalesce(root.<String>get("longDesc"), "")), kw);
                Predicate byProductSku = cb.like(cb.lower(cb.coalesce(root.<String>get("sku"), "")), kw);

                // Tầng 1: Category fields
                Predicate byCatName   = cb.like(cb.lower(cb.coalesce(categoryJoin.<String>get("name"), "")), kw);
                Predicate byCatSlug   = cb.like(cb.lower(cb.coalesce(categoryJoin.<String>get("slug"), "")), kw);

                // Tầng 2: Variant fields
                Predicate byVarColor  = cb.like(cb.lower(cb.coalesce(variantJoin.<String>get("color"), "")), kw);
                Predicate byVarSize   = cb.like(cb.lower(cb.coalesce(variantJoin.<String>get("size"), "")), kw);
                Predicate byVarSku    = cb.like(cb.lower(cb.coalesce(variantJoin.<String>get("sku"), "")), kw);

                predicates.add(cb.or(
                        byName,
                        byBrand,
                        byShortDesc,
                        byLongDesc,
                        byProductSku,
                        byCatName,
                        byCatSlug,
                        byVarColor,
                        byVarSize,
                        byVarSku
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
