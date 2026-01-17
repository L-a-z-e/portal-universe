#!/bin/bash

echo "=== 1. Auth Service에 실제로 전달된 Host 헤더 확인 ==="
docker-compose exec -T auth-service sh -c 'cat > /tmp/test.jsp << "EOL"
package com.test;
import javax.servlet.http.HttpServletRequest;
public class DebugFilter implements javax.servlet.Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        System.out.println("=== REQUEST DEBUG ===");
        System.out.println("Host header: " + httpReq.getHeader("Host"));
        System.out.println("X-Forwarded-Host: " + httpReq.getHeader("X-Forwarded-Host"));
        System.out.println("X-Forwarded-Proto: " + httpReq.getHeader("X-Forwarded-Proto"));
        System.out.println("X-Forwarded-Port: " + httpReq.getHeader("X-Forwarded-Port"));
        System.out.println("request.getServerName(): " + httpReq.getServerName());
        System.out.println("request.getServerPort(): " + httpReq.getServerPort());
        chain.doFilter(req, res);
    }
}
EOL
'

echo ""
echo "=== 2. Gateway가 실제로 보내는 헤더 확인 (Zipkin 추적) ==="
curl -s "http://localhost:9411/api/v2/traces?serviceName=api-gateway&limit=1" 2>/dev/null | head -100

echo ""
echo "=== 3. Auth Service 로그에서 실제 request 정보 확인 ==="
docker-compose logs auth-service 2>&1 | grep -i "host\|forward" | tail -20

echo ""
echo "=== 4. Gateway에서 auth-service로 프록시할 때 헤더 변환 확인 ==="

docker-compose logs api-gateway 2>&1 | grep -i "forward\|host\|auth-service" | tail -30
