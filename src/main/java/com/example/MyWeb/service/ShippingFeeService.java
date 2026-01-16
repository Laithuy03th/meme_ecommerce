package com.example.MyWeb.service;

import com.example.MyWeb.model.Address;
import com.example.MyWeb.model.ShippingMethod;
import com.example.MyWeb.model.Voucher;
import com.example.MyWeb.model.enums.PaymentMethod;

/**
 * Service for calculating shipping fees
 * Based on shipping method, weight, distance, payment method, and vouchers
 * 
 * @author Senior Software Engineer
 */
public interface ShippingFeeService {

    /**
     * Calculate shipping fee with all factors
     * 
     * @param shippingMethod Selected shipping method
     * @param address        Delivery address
     * @param totalWeight    Total weight in kg
     * @param paymentMethod  Payment method (COD has surcharge)
     * @param voucher        Applied voucher (may have free shipping)
     * @return Final shipping fee after all calculations
     */
    Double calculateFee(
            ShippingMethod shippingMethod,
            Address address,
            Double totalWeight,
            PaymentMethod paymentMethod,
            Voucher voucher);

    /**
     * Calculate base shipping fee without voucher
     */
    Double calculateBaseFee(
            ShippingMethod shippingMethod,
            Address address,
            Double totalWeight,
            PaymentMethod paymentMethod);
}
