package com.portal.universe.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
@Data
@Component
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = List.of(
            "Authorization", "Content-Type", "Accept", "Origin",
            "X-Requested-With", "Cache-Control"
    );

    private boolean allowCredentials = true;

    private long maxAge = 3600L;
}
