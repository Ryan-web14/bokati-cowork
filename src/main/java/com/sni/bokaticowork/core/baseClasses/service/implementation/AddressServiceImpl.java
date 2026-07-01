package com.sni.bokaticowork.core.baseClasses.service.implementation;


import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.AddressMapper;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.repository.AddressRepository;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
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


    private void validateAddress(AddressRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Address request cannot be null");
        }
        if (!org.springframework.util.StringUtils.hasText(request.getCity())) {
            throw new IllegalArgumentException("City is required");
        }
    }
}

