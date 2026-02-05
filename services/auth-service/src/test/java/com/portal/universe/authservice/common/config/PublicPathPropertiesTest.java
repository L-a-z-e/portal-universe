package com.portal.universe.authservice.common.config;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PublicPathProperties Test")
class PublicPathPropertiesTest {

    private PublicPathProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PublicPathProperties();
    }

    @Test
    @DisplayName("기본 skipJwtParsing prefix 경로를 포함한다")
    void should_containDefaultSkipPaths() {
        assertThat(properties.getSkipJwtParsing())
                .contains("/api/auth/", "/api/v1/auth/", "/oauth2/", "/actuator/");
    }

    @Test
    @DisplayName("기본 skipJwtParsingExact 경로를 포함한다")
    void should_containDefaultExactPaths() {
        assertThat(properties.getSkipJwtParsingExact())
                .contains("/ping", "/login", "/logout");
    }

    @Test
    @DisplayName("사용자 정의 skipJwtParsing 경로를 설정할 수 있다")
    void should_allowCustomSkipPaths() {
        properties.setSkipJwtParsing(List.of("/custom/path/"));

        assertThat(properties.getSkipJwtParsing()).containsExactly("/custom/path/");
    }

    @Test
    @DisplayName("사용자 정의 skipJwtParsingExact 경로를 설정할 수 있다")
    void should_allowCustomExactPaths() {
        properties.setSkipJwtParsingExact(List.of("/custom-exact"));

        assertThat(properties.getSkipJwtParsingExact()).containsExactly("/custom-exact");
    }
}
