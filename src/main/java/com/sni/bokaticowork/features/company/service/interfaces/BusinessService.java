package com.sni.bokaticowork.features.company.service.interfaces;




import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.company.dto.request.BusinessEntityRequest;
import com.sni.bokaticowork.features.company.dto.request.BusinessSearchCriteria;
import com.sni.bokaticowork.features.company.dto.request.UpdateBusinessStatusRequest;
import com.sni.bokaticowork.features.company.dto.response.BusinessEntityResponse;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BusinessService {

    BusinessEntityResponse createBusiness(BusinessEntityRequest request);
    BusinessEntityResponse updateBusiness(String businessCode, BusinessEntityRequest request);
    BusinessEntityResponse getBusinessByCode(String code);
    BusinessEntityResponse getBusinessByName(String name);
    List<BusinessEntityResponse> basicSearch(String query);
    List<BusinessEntityResponse> getAllBusiness();
    PaginatedResponse<BusinessEntityResponse> list(Pageable pageable);
    PaginatedResponse<BusinessEntityResponse> search(BusinessSearchCriteria criteria, Pageable pageable);
    void updateBusinessAddress(String businessCode, AddressRequest request);
    void updateBusinessStatus(String businessCode, UpdateBusinessStatusRequest request);

    /**
     * Designe cette entite comme exploitante de l espace · celle qui signe face au client.
     *
     * <p>Geste explicite et unique : la designation retire le drapeau a celle qui le portait.
     * Sans entite designee, aucun contrat ne peut etre genere · son adresse determine le lieu de
     * signature et la juridiction competente.</p>
     */
    void designateOperatingBusiness(String businessCode);

    //This function will soft delete the business
    void deleteBusiness(String businessCode);

    //method to be use between services
    BusinessEntity serviceBusinessByCode(String code);
    BusinessEntity serviceBusinessById(Long id);
}
