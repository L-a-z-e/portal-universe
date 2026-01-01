package com.portal.universe.authservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver.client.portal-client")
public class OAuth2ClientProperties {

    private List<String> redirectUris = new ArrayList<>();

    private List<String> postLogoutRedirectUris = new ArrayList<>();

}
