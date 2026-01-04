//package com.portal.universe.apigateway.config;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.core.Ordered;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.net.InetSocketAddress;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@DisplayName("OidcForwardedHeadersGatewayFilterFactory Unit Test")
//public class OidcForwardedHeadersGatewayFilterFactoryUnitTest {
//
//    private OidcForwardedHeadersGatewayFilterFactory factory;
//
//    @Mock
//    private ServerWebExchange exchange;
//
//    @Mock
//    private GatewayFilterChain chain;
//
//    @Mock
//    private ServerHttpRequest request;
//
//    @Mock
//    private ServerHttpResponse response;
//
//    @Mock
//    private HttpHeaders headers;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        // FrontendProperties 설정
//        FrontendProperties frontendProperties = new FrontendProperties();
//        frontendProperties.setHost("localhost");
//        frontendProperties.setScheme("http");
//        frontendProperties.setPort(30000);
//
//        // Factory 생성
//        factory = new OidcForwardedHeadersGatewayFilterFactory(frontendProperties);
//
//        // Mock 설정
//        when(exchange.getRequest()).thenReturn(request);
//        when(exchange.getResponse()).thenReturn(response);
//        when(request.getHeaders()).thenReturn(headers);
//        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.1.100", 12345));
//        when(chain.filter(any())).thenReturn(Mono.empty());
//    }
//
//    // ===== Factory 테스트 =====
//
//    @Test
//    @DisplayName("Config.class로 부모 초기화 확인")
//    void testFactoryInitialization() {
//        assertNotNull(factory);
//    }
//
//    @Test
//    @DisplayName("apply()는 OidcForwardedHeadersFilter 반환")
//    void testApplyReturnsFilter() {
//        OidcForwardedHeadersGatewayFilterFactory.Config config =
//                new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(true);
//
//        GatewayFilter filter = factory.apply(config);
//
//        assertNotNull(filter);
//    }
//
//    @Test
//    @DisplayName("shortcutFieldOrder()는 'enabled' 반환")
//    void testShortcutFieldOrder() {
//        var fieldOrder = factory.shortcutFieldOrder();
//
//        assertNotNull(fieldOrder);
//        assertEquals(1, fieldOrder.size());
//        assertEquals("enabled", fieldOrder.get(0));
//    }
//
//    @Test
//    @DisplayName("Config 객체 생성 확인")
//    void testConfigCreation() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//
//        assertTrue(config.isEnabled());
//    }
//
//    @Test
//    @DisplayName("Config 객체 생성 with NoArgsConstructor")
//    void testConfigNoArgsConstructor() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//
//        assertNotNull(config);
//        assertTrue(config.isEnabled());
//    }
//
//    @Test
//    @DisplayName("Config 객체 생성 with AllArgsConstructor")
//    void testConfigAllArgsConstructor() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config(false);
//
//        assertNotNull(config);
//        assertFalse(config.isEnabled());
//    }
//
//    @Test
//    @DisplayName("Config의 enabled 필드 set/get")
//    void testConfigEnabledField() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(false);
//
//        assertFalse(config.isEnabled());
//
//        config.setEnabled(true);
//        assertTrue(config.isEnabled());
//    }
//
//    // ===== Filter 테스트 =====
//
//    @Test
//    @DisplayName("enabled=false일 때 필터 스킵")
//    void testFilterDisabled() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(false);
//
//        GatewayFilter filter = factory.apply(config);
//
//        Mono<Void> result = filter.filter(exchange, chain);
//
//        StepVerifier.create(result)
//                .expectComplete()
//                .verify();
//
//        // chain.filter()가 원래 exchange로 호출되는지 확인
//        verify(chain, times(1)).filter(exchange);
//    }
//
//    @Test
//    @DisplayName("enabled=true일 때 X-Forwarded 헤더 추가")
//    void testFilterAddHeaders() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(true);
//
//        GatewayFilter filter = factory.apply(config);
//
//        // Mock 설정: request.mutate()
//        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
//        when(request.mutate()).thenReturn(requestBuilder);
//        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
//        when(requestBuilder.build()).thenReturn(request);
//
//        // Mock 설정: exchange.mutate()
//        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
//        when(exchange.mutate()).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.build()).thenReturn(exchange);
//
//        Mono<Void> result = filter.filter(exchange, chain);
//
//        assertNotNull(result);
//    }
//
//    @Test
//    @DisplayName("getClientIp() - X-Forwarded-For 헤더 없을 때")
//    void testGetClientIpWithoutForwardedFor() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(true);
//
//        when(headers.getFirst("X-Forwarded-For")).thenReturn(null);
//
//        GatewayFilter filter = factory.apply(config);
//
//        // Mock 설정: request.mutate()
//        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
//        when(request.mutate()).thenReturn(requestBuilder);
//        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
//        when(requestBuilder.build()).thenReturn(request);
//
//        // Mock 설정: exchange.mutate()
//        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
//        when(exchange.mutate()).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.build()).thenReturn(exchange);
//
//        Mono<Void> result = filter.filter(exchange, chain);
//
//        StepVerifier.create(result)
//                .expectComplete()
//                .verify();
//    }
//
//    @Test
//    @DisplayName("getClientIp() - X-Forwarded-For 헤더 있을 때")
//    void testGetClientIpWithExistingForwardedFor() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(true);
//
//        when(headers.getFirst("X-Forwarded-For")).thenReturn("10.0.0.1");
//
//        GatewayFilter filter = factory.apply(config);
//
//        // Mock 설정: request.mutate()
//        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
//        when(request.mutate()).thenReturn(requestBuilder);
//        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
//        when(requestBuilder.build()).thenReturn(request);
//
//        // Mock 설정: exchange.mutate()
//        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
//        when(exchange.mutate()).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.build()).thenReturn(exchange);
//
//        Mono<Void> result = filter.filter(exchange, chain);
//
//        StepVerifier.create(result)
//                .expectComplete()
//                .verify();
//    }
//
//    @Test
//    @DisplayName("Ordered 구현 확인 - getOrder()")
//    void testOrderedInterface() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        GatewayFilter filter = factory.apply(config);
//
//        // 필터가 Ordered를 구현했는지 확인
//        assertInstanceOf(Ordered.class, filter);
//        Ordered orderedFilter = (Ordered) filter;
//
//        int order = orderedFilter.getOrder();
//        assertEquals(Ordered.HIGHEST_PRECEDENCE + 10, order);
//    }
//
//    @Test
//    @DisplayName("GatewayFilter 구현 확인")
//    void testGatewayFilterInterface() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        GatewayFilter filter = factory.apply(config);
//
//        assertInstanceOf(GatewayFilter.class, filter);
//    }
//
//    @Test
//    @DisplayName("Filter Chain 처리 확인")
//    void testFilterChainProcessing() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(true);
//
//        GatewayFilter filter = factory.apply(config);
//
//        // Mock 설정: request.mutate()
//        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
//        when(request.mutate()).thenReturn(requestBuilder);
//        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
//        when(requestBuilder.build()).thenReturn(request);
//
//        // Mock 설정: exchange.mutate()
//        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
//        when(exchange.mutate()).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
//        when(exchangeBuilder.build()).thenReturn(exchange);
//
//        filter.filter(exchange, chain);
//
//        // chain.filter()가 호출되었는지 확인
//        verify(chain, times(1)).filter(any());
//    }
//
//    @Test
//    @DisplayName("FrontendProperties 주입 확인")
//    void testFrontendPropertiesInjection() {
//        assertNotNull(factory);
//        var filter = factory.apply(new OidcForwardedHeadersGatewayFilterFactory.Config());
//        assertNotNull(filter);
//    }
//
//    @Test
//    @DisplayName("Multiple Config 객체 생성 독립성")
//    void testMultipleConfigInstances() {
//        var config1 = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        var config2 = new OidcForwardedHeadersGatewayFilterFactory.Config();
//
//        config1.setEnabled(true);
//        config2.setEnabled(false);
//
//        assertTrue(config1.isEnabled());
//        assertFalse(config2.isEnabled());
//    }
//
//    @Test
//    @DisplayName("Config toString() 메서드")
//    void testConfigToString() {
//        var config = new OidcForwardedHeadersGatewayFilterFactory.Config();
//        config.setEnabled(true);
//
//        String str = config.toString();
//        assertNotNull(str);
//        assertTrue(str.contains("enabled"));
//    }
//
//    @Test
//    @DisplayName("Config equals() 메서드")
//    void testConfigEquals() {
//        var config1 = new OidcForwardedHeadersGatewayFilterFactory.Config(true);
//        var config2 = new OidcForwardedHeadersGatewayFilterFactory.Config(true);
//        var config3 = new OidcForwardedHeadersGatewayFilterFactory.Config(false);
//
//        assertEquals(config1, config2);
//        assertNotEquals(config1, config3);
//    }
//
//    @Test
//    @DisplayName("Config hashCode() 메서드")
//    void testConfigHashCode() {
//        var config1 = new OidcForwardedHeadersGatewayFilterFactory.Config(true);
//        var config2 = new OidcForwardedHeadersGatewayFilterFactory.Config(true);
//
//        assertEquals(config1.hashCode(), config2.hashCode());
//    }
//}