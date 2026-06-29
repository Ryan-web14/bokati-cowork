package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractTemplateRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractTemplateRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractPreviewResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractTemplateResponse;
import com.sni.bokaticowork.features.contract.model.ContractTemplate;
import com.sni.bokaticowork.features.contract.repository.ContractTemplateRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractTemplateServiceImpl implements ContractTemplateService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final ContractTemplateRepository templateRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public ContractTemplateResponse create(CreateContractTemplateRequest request) {
        ContractTemplate template = ContractTemplate.builder()
                .code(sequenceGenerator.next("CONTRACT_TEMPLATE"))
                .name(request.getName().trim())
                .description(request.getDescription())
                .language(request.getLanguage() != null ? request.getLanguage() : "fr")
                .category(request.getCategory())
                .htmlContent(request.getHtmlContent())
                .cssContent(request.getCssContent())
                .headerHtml(request.getHeaderHtml())
                .footerHtml(request.getFooterHtml())
                .variableDefinitions(request.getVariableDefinitions() != null ? request.getVariableDefinitions() : new ArrayList<>())
                .build();
        return toResponse(templateRepository.save(template));
    }

    @Override
    public ContractTemplateResponse update(String code, UpdateContractTemplateRequest request) {
        ContractTemplate template = findByCode(code);
        if (StringUtils.hasText(request.getName())) template.setName(request.getName().trim());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getLanguage() != null) template.setLanguage(request.getLanguage());
        if (request.getCategory() != null) template.setCategory(request.getCategory());
        if (request.getHtmlContent() != null) template.setHtmlContent(request.getHtmlContent());
        if (request.getCssContent() != null) template.setCssContent(request.getCssContent());
        if (request.getHeaderHtml() != null) template.setHeaderHtml(request.getHeaderHtml());
        if (request.getFooterHtml() != null) template.setFooterHtml(request.getFooterHtml());
        if (request.getVariableDefinitions() != null) template.setVariableDefinitions(request.getVariableDefinitions());
        if (request.getActive() != null) template.setActive(request.getActive());
        return toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTemplateResponse getByCode(String code) {
        return toResponse(findByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractTemplateResponse> list(Pageable pageable) {
        Page<ContractTemplateResponse> page = templateRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractTemplateResponse> search(String query, Pageable pageable) {
        Page<ContractTemplateResponse> page = templateRepository
                .search(query, pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractTemplateResponse> listByCategory(String category, Pageable pageable) {
        Page<ContractTemplateResponse> page = templateRepository
                .findAllByCategoryIgnoreCase(category, pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPreviewResponse previewTemplate(String code, Map<String, String> sampleVariables) {
        ContractTemplate template = findByCode(code);
        if (!StringUtils.hasText(template.getHtmlContent())) {
            throw new BadRequestException("Ce template n'a pas de contenu HTML défini");
        }
        String html = buildFullHtml(template, sampleVariables != null ? sampleVariables : Map.of());
        return ContractPreviewResponse.builder()
                .templateCode(code)
                .html(html)
                .build();
    }

    @Override
    public ContractTemplateResponse duplicate(String code) {
        ContractTemplate source = findByCode(code);
        ContractTemplate copy = ContractTemplate.builder()
                .code(sequenceGenerator.next("CONTRACT_TEMPLATE"))
                .name(source.getName() + " (copie)")
                .description(source.getDescription())
                .language(source.getLanguage())
                .category(source.getCategory())
                .htmlContent(source.getHtmlContent())
                .cssContent(source.getCssContent())
                .headerHtml(source.getHeaderHtml())
                .footerHtml(source.getFooterHtml())
                .variableDefinitions(source.getVariableDefinitions() != null ? new ArrayList<>(source.getVariableDefinitions()) : new ArrayList<>())
                .build();
        return toResponse(templateRepository.save(copy));
    }

    @Override
    public void delete(String code) {
        ContractTemplate template = findByCode(code);
        templateRepository.delete(template);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTemplate getTemplateForService(String code) {
        return findByCode(code);
    }

    String buildFullHtml(ContractTemplate template, Map<String, String> variables) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"").append(template.getLanguage()).append("\">");
        html.append("<head><meta charset=\"UTF-8\"/>");
        html.append("<title>").append(escapeHtml(template.getName())).append("</title>");
        if (StringUtils.hasText(template.getCssContent())) {
            html.append("<style>").append(template.getCssContent()).append("</style>");
        }
        html.append("</head><body>");
        if (StringUtils.hasText(template.getHeaderHtml())) {
            html.append("<header>").append(substituteTokens(template.getHeaderHtml(), variables)).append("</header>");
        }
        html.append("<main>").append(substituteTokens(template.getHtmlContent(), variables)).append("</main>");
        if (StringUtils.hasText(template.getFooterHtml())) {
            html.append("<footer>").append(substituteTokens(template.getFooterHtml(), variables)).append("</footer>");
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

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private ContractTemplate findByCode(String code) {
        return templateRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Template de contrat non trouvé: " + code));
    }

    private ContractTemplateResponse toResponse(ContractTemplate t) {
        return ContractTemplateResponse.builder()
                .code(t.getCode())
                .name(t.getName())
                .description(t.getDescription())
                .language(t.getLanguage())
                .version(t.getVersion())
                .category(t.getCategory())
                .htmlContent(t.getHtmlContent())
                .cssContent(t.getCssContent())
                .headerHtml(t.getHeaderHtml())
                .footerHtml(t.getFooterHtml())
                .variableDefinitions(t.getVariableDefinitions())
                .active(t.getActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
