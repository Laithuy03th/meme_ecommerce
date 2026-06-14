package com.example.MyWeb.service;

import com.example.MyWeb.model.Address;
import com.example.MyWeb.model.ShippingMethod;
import com.example.MyWeb.model.Voucher;
import com.example.MyWeb.model.enums.PaymentMethod;

public interface ShippingFeeService {

        Double calculateFee(
                        ShippingMethod shippingMethod,
                        Address address,
                        Double totalWeight,
                        PaymentMethod paymentMethod,
                        Voucher voucher);

        Double calculateBaseFee(
                        ShippingMethod shippingMethod,
                        Address address,
                        Double totalWeight,
                        PaymentMethod paymentMethod);
}
