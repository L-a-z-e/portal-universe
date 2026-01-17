package com.portal.universe.shoppingservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MySQLContainer;

/**
 * `shopping-service`의 모든 통합 테스트를 위한 추상 базовый(base) 클래스입니다.
 * 주요 역할:
 * 1. Testcontainers를 사용하여 테스트용 MySQL 데이터베이스 인스턴스를 실행합니다.
 * 2. 외부 시스템(Config Server, Eureka)에 대한 의존성을 비활성화합니다.
 * 3. 모든 통합 테스트 클래스는 이 클래스를 상속받아 일관된 테스트 환경을 공유합니다.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class) // 커스텀 설정을 적용하기 위한 Initializer를 등록합니다.
public abstract class IntegrationTest {

    /**
     * 테스트에 사용될 MySQL 데이터베이스 컨테이너입니다.
     * 모든 테스트 클래스에서 동일한 컨테이너 인스턴스를 공유하도록 static으로 선언되었습니다.
     * static 블록에서 명시적으로 시작하여 JUnit lifecycle과 독립적으로 관리합니다.
     */
    private static final MySQLContainer<?> mySQLContainer;

    static {
        mySQLContainer = new MySQLContainer<>("mysql:8.0");
        mySQLContainer.start();
    }

    /**
     * Testcontainers로 실행된 MySQL 컨테이너의 동적 설정을
     * Spring의 ApplicationContext에 주입하는 역할을 합니다.
     */
    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            // Testcontainer가 동적으로 생성한 DB 접속 정보를 Spring 환경 변수에 추가합니다.
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    // --- 외부 시스템 의존성 비활성화 ---
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",
                    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                    "spring.kafka.bootstrap-servers=localhost:9092",

                    // --- Feign Client Mock URL ---
                    "feign.blog-service.url=http://localhost:8082",

                    // --- JPA 설정 ---
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.flyway.enabled=false",

                    // --- Testcontainers를 사용한 동적 DB 설정 ---
                    "spring.datasource.url=" + mySQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mySQLContainer.getUsername(),
                    "spring.datasource.password=" + mySQLContainer.getPassword()
            );
        }
    }
}