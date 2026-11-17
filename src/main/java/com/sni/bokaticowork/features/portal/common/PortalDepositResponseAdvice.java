package com.sni.bokaticowork.features.portal.common;

import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Toute route du portail client qui renvoie un depot mobile money le renvoie sans l'adresse de
 * rappel ni les echanges bruts avec PawaPay ({@link MobileMoneyDepositResponse#forPortal()}).
 * Avant, le membre recevait {@code requestPayloadJson}, {@code providerResponseJson} et
 * {@code callbackUrl}.
 */
@RestControllerAdvice(basePackages = "com.sni.bokaticowork.features.portal")
public class PortalDepositResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        return body instanceof MobileMoneyDepositResponse deposit ? deposit.forPortal() : body;
    }
}
