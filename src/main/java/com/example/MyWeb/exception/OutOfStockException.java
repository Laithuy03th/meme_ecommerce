package com.example.MyWeb.exception;

/**
 * Exception thrown when product stock is insufficient
 */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String message) {
        super(message);
    }

    public OutOfStockException(String productName, Integer requested, Integer available) {
        super(String.format("%s: Yêu cầu %d sản phẩm nhưng chỉ còn %d trong kho",
                productName, requested, available));
    }
}
