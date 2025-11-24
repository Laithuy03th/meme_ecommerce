// service/impl/AddressServiceImpl.java
package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.user.AddressRequest;
import com.example.MyWeb.dto.user.AddressResponse;
import com.example.MyWeb.model.Address;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.AddressRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepo;
    private final UserRepository userRepo;

    private AddressResponse toDto(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .ward(a.getWard())
                .district(a.getDistrict())
                .province(a.getProvince())
                .country(a.getCountry())
                .isDefault(a.isDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }

    @Override
    public List<AddressResponse> list(Long userId) {
        return addressRepo.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public AddressResponse create(Long userId, AddressRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepo.clearDefaultByUserId(userId);
        }

        Address a = Address.builder()
                .user(user)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .addressLine1(req.getAddressLine1())
                .ward(req.getWard())
                .district(req.getDistrict())
                .province(req.getProvince())
                .country(req.getCountry() != null ? req.getCountry() : "Vietnam")
                .createdAt(LocalDateTime.now())
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .build();

        return toDto(addressRepo.save(a));
    }

    @Override
    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest req) {
        Address a = addressRepo.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (req.getFullName() != null)
            a.setFullName(req.getFullName());
        if (req.getPhone() != null)
            a.setPhone(req.getPhone());
        if (req.getAddressLine1() != null)
            a.setAddressLine1(req.getAddressLine1());
        if (req.getWard() != null)
            a.setWard(req.getWard());
        if (req.getDistrict() != null)
            a.setDistrict(req.getDistrict());
        if (req.getProvince() != null)
            a.setProvince(req.getProvince());
        if (req.getCountry() != null)
            a.setCountry(req.getCountry());

        if (req.getIsDefault() != null && req.getIsDefault()) {
            addressRepo.clearDefaultByUserId(userId);
            a.setDefault(true);
        } else if (req.getIsDefault() != null && !req.getIsDefault()) {
            a.setDefault(false);
        }

        return toDto(addressRepo.save(a));
    }

    @Override
    public void delete(Long userId, Long addressId) {
        Address a = addressRepo.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepo.delete(a);
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        Address a = addressRepo.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepo.clearDefaultByUserId(userId);
        a.setDefault(true);
        return toDto(addressRepo.save(a));
    }
}
