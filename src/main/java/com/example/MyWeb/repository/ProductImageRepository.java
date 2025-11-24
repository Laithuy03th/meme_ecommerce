// repository/ProductImageRepository.java
package com.example.MyWeb.repository;

import com.example.MyWeb.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}
