package com.sni.bokaticowork.core.baseClasses.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.V1 + "/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @Audited(module = "ADDRESS", action = "CREATE_ADDRESS", ressource = "address")
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        addressService.createAddress(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    @Audited(module = "ADDRESS", action = "UPDATE_ADDRESS", ressource = "address")
    public ResponseEntity<AddressResponse> update(@PathVariable long id,
                                                   @Valid @RequestBody AddressRequest request) {
        addressService.updateAddress(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> get(@PathVariable long id) {
        return ResponseEntity.ok(addressService.getAddressById(id));
    }

    @DeleteMapping("/{id}")
    @Audited(module = "ADDRESS", action = "DELETE_ADDRESS", ressource = "address")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}
