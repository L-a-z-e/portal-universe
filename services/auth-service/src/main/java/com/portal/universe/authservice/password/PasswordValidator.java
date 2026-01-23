package com.portal.universe.authservice.password;

import com.portal.universe.authservice.domain.User;

/**
 * 비밀번호 정책 검증을 담당하는 인터페이스입니다.
 */
public interface PasswordValidator {

    /**
     * 비밀번호의 기본 정책을 검증합니다.
     * (길이, 복잡도, 연속 문자 등)
     *
     * @param password 검증할 비밀번호
     * @return 검증 결과
     */
    ValidationResult validate(String password);

    /**
     * 사용자 정보를 포함한 비밀번호 정책을 검증합니다.
     * (기본 정책 + 사용자 정보 포함 여부 + 이전 비밀번호 재사용 여부)
     *
     * @param password 검증할 비밀번호
     * @param user 사용자 엔티티
     * @return 검증 결과
     */
    ValidationResult validate(String password, User user);

    /**
     * 사용자의 비밀번호가 만료되었는지 확인합니다.
     *
     * @param user 사용자 엔티티
     * @return 만료 여부
     */
    boolean isExpired(User user);
}
