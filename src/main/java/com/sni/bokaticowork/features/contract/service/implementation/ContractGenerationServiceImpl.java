package com.sni.bokaticowork.features.contract.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
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
import com.sni.bokaticowork.features.contract.model.ContractTemplate;
import com.sni.bokaticowork.features.contract.repository.ContractTemplateRepository;
import com.sni.bokaticowork.features.contract.service.support.ContractEmailNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
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
    private final ContractEmailNotifier contractEmailNotifier;
    private final ContractTemplateRepository contractTemplateRepository;
    private final Locale appLocale;

    @Override
    @Transactional(readOnly = true)
    public List<ContractTemplateResponse> listTemplates() {
        List<ContractTemplate> dbTemplates = contractTemplateRepository.findAllByActiveTrueOrderByNameAsc();
        if (!dbTemplates.isEmpty()) {
            return dbTemplates.stream()
                    .map(t -> ContractTemplateResponse.builder()
                            .code(t.getCode())
                            .name(t.getName())
                            .description(t.getDescription())
                            .language(t.getLanguage())
                            .version(t.getVersion())
                            .category(t.getCategory())
                            .active(t.getActive())
                            .createdAt(t.getCreatedAt())
                            .updatedAt(t.getUpdatedAt())
                            .build())
                    .toList();
        }
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

        DocumentResponse docResponse = documentService.createGeneratedDocument(
                metadata,
                sanitizeFileName(request.getTitle()) + ".pdf",
                "application/pdf",
                pdfBytes
        );

        OwnerView ownerView = resolveOwnerView(request.getOwnerType(), request.getOwnerCode());
        // Send the "contract ready" email only once the surrounding transaction commits.
        // Otherwise a generation that later rolls back (e.g. the subscription contract-repair
        // worker retrying) would still fire the async email on every attempt, spamming the
        // client · and the async reader could race the not-yet-committed document.
        String recipientEmail = ownerView.email();
        String recipientName = ownerView.name();
        String templateCode = request.getTemplateCode();
        String documentCode = docResponse.getCode();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contractEmailNotifier.notifyGenerated(recipientEmail, recipientName, templateCode, documentCode);
                }
            });
        } else {
            contractEmailNotifier.notifyGenerated(recipientEmail, recipientName, templateCode, documentCode);
        }

        return docResponse;
    }

    private String renderHtml(GenerateContractRequest request) {
        String code = normalizeTemplateCode(request.getTemplateCode());
        OwnerView ownerView = resolveOwnerView(request.getOwnerType(), request.getOwnerCode());
        BusinessEntity business = resolveBusiness(request.getBusinessCode());
        Map<String, String> enrichedVars = buildEnrichedVariables(request, ownerView, business);

        java.util.Optional<ContractTemplate> dbTemplate = contractTemplateRepository.findByCode(code);
        if (dbTemplate.isPresent() && org.springframework.util.StringUtils.hasText(dbTemplate.get().getHtmlContent())) {
            return renderFromDbTemplate(dbTemplate.get(), enrichedVars);
        }

        List<String> resolvedClauses = request.getClauses() == null ? List.of() :
                request.getClauses().stream().map(c -> substituteTokens(c, enrichedVars)).toList();
        Context context = new Context(appLocale);
        context.setVariable("request", request);
        context.setVariable("generatedAt", LocalDate.now());
        context.setVariable("ownerType", request.getOwnerType());
        context.setVariable("ownerCode", ownerView.code());
        context.setVariable("ownerName", ownerView.name());
        context.setVariable("ownerEmail", ownerView.email());
        context.setVariable("ownerPhone", ownerView.phone());
        context.setVariable("ownerRccm", ownerView.rccm());
        context.setVariable("ownerAddress", ownerView.address());
        context.setVariable("business", business);
        context.setVariable("clauses", resolvedClauses);
        context.setVariable("variables", enrichedVars);
        return templateEngine.process("contracts/" + code, context);
    }

    private String renderFromDbTemplate(ContractTemplate template, Map<String, String> variables) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"").append(template.getLanguage()).append("\">");
        html.append("<head><meta charset=\"UTF-8\"/>");
        html.append("<title>").append(template.getName().replace("&", "&amp;").replace("<", "&lt;")).append("</title>");
        if (org.springframework.util.StringUtils.hasText(template.getCssContent())) {
            html.append("<style>").append(template.getCssContent()).append("</style>");
        }
        html.append("</head><body>");
        if (org.springframework.util.StringUtils.hasText(template.getHeaderHtml())) {
            html.append(substituteTokens(template.getHeaderHtml(), variables));
        }
        html.append(substituteTokens(template.getHtmlContent(), variables));
        if (org.springframework.util.StringUtils.hasText(template.getFooterHtml())) {
            html.append(substituteTokens(template.getFooterHtml(), variables));
        }
        html.append("</body></html>");
        return html.toString();
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
            putIfNotBlank(vars, "businessName",    business.getName());
            putIfNotBlank(vars, "businessEmail",   business.getEmail());
            putIfNotBlank(vars, "businessPhone",   business.getPhone());
            putIfNotBlank(vars, "operatorRccm",    business.getRccmNumber());
            putIfNotBlank(vars, "operatorAddress", formatAddress(business.getAddress()));
        }
        if (request.getVariables() != null) {
            vars.putAll(request.getVariables());
        }
        return vars;
    }

    private String formatAddress(com.sni.bokaticowork.core.baseClasses.model.Address address) {
        if (address == null) return null;
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(address.getStreetNumber())) sb.append(address.getStreetNumber()).append(" ");
        if (StringUtils.hasText(address.getStreetName())) sb.append(address.getStreetName()).append(", ");
        if (StringUtils.hasText(address.getDistrict())) sb.append(address.getDistrict()).append(", ");
        if (StringUtils.hasText(address.getCity())) sb.append(address.getCity());
        String result = sb.toString().replaceAll(",\\s*$", "").trim();
        return result.isEmpty() ? null : result;
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
                yield new OwnerView(member.getMemberId(), member.getDisplayName().trim(), member.getEmail(), member.getPhone(), null, null);
            }
            case CUSTOMER -> {
                Customer customer = customerService.getCustomerForService(ownerCode.trim());
                String name = customer.getCompanyName() != null && !customer.getCompanyName().isBlank()
                        ? customer.getCompanyName()
                        : ((customer.getFirstname() == null ? "" : customer.getFirstname()) + " " + (customer.getLastname() == null ? "" : customer.getLastname())).trim();
                yield new OwnerView(customer.getCustomerId(), name, customer.getEmail(), customer.getPhone(), null, null);
            }
            case BUSINESS -> {
                BusinessEntity biz = businessService.serviceBusinessByCode(ownerCode.trim());
                yield new OwnerView(biz.getCode(), biz.getName(), biz.getEmail(), biz.getPhone(), biz.getRccmNumber(), formatAddress(biz.getAddress()));
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
        boolean knownInDb = contractTemplateRepository.existsByCode(code);
        boolean knownHardcoded = TEMPLATES.stream().anyMatch(item -> item.code().equals(code));
        if (!knownInDb && !knownHardcoded) {
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

    private record OwnerView(String code, String name, String email, String phone, String rccm, String address) {
    }

}
