package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

    Optional<DocumentFolder> findByCode(String code);

    List<DocumentFolder> findAllByParentAndDeletedFalseOrderBySortOrderAscNameAsc(DocumentFolder parent);

    List<DocumentFolder> findAllByParentIsNullAndSpaceAndDeletedFalseOrderBySortOrderAscNameAsc(DocumentSpace space);

    List<DocumentFolder> findAllByParentIsNullAndDeletedFalseOrderBySortOrderAscNameAsc();

    @Query("SELECT f FROM DocumentFolder f WHERE f.path LIKE :pathPrefix% AND f.deleted = false ORDER BY f.path, f.sortOrder")
    List<DocumentFolder> findAllByPathStartingWith(@Param("pathPrefix") String pathPrefix);

    List<DocumentFolder> findAllByOwnerTypeAndOwnerIdAndDeletedFalseOrderByNameAsc(DocumentOwnerType ownerType, Long ownerId);

    long countByParentAndDeletedFalse(DocumentFolder parent);

    boolean existsByParentAndNameAndDeletedFalse(DocumentFolder parent, String name);

    @Modifying
    @Query("UPDATE DocumentFolder f SET f.path = CONCAT(:newPrefix, SUBSTRING(f.path, LENGTH(:oldPrefix) + 1)), " +
            "f.depth = f.depth + :depthDelta WHERE f.path LIKE :oldPrefix% AND f.deleted = false")
    int updatePathPrefix(@Param("oldPrefix") String oldPrefix,
                         @Param("newPrefix") String newPrefix,
                         @Param("depthDelta") int depthDelta);
}
