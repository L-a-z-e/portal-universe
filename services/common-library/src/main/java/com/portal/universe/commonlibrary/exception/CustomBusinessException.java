package com.portal.universe.commonlibrary.exception;

import lombok.Getter;

/**
 * 시스템 전반에서 사용될 커스텀 비즈니스 예외 클래스입니다.
 * 서비스 로직에서 예측 가능한 예외 상황이 발생했을 때 사용됩니다.
 * 이 예외는 {@link ErrorCode}를 포함하여, 예외 발생 시 상태 코드, 에러 코드, 메시지를 일관되게 처리할 수 있도록 합니다.
 */
@Getter
public class CustomBusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode를 인자로 받는 생성자입니다.
     * @param errorCode 발생한 예외에 해당하는 ErrorCode Enum 값
     */
    public CustomBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}