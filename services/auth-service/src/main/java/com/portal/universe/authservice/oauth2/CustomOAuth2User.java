package com.portal.universe.authservice.oauth2;

import com.portal.universe.authservice.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 인증된 사용자 정보를 담는 클래스입니다.
 * Spring Security의 OAuth2User 인터페이스를 구현하여
 * 소셜 로그인 후 인증된 사용자 정보를 제공합니다.
 */
@Getter
public class CustomOAuth2User implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final List<SimpleGrantedAuthority> authorities;

    public CustomOAuth2User(User user, Map<String, Object> attributes, String nameAttributeKey, List<String> roleKeys) {
        this.user = user;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.authorities = roleKeys.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getUuid() {
        return user.getUuid();
    }
}
