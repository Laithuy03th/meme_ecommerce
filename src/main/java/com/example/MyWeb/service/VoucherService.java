package com.example.MyWeb.service;

import com.example.MyWeb.dto.voucher.VoucherRequest;
import com.example.MyWeb.dto.voucher.VoucherResponse;
import com.example.MyWeb.dto.voucher.VoucherValidationResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VoucherService {

    VoucherResponse createVoucher(VoucherRequest request);

    VoucherValidationResponse validateVoucher(String code, Double orderAmount);

    VoucherResponse getVoucherByCode(String code);

    // ✅ ADDED: Missing CRUD methods
    Page<VoucherResponse> getAllVouchers(int page, int size);

    VoucherResponse getVoucherById(Long id);

    VoucherResponse updateVoucher(Long id, VoucherRequest request);

    void deleteVoucher(Long id);

    VoucherResponse toggleVoucher(Long id);

    List<VoucherResponse> getActiveVouchers();
}
