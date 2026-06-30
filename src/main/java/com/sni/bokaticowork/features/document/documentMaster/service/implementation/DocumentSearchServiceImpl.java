package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentAnalyticsResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentDashboardResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentReviewStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentReview;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentTag;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentReviewRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTagAssignmentRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentSearchService;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentSearchServiceImpl implements DocumentSearchService {

    private final DocumentRepository documentRepository;
    private final DocumentReviewRepository reviewRepository;
    private final DocumentRequirementRepository requirementRepository;
    private final DocumentTagAssignmentRepository tagAssignmentRepository;
    private final DocumentService documentService;
    private final CustomerService customerService;
    private final MemberService memberService;
    private final BusinessService businessService;

    // ── Faceted search ───────────────────────────────────────────────────────

    @Override
    public PaginatedResponse<DocumentResponse> search(
            String q, DocumentSpace space, DocumentCategory category,
            List<String> tags, DocumentOwnerType ownerType, String ownerCode,
            List<DocumentStatus> statuses, Integer expiresInDays, Boolean isExpired,
            LocalDate uploadedAfter, LocalDate uploadedBefore,
            String metaKey, String metaValue, Pageable pageable) {

        Specification<Document> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(q)) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }
        if (space != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("space"), space));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (ownerType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerType"), ownerType));
        }
        if (ownerType != null && StringUtils.hasText(ownerCode)) {
            Long ownerId = resolveOwnerId(ownerType, ownerCode);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerId"), ownerId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }
        if (Boolean.TRUE.equals(isExpired)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), DocumentStatus.EXPIRED));
        }
        if (expiresInDays != null && expiresInDays > 0) {
            LocalDate today = LocalDate.now();
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("expiryDate"), today),
                    cb.lessThanOrEqualTo(root.get("expiryDate"), today.plusDays(expiresInDays))
            ));
        }
        if (uploadedAfter != null) {
            Instant cutoff = uploadedAfter.atStartOfDay().toInstant(ZoneOffset.UTC);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("uploadedAt"), cutoff));
        }
        if (uploadedBefore != null) {
            Instant cutoff = uploadedBefore.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("uploadedAt"), cutoff));
        }
        if (tags != null && !tags.isEmpty()) {
            List<String> normalized = tags.stream().map(String::toLowerCase).toList();
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                var tagAssignRoot = sub.from(
                        com.sni.bokaticowork.features.document.documentMaster.model.DocumentTagAssignment.class);
                var tagJoin = tagAssignRoot.join("tag", JoinType.INNER);
                sub.select(tagAssignRoot.get("document").get("id"))
                        .where(cb.and(
                                cb.equal(tagAssignRoot.get("document").get("id"), root.get("id")),
                                tagJoin.get("code").in(normalized)
                        ));
                return cb.exists(sub);
            });
        }
        if (StringUtils.hasText(metaKey)) {
            final String key = metaKey.trim();
            final String val = metaValue;
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                var metaRoot = sub.from(
                        com.sni.bokaticowork.features.document.documentMaster.model.DocumentMetadata.class);
                sub.select(metaRoot.get("document").get("id"))
                        .where(cb.and(
                                cb.equal(metaRoot.get("document").get("id"), root.get("id")),
                                cb.equal(metaRoot.get("metaKey"), key),
                                StringUtils.hasText(val)
                                        ? cb.equal(metaRoot.get("metaValue"), val.trim())
                                        : cb.conjunction()
                        ));
                return cb.exists(sub);
            });
        }

        Page<Document> page = documentRepository.findAll(spec, pageable);
        return new PaginatedResponse<>(page.map(doc -> documentService.getByCode(doc.getCode())));
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    @Override
    public DocumentDashboardResponse dashboard(DocumentSpace space) {
        Specification<Document> baseSpec = space != null
                ? (root, query, cb) -> cb.equal(root.get("space"), space)
                : (root, query, cb) -> cb.conjunction();

        long total = documentRepository.count(baseSpec);

        Map<DocumentStatus, Long> byStatus = new EnumMap<>(DocumentStatus.class);
        for (DocumentStatus s : DocumentStatus.values()) {
            long count = documentRepository.count(
                    baseSpec.and((root, q, cb) -> cb.equal(root.get("status"), s)));
            if (count > 0) byStatus.put(s, count);
        }

        Map<DocumentCategory, Long> byCategory = new EnumMap<>(DocumentCategory.class);
        for (DocumentCategory c : DocumentCategory.values()) {
            long count = documentRepository.count(
                    baseSpec.and((root, q, cb) -> cb.equal(root.get("category"), c)));
            if (count > 0) byCategory.put(c, count);
        }

        Instant threshold48h = Instant.now().minus(48, ChronoUnit.HOURS);
        long pendingOld = documentRepository.count(
                baseSpec
                        .and((root, q, cb) -> cb.equal(root.get("status"), DocumentStatus.PENDING_REVIEW))
                        .and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("updatedAt"), threshold48h)));

        long needsCorrection = documentRepository.count(
                baseSpec.and((root, q, cb) -> cb.equal(root.get("status"), DocumentStatus.NEEDS_CORRECTION)));

        LocalDate today = LocalDate.now();
        long expiring30d = documentRepository.count(
                baseSpec
                        .and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("expiryDate"), today))
                        .and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("expiryDate"), today.plusDays(30))));

        Instant since30d = Instant.now().minus(30, ChronoUnit.DAYS);
        List<DocumentReview> recentReviews = reviewRepository.findAllByReviewStatusInAndReviewedAtAfter(
                List.of(DocumentReviewStatus.APPROVED, DocumentReviewStatus.REJECTED, DocumentReviewStatus.NEEDS_CORRECTION),
                since30d);
        long recentTotal = recentReviews.size();
        long recentRejected = recentReviews.stream()
                .filter(r -> r.getReviewStatus() == DocumentReviewStatus.REJECTED).count();
        BigDecimal rejectionRate = recentTotal == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(recentRejected * 100.0 / recentTotal).setScale(1, RoundingMode.HALF_UP);

        double avgReviewHours = recentReviews.stream()
                .filter(r -> r.getDocument() != null
                        && r.getDocument().getUploadedAt() != null
                        && r.getReviewedAt() != null)
                .mapToLong(r -> ChronoUnit.HOURS.between(r.getDocument().getUploadedAt(), r.getReviewedAt()))
                .filter(h -> h >= 0)
                .average()
                .orElse(0.0);

        return DocumentDashboardResponse.builder()
                .space(space)
                .total(total)
                .byStatus(byStatus)
                .byCategory(byCategory)
                .pendingReviewOlderThan48h(pendingOld)
                .needsCorrectionCount(needsCorrection)
                .expiringIn30Days(expiring30d)
                .rejectionRate30d(rejectionRate)
                .avgReviewTimeHours(Math.round(avgReviewHours * 10.0) / 10.0)
                .topTags(buildTopTags(space))
                .build();
    }

    private List<DocumentDashboardResponse.TagCount> buildTopTags(DocumentSpace space) {
        return tagAssignmentRepository.findAll().stream()
                .filter(a -> space == null
                        || a.getTag().getSpace() == null
                        || a.getTag().getSpace() == space)
                .collect(Collectors.groupingBy(a -> a.getTag(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<DocumentTag, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> DocumentDashboardResponse.TagCount.builder()
                        .tagCode(e.getKey().getCode())
                        .tagLabel(e.getKey().getLabel())
                        .count(e.getValue())
                        .build())
                .toList();
    }

    // ── Folder view ──────────────────────────────────────────────────────────

    @Override
    public DocumentFolderResponse folder(DocumentOwnerType ownerType, String ownerCode) {
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            throw new BadRequestException("ownerType and ownerCode are required");
        }
        OwnerResolution owner = resolveOwner(ownerType, ownerCode);

        List<Document> allDocs = documentRepository.findAllByOwnerTypeAndOwnerId(ownerType, owner.ownerId());

        Map<DocumentSpace, List<DocumentResponse>> bySpace = new LinkedHashMap<>();
        for (DocumentSpace s : DocumentSpace.values()) bySpace.put(s, new ArrayList<>());
        allDocs.forEach(doc -> bySpace
                .computeIfAbsent(doc.getSpace() != null ? doc.getSpace() : DocumentSpace.GENERIC, k -> new ArrayList<>())
                .add(documentService.getByCode(doc.getCode())));

        List<DocumentFolderResponse.SpaceFolder> spaces = Arrays.stream(DocumentSpace.values())
                .filter(s -> !bySpace.get(s).isEmpty())
                .map(s -> {
                    List<DocumentResponse> docs = bySpace.get(s);
                    long approved = docs.stream()
                            .filter(d -> d.getStatus() == DocumentStatus.APPROVED || d.getStatus() == DocumentStatus.SIGNED)
                            .count();
                    double rate = Math.round(approved * 100.0 / docs.size() * 10) / 10.0;
                    return DocumentFolderResponse.SpaceFolder.builder()
                            .space(s)
                            .totalDocuments(docs.size())
                            .approvedDocuments(approved)
                            .completionRate(rate)
                            .documents(docs)
                            .build();
                })
                .toList();

        List<KycRequirementStatus> missing = requirementRepository
                .findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(ownerType)
                .stream()
                .filter(req -> Boolean.TRUE.equals(req.getRequired()))
                .filter(req -> allDocs.stream().noneMatch(d ->
                        req.getDocumentTypeCode().equals(d.getTypeCode())
                                && d.getStatus() != DocumentStatus.REJECTED
                                && d.getStatus() != DocumentStatus.EXPIRED
                                && d.getStatus() != DocumentStatus.ARCHIVED
                                && d.getStatus() != DocumentStatus.SUPERSEDED))
                .map(req -> KycRequirementStatus.builder()
                        .documentTypeCode(req.getDocumentTypeCode())
                        .documentTypeName(req.getDocumentTypeName())
                        .required(true)
                        .status(null)
                        .documentCode(null)
                        .build())
                .toList();

        LocalDate today = LocalDate.now();
        List<DocumentResponse> expiringSoon = allDocs.stream()
                .filter(d -> d.getExpiryDate() != null
                        && !d.getExpiryDate().isBefore(today)
                        && !d.getExpiryDate().isAfter(today.plusDays(30)))
                .sorted(Comparator.comparing(Document::getExpiryDate))
                .map(d -> documentService.getByCode(d.getCode()))
                .toList();

        return DocumentFolderResponse.builder()
                .owner(DocumentFolderResponse.OwnerInfo.builder()
                        .type(ownerType).code(ownerCode).name(owner.displayName())
                        .build())
                .spaces(spaces)
                .missingRequirements(missing)
                .expiringSoon(expiringSoon)
                .build();
    }

    // ── Analytics ────────────────────────────────────────────────────────────

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MAX_CSV_ROWS = 10_000;

    @Override
    public DocumentAnalyticsResponse analytics(DocumentSpace space, int months) {
        int m = Math.max(1, Math.min(24, months));
        Instant since = Instant.now().minus((long) m * 30, ChronoUnit.DAYS);

        Specification<Document> baseSpec = spaceSpec(space);

        long total        = documentRepository.count(baseSpec);
        long pending      = documentRepository.count(statusSpec(baseSpec, DocumentStatus.PENDING_REVIEW));
        long needsCorr    = documentRepository.count(statusSpec(baseSpec, DocumentStatus.NEEDS_CORRECTION));
        long expired      = documentRepository.count(statusSpec(baseSpec, DocumentStatus.EXPIRED));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DocumentStatus s : DocumentStatus.values()) {
            long c = documentRepository.count(statusSpec(baseSpec, s));
            if (c > 0) byStatus.put(s.name(), c);
        }
        Map<String, Long> bySpaceMap = new LinkedHashMap<>();
        for (DocumentSpace sp : DocumentSpace.values()) {
            long c = documentRepository.count((root, q, cb) -> cb.equal(root.get("space"), sp));
            if (c > 0) bySpaceMap.put(sp.name(), c);
        }
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (DocumentCategory c : DocumentCategory.values()) {
            long cnt = documentRepository.count(
                    baseSpec.and((root, q, cb) -> cb.equal(root.get("category"), c)));
            if (cnt > 0) byCategory.put(c.name(), cnt);
        }

        Specification<Document> periodSpec = baseSpec.and(
                (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("uploadedAt"), since));
        List<Document> periodDocs = documentRepository.findAll(periodSpec);

        List<DocumentAnalyticsResponse.PeriodCount> uploadsByMonth = monthCounts(
                periodDocs.stream().map(Document::getUploadedAt).filter(Objects::nonNull).toList());

        List<DocumentReview> allReviews = reviewRepository.findAllByReviewStatusInAndReviewedAtAfter(
                List.of(DocumentReviewStatus.APPROVED, DocumentReviewStatus.REJECTED, DocumentReviewStatus.NEEDS_CORRECTION),
                since);
        if (space != null) {
            allReviews = allReviews.stream()
                    .filter(r -> r.getDocument() != null && r.getDocument().getSpace() == space)
                    .toList();
        }

        List<DocumentAnalyticsResponse.PeriodCount> approvalsByMonth = monthCounts(filterReviews(allReviews, DocumentReviewStatus.APPROVED));
        List<DocumentAnalyticsResponse.PeriodCount> rejectionsByMonth = monthCounts(filterReviews(allReviews, DocumentReviewStatus.REJECTED));
        List<DocumentAnalyticsResponse.PeriodCount> correctionsByMonth = monthCounts(filterReviews(allReviews, DocumentReviewStatus.NEEDS_CORRECTION));

        Instant since30d = Instant.now().minus(30, ChronoUnit.DAYS);
        List<DocumentReview> reviews30d = reviewRepository.findAllByReviewStatusInAndReviewedAtAfter(
                List.of(DocumentReviewStatus.APPROVED, DocumentReviewStatus.REJECTED, DocumentReviewStatus.NEEDS_CORRECTION),
                since30d);
        if (space != null) {
            reviews30d = reviews30d.stream()
                    .filter(r -> r.getDocument() != null && r.getDocument().getSpace() == space)
                    .toList();
        }
        long total30d = reviews30d.size();
        long rejected30d = reviews30d.stream().filter(r -> r.getReviewStatus() == DocumentReviewStatus.REJECTED).count();
        long corrections30d = reviews30d.stream().filter(r -> r.getReviewStatus() == DocumentReviewStatus.NEEDS_CORRECTION).count();
        double avgHours = reviews30d.stream()
                .filter(r -> r.getDocument() != null && r.getDocument().getUploadedAt() != null && r.getReviewedAt() != null)
                .mapToLong(r -> ChronoUnit.HOURS.between(r.getDocument().getUploadedAt(), r.getReviewedAt()))
                .filter(h -> h >= 0).average().orElse(0.0);

        return DocumentAnalyticsResponse.builder()
                .space(space).periodMonths(m)
                .totalDocuments(total).pendingReview(pending).needsCorrection(needsCorr).expiredDocuments(expired)
                .byStatus(byStatus).bySpace(bySpaceMap).byCategory(byCategory)
                .correctionRate30d(total30d == 0 ? 0 : round1(corrections30d * 100.0 / total30d))
                .rejectionRate30d(total30d == 0 ? 0 : round1(rejected30d * 100.0 / total30d))
                .avgReviewTimeHours(round1(avgHours))
                .uploadsByMonth(uploadsByMonth).approvalsByMonth(approvalsByMonth)
                .rejectionsByMonth(rejectionsByMonth).correctionsByMonth(correctionsByMonth)
                .build();
    }

    // ── CSV export ────────────────────────────────────────────────────────────

    @Override
    public byte[] exportCsv(DocumentSpace space, DocumentCategory category,
                            List<String> tags, DocumentOwnerType ownerType, String ownerCode,
                            List<DocumentStatus> statuses,
                            LocalDate uploadedAfter, LocalDate uploadedBefore) {

        Specification<Document> spec = (root, q, cb) -> cb.conjunction();
        if (space != null)    spec = spec.and((root, q, cb) -> cb.equal(root.get("space"), space));
        if (category != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("category"), category));
        if (ownerType != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("ownerType"), ownerType));
        if (ownerType != null && StringUtils.hasText(ownerCode)) {
            Long ownerId = resolveOwnerId(ownerType, ownerCode);
            spec = spec.and((root, q, cb) -> cb.equal(root.get("ownerId"), ownerId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("status").in(statuses));
        }
        if (uploadedAfter != null) {
            Instant cut = uploadedAfter.atStartOfDay().toInstant(ZoneOffset.UTC);
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("uploadedAt"), cut));
        }
        if (uploadedBefore != null) {
            Instant cut = uploadedBefore.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("uploadedAt"), cut));
        }
        if (tags != null && !tags.isEmpty()) {
            List<String> normalized = tags.stream().map(String::toLowerCase).toList();
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                var ta = sub.from(com.sni.bokaticowork.features.document.documentMaster.model.DocumentTagAssignment.class);
                var tj = ta.join("tag", JoinType.INNER);
                sub.select(ta.get("document").get("id")).where(cb.and(
                        cb.equal(ta.get("document").get("id"), root.get("id")),
                        tj.get("code").in(normalized)));
                return cb.exists(sub);
            });
        }

        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by("uploadedAt").descending();
        List<Document> docs = documentRepository.findAll(spec, sort);
        if (docs.size() > MAX_CSV_ROWS) docs = docs.subList(0, MAX_CSV_ROWS);
        return buildCsv(docs);
    }

    private byte[] buildCsv(List<Document> docs) {
        StringBuilder sb = new StringBuilder();
        sb.append("code,title,ownerType,ownerId,space,category,documentTypeCode,status,uploadedAt,expiryDate\n");
        for (Document d : docs) {
            sb.append(esc(d.getCode())).append(',')
              .append(esc(d.getTitle())).append(',')
              .append(esc(d.getOwnerType() != null ? d.getOwnerType().name() : "")).append(',')
              .append(d.getOwnerId() != null ? d.getOwnerId() : "").append(',')
              .append(esc(d.getSpace() != null ? d.getSpace().name() : "")).append(',')
              .append(esc(d.getCategory() != null ? d.getCategory().name() : "")).append(',')
              .append(esc(d.getDocumentType() != null ? d.getDocumentType().getCode() : "")).append(',')
              .append(esc(d.getStatus() != null ? d.getStatus().name() : "")).append(',')
              .append(d.getUploadedAt() != null ? d.getUploadedAt() : "").append(',')
              .append(d.getExpiryDate() != null ? d.getExpiryDate() : "").append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String esc(String v) {
        if (v == null || v.isBlank()) return "";
        String s = v.trim();
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Specification<Document> spaceSpec(DocumentSpace space) {
        if (space == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("space"), space);
    }

    private Specification<Document> statusSpec(Specification<Document> base, DocumentStatus status) {
        return base.and((root, q, cb) -> cb.equal(root.get("status"), status));
    }

    private List<Instant> filterReviews(List<DocumentReview> reviews, DocumentReviewStatus status) {
        return reviews.stream()
                .filter(r -> r.getReviewStatus() == status)
                .map(DocumentReview::getReviewedAt)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<DocumentAnalyticsResponse.PeriodCount> monthCounts(List<Instant> instants) {
        return instants.stream()
                .collect(Collectors.groupingBy(
                        i -> ZonedDateTime.ofInstant(i, ZoneOffset.UTC).format(MONTH_FMT),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> DocumentAnalyticsResponse.PeriodCount.builder()
                        .period(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    private double round1(double value) { return Math.round(value * 10.0) / 10.0; }

    // ── Owner resolution ─────────────────────────────────────────────────────

    private Long resolveOwnerId(DocumentOwnerType ownerType, String ownerCode) {
        return resolveOwner(ownerType, ownerCode).ownerId();
    }

    private OwnerResolution resolveOwner(DocumentOwnerType ownerType, String ownerCode) {
        return switch (ownerType) {
            case MEMBER -> {
                Member m = memberService.getByMemberIdForService(ownerCode.trim());
                yield new OwnerResolution(m.getId(), m.getDisplayName());
            }
            case CUSTOMER -> {
                Customer c = customerService.getCustomerForService(ownerCode.trim());
                String name = StringUtils.hasText(c.getCompanyName())
                        ? c.getCompanyName()
                        : (c.getFirstname() + " " + c.getLastname()).trim();
                yield new OwnerResolution(c.getId(), name);
            }
            case BUSINESS -> {
                BusinessEntity b = businessService.serviceBusinessByCode(ownerCode.trim());
                yield new OwnerResolution(b.getId(), b.getName());
            }
            default -> {
                try {
                    yield new OwnerResolution(Long.parseLong(ownerCode.trim()), ownerCode);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Owner code must be numeric for owner type " + ownerType);
                }
            }
        };
    }

    private record OwnerResolution(Long ownerId, String displayName) {}
}
