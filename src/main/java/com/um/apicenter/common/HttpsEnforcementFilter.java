package com.um.apicenter.common;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HttpsEnforcementFilter implements Filter {

    private final boolean requireHttps;

    public HttpsEnforcementFilter(@Value("${um.security.require-https:true}") boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (!requireHttps || !httpRequest.getRequestURI().startsWith("/api/") || httpRequest.isSecure()) {
            chain.doFilter(request, response);
            return;
        }

        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.setContentType("application/json;charset=UTF-8");
        httpResponse.getWriter().write("{\"request_id\":\"" + requestId
                + "\",\"error\":{\"code\":\"HTTPS_REQUIRED\",\"message\":\"仅接受 HTTPS 请求\"}}");
    }
}
