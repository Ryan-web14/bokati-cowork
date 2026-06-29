package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.GrantPermissionRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentPermissionResponse;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentPermission;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentPermissionRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentPermissionService {

    private static final Set<String> VALID_PERMISSIONS = Set.of("READ", "DOWNLOAD", "EDIT", "REVIEW", "DELETE", "ADMIN");
    private static final Set<String> VALID_TARGET_TYPES = Set.of("DOCUMENT", "FOLDER");
    private static final Set<String> VALID_GRANTEE_TYPES = Set.of("USER", "ROLE");

    private final DocumentPermissionRepository permissionRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFolderRepository folderRepository;
    private final UserRepository userRepository;

    public DocumentPermissionResponse grant(GrantPermissionRequest request) {
        validate(request);
        Long targetId = resolveTargetId(request.getTargetType(), request.getTargetCode());

        if (permissionRepository.existsByTargetTypeAndTargetIdAndGranteeTypeAndGranteeIdAndPermission(
                request.getTargetType(), targetId, request.getGranteeType(), request.getGranteeId(), request.getPermission())) {
            throw new BadRequestException("Permission already granted");
        }

        DocumentPermission perm = DocumentPermission.builder()
                .targetType(request.getTargetType())
                .targetId(targetId)
                .granteeType(request.getGranteeType())
                .granteeId(request.getGranteeId())
                .permission(request.getPermission())
                .grantedBy(currentUserId())
                .build();
        permissionRepository.save(perm);
        return toResponse(perm, request.getTargetCode());
    }

    public void revoke(String targetType, String targetCode, String granteeType, Long granteeId, String permission) {
        Long targetId = resolveTargetId(targetType, targetCode);
        DocumentPermission perm = permissionRepository.findByTargetTypeAndTargetIdAndGranteeTypeAndGranteeIdAndPermission(
                        targetType, targetId, granteeType, granteeId, permission)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        permissionRepository.delete(perm);
    }

    @Transactional(readOnly = true)
    public List<DocumentPermissionResponse> listPermissions(String targetType, String targetCode) {
        Long targetId = resolveTargetId(targetType, targetCode);
        return permissionRepository.findAllByTargetTypeAndTargetIdOrderByPermissionAsc(targetType, targetId)
                .stream().map(p -> toResponse(p, targetCode)).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String targetType, Long targetId, String permission) {
        if (permissionRepository.existsByTargetTypeAndTargetIdAndGranteeTypeAndGranteeIdAndPermission(
                targetType, targetId, "USER", userId, permission)) return true;
        if (permissionRepository.existsByTargetTypeAndTargetIdAndGranteeTypeAndGranteeIdAndPermission(
                targetType, targetId, "USER", userId, "ADMIN")) return true;
        return false;
    }

    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(Long userId, String documentCode) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        Set<String> permissions = new HashSet<>();

        permissionRepository.findAllByTargetTypeAndTargetIdOrderByPermissionAsc("DOCUMENT", document.getId())
                .stream().filter(p -> "USER".equals(p.getGranteeType()) && p.getGranteeId().equals(userId))
                .forEach(p -> permissions.add(p.getPermission()));

        if (document.getFolder() != null) {
            addFolderPermissions(userId, document.getFolder(), permissions);
        }
        return permissions;
    }

    private void addFolderPermissions(Long userId, DocumentFolder folder, Set<String> permissions) {
        permissionRepository.findAllByTargetTypeAndTargetIdOrderByPermissionAsc("FOLDER", folder.getId())
                .stream().filter(p -> "USER".equals(p.getGranteeType()) && p.getGranteeId().equals(userId))
                .forEach(p -> permissions.add(p.getPermission()));
        if (folder.getParent() != null) {
            addFolderPermissions(userId, folder.getParent(), permissions);
        }
    }

    private void validate(GrantPermissionRequest request) {
        if (!VALID_TARGET_TYPES.contains(request.getTargetType()))
            throw new BadRequestException("Invalid target type. Allowed: " + VALID_TARGET_TYPES);
        if (!VALID_GRANTEE_TYPES.contains(request.getGranteeType()))
            throw new BadRequestException("Invalid grantee type. Allowed: " + VALID_GRANTEE_TYPES);
        if (!VALID_PERMISSIONS.contains(request.getPermission()))
            throw new BadRequestException("Invalid permission. Allowed: " + VALID_PERMISSIONS);
    }

    private Long resolveTargetId(String targetType, String targetCode) {
        return switch (targetType) {
            case "DOCUMENT" -> documentRepository.findByCode(targetCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + targetCode)).getId();
            case "FOLDER" -> folderRepository.findByCode(targetCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found: " + targetCode)).getId();
            default -> throw new BadRequestException("Unknown target type: " + targetType);
        };
    }

    private DocumentPermissionResponse toResponse(DocumentPermission perm, String targetCode) {
        return DocumentPermissionResponse.builder()
                .id(perm.getId()).targetType(perm.getTargetType()).targetId(perm.getTargetId())
                .targetCode(targetCode).granteeType(perm.getGranteeType()).granteeId(perm.getGranteeId())
                .granteeEmail("USER".equals(perm.getGranteeType())
                        ? userRepository.findById(perm.getGranteeId()).map(u -> u.getEmail()).orElse(null) : null)
                .permission(perm.getPermission()).grantedBy(perm.getGrantedBy())
                .grantedByEmail(perm.getGrantedBy() != null
                        ? userRepository.findById(perm.getGrantedBy()).map(u -> u.getEmail()).orElse(null) : null)
                .grantedAt(perm.getGrantedAt()).build();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) return p.getUser().getId();
        return null;
    }
}
