package com.sni.bokaticowork.core.reference.repository;

import com.sni.bokaticowork.core.reference.model.ReferenceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferenceListRepository extends JpaRepository<ReferenceList, Long> {

    Optional<ReferenceList> findByCode(String code);

    List<ReferenceList> findAllByOrderByModuleAscLabelAsc();

    List<ReferenceList> findByModuleOrderByLabelAsc(String module);
}
