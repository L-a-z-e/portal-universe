package com.portal.universe.blogservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * BlogServiceApplication의 기본 통합 테스트 클래스입니다.
 * {@link IntegrationTest}를 상속받아 테스트 환경을 구성합니다.
 * MongoDB Testcontainer를 필요로 하므로 단위 테스트 실행 시에는 비활성화합니다.
 */
@Disabled("Integration test requiring MongoDB Testcontainer")
class BlogServiceApplicationTests extends IntegrationTest {

    /**
     * Spring 애플리케이션 컨텍스트가 성공적으로 로드되는지 확인하는 간단한 테스트입니다.
     * 이 테스트가 통과하면, 애플리케이션의 기본 설정과 의존성 주입이 올바르게 구성되었음을 의미합니다.
     */
    @Test
    void contextLoads() {
    }

}