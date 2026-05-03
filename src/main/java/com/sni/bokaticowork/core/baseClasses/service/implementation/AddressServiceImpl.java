package com.sni.bokaticowork.core.baseClasses.service.implementation;


import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.AddressMapper;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.repository.AddressRepository;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
@Slf4j
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;
    private final AddressRepository addressRepo;

    @Override
    public Address createAddress(@NotNull AddressRequest request){

        try {
            validateAddress(request);
        }catch (Exception e){
            log.debug("Invalid address request", e);
            throw new IllegalArgumentException("Invalid address request", e);
        }

        return addressRepo.save(addressMapper.toEntity(request));
    }

    @Override
    public Address updateAddress(long id, @NotNull AddressRequest request){

        try{
            validateAddress(request);
        }catch (Exception e){
            log.debug("Invalid address request", e);
            throw new IllegalArgumentException("Invalid address request", e);
        }

        Address obj = addressRepo.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Address with id " + id + " not found"));
        return addressMapper.mapAddressUpdate(obj, request);
    }

    public AddressResponse getAddressById(long id){
        return addressMapper.toDTO(addressRepo.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Address with id " + id + " not found")));
    }

    public void deleteAddress(long id){
        addressRepo.deleteById(id);
        return;
    }


    //Validate address request
    private void validateAddress(AddressRequest request){

        log.debug("Validating address request");

        if (request == null){
            throw new IllegalArgumentException("Address request cannot be null");
        }

        if( request.getDistrict().isEmpty() || request.getCity().isEmpty()){
            log.debug("Invalid address request, one or more fields are empty");
            throw new IllegalArgumentException("Invalid address request, one or more fields are empty");
        }

        if(!ValidationUtils.isDigit(request.getStreetNumber())){
            log.debug("Invalid address request, street number is not a number");
            throw new IllegalArgumentException("Invalid address request, street number is not a number");
        }else if (request.getStreetNumber().isEmpty()){
            return;
        }

        if(!ValidationUtils.validateString(request.getStreetName())){
            log.debug("Invalid address request, street name is not a string");
            throw new IllegalArgumentException("Invalid address request, street name is not a string");
        }else if (request.getStreetName().isEmpty()){
            return;
        }

        if(!ValidationUtils.validateString(request.getDistrict())){
            log.debug("Invalid address request, district is not a string");
            throw new IllegalArgumentException("Invalid address request, district is not a string");
        }
    }
}

