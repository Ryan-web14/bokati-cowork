package com.sni.bokaticowork.features.contract.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.contract.dto.request.ContractAmendmentSectionRequest;
import com.sni.bokaticowork.features.contract.dto.request.ContractAmendmentVariableRequest;
import com.sni.bokaticowork.features.contract.dto.request.ProposeAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.request.ReviewAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.request.SignAmendmentRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentSectionResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractAmendmentVariableResponse;
import com.sni.bokaticowork.features.contract.enums.AmendmentStatus;
import com.sni.bokaticowork.features.contract.enums.ContractAmendmentSectionAction;
import com.sni.bokaticowork.features.contract.enums.ContractSectionType;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractAmendment;
import com.sni.bokaticowork.features.contract.model.ContractAmendmentSection;
import com.sni.bokaticowork.features.contract.model.ContractAmendmentVariable;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentSectionRepository;
import com.sni.bokaticowork.features.contract.repository.ContractAmendmentVariableRepository;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAmendmentService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractAuditEventService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class ContractAmendmentServiceImpl implements ContractAmendmentService {

    private static final List<AmendmentStatus> OPEN_STATUSES = List.of(
            AmendmentStatus.DRAFT, AmendmentStatus.UNDER_REVIEW, AmendmentStatus.PENDING_SIGNATURE
    );

    private final ContractAmendmentRepository amendmentRepository;
    private final ContractAmendmentSectionRepository sectionRepository;
    private final ContractAmendmentVariableRepository variableRepository;
    private final ContractRepository contractRepository;
    private final ContractService contractService;
    private final ContractAuditEventService auditEventService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final DocumentService documentService;

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ContractAmendmentResponse propose(String contractCode, Long proposedBy, ProposeAmendmentRequest request) {
        Contract contract = contractService.serviceByCode(contractCode);
        if (contract.getStatus() != ContractStatus.ACTIVE
                && contract.getStatus() != ContractStatus.SUSPENDED
                && contract.getStatus() != ContractStatus.AMENDED) {
            throw new BadRequestException(
                    "Un avenant ne peut être proposé que sur un contrat ACTIVE, SUSPENDED ou AMENDED. Statut actuel : " + contract.getStatus());
        }
        List<ContractAmendment> open = amendmentRepository
                .findAllByOriginalContractCodeAndStatusIn(contractCode, OPEN_STATUSES);
        if (!open.isEmpty()) {
            throw new BadRequestException(
                    "Un avenant est déjà en cours de traitement pour ce contrat (statut : " + open.get(0).getStatus() + ")");
        }
        ContractAmendment amendment = amendmentRepository.save(ContractAmendment.builder()
                .code(sequenceGenerator.next("AMENDMENT", LocalDate.now()))
                .originalContract(contract)
                .originalContractCode(contractCode)
                .status(AmendmentStatus.DRAFT)
                .description(request.getDescription().trim())
                .proposedBy(proposedBy)
                .proposedAt(Instant.now())
                .effectiveDate(request.getEffectiveDate())
                .build());
        auditEventService.record(contractCode, amendment.getCode(), "AMENDMENT_PROPOSED",
                proposedBy, "USER", null,
                "Avenant proposé : " + request.getDescription(),
                "{\"amendmentCode\":\"" + amendment.getCode() + "\"}");
        return toResponse(amendment);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractAmendmentResponse getByCode(String amendmentCode) {
        return toResponse(findByCode(amendmentCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractAmendmentResponse> listByContract(String contractCode) {
        return amendmentRepository.findAllByOriginalContractCodeOrderByCreatedAtDesc(contractCode)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public ContractAmendmentResponse submitForReview(String amendmentCode) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireStatus(amendment, AmendmentStatus.DRAFT);
        requireAtLeastOneChange(amendment);
        amendment.setStatus(AmendmentStatus.UNDER_REVIEW);
        amendmentRepository.save(amendment);
        auditEventService.record(amendment.getOriginalContractCode(), amendmentCode,
                "AMENDMENT_SUBMITTED_FOR_REVIEW", null, "SYSTEM", null,
                "Avenant soumis pour examen", null);
        return toResponse(amendment);
    }

    @Override
    public ContractAmendmentResponse approveReview(String amendmentCode, Long reviewedBy, ReviewAmendmentRequest request) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireStatus(amendment, AmendmentStatus.UNDER_REVIEW);
        amendment.setStatus(AmendmentStatus.PENDING_SIGNATURE);
        amendment.setReviewedBy(reviewedBy);
        amendment.setReviewComment(request != null ? request.getComment() : null);
        amendmentRepository.save(amendment);
        auditEventService.record(amendment.getOriginalContractCode(), amendmentCode,
                "AMENDMENT_REVIEW_APPROVED", reviewedBy, "USER", null,
                request != null ? request.getComment() : null, null);
        return toResponse(amendment);
    }

    @Override
    public ContractAmendmentResponse rejectReview(String amendmentCode, Long reviewedBy, ReviewAmendmentRequest request) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireStatus(amendment, AmendmentStatus.UNDER_REVIEW);
        amendment.setStatus(AmendmentStatus.REJECTED);
        amendment.setReviewedBy(reviewedBy);
        amendment.setRejectionReason(request != null ? request.getComment() : null);
        amendmentRepository.save(amendment);
        auditEventService.record(amendment.getOriginalContractCode(), amendmentCode,
                "AMENDMENT_REVIEW_REJECTED", reviewedBy, "USER", null,
                request != null ? request.getComment() : null, null);
        return toResponse(amendment);
    }

    @Override
    public ContractAmendmentResponse sign(String amendmentCode, Long actorId, SignAmendmentRequest request) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireStatus(amendment, AmendmentStatus.PENDING_SIGNATURE);
        Instant now = Instant.now();
        if (request != null && StringUtils.hasText(request.getSignedDocumentCode())) {
            amendment.setSignedDocumentCode(request.getSignedDocumentCode());
        }
        amendment.setStatus(AmendmentStatus.ACTIVE);
        amendment.setSignedAt(now);
        amendment.setActivatedAt(now);
        amendmentRepository.save(amendment);

        Contract contract = contractService.serviceByCode(amendment.getOriginalContractCode());
        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.SUSPENDED) {
            throw new BadRequestException(
                    "Le contrat original doit être ACTIVE ou SUSPENDED pour appliquer un avenant. Statut : " + contract.getStatus());
        }
        contract.setStatus(ContractStatus.AMENDED);
        contractRepository.save(contract);

        String justification = request != null && StringUtils.hasText(request.getJustification())
                ? request.getJustification() : "Avenant signé et appliqué";
        auditEventService.record(amendment.getOriginalContractCode(), amendmentCode,
                "AMENDMENT_SIGNED", actorId, "USER", null, justification,
                "{\"amendmentCode\":\"" + amendmentCode + "\",\"signedAt\":\"" + now + "\"}");
        auditEventService.record(amendment.getOriginalContractCode(), null,
                "CONTRACT_AMENDED", actorId, "USER", null, justification,
                "{\"amendmentCode\":\"" + amendmentCode + "\",\"previousStatus\":\"ACTIVE\"}");
        return toResponse(amendment);
    }

    @Override
    public ContractAmendmentResponse cancel(String amendmentCode, Long actorId, String reason) {
        ContractAmendment amendment = findByCode(amendmentCode);
        if (amendment.getStatus() == AmendmentStatus.ACTIVE
                || amendment.getStatus() == AmendmentStatus.REJECTED
                || amendment.getStatus() == AmendmentStatus.CANCELLED) {
            throw new BadRequestException("Cet avenant ne peut plus être annulé (statut : " + amendment.getStatus() + ")");
        }
        amendment.setStatus(AmendmentStatus.CANCELLED);
        amendment.setCancellationReason(StringUtils.hasText(reason) ? reason.trim() : null);
        amendmentRepository.save(amendment);
        auditEventService.record(amendment.getOriginalContractCode(), amendmentCode,
                "AMENDMENT_CANCELLED", actorId, "USER", null, reason, null);
        return toResponse(amendment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section content management
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ContractAmendmentSectionResponse addSection(String amendmentCode, ContractAmendmentSectionRequest request) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireDraftStatus(amendment, "modifier les sections");
        validateSectionRequest(request);
        int order = request.getSectionOrder() != null ? request.getSectionOrder()
                : sectionRepository.findAllByAmendmentCodeOrderBySectionOrderAsc(amendmentCode).size();
        ContractAmendmentSection section = sectionRepository.save(ContractAmendmentSection.builder()
                .amendment(amendment)
                .amendmentCode(amendmentCode)
                .action(request.getAction())
                .targetSectionRef(request.getTargetSectionRef())
                .sectionType(request.getSectionType() != null ? request.getSectionType() : ContractSectionType.ARTICLE)
                .title(request.getTitle())
                .content(request.getContent())
                .sectionOrder(order)
                .build());
        return toSectionResponse(section);
    }

    @Override
    public ContractAmendmentSectionResponse updateSection(String amendmentCode, Long sectionId, ContractAmendmentSectionRequest request) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireDraftStatus(amendment, "modifier les sections");
        ContractAmendmentSection section = sectionRepository.findByIdAndAmendmentCode(sectionId, amendmentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Section introuvable pour cet avenant"));
        if (request.getAction() != null) section.setAction(request.getAction());
        if (request.getTargetSectionRef() != null) section.setTargetSectionRef(request.getTargetSectionRef());
        if (request.getSectionType() != null) section.setSectionType(request.getSectionType());
        if (request.getTitle() != null) section.setTitle(request.getTitle());
        if (request.getContent() != null) section.setContent(request.getContent());
        if (request.getSectionOrder() != null) section.setSectionOrder(request.getSectionOrder());
        sectionRepository.save(section);
        return toSectionResponse(section);
    }

    @Override
    public void removeSection(String amendmentCode, Long sectionId) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireDraftStatus(amendment, "supprimer une section");
        ContractAmendmentSection section = sectionRepository.findByIdAndAmendmentCode(sectionId, amendmentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Section introuvable pour cet avenant"));
        sectionRepository.delete(section);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractAmendmentSectionResponse> listSections(String amendmentCode) {
        return sectionRepository.findAllByAmendmentCodeOrderBySectionOrderAsc(amendmentCode)
                .stream().map(this::toSectionResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Variable management
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<ContractAmendmentVariableResponse> setVariables(String amendmentCode, List<ContractAmendmentVariableRequest> variables) {
        ContractAmendment amendment = findByCode(amendmentCode);
        requireDraftStatus(amendment, "modifier les variables");
        variableRepository.deleteAllByAmendmentCode(amendmentCode);
        if (variables == null || variables.isEmpty()) return List.of();
        variables.forEach(req -> variableRepository.save(ContractAmendmentVariable.builder()
                .amendment(amendment)
                .amendmentCode(amendmentCode)
                .variableKey(req.getVariableKey().trim())
                .previousValue(req.getPreviousValue())
                .newValue(req.getNewValue().trim())
                .build()));
        return variableRepository.findAllByAmendmentCodeOrderByVariableKeyAsc(amendmentCode)
                .stream().map(this::toVariableResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractAmendmentVariableResponse> listVariables(String amendmentCode) {
        return variableRepository.findAllByAmendmentCodeOrderByVariableKeyAsc(amendmentCode)
                .stream().map(this::toVariableResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Preview & PDF
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public String previewHtml(String amendmentCode) {
        ContractAmendment amendment = findByCode(amendmentCode);
        List<ContractAmendmentSection> sections =
                sectionRepository.findAllByAmendmentCodeOrderBySectionOrderAsc(amendmentCode);
        List<ContractAmendmentVariable> variables =
                variableRepository.findAllByAmendmentCodeOrderByVariableKeyAsc(amendmentCode);
        return buildAmendmentHtml(amendment, sections, variables);
    }

    @Override
    public DocumentResponse generatePdf(String amendmentCode, Long uploadedBy) {
        ContractAmendment amendment = findByCode(amendmentCode);
        List<ContractAmendmentSection> sections =
                sectionRepository.findAllByAmendmentCodeOrderBySectionOrderAsc(amendmentCode);
        List<ContractAmendmentVariable> variables =
                variableRepository.findAllByAmendmentCodeOrderByVariableKeyAsc(amendmentCode);
        String html = buildAmendmentHtml(amendment, sections, variables);
        byte[] pdfBytes = renderPdf(html);

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setDocumentTypeCode("CONTRACT_AMENDMENT");
        metadata.setTitle("Avenant " + amendmentCode);
        metadata.setDescription("Avenant au contrat " + amendment.getOriginalContractCode());
        metadata.setIssueDate(LocalDate.now());

        DocumentResponse doc = documentService.createGeneratedDocument(
                metadata,
                "avenant-" + amendmentCode.toLowerCase(Locale.ROOT) + ".pdf",
                "application/pdf",
                pdfBytes);

        amendment.setDraftDocumentCode(doc.getCode());
        amendmentRepository.save(amendment);
        return doc;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML + PDF rendering
    // ─────────────────────────────────────────────────────────────────────────

    private String buildAmendmentHtml(ContractAmendment amendment,
                                       List<ContractAmendmentSection> sections,
                                       List<ContractAmendmentVariable> variables) {
        String today = LocalDate.now().toString();
        String effectiveDate = amendment.getEffectiveDate() != null
                ? amendment.getEffectiveDate().toString() : today;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"/>");
        html.append("<title>Avenant ").append(esc(amendment.getCode())).append("</title>");
        html.append("<style>").append(AMENDMENT_CSS).append("</style></head><body>");

        // ── En-tête ──────────────────────────────────────────────────────────
        html.append("<div class=\"amendment-header\">");
        html.append("<h1>AVENANT AU CONTRAT</h1>");
        html.append("<div class=\"ref-block\">");
        html.append("<span>Réf. avenant : <strong>").append(esc(amendment.getCode())).append("</strong></span> &nbsp;|&nbsp; ");
        html.append("<span>Contrat original : <strong>").append(esc(amendment.getOriginalContractCode())).append("</strong></span> &nbsp;|&nbsp; ");
        html.append("<span>Date d'émission : <strong>").append(today).append("</strong></span>");
        html.append("</div></div>");

        // ── Objet ────────────────────────────────────────────────────────────
        html.append("<div class=\"amendment-section\">");
        html.append("<h2>OBJET DE L'AVENANT</h2>");
        html.append("<p>").append(esc(amendment.getDescription())).append("</p>");
        html.append("</div>");

        // ── Modifications des clauses ─────────────────────────────────────────
        if (!sections.isEmpty()) {
            html.append("<div class=\"amendment-section\">");
            html.append("<h2>MODIFICATIONS DES CLAUSES</h2>");
            for (ContractAmendmentSection s : sections) {
                html.append("<div class=\"change-block change-").append(s.getAction().name().toLowerCase(Locale.ROOT)).append("\">");
                html.append("<span class=\"badge badge-").append(s.getAction().name().toLowerCase(Locale.ROOT)).append("\">")
                        .append(badgeLabel(s.getAction())).append("</span>");
                if (StringUtils.hasText(s.getTargetSectionRef())) {
                    html.append("<p class=\"target-ref\">Section visée : <em>").append(esc(s.getTargetSectionRef())).append("</em></p>");
                }
                if (s.getAction() != ContractAmendmentSectionAction.REMOVE) {
                    if (StringUtils.hasText(s.getTitle())) {
                        html.append("<h3>").append(esc(s.getTitle())).append("</h3>");
                    }
                    if (StringUtils.hasText(s.getContent())) {
                        html.append("<div class=\"section-content\">").append(s.getContent()).append("</div>");
                    }
                } else {
                    html.append("<p>La section référencée ci-dessus est supprimée du contrat original.</p>");
                }
                html.append("</div>");
            }
            html.append("</div>");
        }

        // ── Modifications des variables ───────────────────────────────────────
        if (!variables.isEmpty()) {
            html.append("<div class=\"amendment-section\">");
            html.append("<h2>MODIFICATIONS DES CONDITIONS CONTRACTUELLES</h2>");
            html.append("<table class=\"var-table\"><thead><tr>");
            html.append("<th>Variable</th><th>Ancienne valeur</th><th>Nouvelle valeur</th>");
            html.append("</tr></thead><tbody>");
            for (ContractAmendmentVariable v : variables) {
                html.append("<tr><td><code>").append(esc(v.getVariableKey())).append("</code></td>");
                html.append("<td class=\"old-val\">").append(esc(v.getPreviousValue())).append("</td>");
                html.append("<td class=\"new-val\">").append(esc(v.getNewValue())).append("</td></tr>");
            }
            html.append("</tbody></table></div>");
        }

        // ── Clause de sauvegarde ──────────────────────────────────────────────
        html.append("<div class=\"amendment-section legal\">");
        html.append("<p>Le présent avenant entre en vigueur à compter du <strong>").append(effectiveDate).append("</strong>. ");
        html.append("Toutes les autres dispositions du contrat original N° <strong>")
                .append(esc(amendment.getOriginalContractCode()))
                .append("</strong> non visées par le présent avenant restent inchangées et pleinement en vigueur.</p>");
        html.append("</div>");

        // ── Signatures ────────────────────────────────────────────────────────
        html.append("<div class=\"signature-block\">");
        html.append("<p>Fait à Pointe-Noire, le ").append(today).append("</p><br/>");
        html.append("<table class=\"sig-table\"><tr>");
        html.append("<td><div class=\"sig-col\"><p class=\"sig-label\">Pour le Prestataire</p>")
                .append("<div class=\"sig-box\"></div></div></td>");
        html.append("<td><div class=\"sig-col\"><p class=\"sig-label\">Pour le Bénéficiaire</p>")
                .append("<div class=\"sig-box\"></div></div></td>");
        html.append("</tr></table></div>");

        html.append("</body></html>");
        return html.toString();
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.parse(html);
            jsoupDoc.outputSettings()
                    .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                    .charset(StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(jsoupDoc), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Impossible de générer le PDF de l'avenant : "
                    + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ContractAmendment findByCode(String code) {
        return amendmentRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Avenant introuvable : " + code));
    }

    private void requireStatus(ContractAmendment amendment, AmendmentStatus expected) {
        if (amendment.getStatus() != expected) {
            throw new BadRequestException(
                    "Transition invalide : l'avenant est en statut " + amendment.getStatus()
                            + ", statut attendu : " + expected);
        }
    }

    private void requireDraftStatus(ContractAmendment amendment, String operation) {
        if (amendment.getStatus() != AmendmentStatus.DRAFT) {
            throw new BadRequestException(
                    "Impossible de " + operation + " : l'avenant n'est plus en DRAFT (statut : "
                            + amendment.getStatus() + ")");
        }
    }

    private void requireAtLeastOneChange(ContractAmendment amendment) {
        boolean hasSections = !sectionRepository
                .findAllByAmendmentCodeOrderBySectionOrderAsc(amendment.getCode()).isEmpty();
        boolean hasVariables = !variableRepository
                .findAllByAmendmentCodeOrderByVariableKeyAsc(amendment.getCode()).isEmpty();
        if (!hasSections && !hasVariables) {
            throw new BadRequestException(
                    "L'avenant doit contenir au moins une modification de section ou de variable avant soumission.");
        }
    }

    private void validateSectionRequest(ContractAmendmentSectionRequest request) {
        if (request.getAction() == ContractAmendmentSectionAction.REMOVE
                && !StringUtils.hasText(request.getTargetSectionRef())) {
            throw new BadRequestException("targetSectionRef est requis pour l'action REMOVE");
        }
        if (request.getAction() != ContractAmendmentSectionAction.REMOVE
                && !StringUtils.hasText(request.getContent())) {
            throw new BadRequestException("content est requis pour les actions ADD et MODIFY");
        }
        if (request.getAction() == ContractAmendmentSectionAction.MODIFY
                && !StringUtils.hasText(request.getTargetSectionRef())) {
            throw new BadRequestException("targetSectionRef est requis pour l'action MODIFY");
        }
    }

    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String badgeLabel(ContractAmendmentSectionAction action) {
        return switch (action) {
            case ADD -> "ARTICLE NOUVEAU";
            case MODIFY -> "ARTICLE MODIFIÉ";
            case REMOVE -> "ARTICLE SUPPRIMÉ";
        };
    }

    private ContractAmendmentResponse toResponse(ContractAmendment a) {
        List<ContractAmendmentSectionResponse> sections =
                sectionRepository.findAllByAmendmentCodeOrderBySectionOrderAsc(a.getCode())
                        .stream().map(this::toSectionResponse).toList();
        List<ContractAmendmentVariableResponse> vars =
                variableRepository.findAllByAmendmentCodeOrderByVariableKeyAsc(a.getCode())
                        .stream().map(this::toVariableResponse).toList();
        return ContractAmendmentResponse.builder()
                .code(a.getCode())
                .originalContractCode(a.getOriginalContractCode())
                .status(a.getStatus())
                .description(a.getDescription())
                .proposedBy(a.getProposedBy())
                .proposedAt(a.getProposedAt())
                .effectiveDate(a.getEffectiveDate())
                .draftDocumentCode(a.getDraftDocumentCode())
                .signedDocumentCode(a.getSignedDocumentCode())
                .signedAt(a.getSignedAt())
                .activatedAt(a.getActivatedAt())
                .reviewedBy(a.getReviewedBy())
                .reviewComment(a.getReviewComment())
                .rejectionReason(a.getRejectionReason())
                .cancellationReason(a.getCancellationReason())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .sections(sections)
                .variableChanges(vars)
                .build();
    }

    private ContractAmendmentSectionResponse toSectionResponse(ContractAmendmentSection s) {
        return ContractAmendmentSectionResponse.builder()
                .id(s.getId())
                .action(s.getAction())
                .targetSectionRef(s.getTargetSectionRef())
                .sectionType(s.getSectionType())
                .title(s.getTitle())
                .content(s.getContent())
                .sectionOrder(s.getSectionOrder())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ContractAmendmentVariableResponse toVariableResponse(ContractAmendmentVariable v) {
        return ContractAmendmentVariableResponse.builder()
                .id(v.getId())
                .variableKey(v.getVariableKey())
                .previousValue(v.getPreviousValue())
                .newValue(v.getNewValue())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private static final String AMENDMENT_CSS = """
            @page { size: A4; margin: 18mm 20mm 20mm 20mm; }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body { font-family: "Times New Roman", Times, serif; font-size: 10pt; line-height: 1.6; color: #111; }
            .amendment-header { text-align: center; border-bottom: 2px solid #333; padding-bottom: 10pt; margin-bottom: 16pt; }
            .amendment-header h1 { font-size: 15pt; letter-spacing: 1px; }
            .ref-block { font-size: 9pt; margin-top: 6pt; color: #444; }
            .amendment-section { margin-bottom: 16pt; }
            .amendment-section h2 { font-size: 12pt; text-transform: uppercase; border-bottom: 1px solid #bbb;
                                    padding-bottom: 4pt; margin-bottom: 8pt; }
            .change-block { border-left: 3px solid #ccc; padding: 8pt 12pt; margin-bottom: 10pt; }
            .change-block.change-add { border-left-color: #2a7f4e; background: #f0fff4; }
            .change-block.change-modify { border-left-color: #b46e10; background: #fff8ee; }
            .change-block.change-remove { border-left-color: #c0392b; background: #fff0ee; }
            .badge { display: inline-block; padding: 2pt 6pt; border-radius: 3pt; font-size: 7.5pt;
                     font-weight: bold; letter-spacing: 0.5px; margin-bottom: 6pt; }
            .badge-add { background: #2a7f4e; color: #fff; }
            .badge-modify { background: #b46e10; color: #fff; }
            .badge-remove { background: #c0392b; color: #fff; }
            .target-ref { font-size: 9pt; color: #555; margin-bottom: 4pt; }
            .section-content { text-align: justify; }
            .legal { font-style: italic; font-size: 9.5pt; border-top: 1px solid #ddd; padding-top: 10pt; }
            .var-table { width: 100%; border-collapse: collapse; font-size: 9pt; }
            .var-table th, .var-table td { border: 1px solid #ccc; padding: 4pt 6pt; }
            .var-table th { background: #f5f5f5; font-weight: bold; }
            .old-val { color: #c0392b; text-decoration: line-through; }
            .new-val { color: #2a7f4e; font-weight: bold; }
            .signature-block { margin-top: 30pt; }
            .sig-table { width: 100%; border-collapse: collapse; }
            .sig-col { text-align: center; padding: 0 20pt; }
            .sig-label { font-weight: bold; margin-bottom: 6pt; }
            .sig-box { border-bottom: 1px solid #333; height: 40pt; margin: 0 auto; width: 80%; }
            """;
}
