package com.portal.universe.shoppingservice.support;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

/**
 * WebMvcTest 전용 설정 클래스.
 * ShoppingServiceApplication의 @ComponentScan과 @EnableFeignClients가
 * @WebMvcTest의 TypeExcludeFilter를 무력화하는 문제를 우회하기 위해 사용합니다.
 *
 * 컨트롤러 테스트에서 @ContextConfiguration(classes = {WebMvcTestConfig.class, XxxController.class})로 사용합니다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        RedisAutoConfiguration.class
})
public class WebMvcTestConfig {
}
