package com.sni.bokaticowork.core.baseClasses.mapper.decorator;



import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.AddressMapper;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Country;
import com.sni.bokaticowork.core.baseClasses.repository.CountryRepository;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

//TODO: improve address decorator

@Component
@Slf4j
public abstract class AddressMapperDecorator implements AddressMapper {

    @Autowired
    @Qualifier("delegate")
    private  AddressMapper delegate;
    private CountryRepository countryRepo;


    public AddressMapperDecorator(){}

    @Autowired
    public void setCountryRepository(CountryRepository countryRepo) {
        this.countryRepo = countryRepo;
    }

    @Override
    public AddressResponse toDTO(Address obj){

        if(obj == null){
            return null;
        }

        AddressResponse response = delegate.toDTO(obj);
        response.setCountry(obj.getCountry().getName());
        log.debug("Mapping Address {} to DTO", obj.getId());

        try{
            log.debug("Formatting full address for address {}", obj.getId());
            response.setFullAddress(formatFullAddress(obj));
        }catch(Exception ex){
            response.setFullAddress("Error formatting full address for address " + obj.getId());
            log.error("Error formatting full address for address {}", obj.getId(), ex);
        }

        log.debug("Mapping country {} and ID - {}", obj.getCountry().getName(), obj.getCountry().getId());
        response.setCountry(obj.getCountry().getName());

        return response;
    }

    @Override
    public Address toEntity(AddressRequest request){

        if(request == null){
            return null;
        }

        log.debug("Mapping address request");
        Address obj = delegate.toEntity(request);

        try {
            Country country = countryRepo.findByCountryCode(request.getCountryCode())
                    .orElseThrow(()-> new ResourceNotFoundException("Country with code " +
                            request.getCountryCode() + " not found"));
            obj.setCountry(country);
        }catch (ResourceNotFoundException ex){
            log.error("No country found for country code {}", request.getCountryCode());
            throw new BadRequestException("No country found for country code " + request.getCountryCode(), ex);
        }


        return obj;
    }

    @Override
    public Address mapAddressUpdate(Address obj, AddressRequest request){

        if(obj == null || request == null){
            return null;
        }

        log.debug("Mapping update address {} and ID - {}", obj.getId(), obj.getId());
        obj.setStreetNumber(request.getStreetNumber());
        obj.setStreetName(request.getStreetName());
        obj.setDistrict(request.getDistrict());
        obj.setCity(request.getCity());

        if(!obj.getCountry().getCountryCode().equals(request.getCountryCode())){
            log.debug("Updating country for address {}", obj.getId());

            try {
                Country country = countryRepo.findByCountryCode(request.getCountryCode())
                        .orElseThrow(()-> new ResourceNotFoundException("Country with code " +
                                request.getCountryCode() + " not found"));
                obj.setCountry(country);
                log.debug("Country updated for address {}", obj.getId());
            }catch (ResourceNotFoundException ex){
                log.error("the updated address: No country found for country code {}", request.getCountryCode());
                throw new BadRequestException("No country found for country code " + request.getCountryCode(), ex);
            }

        }
        return obj;
    }

    private String formatFullAddress(Address obj){

        if(obj == null){
            return null;
        }

        StringBuilder str = new StringBuilder();

        if(StringUtils.hasText(obj.getStreetName())){
            str.append(obj.getStreetNumber()).append(" ").append(obj.getStreetName()).append(", ");
        }
        if (StringUtils.hasText(obj.getDistrict())){
            str.append(obj.getDistrict()).append(" ");
        }
        if(StringUtils.hasText(obj.getCity())){
            str.append(obj.getCity()).append(", ");
        }
        if(StringUtils.hasText(obj.getCountry().getName())){
            str.append(obj.getCountry().getName());
        }

        return str.toString().replaceAll(", $", "") ;
    }
}
