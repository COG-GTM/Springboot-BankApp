package com.example.bankapp.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Puts a correlation id on every request so audit records can be tied back to the
 * originating interaction across services.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final int MAX_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = sanitize(request.getHeader(CORRELATION_ID_HEADER));
        MDC.put(CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private String sanitize(String headerValue) {
        if (headerValue == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = headerValue.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH || !trimmed.matches("[A-Za-z0-9._:-]+")) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }
}
