package com.sni.bokaticowork.features.notification.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        String resolvedTemplate = StringUtils.hasText(templateName) ? templateName : "generic-notification";
        return templateEngine.process(resolvedTemplate, context);
    }
}
