package com.example.MyWeb.repository;

import com.example.MyWeb.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    // L6: Query có điều kiện — không load toàn bộ bảng
    @Query("SELECT v FROM Voucher v WHERE v.isActive = true " +
           "AND (v.startDate IS NULL OR v.startDate <= :now) " +
           "AND (v.endDate IS NULL OR v.endDate >= :now) " +
           "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    List<Voucher> findActiveVouchers(@Param("now") LocalDateTime now);

    /**
     * FIX Race Condition: Tăng usedCount nguyên tử (Atomic SQL UPDATE).
     * Điều kiện WHERE đảm bảo chỉ update khi còn lượt — nếu nhiều request
     * cùng lúc, chỉ đúng 1 request thành công (rows = 1), các request còn
     * lại trả về 0 → bị chặn lại.
     */
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 " +
           "WHERE v.id = :id " +
           "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    int incrementUsedCount(@Param("id") Long id);
}
