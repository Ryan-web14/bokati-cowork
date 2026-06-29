package com.sni.bokaticowork.security.config;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.authorization.AdminApiAuthorizationManager;
import com.sni.bokaticowork.security.authorization.ClientPortalAuthorizationManager;
import com.sni.bokaticowork.security.filter.JWTFilter;
import com.sni.bokaticowork.security.ratelimit.RateLimitingFilter;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private final CustomUserDetailService userDetailService;
    private final PasswordEncoder passwordEncoder;
    private final JWTFilter jwtFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final AdminApiAuthorizationManager adminApiAuthorizationManager;
    private final ClientPortalAuthorizationManager clientPortalAuthorizationManager;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                ApiPath.V1 + "/auth/login",
                                ApiPath.V1 + "/auth/refresh",
                                ApiPath.V1 + "/auth/register",
                                ApiPath.V1 + "/auth/ott/**",
                                ApiPath.V1 + "/auth/password-reset/**",
                                ApiPath.V1 + "/auth/unlock-account",
                                ApiPath.V1 + "/auth/unlock-account/confirm",
                                ApiPath.V1 + "/auth/email/verify/resend",
                                ApiPath.V1 + "/payments/mobile-money/providers",
                                ApiPath.V1 + "/payments/mobile-money/pawapay/callback",
                                ApiPath.V1 + "/payments/mobile-money/pawaypay/callback",
                                ApiPath.V1 + "/payments/mobile-money/pawapay/refund-callback",
                                ApiPath.V1 + "/payments/mobile-money/pawaypay/refund-callback",
                                "/verify/**",
                                ApiPath.V1 + "/admin/provisioning/bootstrap-admin",
                                ApiPath.V1 + "/public/**",
                                ApiPath.V1 + "/client/catalog/plans",
                                ApiPath.V1 + "/client/catalog/plans/**",
                                ApiPath.V1 + "/shares/**"

                        ).permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(ApiPath.V1 + "/client/**").access(clientPortalAuthorizationManager)
                        .requestMatchers(ApiPath.V1 + "/**").access(adminApiAuthorizationManager)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("status", 401);
                            body.put("error", "Unauthorized");
                            body.put("message", "Authentication is required to access this resource.");
                            body.put("path", request.getRequestURI());
                            objectMapper.writeValue(response.getOutputStream(), body);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("status", 403);
                            body.put("error", "Forbidden");
                            body.put("message", "You do not have permission to access this resource.");
                            body.put("path", request.getRequestURI());
                            objectMapper.writeValue(response.getOutputStream(), body);
                        }))
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManger() throws AuthenticationException {
        return new ProviderManager(daoAuthenticationProvider());
    }

    private DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
