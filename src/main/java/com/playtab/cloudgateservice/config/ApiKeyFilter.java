package com.playtab.cloudgateservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class ApiKeyFilter implements Filter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${cloudgate.api-key:}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Only enforce API key on tag endpoint (MCU calls)
        if (path.startsWith("/api/v1/tags") && "POST".equalsIgnoreCase(httpRequest.getMethod())) {
            if (apiKey != null && !apiKey.isBlank()) {
                String providedKey = httpRequest.getHeader(API_KEY_HEADER);
                if (!apiKey.equals(providedKey)) {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.getWriter().write("{\"error\":\"Invalid API key\"}");
                    httpResponse.setContentType("application/json");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }
}
