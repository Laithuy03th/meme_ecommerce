package com.example.MyWeb.service;

import com.example.MyWeb.dto.voucher.VoucherRequest;
import com.example.MyWeb.dto.voucher.VoucherResponse;
import com.example.MyWeb.dto.voucher.VoucherValidationResponse;

public interface VoucherService {

    VoucherResponse createVoucher(VoucherRequest request);

    VoucherValidationResponse validateVoucher(String code, Double orderAmount);

    VoucherResponse getVoucherByCode(String code);
}
