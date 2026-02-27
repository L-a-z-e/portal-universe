package com.portal.universe.commonlibrary.security.filter;

import com.portal.universe.commonlibrary.security.constants.AuthConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 서비스 간 내부 API 호출을 인증하는 필터입니다.
 * GatewayAuthenticationFilter가 외부(Gateway → Service) 흐름을 담당한다면,
 * 이 필터는 내부(Service → Service) 흐름을 담당합니다.
 *
 * X-Internal-Token 헤더의 공유 시크릿을 검증하여 인증합니다.
 */
public class InternalTokenAuthFilter extends OncePerRequestFilter {

    private final String expectedToken;

    public InternalTokenAuthFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader(AuthConstants.Headers.INTERNAL_TOKEN);

        if (expectedToken.equals(token)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("INTERNAL_SERVICE", null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
        }

        filterChain.doFilter(request, response);
    }
}
