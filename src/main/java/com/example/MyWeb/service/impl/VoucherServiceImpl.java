package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.voucher.VoucherRequest;
import com.example.MyWeb.dto.voucher.VoucherResponse;
import com.example.MyWeb.dto.voucher.VoucherValidationResponse;
import com.example.MyWeb.exception.ResourceNotFoundException;
import com.example.MyWeb.model.Voucher;
import com.example.MyWeb.model.enums.DiscountType;
import com.example.MyWeb.repository.VoucherRepository;
import com.example.MyWeb.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Voucher code already exists");
        }

        DiscountType discountType;
        try {
            discountType = DiscountType.valueOf(request.getDiscountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid discount type. Must be PERCENT or AMOUNT");
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode().toUpperCase())
                .discountType(discountType)
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .build();

        voucher = voucherRepository.save(voucher);
        return toDto(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(String code, Double orderAmount) {
        Voucher voucher = voucherRepository.findByCode(code.toUpperCase())
                .orElse(null);

        if (voucher == null) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .message("Voucher code not found")
                    .discountAmount(0.0)
                    .build();
        }

        if (!voucher.isValid()) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .message("Voucher is not valid or has expired")
                    .discountAmount(0.0)
                    .voucher(toDto(voucher))
                    .build();
        }

        if (voucher.getMinOrderAmount() != null && orderAmount < voucher.getMinOrderAmount()) {
            return VoucherValidationResponse.builder()
                    .valid(false)
                    .message("Order amount must be at least " + voucher.getMinOrderAmount())
                    .discountAmount(0.0)
                    .voucher(toDto(voucher))
                    .build();
        }

        Double discountAmount = voucher.calculateDiscount(orderAmount);

        return VoucherValidationResponse.builder()
                .valid(true)
                .message("Voucher is valid")
                .discountAmount(discountAmount)
                .voucher(toDto(voucher))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(String code) {
        Voucher voucher = voucherRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        return toDto(voucher);
    }

    private VoucherResponse toDto(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType().name())
                .discountValue(voucher.getDiscountValue())
                .minOrderAmount(voucher.getMinOrderAmount())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .isActive(voucher.getIsActive())
                .build();
    }
}
