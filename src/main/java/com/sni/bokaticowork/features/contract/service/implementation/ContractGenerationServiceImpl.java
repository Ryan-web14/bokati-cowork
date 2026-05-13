package com.sni.bokaticowork.features.contract.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractTemplateResponse;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractGenerationService;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractGenerationServiceImpl implements ContractGenerationService {

    private static final List<ContractTemplateDescriptor> TEMPLATES = List.of(
            new ContractTemplateDescriptor("membership-agreement", "Membership Agreement", "Standard membership contract template"),
            new ContractTemplateDescriptor("business-service-agreement", "Business Service Agreement", "Business service contract template"),
            new ContractTemplateDescriptor("contrat-domiciliation", "Contrat de domiciliation", "Domiciliation contract template"),
            new ContractTemplateDescriptor("subscription-pass-non-refundable", "Subscription / Pass Non Refundable Agreement", "Non refundable subscription, pass and addon contract template")
    );

    private final SpringTemplateEngine templateEngine;
    private final DocumentService documentService;
    private final MemberService memberService;
    private final CustomerService customerService;
    private final BusinessService businessService;
    private final OutboxService outboxService;

    @Override
    @Transactional(readOnly = true)
    public List<ContractTemplateResponse> listTemplates() {
        return TEMPLATES.stream()
                .map(item -> ContractTemplateResponse.builder()
                        .code(item.code())
                        .name(item.name())
                        .description(item.description())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPreviewResponse preview(GenerateContractRequest request) {
        validate(request);
        return ContractPreviewResponse.builder()
                .templateCode(request.getTemplateCode())
                .html(renderHtml(request))
                .build();
    }

    @Override
    public DocumentResponse generatePdf(GenerateContractRequest request) {
        validate(request);
        String html = renderHtml(request);
        byte[] pdfBytes = renderPdf(html);

        DocumentUploadMetadataRequest metadata = new DocumentUploadMetadataRequest();
        metadata.setOwnerType(request.getOwnerType());
        metadata.setOwnerCode(request.getOwnerCode());
        metadata.setDocumentTypeCode("CONTRACT_DRAFT");
        metadata.setTitle(request.getTitle());
        metadata.setDescription(request.getDescription());
        metadata.setIssueDate(request.getEffectiveDate() == null ? LocalDate.now() : request.getEffectiveDate());

        DocumentResponse response = documentService.createGeneratedDocument(
                metadata,
                sanitizeFileName(request.getTitle()) + ".pdf",
                "application/pdf",
                pdfBytes
        );
        publishContractEvent(request, response);
        return response;
    }

    private String renderHtml(GenerateContractRequest request) {
        Context context = new Context(Locale.FRANCE);
        OwnerView ownerView = resolveOwnerView(request.getOwnerType(), request.getOwnerCode());
        BusinessEntity business = resolveBusiness(request.getBusinessCode());
        Map<String, String> enrichedVars = buildEnrichedVariables(request, ownerView, business);
        List<String> resolvedClauses = request.getClauses() == null ? List.of() :
                request.getClauses().stream().map(c -> substituteTokens(c, enrichedVars)).toList();
        context.setVariable("request", request);
        context.setVariable("generatedAt", LocalDate.now());
        context.setVariable("ownerType", request.getOwnerType());
        context.setVariable("ownerCode", ownerView.code());
        context.setVariable("ownerName", ownerView.name());
        context.setVariable("ownerEmail", ownerView.email());
        context.setVariable("ownerPhone", ownerView.phone());
        context.setVariable("business", business);
        context.setVariable("clauses", resolvedClauses);
        context.setVariable("variables", enrichedVars);
        return templateEngine.process("contracts/" + normalizeTemplateCode(request.getTemplateCode()), context);
    }

    private Map<String, String> buildEnrichedVariables(GenerateContractRequest request, OwnerView owner, BusinessEntity business) {
        Map<String, String> vars = new java.util.LinkedHashMap<>();
        putIfNotBlank(vars, "clientName",      owner.name());
        putIfNotBlank(vars, "clientEmail",     owner.email());
        putIfNotBlank(vars, "clientPhone",     owner.phone());
        putIfNotBlank(vars, "clientCode",      owner.code());
        putIfNotBlank(vars, "startDate",       request.getStartDate()     != null ? request.getStartDate().toString()     : null);
        putIfNotBlank(vars, "endDate",         request.getEndDate()       != null ? request.getEndDate().toString()       : null);
        putIfNotBlank(vars, "effectiveDate",   request.getEffectiveDate() != null ? request.getEffectiveDate().toString() : null);
        putIfNotBlank(vars, "signatoryName",   request.getSignatoryName());
        putIfNotBlank(vars, "signatoryRole",   request.getSignatoryRole());
        putIfNotBlank(vars, "contractTitle",   request.getTitle());
        vars.put("operatorName", business != null && StringUtils.hasText(business.getName()) ? business.getName().trim() : "ELLE A OSE");
        if (business != null) {
            putIfNotBlank(vars, "businessName",  business.getName());
            putIfNotBlank(vars, "businessEmail", business.getEmail());
            putIfNotBlank(vars, "businessPhone", business.getPhone());
        }
        if (request.getVariables() != null) {
            vars.putAll(request.getVariables());
        }
        return vars;
    }

    private static void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value.trim());
        }
    }

    private static String substituteTokens(String text, Map<String, String> vars) {
        if (!StringUtils.hasText(text) || vars.isEmpty()) {
            return text;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{(\\w+)}}").matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = vars.getOrDefault(matcher.group(1), matcher.group(0));
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
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
                    .charset(java.nio.charset.StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(jsoupDoc), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            Throwable root = ex.getCause() != null ? ex.getCause() : ex;
            throw new BadRequestException("Unable to generate contract PDF: " + root.getMessage(), ex);
        }
    }

    private OwnerView resolveOwnerView(DocumentOwnerType ownerType, String ownerCode) {
        return switch (ownerType) {
            case MEMBER -> {
                Member member = memberService.getByMemberIdForService(ownerCode.trim());
                yield new OwnerView(member.getMemberId(), member.getDisplayName().trim(), member.getEmail(), member.getPhone());
            }
            case CUSTOMER -> {
                Customer customer = customerService.getCustomerForService(ownerCode.trim());
                String name = customer.getCompanyName() != null && !customer.getCompanyName().isBlank()
                        ? customer.getCompanyName()
                        : ((customer.getFirstname() == null ? "" : customer.getFirstname()) + " " + (customer.getLastname() == null ? "" : customer.getLastname())).trim();
                yield new OwnerView(customer.getCustomerId(), name, customer.getEmail(), customer.getPhone());
            }
            case BUSINESS -> {
                BusinessEntity business = businessService.serviceBusinessByCode(ownerCode.trim());
                yield new OwnerView(business.getCode(), business.getName(), business.getEmail(), business.getPhone());
            }
            default -> throw new BadRequestException("Unsupported contract owner type: " + ownerType);
        };
    }

    private BusinessEntity resolveBusiness(String businessCode) {
        if (!StringUtils.hasText(businessCode)) {
            return null;
        }
        return businessService.serviceBusinessByCode(businessCode.trim());
    }

    private void validate(GenerateContractRequest request) {
        String code = normalizeTemplateCode(request.getTemplateCode());
        if (TEMPLATES.stream().noneMatch(item -> item.code().equals(code))) {
            throw new BadRequestException("Unknown contract template: " + request.getTemplateCode());
        }
        if (request.getOwnerType() == DocumentOwnerType.CONTRACT
                || request.getOwnerType() == DocumentOwnerType.INVOICE
                || request.getOwnerType() == DocumentOwnerType.PAYMENT
                || request.getOwnerType() == DocumentOwnerType.PROPOSAL
                || request.getOwnerType() == DocumentOwnerType.ASSET) {
            throw new BadRequestException("This contract generator supports MEMBER, CUSTOMER or BUSINESS owners only");
        }
    }

    private String normalizeTemplateCode(String templateCode) {
        if (!StringUtils.hasText(templateCode)) {
            throw new BadRequestException("Template code is required");
        }
        return templateCode.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String title) {
        return title.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private record ContractTemplateDescriptor(String code, String name, String description) {
    }

    private record OwnerView(String code, String name, String email, String phone) {
    }

    private void publishContractEvent(GenerateContractRequest request, DocumentResponse response) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("templateCode", request.getTemplateCode());
        payload.put("ownerType", request.getOwnerType());
        payload.put("ownerCode", request.getOwnerCode());
        payload.put("businessCode", request.getBusinessCode());
        payload.put("documentCode", response.getCode());
        payload.put("uploadedBy", request.getUploadedBy());
        outboxService.publish("CONTRACT_DRAFT_GENERATED", "CONTRACT", request.getOwnerCode(), payload);
    }
}
