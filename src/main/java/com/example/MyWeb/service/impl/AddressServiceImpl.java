package com.example.MyWeb.service.impl;

import com.example.MyWeb.dto.user.AddressRequest;
import com.example.MyWeb.dto.user.AddressResponse;
import com.example.MyWeb.model.Address;
import com.example.MyWeb.model.User;
import com.example.MyWeb.repository.AddressRepository;
import com.example.MyWeb.repository.UserRepository;
import com.example.MyWeb.security.CustomUserDetails;
import com.example.MyWeb.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    // Lấy user hiện tại từ SecurityContext
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        Long userId = principal.getId();

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private AddressResponse toAddressResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .ward(a.getWard())
                .district(a.getDistrict())
                .province(a.getProvince())
                .country(a.getCountry())
                .default(a.isDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }

    @Override
    public List<AddressResponse> getMyAddresses() {
        User user = getCurrentUser();
        List<Address> list = addressRepository
                .findByUserOrderByCreatedAtDesc(user);
        return list.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse createAddress(AddressRequest req) {
        User user = getCurrentUser();

        // Nếu isDefault = true -> clear default cũ
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepository.clearDefaultForUser(user);
        }

        Address address = Address.builder()
                .user(user)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .addressLine1(req.getAddressLine1())
                .ward(req.getWard())
                .district(req.getDistrict())
                .province(req.getProvince())
                .country(req.getCountry() != null ? req.getCountry() : "Vietnam")
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .createdAt(LocalDateTime.now())
                .build();

        address = addressRepository.save(address);
        return toAddressResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long id, AddressRequest req) {
        User user = getCurrentUser();

        Address a = addressRepository.findByIdAndUser(id, user)
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

        if (req.getIsDefault() != null) {
            if (req.getIsDefault()) {
                addressRepository.clearDefaultForUser(user);
                a.setDefault(true);
            } else {
                a.setDefault(false);
            }
        }

        return toAddressResponse(a);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long id) {
        User user = getCurrentUser();

        Address a = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Clear default cũ
        addressRepository.clearDefaultForUser(user);
        a.setDefault(true);

        return toAddressResponse(a);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        User user = getCurrentUser();

        Address a = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        addressRepository.delete(a);
    }
}
