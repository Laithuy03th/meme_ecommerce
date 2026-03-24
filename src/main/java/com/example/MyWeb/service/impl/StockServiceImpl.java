package com.example.MyWeb.service.impl;

import com.example.MyWeb.exception.OutOfStockException;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of StockService.
 *
 * Chiến lược chống race condition 2 tầng:
 *
 * [Tầng 1 - DB Atomic Update]
 * atomicDecreaseStock() sinh SQL: UPDATE products SET stock = stock - ? WHERE
 * id = ? AND stock >= ?
 * DB tự khóa dòng và kiểm tra điều kiện trong 1 bước duy nhất.
 * Nếu stock=0, WHERE không thỏa → row affected = 0 → throw OutOfStockException.
 * → Không bao giờ bán âm, ngay cả khi 100 request đồng thời.
 *
 * [Tầng 2 - Optimistic Locking (@Version)]
 * Mỗi Product/Variant có cột version. JPA tự sinh:
 * UPDATE products SET ... WHERE id=? AND version=?
 * Nếu version đã thay đổi → ObjectOptimisticLockingFailureException
 * → @Retryable tự retry.
 * → Phù hợp cho các thao tác đọc-sửa-ghi phức tạp (updateProductRating, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;

    // ===================================================================
    // TẦNG 1: DB ATOMIC UPDATE — Chốt chặn cuối cùng chống oversell
    // ===================================================================

    /**
     * Trừ tồn kho bằng Atomic SQL Update.
     * DB tự kiểm tra và trừ trong 1 bước nguyên tử.
     * Row affected = 0 → hết hàng → throw OutOfStockException.
     */
    @Override
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        log.debug("Atomic decrease stock: productId={}, qty={}", productId, quantity);

        int rowsAffected = productRepository.atomicDecreaseStock(productId, quantity);

        if (rowsAffected == 0) {
            // Lấy tên sản phẩm để thông báo lỗi rõ ràng
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            Integer currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            throw new OutOfStockException(product.getName(), quantity, currentStock);
        }

        log.info("Atomic stock decreased: productId={}, qty={}", productId, quantity);
    }

    /**
     * Tăng tồn kho bằng Atomic SQL Update (dùng khi hủy đơn hàng).
     */
    @Override
    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        log.debug("Atomic increase stock: productId={}, qty={}", productId, quantity);
        productRepository.atomicIncreaseStock(productId, quantity);
        log.info("Atomic stock increased: productId={}, qty={}", productId, quantity);
    }

    /**
     * Kiểm tra tồn kho (chỉ đọc, không lock).
     * Đây là kiểm tra sơ bộ — kiểm tra thực sự xảy ra trong atomicDecreaseStock().
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Integer currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        return currentStock >= quantity;
    }

    /**
     * Validate sơ bộ trước khi tạo đơn (nhanh, không lock).
     * Vẫn có thể không chính xác nếu đang có request đồng thời —
     * nhưng atomicDecreaseStock() sẽ là "chốt chặn" cuối cùng.
     */
    @Override
    @Transactional(readOnly = true)
    public void validateStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Integer currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (currentStock < quantity) {
            throw new OutOfStockException(product.getName(), quantity, currentStock);
        }
    }

    // ===================================================================
    // TẦNG 2: OPTIMISTIC LOCKING — Retry khi bị conflict
    // ===================================================================

    /**
     * Trừ stock với Optimistic Locking + @Retryable tự động retry.
     * Dùng khi cần thao tác đọc-sửa-ghi phức tạp hơn Atomic Update.
     *
     * @Retryable: nếu bị OptimisticLockingFailureException, tự động retry
     *             tối đa 3 lần, chờ 100ms trước mỗi lần retry.
     */
    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void decreaseStockWithOptimisticLock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Integer currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (currentStock < quantity) {
            throw new OutOfStockException(product.getName(), quantity, currentStock);
        }

        // JPA sẽ tự động sinh: UPDATE ... WHERE id=? AND version=?
        // Nếu version thay đổi → OptimisticLockingFailureException → @Retryable retry
        product.setStockQuantity(currentStock - quantity);
        product.setSoldCount((product.getSoldCount() != null ? product.getSoldCount() : 0) + quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }
}
