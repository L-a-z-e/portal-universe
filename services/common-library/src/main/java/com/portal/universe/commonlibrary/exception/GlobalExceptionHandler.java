package com.portal.universe.commonlibrary.exception;

import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 애플리케이션 전역에서 발생하는 예외를 처리하는 핸들러 클래스입니다.
 * {@code @RestControllerAdvice} 어노테이션을 통해 모든 {@code @RestController}에서 발생하는 예외를 가로채
 * 일관된 형식의 {@link ApiResponse}로 변환하여 클라이언트에게 반환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 직접 정의한 비즈니스 예외({@link CustomBusinessException})를 처리합니다.
     * 가장 구체적인 예외이므로 최우선으로 처리됩니다.
     * @param e 발생한 CustomBusinessException 객체
     * @return ErrorCode에 정의된 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(CustomBusinessException.class)
    protected ResponseEntity<ApiResponse<Object>> handleCustomBusinessException(CustomBusinessException e) {
        log.error("handleCustomBusinessException: {}", e.getErrorCode().getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Object> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * 잘못된 URL 요청 시 발생하는 {@link NoResourceFoundException}을 처리합니다.
     * @param e 발생한 NoResourceFoundException 객체
     * @return 404 Not Found 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("handleNoResourceFoundException: {}", e.getMessage());
        ErrorCode errorCode = CommonErrorCode.NOT_FOUND;
        ApiResponse<Object> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    /**
     * 위에서 처리되지 않은 모든 종류의 예외({@link Exception})를 처리합니다.
     * 가장 마지막에 실행되는 최종 예외 핸들러입니다.
     * @param e 발생한 Exception 객체
     * @return 500 Internal Server Error 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("handleException: {}", e.getMessage(), e);
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<Object> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }
}
