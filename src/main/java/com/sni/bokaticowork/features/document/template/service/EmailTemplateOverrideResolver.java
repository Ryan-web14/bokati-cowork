package com.sni.bokaticowork.features.document.template.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.util.Map;
import java.util.Optional;

/**
 * Premier resolveur du moteur Thymeleaf (lot E1) : quand un courriel a un modele publie, il sert le
 * gabarit genere a la place du fichier d'origine ; sinon il ne repond pas et les resolveurs
 * habituels prennent la main. Aucun service d'envoi n'a donc a changer.
 */
@Component
public class EmailTemplateOverrideResolver implements ITemplateResolver {

    private final ObjectProvider<EmailTemplateRegistry> registry;

    public EmailTemplateOverrideResolver(ObjectProvider<EmailTemplateRegistry> registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "document-template-email-override";
    }

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Override
    public TemplateResolution resolveTemplate(IEngineConfiguration configuration, String ownerTemplate, String template,
                                              Map<String, Object> templateResolutionAttributes) {
        EmailTemplateRegistry current = registry.getIfAvailable();
        if (current == null) {
            return null;
        }
        Optional<String> html = current.lookup(template);
        return html.map(source -> new TemplateResolution(new StringTemplateResource(source), TemplateMode.HTML,
                NonCacheableCacheEntryValidity.INSTANCE)).orElse(null);
    }
}
