package com.sni.bokaticowork.features.document.retention.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.retention.dto.request.DocumentRetentionPolicyRequest;
import com.sni.bokaticowork.features.document.retention.dto.response.DocumentRetentionPolicyResponse;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionAction;
import com.sni.bokaticowork.features.document.retention.enums.DocumentRetentionReference;
import com.sni.bokaticowork.features.document.retention.model.DocumentRetentionPolicy;
import com.sni.bokaticowork.features.document.retention.repository.DocumentRetentionPolicyRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.sni.bokaticowork.features.document.retention.service.interfaces.DocumentRetentionService;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentRetentionServiceImpl implements DocumentRetentionService {

    private static final List<DocumentStatus> RETAINABLE_STATUSES = List.of(
            DocumentStatus.APPROVED, DocumentStatus.SIGNED, DocumentStatus.EXPIRED);

    private final DocumentRetentionPolicyRepository policyRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @Override
    public DocumentRetentionPolicyResponse create(DocumentRetentionPolicyRequest request) {
        if (policyRepository.existsByCode(request.getCode().trim().toUpperCase())) {
            throw new BadRequestException("Retention policy code already exists: " + request.getCode());
        }
        DocumentRetentionPolicy policy = DocumentRetentionPolicy.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .space(request.getSpace())
                .documentTypeCode(trimToNull(request.getDocumentTypeCode()))
                .retentionDays(request.getRetentionDays())
                .retentionReference(request.getRetentionReference() != null ? request.getRetentionReference() : DocumentRetentionReference.UPLOAD_DATE)
                .action(request.getAction() != null ? request.getAction() : DocumentRetentionAction.ARCHIVE)
                .active(Boolean.TRUE)
                .createdBy(currentUserId())
                .build();
        return toResponse(policyRepository.save(policy));
    }

    @Override
    public DocumentRetentionPolicyResponse update(String code, DocumentRetentionPolicyRequest request) {
        DocumentRetentionPolicy policy = findByCode(code);
        policy.setName(request.getName().trim());
        policy.setSpace(request.getSpace());
        policy.setDocumentTypeCode(trimToNull(request.getDocumentTypeCode()));
        policy.setRetentionDays(request.getRetentionDays());
        policy.setRetentionReference(request.getRetentionReference() != null ? request.getRetentionReference() : DocumentRetentionReference.UPLOAD_DATE);
        policy.setAction(request.getAction() != null ? request.getAction() : DocumentRetentionAction.ARCHIVE);
        return toResponse(policyRepository.save(policy));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentRetentionPolicyResponse getByCode(String code) {
        return toResponse(findByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentRetentionPolicyResponse> listAll() {
        return policyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public void deactivate(String code) {
        DocumentRetentionPolicy policy = findByCode(code);
        policy.setActive(Boolean.FALSE);
        policyRepository.save(policy);
    }

    @Override
    public int applyPolicies() {
        List<DocumentRetentionPolicy> policies = policyRepository.findAllByActiveTrue();
        int total = 0;
        for (DocumentRetentionPolicy policy : policies) {
            try {
                total += applyPolicy(policy);
            } catch (Exception ex) {
                log.warn("Failed to apply retention policy {}: {}", policy.getCode(), ex.getMessage());
            }
        }
        return total;
    }

    private int applyPolicy(DocumentRetentionPolicy policy) {
        List<Document> candidates = documentRepository.findAll(buildSpec(policy));
        int count = 0;
        for (Document doc : candidates) {
            if (!hasReachedRetentionCutoff(doc, policy)) continue;
            try {
                if (policy.getAction() == DocumentRetentionAction.ARCHIVE) {
                    documentService.archive(doc.getCode(), "Archived by retention policy: " + policy.getCode());
                }
                count++;
            } catch (Exception ex) {
                log.warn("Retention policy {} failed for document {}: {}", policy.getCode(), doc.getCode(), ex.getMessage());
            }
        }
        return count;
    }

    private boolean hasReachedRetentionCutoff(Document doc, DocumentRetentionPolicy policy) {
        if (policy.getRetentionReference() == DocumentRetentionReference.EXPIRY_DATE) {
            if (doc.getExpiryDate() == null) return false;
            return !doc.getExpiryDate().plusDays(policy.getRetentionDays()).isAfter(LocalDate.now());
        }
        if (doc.getUploadedAt() == null) return false;
        Instant cutoff = Instant.now().minus(policy.getRetentionDays(), ChronoUnit.DAYS);
        return doc.getUploadedAt().isBefore(cutoff);
    }

    private Specification<Document> buildSpec(DocumentRetentionPolicy policy) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(RETAINABLE_STATUSES));
            if (policy.getSpace() != null) {
                predicates.add(cb.equal(root.get("space"), policy.getSpace()));
            }
            if (StringUtils.hasText(policy.getDocumentTypeCode())) {
                predicates.add(cb.equal(root.join("documentType").get("code"), policy.getDocumentTypeCode()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DocumentRetentionPolicy findByCode(String code) {
        return policyRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Retention policy not found: " + code));
    }

    private DocumentRetentionPolicyResponse toResponse(DocumentRetentionPolicy p) {
        return DocumentRetentionPolicyResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .space(p.getSpace())
                .documentTypeCode(p.getDocumentTypeCode())
                .retentionDays(p.getRetentionDays())
                .retentionReference(p.getRetentionReference())
                .action(p.getAction())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .createdBy(p.getCreatedBy())
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return 0L;
    }
}
