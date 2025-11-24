// repository/ProductVariantRepository.java
package com.example.MyWeb.repository;

import com.example.MyWeb.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
}
