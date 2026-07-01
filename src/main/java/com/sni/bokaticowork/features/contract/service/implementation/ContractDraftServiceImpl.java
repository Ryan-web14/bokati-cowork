package com.sni.bokaticowork.features.contract.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.contract.dto.request.ContractDraftSectionRequest;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractDraftRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractDraftResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractDraftSectionResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.enums.ContractDraftStatus;
import com.sni.bokaticowork.features.contract.enums.ContractSectionType;
import com.sni.bokaticowork.features.contract.model.ContractDraft;
import com.sni.bokaticowork.features.contract.model.ContractDraftSection;
import com.sni.bokaticowork.features.contract.model.ContractTemplate;
import com.sni.bokaticowork.features.contract.model.ContractTemplateSection;
import com.sni.bokaticowork.features.contract.repository.ContractDraftRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractDraftService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractTemplateService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractDraftServiceImpl implements ContractDraftService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final ContractDraftRepository draftRepository;
    private final ContractTemplateService templateService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final MemberService memberService;
    private final CustomerService customerService;
    private final BusinessService businessService;
    private final DocumentService documentService;

    @Override
    public ContractDraftResponse create(CreateContractDraftRequest request) {
        ContractDraft draft = ContractDraft.builder()
                .code(sequenceGenerator.next("CONTRACT_DRAFT"))
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .templateCode(request.getTemplateCode())
                .ownerType(request.getOwnerType())
                .ownerCode(request.getOwnerCode())
                .businessCode(request.getBusinessCode())
                .cssContent(request.getCssContent())
                .headerHtml(request.getHeaderHtml())
                .footerHtml(request.getFooterHtml())
                .createdBy(request.getCreatedBy())
                .build();

        if (request.getSections() != null) {
            int order = 0;
            for (ContractDraftSectionRequest sectionReq : request.getSections()) {
                ContractDraftSection section = buildSection(sectionReq, draft, order++);
                draft.getSections().add(section);
            }
        }

        return toResponse(draftRepository.save(draft));
    }

    @Override
    public ContractDraftResponse createFromTemplate(String templateCode, CreateContractDraftRequest request) {
        ContractTemplate template = templateService.getTemplateForService(templateCode);

        ContractDraft draft = ContractDraft.builder()
                .code(sequenceGenerator.next("CONTRACT_DRAFT"))
                .title(request.getTitle() != null ? request.getTitle().trim() : template.getName())
                .description(request.getDescription() != null ? request.getDescription() : template.getDescription())
                .templateCode(templateCode)
                .ownerType(request.getOwnerType())
                .ownerCode(request.getOwnerCode())
                .businessCode(request.getBusinessCode())
                .cssContent(template.getCssContent())
                .headerHtml(template.getHeaderHtml())
                .footerHtml(template.getFooterHtml())
                .createdBy(request.getCreatedBy())
                .build();

        if (!template.getSections().isEmpty()) {
            int order = 0;
            for (var ts : template.getSections()) {
                if (!Boolean.TRUE.equals(ts.getActive())) continue;
                draft.getSections().add(ContractDraftSection.builder()
                        .draft(draft)
                        .title(ts.getTitle())
                        .content(ts.getContent())
                        .sectionType(ts.getSectionType())
                        .sectionOrder(order++)
                        .build());
            }
        } else if (StringUtils.hasText(template.getHtmlContent())) {
            draft.getSections().add(ContractDraftSection.builder()
                    .draft(draft)
                    .title(template.getName())
                    .content(template.getHtmlContent())
                    .sectionType(ContractSectionType.ARTICLE)
                    .sectionOrder(0)
                    .build());
        }

        if (request.getSections() != null) {
            int order = draft.getSections().size();
            for (ContractDraftSectionRequest sectionReq : request.getSections()) {
                ContractDraftSection section = buildSection(sectionReq, draft, order++);
                draft.getSections().add(section);
            }
        }

        return toResponse(draftRepository.save(draft));
    }

    @Override
    public ContractDraftResponse update(String code, UpdateContractDraftRequest request) {
        ContractDraft draft = findByCode(code);
        if (StringUtils.hasText(request.getTitle())) draft.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) draft.setDescription(request.getDescription());
        if (request.getOwnerType() != null) draft.setOwnerType(request.getOwnerType());
        if (request.getOwnerCode() != null) draft.setOwnerCode(request.getOwnerCode());
        if (request.getBusinessCode() != null) draft.setBusinessCode(request.getBusinessCode());
        if (request.getCssContent() != null) draft.setCssContent(request.getCssContent());
        if (request.getHeaderHtml() != null) draft.setHeaderHtml(request.getHeaderHtml());
        if (request.getFooterHtml() != null) draft.setFooterHtml(request.getFooterHtml());

        if (request.getSections() != null) {
            draft.getSections().clear();
            int order = 0;
            for (ContractDraftSectionRequest sectionReq : request.getSections()) {
                ContractDraftSection section = buildSection(sectionReq, draft, order++);
                draft.getSections().add(section);
            }
        }

        return toResponse(draftRepository.save(draft));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDraftResponse getByCode(String code) {
        return toResponse(findByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractDraftResponse> list(Pageable pageable) {
        Page<ContractDraftResponse> page = draftRepository
                .findAllByOrderByUpdatedAtDesc(pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractDraftResponse> listByStatus(ContractDraftStatus status, Pageable pageable) {
        Page<ContractDraftResponse> page = draftRepository
                .findAllByStatus(status, pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractDraftResponse> listByOwner(DocumentOwnerType ownerType, String ownerCode, Pageable pageable) {
        Page<ContractDraftResponse> page = draftRepository
                .findAllByOwnerTypeAndOwnerCode(ownerType, ownerCode, pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractDraftResponse> search(String query, Pageable pageable) {
        Page<ContractDraftResponse> page = draftRepository
                .search(query, pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    public ContractDraftSectionResponse addSection(String draftCode, ContractDraftSectionRequest request) {
        ContractDraft draft = findByCode(draftCode);
        int maxOrder = draft.getSections().stream()
                .mapToInt(ContractDraftSection::getSectionOrder)
                .max().orElse(-1);
        ContractDraftSection section = buildSection(request, draft, request.getSectionOrder() != null ? request.getSectionOrder() : maxOrder + 1);
        draft.getSections().add(section);
        draftRepository.save(draft);
        return toSectionResponse(section);
    }

    @Override
    public ContractDraftSectionResponse updateSection(String draftCode, Long sectionId, ContractDraftSectionRequest request) {
        ContractDraft draft = findByCode(draftCode);
        ContractDraftSection section = draft.getSections().stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Section non trouvée: " + sectionId));
        if (request.getTitle() != null) section.setTitle(request.getTitle());
        if (StringUtils.hasText(request.getContent())) section.setContent(request.getContent());
        if (request.getSectionType() != null) section.setSectionType(request.getSectionType());
        if (request.getSectionOrder() != null) section.setSectionOrder(request.getSectionOrder());
        if (request.getActive() != null) section.setActive(request.getActive());
        draftRepository.save(draft);
        return toSectionResponse(section);
    }

    @Override
    public void removeSection(String draftCode, Long sectionId) {
        ContractDraft draft = findByCode(draftCode);
        draft.getSections().removeIf(s -> s.getId().equals(sectionId));
        draftRepository.save(draft);
    }

    @Override
    public ContractDraftResponse updateStatus(String code, ContractDraftStatus status) {
        ContractDraft draft = findByCode(code);
        draft.setStatus(status);
        return toResponse(draftRepository.save(draft));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPreviewResponse preview(String code, Map<String, String> extraVariables) {
        ContractDraft draft = findByCode(code);
        Map<String, String> resolvedVars = buildResolvedVariables(draft, extraVariables);
        String html = renderDraftHtml(draft, resolvedVars);
        return ContractPreviewResponse.builder()
                .templateCode(draft.getTemplateCode())
                .html(html)
                .build();
    }

    @Override
    public DocumentResponse generatePdf(String code, Map<String, String> extraVariables, Long uploadedBy) {
        ContractDraft draft = findByCode(code);
        Map<String, String> resolvedVars = buildResolvedVariables(draft, extraVariables);
        String html = renderDraftHtml(draft, resolvedVars);
        byte[] pdfBytes = renderPdf(html);

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        if (draft.getOwnerType() != null) metadata.setOwnerType(draft.getOwnerType());
        if (draft.getOwnerCode() != null) metadata.setOwnerCode(draft.getOwnerCode());
        metadata.setDocumentTypeCode("CONTRACT_DRAFT");
        metadata.setTitle(draft.getTitle());
        metadata.setDescription(draft.getDescription());
        metadata.setIssueDate(LocalDate.now());

        DocumentResponse docResponse = documentService.createGeneratedDocument(
                metadata,
                sanitizeFileName(draft.getTitle()) + ".pdf",
                "application/pdf",
                pdfBytes
        );

        draft.setStatus(ContractDraftStatus.GENERATED);
        draftRepository.save(draft);

        return docResponse;
    }

    @Override
    public void delete(String code) {
        ContractDraft draft = findByCode(code);
        draftRepository.delete(draft);
    }

    private Map<String, String> buildResolvedVariables(ContractDraft draft, Map<String, String> extraVariables) {
        Map<String, String> vars = new LinkedHashMap<>();

        vars.put("contractTitle", draft.getTitle());
        vars.put("generatedDate", LocalDate.now().toString());

        if (draft.getOwnerType() != null && StringUtils.hasText(draft.getOwnerCode())) {
            resolveOwnerVariables(draft.getOwnerType(), draft.getOwnerCode(), vars);
        }

        if (StringUtils.hasText(draft.getBusinessCode())) {
            resolveBusinessVariables(draft.getBusinessCode(), vars);
        }

        if (extraVariables != null) {
            vars.putAll(extraVariables);
        }
        return vars;
    }

    private void resolveOwnerVariables(DocumentOwnerType ownerType, String ownerCode, Map<String, String> vars) {
        switch (ownerType) {
            case MEMBER -> {
                Member member = memberService.getByMemberIdForService(ownerCode.trim());
                putIfNotBlank(vars, "clientName", member.getDisplayName());
                putIfNotBlank(vars, "clientEmail", member.getEmail());
                putIfNotBlank(vars, "clientPhone", member.getPhone());
                putIfNotBlank(vars, "clientCode", member.getMemberId());
            }
            case CUSTOMER -> {
                Customer customer = customerService.getCustomerForService(ownerCode.trim());
                String name = StringUtils.hasText(customer.getCompanyName())
                        ? customer.getCompanyName()
                        : ((customer.getFirstname() == null ? "" : customer.getFirstname()) + " " +
                           (customer.getLastname() == null ? "" : customer.getLastname())).trim();
                putIfNotBlank(vars, "clientName", name);
                putIfNotBlank(vars, "clientEmail", customer.getEmail());
                putIfNotBlank(vars, "clientPhone", customer.getPhone());
                putIfNotBlank(vars, "clientCode", customer.getCustomerId());
            }
            case BUSINESS -> {
                BusinessEntity biz = businessService.serviceBusinessByCode(ownerCode.trim());
                putIfNotBlank(vars, "clientName", biz.getName());
                putIfNotBlank(vars, "clientEmail", biz.getEmail());
                putIfNotBlank(vars, "clientPhone", biz.getPhone());
                putIfNotBlank(vars, "clientCode", biz.getCode());
                putIfNotBlank(vars, "clientRccm", biz.getRccmNumber());
            }
            default -> { }
        }
    }

    private void resolveBusinessVariables(String businessCode, Map<String, String> vars) {
        BusinessEntity business = businessService.serviceBusinessByCode(businessCode.trim());
        putIfNotBlank(vars, "businessName", business.getName());
        putIfNotBlank(vars, "businessEmail", business.getEmail());
        putIfNotBlank(vars, "businessPhone", business.getPhone());
        putIfNotBlank(vars, "operatorName", business.getName());
        putIfNotBlank(vars, "operatorRccm", business.getRccmNumber());
    }

    private String renderDraftHtml(ContractDraft draft, Map<String, String> variables) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"fr\">");
        html.append("<head><meta charset=\"UTF-8\"/>");
        html.append("<title>").append(escapeHtml(draft.getTitle())).append("</title>");
        html.append("<style>");
        html.append(DEFAULT_CSS);
        if (StringUtils.hasText(draft.getCssContent())) {
            html.append(draft.getCssContent());
        }
        html.append("</style></head><body>");

        if (StringUtils.hasText(draft.getHeaderHtml())) {
            html.append(substituteTokens(draft.getHeaderHtml(), variables));
        }

        List<ContractDraftSection> activeSections = draft.getSections().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .sorted((a, b) -> Integer.compare(a.getSectionOrder(), b.getSectionOrder()))
                .toList();

        for (ContractDraftSection section : activeSections) {
            html.append("<div class=\"contract-section section-").append(section.getSectionType().name().toLowerCase(Locale.ROOT)).append("\">");
            if (StringUtils.hasText(section.getTitle())) {
                String tag = section.getSectionType() == ContractSectionType.PREAMBLE ? "h2" : "h3";
                html.append("<").append(tag).append(">").append(escapeHtml(substituteTokens(section.getTitle(), variables))).append("</").append(tag).append(">");
            }
            html.append("<div class=\"section-content\">").append(substituteTokens(section.getContent(), variables)).append("</div>");
            html.append("</div>");
        }

        if (StringUtils.hasText(draft.getFooterHtml())) {
            html.append(substituteTokens(draft.getFooterHtml(), variables));
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String substituteTokens(String text, Map<String, String> vars) {
        if (!StringUtils.hasText(text) || vars.isEmpty()) return text;
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = vars.getOrDefault(matcher.group(1), matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
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
            Throwable root = ex.getCause() != null ? ex.getCause() : ex;
            throw new BadRequestException("Impossible de générer le PDF du brouillon: " + root.getMessage(), ex);
        }
    }

    private ContractDraftSection buildSection(ContractDraftSectionRequest req, ContractDraft draft, int defaultOrder) {
        return ContractDraftSection.builder()
                .draft(draft)
                .title(req.getTitle())
                .content(req.getContent())
                .sectionType(req.getSectionType() != null ? req.getSectionType() : ContractSectionType.ARTICLE)
                .sectionOrder(req.getSectionOrder() != null ? req.getSectionOrder() : defaultOrder)
                .active(req.getActive() != null ? req.getActive() : Boolean.TRUE)
                .build();
    }

    private ContractDraft findByCode(String code) {
        return draftRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Brouillon de contrat non trouvé: " + code));
    }

    private ContractDraftResponse toResponse(ContractDraft draft) {
        return ContractDraftResponse.builder()
                .code(draft.getCode())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .templateCode(draft.getTemplateCode())
                .ownerType(draft.getOwnerType())
                .ownerCode(draft.getOwnerCode())
                .businessCode(draft.getBusinessCode())
                .cssContent(draft.getCssContent())
                .headerHtml(draft.getHeaderHtml())
                .footerHtml(draft.getFooterHtml())
                .status(draft.getStatus())
                .createdBy(draft.getCreatedBy())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .sections(draft.getSections().stream().map(this::toSectionResponse).toList())
                .build();
    }

    private ContractDraftSectionResponse toSectionResponse(ContractDraftSection section) {
        return ContractDraftSectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .content(section.getContent())
                .sectionType(section.getSectionType())
                .sectionOrder(section.getSectionOrder())
                .active(section.getActive())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }

    private static void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (StringUtils.hasText(value)) map.put(key, value.trim());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String sanitizeFileName(String title) {
        return title.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static final String DEFAULT_CSS = """
            @page { size: A4; margin: 18mm 20mm 20mm 20mm; }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body { font-family: "Times New Roman", Times, serif; font-size: 10pt; line-height: 1.6; color: #111; }
            .contract-section { margin-bottom: 14pt; }
            .contract-section h2 { font-size: 13pt; text-align: center; margin-bottom: 10pt; }
            .contract-section h3 { font-size: 11pt; margin-bottom: 6pt; text-decoration: underline; }
            .section-content { text-align: justify; }
            .section-preamble { font-style: italic; margin-bottom: 18pt; }
            .section-signature_block { margin-top: 40pt; }
            """;
}
