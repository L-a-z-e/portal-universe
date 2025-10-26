package com.portal.universe.commonlibrary.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 서비스에서 사용될 오류 코드의 공통 규약(Interface)입니다.
 * 각 서비스의 오류 코드 Enum은 이 인터페이스를 구현해야 합니다.
 */
public interface ErrorCode {
    /**
     * 오류에 해당하는 HTTP 상태 코드를 반환합니다.
     * @return HttpStatus
     */
    HttpStatus getStatus();

    /**
     * 애플리케이션 내에서 오류를 식별하기 위한 고유 코드(예: "C001", "A001")를 반환합니다.
     * @return 오류 코드 문자열
     */
    String getCode();

    /**
     * 클라이언트에게 보여줄 오류 메시지를 반환합니다.
     * @return 오류 메시지 문자열
     */
    String getMessage();
}
