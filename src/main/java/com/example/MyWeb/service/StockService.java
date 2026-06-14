package com.example.MyWeb.service;

public interface StockService {

    void decreaseStock(Long productId, Integer quantity);

    void increaseStock(Long productId, Integer quantity);

    boolean hasStock(Long productId, Integer quantity);

    void validateStock(Long productId, Integer quantity);
}
