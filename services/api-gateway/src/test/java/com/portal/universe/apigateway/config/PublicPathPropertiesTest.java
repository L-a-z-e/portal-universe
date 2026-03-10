package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PublicPathProperties")
class PublicPathPropertiesTest {

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("should_returnEmptyLists_when_default")
        void should_returnEmptyLists_when_default() {
            var props = new PublicPathProperties();

            assertThat(props.getPermitAll()).isEmpty();
            assertThat(props.getPermitAllGet()).isEmpty();
            assertThat(props.getSkipJwtParsing()).isEmpty();
        }
    }

    @Nested
    @DisplayName("setters and getters")
    class SettersAndGetters {

        @Test
        @DisplayName("should_setAndGetPermitAll")
        void should_setAndGetPermitAll() {
            var props = new PublicPathProperties();
            props.setPermitAll(List.of("/actuator/**", "/fallback/**"));

            assertThat(props.getPermitAll()).containsExactly("/actuator/**", "/fallback/**");
        }

        @Test
        @DisplayName("should_setAndGetPermitAllGet")
        void should_setAndGetPermitAllGet() {
            var props = new PublicPathProperties();
            props.setPermitAllGet(List.of("/api/v1/blog/**", "/api/v1/shopping/products/**"));

            assertThat(props.getPermitAllGet()).containsExactly("/api/v1/blog/**", "/api/v1/shopping/products/**");
        }

        @Test
        @DisplayName("should_setAndGetSkipJwtParsing")
        void should_setAndGetSkipJwtParsing() {
            var props = new PublicPathProperties();
            props.setSkipJwtParsing(List.of("/actuator", "/fallback"));

            assertThat(props.getSkipJwtParsing()).containsExactly("/actuator", "/fallback");
        }
    }
}
