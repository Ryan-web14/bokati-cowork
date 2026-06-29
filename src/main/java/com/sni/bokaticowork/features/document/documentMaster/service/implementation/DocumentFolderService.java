package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.CreateFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.MoveFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.UpdateFolderRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.BreadcrumbItem;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderTreeNode;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.FolderDetailResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentFolder;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentFolderRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentFolderService {

    private static final int MAX_DEPTH = 10;

    private final DocumentFolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    public FolderDetailResponse create(CreateFolderRequest request) {
        DocumentFolder parent = null;
        DocumentSpace space = request.getSpace() != null ? request.getSpace() : DocumentSpace.GENERIC;
        String path = "/";
        int depth = 0;

        if (StringUtils.hasText(request.getParentCode())) {
            parent = findByCode(request.getParentCode());
            space = parent.getSpace();
            depth = parent.getDepth() + 1;
            if (depth > MAX_DEPTH) {
                throw new BadRequestException("Maximum folder depth (" + MAX_DEPTH + ") exceeded");
            }
        }

        if (folderRepository.existsByParentAndNameAndDeletedFalse(parent, request.getName().trim())) {
            throw new BadRequestException("A folder with this name already exists in the same parent");
        }

        Long ownerId = resolveOwnerId(request.getOwnerType(), request.getOwnerCode());

        DocumentFolder folder = DocumentFolder.builder()
                .code(sequenceGenerator.next("FOLDER", LocalDate.now()))
                .name(request.getName().trim())
                .description(request.getDescription())
                .parent(parent)
                .space(space)
                .ownerType(request.getOwnerType())
                .ownerId(ownerId)
                .depth(depth)
                .sortOrder(0)
                .color(request.getColor())
                .icon(request.getIcon())
                .createdBy(currentUserId())
                .build();
        folderRepository.save(folder);

        folder.setPath(buildPath(folder));
        folderRepository.save(folder);

        return toResponse(folder);
    }

    @Transactional(readOnly = true)
    public FolderDetailResponse getByCode(String code) {
        return toResponse(findByCode(code));
    }

    public FolderDetailResponse update(String code, UpdateFolderRequest request) {
        DocumentFolder folder = findByCode(code);

        if (StringUtils.hasText(request.getName())) {
            String newName = request.getName().trim();
            if (!newName.equals(folder.getName())
                    && folderRepository.existsByParentAndNameAndDeletedFalse(folder.getParent(), newName)) {
                throw new BadRequestException("A folder with this name already exists in the same parent");
            }
            folder.setName(newName);
        }
        if (request.getDescription() != null) folder.setDescription(request.getDescription());
        if (request.getColor() != null) folder.setColor(request.getColor());
        if (request.getIcon() != null) folder.setIcon(request.getIcon());
        if (request.getSortOrder() != null) folder.setSortOrder(request.getSortOrder());

        folderRepository.save(folder);
        return toResponse(folder);
    }

    public void delete(String code) {
        DocumentFolder folder = findByCode(code);
        long children = folderRepository.countByParentAndDeletedFalse(folder);
        if (children > 0) {
            throw new BadRequestException("Cannot delete a folder that contains sub-folders. Move or delete them first.");
        }
        long docs = documentRepository.countByFolderAndDeletedFalse(folder);
        if (docs > 0) {
            throw new BadRequestException("Cannot delete a folder that contains documents. Move or delete them first.");
        }
        folderRepository.delete(folder);
    }

    public FolderDetailResponse move(String code, MoveFolderRequest request) {
        DocumentFolder folder = findByCode(code);
        String oldPath = folder.getPath();
        int oldDepth = folder.getDepth();

        DocumentFolder newParent = null;
        int newDepth = 0;

        if (StringUtils.hasText(request.getTargetParentCode())) {
            newParent = findByCode(request.getTargetParentCode());
            newDepth = newParent.getDepth() + 1;

            if (newParent.getPath().startsWith(folder.getPath())) {
                throw new BadRequestException("Cannot move a folder into one of its descendants");
            }
            if (newDepth > MAX_DEPTH) {
                throw new BadRequestException("Maximum folder depth (" + MAX_DEPTH + ") exceeded");
            }
        }

        folder.setParent(newParent);
        folder.setDepth(newDepth);
        folder.setPath(buildPath(folder));
        if (newParent != null) {
            folder.setSpace(newParent.getSpace());
        }
        folderRepository.save(folder);

        String newPath = folder.getPath();
        int depthDelta = newDepth - oldDepth;
        folderRepository.updatePathPrefix(oldPath, newPath, depthDelta);

        return toResponse(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderDetailResponse> getRoots(DocumentSpace space) {
        List<DocumentFolder> roots = space != null
                ? folderRepository.findAllByParentIsNullAndSpaceAndDeletedFalseOrderBySortOrderAscNameAsc(space)
                : folderRepository.findAllByParentIsNullAndDeletedFalseOrderBySortOrderAscNameAsc();
        return roots.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FolderDetailResponse> getChildren(String code) {
        DocumentFolder parent = findByCode(code);
        return folderRepository.findAllByParentAndDeletedFalseOrderBySortOrderAscNameAsc(parent)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentFolderTreeNode getTree(String code) {
        DocumentFolder root = findByCode(code);
        List<DocumentFolder> descendants = folderRepository.findAllByPathStartingWith(root.getPath());
        return buildTreeNode(root, descendants);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<DocumentResponse> getDocuments(String code, Pageable pageable) {
        DocumentFolder folder = findByCode(code);
        Page<Document> page = documentRepository.findAllByFolderAndDeletedFalse(folder, pageable);
        DocumentService docService = null; // We map manually to avoid circular deps
        return new PaginatedResponse<>(page.map(this::toDocumentResponse));
    }

    @Transactional(readOnly = true)
    public List<BreadcrumbItem> getBreadcrumb(String code) {
        DocumentFolder folder = findByCode(code);
        List<BreadcrumbItem> crumbs = new ArrayList<>();
        DocumentFolder current = folder;
        while (current != null) {
            crumbs.addFirst(BreadcrumbItem.builder()
                    .code(current.getCode())
                    .name(current.getName())
                    .depth(current.getDepth())
                    .build());
            current = current.getParent();
        }
        return crumbs;
    }

    public DocumentResponse moveDocument(String documentCode, String folderCode) {
        Document document = documentRepository.findByCode(documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentCode));

        if (StringUtils.hasText(folderCode)) {
            DocumentFolder folder = findByCode(folderCode);
            document.setFolder(folder);
        } else {
            document.setFolder(null);
        }
        documentRepository.save(document);
        return toDocumentResponse(document);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private DocumentFolder findByCode(String code) {
        return folderRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found: " + code));
    }

    private String buildPath(DocumentFolder folder) {
        if (folder.getParent() == null) {
            return "/" + folder.getId() + "/";
        }
        return folder.getParent().getPath() + folder.getId() + "/";
    }

    private Long resolveOwnerId(DocumentOwnerType ownerType, String ownerCode) {
        if (ownerType == null || !StringUtils.hasText(ownerCode)) return null;
        return switch (ownerType) {
            case MEMBER -> memberRepository.findByMemberIdAndDeletedFalse(ownerCode.trim()).map(m -> m.getId()).orElse(null);
            case CUSTOMER -> customerRepository.findByCustomerId(ownerCode.trim()).map(Customer::getId).orElse(null);
            case BUSINESS -> businessRepository.findByCode(ownerCode.trim()).map(b -> b.getId()).orElse(null);
            default -> null;
        };
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.sni.bokaticowork.security.admin.user.model.UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return null;
    }

    private DocumentFolderTreeNode buildTreeNode(DocumentFolder folder, List<DocumentFolder> all) {
        Map<Long, List<DocumentFolder>> byParentId = all.stream()
                .filter(f -> f.getParent() != null)
                .collect(Collectors.groupingBy(f -> f.getParent().getId()));

        return buildNodeRecursive(folder, byParentId);
    }

    private DocumentFolderTreeNode buildNodeRecursive(DocumentFolder folder, Map<Long, List<DocumentFolder>> byParentId) {
        List<DocumentFolder> children = byParentId.getOrDefault(folder.getId(), List.of());
        return DocumentFolderTreeNode.builder()
                .code(folder.getCode())
                .name(folder.getName())
                .icon(folder.getIcon())
                .color(folder.getColor())
                .depth(folder.getDepth())
                .documentsCount(documentRepository.countByFolderAndDeletedFalse(folder))
                .children(children.stream()
                        .map(child -> buildNodeRecursive(child, byParentId))
                        .toList())
                .build();
    }

    private FolderDetailResponse toResponse(DocumentFolder folder) {
        return FolderDetailResponse.builder()
                .code(folder.getCode())
                .name(folder.getName())
                .description(folder.getDescription())
                .parentCode(folder.getParent() != null ? folder.getParent().getCode() : null)
                .space(folder.getSpace())
                .ownerType(folder.getOwnerType())
                .ownerId(folder.getOwnerId())
                .ownerName(resolveOwnerName(folder.getOwnerType(), folder.getOwnerId()))
                .path(folder.getPath())
                .depth(folder.getDepth())
                .sortOrder(folder.getSortOrder())
                .color(folder.getColor())
                .icon(folder.getIcon())
                .childrenCount(folderRepository.countByParentAndDeletedFalse(folder))
                .documentsCount(documentRepository.countByFolderAndDeletedFalse(folder))
                .createdBy(folder.getCreatedBy())
                .createdByEmail(resolveUserEmail(folder.getCreatedBy()))
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    private DocumentResponse toDocumentResponse(Document doc) {
        return DocumentResponse.builder()
                .code(doc.getCode())
                .ownerId(doc.getOwnerId())
                .ownerType(doc.getOwnerType())
                .category(doc.getCategory())
                .space(doc.getSpace())
                .title(doc.getTitle())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .status(doc.getStatus())
                .issueDate(doc.getIssueDate())
                .expiryDate(doc.getExpiryDate())
                .uploadedAt(doc.getUploadedAt())
                .updatedAt(doc.getUpdatedAt())
                .currentVersionNumber(doc.getCurrentVersionNumber())
                .build();
    }

    private String resolveOwnerName(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) return null;
        try {
            return switch (ownerType) {
                case MEMBER -> memberRepository.findById(ownerId).map(m -> m.getDisplayName()).orElse(null);
                case CUSTOMER -> customerRepository.findById(ownerId).map(c -> {
                    if (StringUtils.hasText(c.getCompanyName())) return c.getCompanyName();
                    return ((c.getFirstname() == null ? "" : c.getFirstname()) + " " +
                            (c.getLastname() == null ? "" : c.getLastname())).trim();
                }).orElse(null);
                case BUSINESS -> businessRepository.findById(ownerId).map(b -> b.getName()).orElse(null);
                default -> null;
            };
        } catch (Exception e) { return null; }
    }

    private String resolveUserEmail(Long userId) {
        if (userId == null) return null;
        try {
            return userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
        } catch (Exception e) { return null; }
    }
}
