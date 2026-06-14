package com.example.MyWeb.repository.spec;

import com.example.MyWeb.model.Product;
import com.example.MyWeb.model.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;
import java.util.stream.Collectors;

public class ProductSpecifications {

    private static final Set<String> STOP_WORDS = Set.of(
            "toi", "tôi", "muon", "muốn", "tim", "tìm", "mua", "cho", "và", "của",
            "san", "sản", "pham", "phẩm", "loai", "loại", "con", "cai", "cái",
            "nao", "nào", "tam", "tầm", "khoang", "khoảng", "gia", "giá",
            "mau", "màu", "size", "duoi", "dưới", "tren", "trên", "duoc", "được");

    private ProductSpecifications() {
    }

    public static Specification<Product> search(
            String keyword,
            String categorySlug,
            Double minPrice,
            Double maxPrice,
            String brand,
            Double minRating) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            Join<Object, Object> categoryJoin = root.join("category", JoinType.INNER);
            Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);

            // chỉ lấy variant ACTIVE hoặc sản phẩm không có variant
            predicates.add(cb.or(
                    cb.isNull(variantJoin.get("status")),
                    cb.equal(cb.upper(cb.coalesce(variantJoin.get("status"), "ACTIVE")), "ACTIVE")));

            if (categorySlug != null && !categorySlug.isBlank()) {
                predicates.add(cb.equal(categoryJoin.get("slug"), categorySlug.trim()));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }

            if (brand != null && !brand.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(cb.coalesce(root.get("brand"), "")),
                        "%" + brand.trim().toLowerCase(Locale.ROOT) + "%"));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        cb.coalesce(root.get("averageRating"), 0.0),
                        minRating));
            }

            if (keyword != null && !keyword.isBlank()) {
                List<String> tokens = tokenize(keyword);

                if (tokens.isEmpty()) {
                    tokens = List.of(keyword.trim().toLowerCase(Locale.ROOT));
                }

                List<Predicate> perTokenPredicates = new ArrayList<>();

                for (String token : tokens) {
                    String kw = "%" + token + "%";

                    // Chỉ match tên, brand, sku — KHÔNG match shortDesc/longDesc
                    // để tránh sản phẩm không liên quan lọt vào kết quả
                    // (ví dụ: áo thun có mô tả chứa "váy" sẽ không bị match sai)
                    Predicate byName = cb.like(cb.lower(cb.coalesce(root.get("name"), "")), kw);
                    Predicate byBrand = cb.like(cb.lower(cb.coalesce(root.get("brand"), "")), kw);
                    Predicate byProductSku = cb.like(cb.lower(cb.coalesce(root.get("sku"), "")), kw);

                    Predicate byVarColor = cb.like(cb.lower(cb.coalesce(variantJoin.get("color"), "")), kw);
                    Predicate byVarSize = cb.like(cb.lower(cb.coalesce(variantJoin.get("size"), "")), kw);
                    Predicate byVarSku = cb.like(cb.lower(cb.coalesce(variantJoin.get("sku"), "")), kw);

                    perTokenPredicates.add(cb.or(
                            byName,
                            byBrand,
                            byProductSku,
                            byVarColor,
                            byVarSize,
                            byVarSku));
                }

                // AND giữa các token để tăng độ chính xác
                predicates.add(cb.and(perTokenPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static List<String> tokenize(String keyword) {
        return Arrays.stream(keyword.toLowerCase(Locale.ROOT).trim().split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !STOP_WORDS.contains(s))
                .distinct()
                .collect(Collectors.toList());
    }
}