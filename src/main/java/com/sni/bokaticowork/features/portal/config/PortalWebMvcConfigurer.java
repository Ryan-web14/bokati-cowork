package com.sni.bokaticowork.features.portal.config;

import com.sni.bokaticowork.features.portal.guard.ClientOnboardingGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class PortalWebMvcConfigurer implements WebMvcConfigurer {

    private final ClientOnboardingGuard clientOnboardingGuard;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientOnboardingGuard)
                .addPathPatterns("/sni/api/v1/client/**");
    }
}
