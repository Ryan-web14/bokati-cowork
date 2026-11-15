package com.sni.bokaticowork.core.observability;

import com.sni.bokaticowork.security.ratelimit.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Premier filtre de chaque requete (lot A0) : identifiant de requete et adresse reelle du client,
 * poses dans la requete, le MDC et la reponse ({@code X-Request-Id}), retires a la fin.
 */
@Component("bokatiRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestContextFilter extends OncePerRequestFilter {

    private final ClientIpResolver clientIpResolver;

    public RequestContextFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = RequestContext.requestIdFrom(request.getHeader(RequestContext.HEADER));
        String clientIp = clientIpResolver.resolve(request);
        request.setAttribute(RequestContext.ATTRIBUTE_REQUEST_ID, requestId);
        request.setAttribute(RequestContext.ATTRIBUTE_CLIENT_IP, clientIp);
        response.setHeader(RequestContext.HEADER, requestId);
        MDC.put(RequestContext.MDC_REQUEST_ID, requestId);
        MDC.put(RequestContext.MDC_CLIENT_IP, clientIp);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(RequestContext.MDC_REQUEST_ID);
            MDC.remove(RequestContext.MDC_CLIENT_IP);
            MDC.remove(RequestContext.MDC_USER);
        }
    }
}
