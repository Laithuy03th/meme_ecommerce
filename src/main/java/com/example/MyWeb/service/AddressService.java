// service/AddressService.java
package com.example.MyWeb.service;

import com.example.MyWeb.dto.user.AddressRequest;
import com.example.MyWeb.dto.user.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> list(Long userId);

    AddressResponse create(Long userId, AddressRequest req);

    AddressResponse update(Long userId, Long addressId, AddressRequest req);

    void delete(Long userId, Long addressId);

    AddressResponse setDefault(Long userId, Long addressId);
}
