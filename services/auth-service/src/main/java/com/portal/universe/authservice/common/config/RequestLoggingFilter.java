package com.portal.universe.authservice.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        log.info("========== REQUEST ==========");
        log.info("URL: {}", httpRequest.getRequestURL());
        log.info("Method: {}", httpRequest.getMethod());
        log.info("Host header: {}", httpRequest.getHeader("Host"));
        log.info("X-Forwarded-Host: {}", httpRequest.getHeader("X-Forwarded-Host"));
        log.info("X-Forwarded-Proto: {}", httpRequest.getHeader("X-Forwarded-Proto"));
        log.info("X-Forwarded-Port: {}", httpRequest.getHeader("X-Forwarded-Port"));
        log.info("X-Forwarded-For: {}", httpRequest.getHeader("X-Forwarded-For"));
        log.info("============================");

        chain.doFilter(request, response);
    }
}