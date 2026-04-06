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
                // New fields
                .usageLimitPerUser(request.getUsageLimitPerUser())
                .freeShipping(request.getFreeShipping())
                .maxShippingDiscount(request.getMaxShippingDiscount())
                .applicableCategoryIds(request.getApplicableCategoryIds())
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

                // New fields
                .usageLimitPerUser(voucher.getUsageLimitPerUser())
                .freeShipping(voucher.getFreeShipping())
                .maxShippingDiscount(voucher.getMaxShippingDiscount())
                .applicableCategoryIds(voucher.getApplicableCategoryIds())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<VoucherResponse> getAllVouchers(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        return voucherRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
        return toDto(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));

        DiscountType discountType;
        try {
            discountType = DiscountType.valueOf(request.getDiscountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid discount type. Must be PERCENT or AMOUNT");
        }

        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setUsageLimit(request.getUsageLimit());

        // New fields update
        voucher.setUsageLimitPerUser(request.getUsageLimitPerUser());
        voucher.setFreeShipping(request.getFreeShipping());
        voucher.setMaxShippingDiscount(request.getMaxShippingDiscount());
        voucher.setApplicableCategoryIds(request.getApplicableCategoryIds());

        voucher = voucherRepository.save(voucher);
        return toDto(voucher);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
        voucherRepository.delete(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse toggleVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found with id: " + id));
        voucher.setIsActive(!voucher.getIsActive());
        voucher = voucherRepository.save(voucher);
        return toDto(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<VoucherResponse> getActiveVouchers() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // L6 FIX: Dùng query có điều kiện, không load toàn bộ bảng vào RAM
        return voucherRepository.findActiveVouchers(now).stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }
}
