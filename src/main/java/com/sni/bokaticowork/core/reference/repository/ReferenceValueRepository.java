package com.sni.bokaticowork.core.reference.repository;

import com.sni.bokaticowork.core.reference.model.ReferenceValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReferenceValueRepository extends JpaRepository<ReferenceValue, Long> {

    List<ReferenceValue> findByListCodeOrderBySortOrderAscLabelAsc(String listCode);

    List<ReferenceValue> findByListCodeInOrderByListCodeAscSortOrderAscLabelAsc(Collection<String> listCodes);

    List<ReferenceValue> findAllByOrderByListCodeAscSortOrderAscLabelAsc();

    Optional<ReferenceValue> findByListCodeAndCode(String listCode, String code);
}
