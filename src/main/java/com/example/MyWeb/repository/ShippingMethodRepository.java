package com.example.MyWeb.repository;

import com.example.MyWeb.model.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ShippingMethod entity
 */
public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, Long> {

    /**
     * Find shipping method by code
     */
    Optional<ShippingMethod> findByCode(String code);

    /**
     * Find active shipping method by ID
     */
    Optional<ShippingMethod> findByIdAndIsActive(Long id, Boolean isActive);

    /**
     * Get all active shipping methods ordered by sort order
     */
    List<ShippingMethod> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * Check if code exists
     */
    boolean existsByCode(String code);
}
