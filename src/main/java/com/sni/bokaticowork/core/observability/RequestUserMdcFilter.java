package com.sni.bokaticowork.core.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Pose dans le MDC le compte connecte ({@code user}), une fois la securite passee (lot A0) : chaque
 * ligne de journal d'une requete dit qui l'a faite.
 */
@Configuration
public class RequestUserMdcFilter {

    /** Apres la chaine de Spring Security (ordre -100). */
    static final int ORDER = 0;

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> requestUserMdcFilterRegistration() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
                    MDC.put(RequestContext.MDC_USER, authentication.getName());
                    request.setAttribute(RequestContext.ATTRIBUTE_USER, authentication.getName());
                }
                chain.doFilter(request, response);
            }
        });
        registration.setOrder(ORDER);
        registration.setName("requestUserMdcFilter");
        return registration;
    }
}
