package com.sni.bokaticowork.features.company.repository;

import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.model.BusinessEntityIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessEntityIdentifierRepository extends JpaRepository<BusinessEntityIdentifier, Long> {

    List<BusinessEntityIdentifier> findAllByBusinessEntityOrderByIdAsc(BusinessEntity businessEntity);

    void deleteAllByBusinessEntity(BusinessEntity businessEntity);
}
