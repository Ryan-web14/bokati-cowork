package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {
    List<DocumentPermission> findAllByTargetTypeAndTargetIdOrderByPermissionAsc(String targetType, Long targetId);
    List<DocumentPermission> findAllByGranteeTypeAndGranteeId(String granteeType, Long granteeId);
    Optional<DocumentPermission> findByTargetTypeAndTargetIdAndGranteeTypeAndGranteeIdAndPermission(
            String targetType, Long targetId, String granteeType, Long granteeId, String permission);
    boolean existsByTargetTypeAndTargetIdAndGranteeTypeAndGranteeIdAndPermission(
            String targetType, Long targetId, String granteeType, Long granteeId, String permission);
    void deleteByTargetTypeAndTargetIdAndGranteeTypeAndGranteeIdAndPermission(
            String targetType, Long targetId, String granteeType, Long granteeId, String permission);
}
