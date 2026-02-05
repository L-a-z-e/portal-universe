package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PublicPathProperties Test")
class PublicPathPropertiesTest {

    @Test
    @DisplayName("기본 생성 시 빈 리스트를 반환한다")
    void should_returnEmptyList_when_default() {
        var props = new PublicPathProperties();

        assertThat(props.getPermitAll()).isEmpty();
        assertThat(props.getPermitAllGet()).isEmpty();
        assertThat(props.getSkipJwtParsing()).isEmpty();
    }

    @Test
    @DisplayName("permitAll 경로를 설정하고 조회할 수 있다")
    void should_setAndGetPermitAll() {
        var props = new PublicPathProperties();
        props.setPermitAll(List.of("/actuator/**", "/fallback/**"));

        assertThat(props.getPermitAll()).containsExactly("/actuator/**", "/fallback/**");
    }

    @Test
    @DisplayName("permitAllGet 경로를 설정하고 조회할 수 있다")
    void should_setAndGetPermitAllGet() {
        var props = new PublicPathProperties();
        props.setPermitAllGet(List.of("/api/v1/blog/**", "/api/v1/shopping/products/**"));

        assertThat(props.getPermitAllGet()).containsExactly("/api/v1/blog/**", "/api/v1/shopping/products/**");
    }

    @Test
    @DisplayName("skipJwtParsing 경로를 설정하고 조회할 수 있다")
    void should_setAndGetSkipJwtParsing() {
        var props = new PublicPathProperties();
        props.setSkipJwtParsing(List.of("/actuator", "/fallback"));

        assertThat(props.getSkipJwtParsing()).containsExactly("/actuator", "/fallback");
    }
}
