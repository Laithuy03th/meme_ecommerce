package com.example.MyWeb.service.impl;

import com.example.MyWeb.model.Address;
import com.example.MyWeb.model.ShippingMethod;
import com.example.MyWeb.model.Voucher;
import com.example.MyWeb.model.enums.PaymentMethod;
import com.example.MyWeb.service.ShippingFeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShippingFeeServiceImpl implements ShippingFeeService {

    // Free shipping threshold
    private static final Double FREE_SHIPPING_THRESHOLD = 500.0; // 500k

    @Override
    public Double calculateFee(
            ShippingMethod shippingMethod,
            Address address,
            Double totalWeight,
            PaymentMethod paymentMethod,
            Voucher voucher) {
        if (shippingMethod == null) {
            throw new IllegalArgumentException("Shipping method is required");
        }

        // Get base fee from shipping method (30k for Standard, 50k for Express)
        double fee = shippingMethod.getBaseFee();
        log.debug("Base shipping fee: {} (Method: {})", fee, shippingMethod.getName());

        // Apply voucher discounts
        if (voucher != null) {
            // Free shipping voucher (Full discount)
            if (Boolean.TRUE.equals(voucher.getFreeShipping())) {
                log.info("Free shipping voucher applied. Original fee: {}, Final fee: 0", fee);
                return 0.0;
            }

            // Max shipping discount (Partial discount)
            if (voucher.getMaxShippingDiscount() != null && voucher.getMaxShippingDiscount() > 0) {
                double discount = Math.min(fee, voucher.getMaxShippingDiscount());
                fee -= discount;
                log.info("Shipping discount applied: -{}. New fee: {}", discount, fee);
            }
        }

        log.info("Final shipping fee: {} (Method: {})", fee, shippingMethod.getName());

        return Math.max(0.0, fee); // Never negative
    }

    @Override
    public Double calculateBaseFee(
            ShippingMethod shippingMethod,
            Address address,
            Double totalWeight,
            PaymentMethod paymentMethod) {
        if (shippingMethod == null) {
            throw new IllegalArgumentException("Shipping method is required");
        }

        // Simple: Just return base fee
        // No complex calculations needed for graduation project
        double fee = shippingMethod.getBaseFee();

        log.info("Shipping fee: {} (Method: {})", fee, shippingMethod.getName());

        return fee;
    }
}
