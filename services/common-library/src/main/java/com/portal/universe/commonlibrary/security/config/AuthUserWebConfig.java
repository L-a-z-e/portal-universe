package com.portal.universe.commonlibrary.security.config;

import com.portal.universe.commonlibrary.security.context.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * {@link CurrentUserArgumentResolver}를 Spring MVC에 등록하는 auto-configuration.
 */
@Configuration
public class AuthUserWebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
