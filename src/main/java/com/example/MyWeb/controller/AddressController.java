// controller/AddressController.java
package com.example.MyWeb.controller;

import com.example.MyWeb.dto.user.AddressRequest;
import com.example.MyWeb.dto.user.AddressResponse;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(@AuthenticationPrincipal CustomUserDetails me) {
        return ResponseEntity.ok(addressService.list(me.getId()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal CustomUserDetails me,
            @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.ok(addressService.create(me.getId(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.ok(addressService.update(me.getId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long id) {
        addressService.delete(me.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(@AuthenticationPrincipal CustomUserDetails me,
            @PathVariable Long id) {
        return ResponseEntity.ok(addressService.setDefault(me.getId(), id));
    }
}
