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
    private final String[] messageParams;

    /**
     * ErrorCode를 인자로 받는 생성자입니다.
     * @param errorCode 발생한 예외에 해당하는 ErrorCode Enum 값
     */
    public CustomBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.messageParams = new String[0];
    }

    /**
     * ErrorCode와 메시지 파라미터를 인자로 받는 생성자입니다.
     * 에러 메시지에 동적 값을 삽입할 때 사용합니다.
     *
     * @param errorCode 발생한 예외에 해당하는 ErrorCode Enum 값
     * @param messageParams 메시지 포맷팅에 사용될 파라미터들
     */
    public CustomBusinessException(ErrorCode errorCode, String... messageParams) {
        super(formatMessage(errorCode.getMessage(), messageParams));
        this.errorCode = errorCode;
        this.messageParams = messageParams;
    }

    /**
     * 메시지 포맷팅을 수행합니다.
     * {0}, {1} 형태의 플레이스홀더를 파라미터로 치환합니다.
     *
     * @param message 포맷팅할 메시지
     * @param params 치환할 파라미터들
     * @return 포맷팅된 메시지
     */
    private static String formatMessage(String message, String... params) {
        if (params == null || params.length == 0) {
            return message;
        }

        String result = message;
        for (int i = 0; i < params.length; i++) {
            result = result.replace("{" + i + "}", params[i]);
        }
        return result;
    }
}