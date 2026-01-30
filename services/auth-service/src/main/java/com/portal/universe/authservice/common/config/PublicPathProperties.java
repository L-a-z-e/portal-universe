package com.portal.universe.authservice.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT 필터가 건너뛸 공개 경로를 외부 설정으로 관리합니다.
 * application.yml의 auth.public-paths 설정과 매핑됩니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.public-paths")
public class PublicPathProperties {

    /**
     * JWT 파싱을 건너뛸 경로 prefix 목록
     */
    private List<String> skipJwtParsing = new ArrayList<>(List.of(
            "/api/auth/",
            "/api/v1/auth/",
            "/oauth2/",
            "/login/oauth2/",
            "/.well-known/",
            "/actuator/",
            "/api/users/signup",
            "/api/v1/users/signup"
    ));

    /**
     * JWT 파싱을 건너뛸 exact match 경로 목록
     */
    private List<String> skipJwtParsingExact = new ArrayList<>(List.of(
            "/ping",
            "/login",
            "/logout"
    ));
}
