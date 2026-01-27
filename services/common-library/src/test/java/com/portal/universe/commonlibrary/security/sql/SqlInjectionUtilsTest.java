package com.portal.universe.commonlibrary.security.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlInjectionUtils 테스트")
class SqlInjectionUtilsTest {

    @Test
    @DisplayName("SQL 주석 탐지")
    void detectSqlComment() {
        // given
        String input = "username' --";

        // when
        boolean result = SqlInjectionUtils.containsSqlInjection(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UNION SELECT 공격 탐지")
    void detectUnionSelect() {
        // given
        String input = "1' UNION SELECT * FROM users--";

        // when
        boolean result = SqlInjectionUtils.containsSqlInjection(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("OR 1=1 공격 탐지")
    void detectOrCondition() {
        // given
        String input = "admin' OR '1'='1";

        // when
        boolean result = SqlInjectionUtils.containsSqlInjection(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("세미콜론 기반 다중 쿼리 탐지")
    void detectMultipleQueries() {
        // given
        String input = "test'; DROP TABLE users--";

        // when
        boolean result = SqlInjectionUtils.containsSqlInjection(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("SLEEP 함수를 이용한 공격 탐지")
    void detectSleepFunction() {
        // given
        String input = "1' AND SLEEP(5)--";

        // when
        boolean result = SqlInjectionUtils.containsSqlInjection(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("안전한 입력은 통과")
    void safeInput() {
        // given
        String input = "John Doe";

        // when
        boolean result = SqlInjectionUtils.isSafe(input);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정렬 필드명 검증 - 유효한 경우")
    void validSortField() {
        assertThat(SqlInjectionUtils.isSafeSortField("name")).isTrue();
        assertThat(SqlInjectionUtils.isSafeSortField("user_name")).isTrue();
        assertThat(SqlInjectionUtils.isSafeSortField("user.name")).isTrue();
        assertThat(SqlInjectionUtils.isSafeSortField("createdAt")).isTrue();
    }

    @Test
    @DisplayName("정렬 필드명 검증 - 무효한 경우")
    void invalidSortField() {
        assertThat(SqlInjectionUtils.isSafeSortField("name; DROP TABLE")).isFalse();
        assertThat(SqlInjectionUtils.isSafeSortField("name--")).isFalse();
        assertThat(SqlInjectionUtils.isSafeSortField("name OR 1=1")).isFalse();
        assertThat(SqlInjectionUtils.isSafeSortField(null)).isFalse();
        assertThat(SqlInjectionUtils.isSafeSortField("")).isFalse();
    }

    @Test
    @DisplayName("정렬 방향 검증 - 유효한 경우")
    void validSortDirection() {
        assertThat(SqlInjectionUtils.isSafeSortDirection("ASC")).isTrue();
        assertThat(SqlInjectionUtils.isSafeSortDirection("DESC")).isTrue();
        assertThat(SqlInjectionUtils.isSafeSortDirection("asc")).isTrue();
        assertThat(SqlInjectionUtils.isSafeSortDirection("desc")).isTrue();
        assertThat(SqlInjectionUtils.isSafeSortDirection(" ASC ")).isTrue();
    }

    @Test
    @DisplayName("정렬 방향 검증 - 무효한 경우")
    void invalidSortDirection() {
        assertThat(SqlInjectionUtils.isSafeSortDirection("DROP")).isFalse();
        assertThat(SqlInjectionUtils.isSafeSortDirection("ASC; DROP")).isFalse();
        assertThat(SqlInjectionUtils.isSafeSortDirection(null)).isFalse();
        assertThat(SqlInjectionUtils.isSafeSortDirection("")).isFalse();
    }

    @Test
    @DisplayName("입력값 정규화")
    void normalize() {
        // given
        String input = "  hello    world  ";

        // when
        String result = SqlInjectionUtils.normalize(input);

        // then
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    @DisplayName("null과 빈 문자열 처리")
    void handleNullAndEmpty() {
        assertThat(SqlInjectionUtils.containsSqlInjection(null)).isFalse();
        assertThat(SqlInjectionUtils.containsSqlInjection("")).isFalse();
        assertThat(SqlInjectionUtils.isSafe(null)).isTrue();
        assertThat(SqlInjectionUtils.normalize(null)).isNull();
    }
}
