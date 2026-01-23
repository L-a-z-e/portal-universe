package com.portal.universe.commonlibrary.security.sql;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@NoSqlInjection 어노테이션 검증 테스트")
class NoSqlInjectionValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("안전한 입력은 통과")
    void validInput() {
        // given
        TestDto dto = new TestDto("John Doe");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("SQL 주석이 있으면 실패")
    void invalidInputWithComment() {
        // given
        TestDto dto = new TestDto("admin' --");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("SQL Injection 위험 패턴이 감지되었습니다");
    }

    @Test
    @DisplayName("UNION SELECT가 있으면 실패")
    void invalidInputWithUnion() {
        // given
        TestDto dto = new TestDto("1' UNION SELECT * FROM users");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("OR 1=1이 있으면 실패")
    void invalidInputWithOrCondition() {
        // given
        TestDto dto = new TestDto("admin' OR '1'='1");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("DROP TABLE이 있으면 실패")
    void invalidInputWithDrop() {
        // given
        TestDto dto = new TestDto("test'; DROP TABLE users--");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("null 값은 통과")
    void nullValueIsValid() {
        // given
        TestDto dto = new TestDto(null);

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열은 통과")
    void emptyStringIsValid() {
        // given
        TestDto dto = new TestDto("");

        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    // 테스트용 DTO
    record TestDto(@NoSqlInjection String value) {
    }
}
