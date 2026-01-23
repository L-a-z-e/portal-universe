package com.portal.universe.authservice.password;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 비밀번호 검증 결과를 담는 클래스입니다.
 */
@Getter
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    /**
     * 검증 성공 결과를 생성합니다.
     */
    public static ValidationResult success() {
        return new ValidationResult(true, new ArrayList<>());
    }

    /**
     * 검증 실패 결과를 생성합니다.
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * 단일 에러로 검증 실패 결과를 생성합니다.
     */
    public static ValidationResult failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors);
    }

    /**
     * 첫 번째 에러 메시지를 반환합니다.
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
}
