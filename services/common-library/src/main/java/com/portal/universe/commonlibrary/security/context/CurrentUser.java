package com.portal.universe.commonlibrary.security.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 메서드 파라미터에 선언하면 {@link GatewayUser}를 자동 주입합니다.
 *
 * <pre>{@code
 * @GetMapping("/me")
 * public ApiResponse<UserResponse> me(@CurrentUser GatewayUser user) {
 *     // user.uuid(), user.name(), user.nickname()
 * }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
