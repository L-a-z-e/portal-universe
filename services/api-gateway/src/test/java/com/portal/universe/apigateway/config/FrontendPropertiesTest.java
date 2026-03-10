package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FrontendProperties")
class FrontendPropertiesTest {

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("should_parseSchemeHostPort_when_httpsUrl")
        void should_parseSchemeHostPort_when_httpsUrl() {
            FrontendProperties properties = new FrontendProperties();
            properties.setBaseUrl("https://portal-universe:30000");

            properties.init();

            assertThat(properties.getScheme()).isEqualTo("https");
            assertThat(properties.getHost()).contains("portal-universe");
            assertThat(properties.getPort()).isEqualTo(30000);
        }

        @Test
        @DisplayName("should_parseSchemeHostPort_when_httpUrl")
        void should_parseSchemeHostPort_when_httpUrl() {
            FrontendProperties properties = new FrontendProperties();
            properties.setBaseUrl("http://localhost:3000");

            properties.init();

            assertThat(properties.getScheme()).isEqualTo("http");
            assertThat(properties.getHost()).contains("localhost");
            assertThat(properties.getPort()).isEqualTo(3000);
        }
    }

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("should_returnDefaultBaseUrl_when_notSet")
        void should_returnDefaultBaseUrl_when_notSet() {
            FrontendProperties properties = new FrontendProperties();

            assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:30000");
        }

        @Test
        @DisplayName("should_returnDefaultScheme_when_initNotCalled")
        void should_returnDefaultScheme_when_initNotCalled() {
            FrontendProperties properties = new FrontendProperties();

            assertThat(properties.getScheme()).isEqualTo("http");
        }

        @Test
        @DisplayName("should_returnDefaultPort_when_initNotCalled")
        void should_returnDefaultPort_when_initNotCalled() {
            FrontendProperties properties = new FrontendProperties();

            assertThat(properties.getPort()).isEqualTo(30000);
        }
    }
}
