package com.portal.universe.commonlibrary.security.context;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUser} 어노테이션이 붙은 파라미터에
 * request attribute에서 {@link GatewayUser}를 꺼내 주입하는 resolver.
 *
 * {@link com.portal.universe.commonlibrary.security.filter.GatewayAuthenticationFilter}에서
 * request attribute "gatewayUser"에 미리 저장합니다.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String GATEWAY_USER_ATTRIBUTE = "gatewayUser";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && GatewayUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        return webRequest.getAttribute(GATEWAY_USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    }
}
