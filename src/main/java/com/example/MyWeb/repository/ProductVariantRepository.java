// repository/ProductVariantRepository.java
package com.example.MyWeb.repository;

import com.example.MyWeb.model.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
        List<ProductVariant> findByProductId(Long productId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
        Optional<ProductVariant> findByIdWithLock(@Param("id") Long id);

        /**
         * Trừ stock variant nguyên tử.
         * Điều kiện WHERE v.stock >= :qty đảm bảo không bao giờ bán âm.
         * 
         * @return 1 = thành công, 0 = hết hàng
         */
        @Modifying
        @Query("UPDATE ProductVariant v SET v.stock = v.stock - :qty, "
                        + "v.updatedAt = CURRENT_TIMESTAMP "
                        + "WHERE v.id = :variantId AND v.stock >= :qty")
        int atomicDecreaseStock(@Param("variantId") Long variantId, @Param("qty") int qty);

        /**
         * Tăng stock variant nguyên tử - dùng khi hủy đơn hàng.
         */
        @Modifying
        @Query("UPDATE ProductVariant v SET v.stock = v.stock + :qty, "
                        + "v.updatedAt = CURRENT_TIMESTAMP "
                        + "WHERE v.id = :variantId")
        int atomicIncreaseStock(@Param("variantId") Long variantId, @Param("qty") int qty);
}
