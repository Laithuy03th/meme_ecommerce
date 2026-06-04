package com.example.MyWeb.controller;

import com.example.MyWeb.dto.voucher.VoucherRequest;
import com.example.MyWeb.dto.voucher.VoucherResponse;
import com.example.MyWeb.dto.voucher.VoucherValidationResponse;
import com.example.MyWeb.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // ==================== ADMIN ENDPOINTS ====================

    @PostMapping("/admin/vouchers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.createVoucher(request));
    }

    // ✅ ADDED: Get all vouchers with pagination
    @GetMapping("/admin/vouchers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<VoucherResponse>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voucherService.getAllVouchers(page, size));
    }

    // ✅ ADDED: Get voucher by ID
    @GetMapping("/admin/vouchers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    // ✅ ADDED: Update voucher
    @PutMapping("/admin/vouchers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherResponse> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, request));
    }

    // ✅ ADDED: Delete voucher
    @DeleteMapping("/admin/vouchers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ ADDED: Toggle voucher active status
    @PatchMapping("/admin/vouchers/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherResponse> toggleVoucher(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.toggleVoucher(id));
    }

    // ==================== PUBLIC ENDPOINTS ====================

    // ✅ ADDED: Get active vouchers (for customers)
    @GetMapping("/vouchers")
    public ResponseEntity<List<VoucherResponse>> getActiveVouchers() {
        return ResponseEntity.ok(voucherService.getActiveVouchers());
    }

    @GetMapping("/vouchers/validate")
    public ResponseEntity<VoucherValidationResponse> validateVoucher(
            @RequestParam String code,
            @RequestParam Double orderAmount,
            @RequestParam(required = false) Long userId) { // FIX: Thêm userId để check per-user limit
        return ResponseEntity.ok(voucherService.validateVoucher(code, orderAmount, userId));
    }
}
