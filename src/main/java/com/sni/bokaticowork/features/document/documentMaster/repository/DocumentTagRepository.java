package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentTagRepository extends JpaRepository<DocumentTag, Long> {

    Optional<DocumentTag> findByCode(String code);

    boolean existsByCode(String code);

    List<DocumentTag> findAllBySpaceOrSpaceIsNullOrderByLabelAsc(DocumentSpace space);

    List<DocumentTag> findAllByOrderByLabelAsc();

    List<DocumentTag> findAllByCodeIn(Collection<String> codes);
}
