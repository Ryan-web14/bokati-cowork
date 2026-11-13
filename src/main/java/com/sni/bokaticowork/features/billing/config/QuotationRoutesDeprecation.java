package com.sni.bokaticowork.features.billing.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lot B10 (ecart E18) : une seule famille de routes, {@code /billing/quotes/...}. Les routes
 * {@code /billing/quotations/...} repondent encore pendant six mois, marquees depreciees (en-tetes
 * Deprecation, Sunset et Link vers la route qui les remplace), puis disparaissent.
 */
@Configuration
public class QuotationRoutesDeprecation implements WebMvcConfigurer {

    /** Fin de vie : six mois apres la livraison du lot B10. */
    static final String SUNSET = "Thu, 13 May 2027 00:00:00 GMT";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String uri = request.getRequestURI();
                if (uri != null && uri.contains("/billing/quotations")) {
                    response.setHeader("Deprecation", "true");
                    response.setHeader("Sunset", SUNSET);
                    response.setHeader("Link", "<" + uri.replace("/billing/quotations", "/billing/quotes") + ">; rel=\"successor-version\"");
                }
                return true;
            }
        }).addPathPatterns("/**");
    }
}
