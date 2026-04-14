package com.sni.bokaticowork.core.baseClasses.service.interfaces;


import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import com.sni.bokaticowork.core.baseClasses.model.Address;

/*
* The feature in address service will be for most use between service not by the controller
* */
public interface AddressService {


    Address createAddress(AddressRequest request);
    Address updateAddress(long id, AddressRequest request);
    void deleteAddress(long id);
    AddressResponse getAddressById(long id);
}
