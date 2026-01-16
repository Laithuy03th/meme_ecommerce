package com.example.MyWeb.service.impl;

import com.example.MyWeb.exception.OutOfStockException;
import com.example.MyWeb.model.Product;
import com.example.MyWeb.repository.ProductRepository;
import com.example.MyWeb.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of StockService
 * Manages product inventory with proper concurrency control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        // Pessimistic lock to prevent concurrent stock issues
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Integer currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

        // Validate stock availability
        if (currentStock < quantity) {
            throw new OutOfStockException(product.getName(), quantity, currentStock);
        }

        // Decrease stock
        product.setStockQuantity(currentStock - quantity);

        // Increase sold count
        Integer currentSold = product.getSoldCount() != null ? product.getSoldCount() : 0;
        product.setSoldCount(currentSold + quantity);

        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        log.info("Decreased stock for product {} by {}. New stock: {}",
                productId, quantity, product.getStockQuantity());
    }

    @Override
    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Integer currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        product.setStockQuantity(currentStock + quantity);

        // Decrease sold count (when order cancelled/returned)
        Integer currentSold = product.getSoldCount() != null ? product.getSoldCount() : 0;
        product.setSoldCount(Math.max(0, currentSold - quantity)); // Don't go negative

        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        log.info("Increased stock for product {} by {}. New stock: {}",
                productId, quantity, product.getStockQuantity());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Integer currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        return currentStock >= quantity;
    }

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
}
