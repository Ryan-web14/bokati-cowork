package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagAssignRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentTagUpdateRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentTagResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentTag;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentTagAssignment;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTagAssignmentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTagRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentTagService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentTagServiceImpl implements DocumentTagService {

    private final DocumentTagRepository tagRepository;
    private final DocumentTagAssignmentRepository assignmentRepository;
    private final DocumentRepository documentRepository;

    @Override
    public DocumentTagResponse create(DocumentTagRequest request) {
        String code = request.getCode().toLowerCase(Locale.ROOT).trim();
        if (tagRepository.existsByCode(code)) {
            throw new BadRequestException("Tag with code '" + code + "' already exists");
        }
        DocumentTag tag = DocumentTag.builder()
                .code(code)
                .label(request.getLabel().trim())
                .color(request.getColor())
                .space(request.getSpace())
                .createdBy(currentUserId())
                .build();
        return toResponse(tagRepository.save(tag));
    }

    @Override
    public DocumentTagResponse update(String code, DocumentTagUpdateRequest request) {
        DocumentTag tag = serviceTag(code);
        if (StringUtils.hasText(request.getLabel())) {
            tag.setLabel(request.getLabel().trim());
        }
        if (request.getColor() != null) {
            tag.setColor(StringUtils.hasText(request.getColor()) ? request.getColor() : null);
        }
        return toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentTagResponse getByCode(String code) {
        return toResponse(serviceTag(code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTagResponse> list(DocumentSpace space) {
        List<DocumentTag> tags = space != null
                ? tagRepository.findAllBySpaceOrSpaceIsNullOrderByLabelAsc(space)
                : tagRepository.findAllByOrderByLabelAsc();
        return tags.stream().map(this::toResponse).toList();
    }

    @Override
    public void delete(String code) {
        DocumentTag tag = serviceTag(code);
        if (assignmentRepository.existsByTag(tag)) {
            throw new BadRequestException("Tag '" + code + "' is in use and cannot be deleted");
        }
        tagRepository.delete(tag);
    }

    @Override
    public List<DocumentTagResponse> assignTags(String documentCode, DocumentTagAssignRequest request) {
        Document document = serviceDocument(documentCode);
        Long actorId = currentUserId();
        List<DocumentTag> tags = tagRepository.findAllByCodeIn(request.getTagCodes());
        if (tags.size() != request.getTagCodes().size()) {
            throw new BadRequestException("One or more tag codes not found");
        }
        for (DocumentTag tag : tags) {
            if (!assignmentRepository.existsByDocumentAndTag(document, tag)) {
                assignmentRepository.save(DocumentTagAssignment.builder()
                        .document(document)
                        .tag(tag)
                        .taggedBy(actorId)
                        .build());
            }
        }
        return getDocumentTags(documentCode);
    }

    @Override
    public void removeTag(String documentCode, String tagCode) {
        Document document = serviceDocument(documentCode);
        DocumentTag tag = serviceTag(tagCode);
        assignmentRepository.deleteByDocumentAndTag(document, tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTagResponse> getDocumentTags(String documentCode) {
        Document document = serviceDocument(documentCode);
        return assignmentRepository.findAllByDocument(document).stream()
                .map(a -> toResponse(a.getTag()))
                .toList();
    }

    private DocumentTag serviceTag(String code) {
        return tagRepository.findByCode(code.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + code));
    }

    private Document serviceDocument(String code) {
        return documentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + code));
    }

    private DocumentTagResponse toResponse(DocumentTag tag) {
        return DocumentTagResponse.builder()
                .id(tag.getId())
                .code(tag.getCode())
                .label(tag.getLabel())
                .color(tag.getColor())
                .space(tag.getSpace())
                .createdBy(tag.getCreatedBy())
                .createdAt(tag.getCreatedAt())
                .build();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return 0L;
    }
}
