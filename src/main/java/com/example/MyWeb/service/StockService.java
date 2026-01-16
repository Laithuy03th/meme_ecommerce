package com.example.MyWeb.service;

/**
 * Service for managing product stock/inventory
 * Handles stock decrease, increase, and validation with concurrency control
 * 
 * @author Senior Software Engineer
 */
public interface StockService {

    /**
     * Decrease stock quantity for a product (when order is placed)
     * Uses pessimistic locking to prevent overselling in concurrent scenarios
     * 
     * @param productId the product ID
     * @param quantity  quantity to decrease
     * @throws OutOfStockException if insufficient stock
     */
    void decreaseStock(Long productId, Integer quantity);

    /**
     * Increase stock quantity for a product (when order is cancelled/returned)
     * 
     * @param productId the product ID
     * @param quantity  quantity to increase
     */
    void increaseStock(Long productId, Integer quantity);

    /**
     * Check if product has sufficient stock
     * 
     * @param productId the product ID
     * @param quantity  required quantity
     * @return true if stock is sufficient
     */
    boolean hasStock(Long productId, Integer quantity);

    /**
     * Validate stock availability for multiple products
     * Used in cart validation before checkout
     * 
     * @param productId the product ID
     * @param quantity  required quantity
     * @throws OutOfStockException if insufficient stock
     */
    void validateStock(Long productId, Integer quantity);
}
