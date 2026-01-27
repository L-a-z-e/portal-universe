package com.portal.universe.commonlibrary.security.filter;

import com.portal.universe.commonlibrary.security.xss.XssUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 모든 요청 파라미터에 자동으로 XSS 필터링을 적용하는 필터입니다.
 * HttpServletRequestWrapper를 사용하여 파라미터 값을 정제합니다.
 *
 * <p>주의: 이 필터는 선택적으로 사용하며, @NoXss 어노테이션을 선호합니다.</p>
 * <p>필터를 활성화하려면 이 클래스의 @Component 어노테이션 주석을 해제하세요.</p>
 */
//@Component  // 주석을 제거하면 필터 활성화
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            XssRequestWrapper wrappedRequest = new XssRequestWrapper(httpRequest);
            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * HttpServletRequestWrapper를 확장하여 파라미터 값을 XSS 필터링합니다.
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            return Arrays.stream(values)
                    .map(this::sanitize)
                    .toArray(String[]::new);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> originalMap = super.getParameterMap();
            return originalMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> Arrays.stream(entry.getValue())
                                    .map(this::sanitize)
                                    .toArray(String[]::new)
                    ));
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitize(value);
        }

        /**
         * 값을 XSS 필터링합니다.
         * null이거나 빈 문자열은 그대로 반환합니다.
         */
        private String sanitize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            // XSS 위험 패턴이 있으면 태그를 제거
            if (XssUtils.containsXssPattern(value)) {
                return XssUtils.stripTags(value);
            }

            return value;
        }
    }
}
