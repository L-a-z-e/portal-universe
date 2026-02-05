package com.portal.universe.shoppingservice.common.aop;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.annotation.DistributedLock;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockAspectTest {

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private DistributedLockAspect distributedLockAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private RLock rLock;

    private DistributedLock createDistributedLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        return new DistributedLock() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DistributedLock.class;
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public long waitTime() {
                return waitTime;
            }

            @Override
            public long leaseTime() {
                return leaseTime;
            }

            @Override
            public TimeUnit timeUnit() {
                return timeUnit;
            }
        };
    }

    private void setupJoinPoint(String methodName) throws NoSuchMethodException {
        Method method = TestService.class.getMethod(methodName, Long.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L});
    }

    @Test
    @DisplayName("should_proceedAndReleaseLock_when_lockAcquired")
    void should_proceedAndReleaseLock_when_lockAcquired() throws Throwable {
        // given
        setupJoinPoint("testMethod");
        DistributedLock distributedLock = createDistributedLock("'test:' + #id", 5, 10, TimeUnit.SECONDS);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        // when
        Object result = distributedLockAspect.around(joinPoint, distributedLock);

        // then
        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("should_throwException_when_lockNotAcquired")
    void should_throwException_when_lockNotAcquired() throws Throwable {
        // given
        setupJoinPoint("testMethod");
        DistributedLock distributedLock = createDistributedLock("'test:' + #id", 5, 10, TimeUnit.SECONDS);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

        // when/then
        assertThatThrownBy(() -> distributedLockAspect.around(joinPoint, distributedLock))
                .isInstanceOf(CustomBusinessException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("should_releaseLock_when_executionCompletes")
    void should_releaseLock_when_executionCompletes() throws Throwable {
        // given
        setupJoinPoint("testMethod");
        DistributedLock distributedLock = createDistributedLock("'test:' + #id", 5, 10, TimeUnit.SECONDS);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        distributedLockAspect.around(joinPoint, distributedLock);

        // then
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("should_releaseLock_when_exceptionOccurs")
    void should_releaseLock_when_exceptionOccurs() throws Throwable {
        // given
        setupJoinPoint("testMethod");
        DistributedLock distributedLock = createDistributedLock("'test:' + #id", 5, 10, TimeUnit.SECONDS);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Service error"));

        // when/then
        assertThatThrownBy(() -> distributedLockAspect.around(joinPoint, distributedLock))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service error");
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("should_resolveSpELKey_when_expressionProvided")
    void should_resolveSpELKey_when_expressionProvided() throws Throwable {
        // given
        setupJoinPoint("testMethod");
        DistributedLock distributedLock = createDistributedLock("'coupon:' + #id", 5, 10, TimeUnit.SECONDS);

        when(redissonClient.getLock("lock:coupon:1")).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        distributedLockAspect.around(joinPoint, distributedLock);

        // then
        verify(redissonClient).getLock("lock:coupon:1");
    }

    /**
     * Test helper class to provide method signatures for reflection
     */
    public static class TestService {
        public void testMethod(Long id) {
            // test method
        }
    }
}
