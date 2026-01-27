package com.portal.universe.blogservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * `blog-service`의 모든 통합 테스트를 위한 추상 클래스입니다.
 * 주요 역할:
 * 1. Testcontainers를 사용하여 테스트용 MongoDB 인스턴스를 실행합니다.
 * 2. 외부 시스템(Config Server, Eureka)에 대한 의존성을 비활성화합니다.
 * 3. 모든 통합 테스트 클래스는 이 클래스를 상속받아 일관된 테스트 환경을 공유합니다.
 */
@SpringBootTest
@Testcontainers // Testcontainers 기능을 활성화합니다.
@ContextConfiguration(initializers = IntegrationTest.MongoDbInitializer.class) // 커스텀 설정을 적용하기 위한 Initializer를 등록합니다.
public abstract class IntegrationTest {

    /**
     * 테스트에 사용될 MongoDB 컨테이너입니다.
     * 모든 테스트 클래스에서 동일한 컨테이너 인스턴스를 공유하도록 static으로 선언되었습니다.
     */
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0");

    /**
     * Testcontainers로 실행된 MongoDB 컨테이너의 동적 URI 설정을
     * Spring의 ApplicationContext에 주입하는 역할을 합니다.
     */
    public static class MongoDbInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    // --- 외부 시스템 의존성 비활성화 ---
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",
                    
                    // --- Testcontainers를 사용한 동적 DB 설정 ---
                    "spring.data.mongodb.uri=" + mongoDBContainer.getReplicaSetUrl()
            );
        }
    }
}