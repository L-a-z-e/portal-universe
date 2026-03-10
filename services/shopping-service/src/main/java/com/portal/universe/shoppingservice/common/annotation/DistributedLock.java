package com.portal.universe.shoppingservice.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 분산 락 어노테이션
 *
 * 사용 예시:
 * <pre>
 * @DistributedLock(key = "'coupon:' + #couponId", waitTime = 5, leaseTime = 10)
 * public void issueCoupon(Long couponId, Long userId) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** SpEL 표현식 지원 */
    String key();

    long waitTime() default 5L;

    /** 자동 해제 */
    long leaseTime() default 10L;

    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
