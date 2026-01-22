package com.portal.universe.commonlibrary.exception;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 애플리케이션 전역에서 발생하는 예외를 처리하는 핸들러 클래스입니다.
 * {@code @RestControllerAdvice} 어노테이션을 통해 모든 {@code @RestController}에서 발생하는 예외를 가로채
 * 일관된 형식의 {@link ApiResponse}로 변환하여 클라이언트에게 반환합니다.
 *
 * <p>처리 우선순위:</p>
 * <ol>
 *   <li>CustomBusinessException - 비즈니스 로직 예외</li>
 *   <li>MethodArgumentNotValidException - @Valid 검증 실패</li>
 *   <li>ConstraintViolationException - @Validated 검증 실패</li>
 *   <li>HttpMessageNotReadableException - JSON 파싱 오류</li>
 *   <li>MissingServletRequestParameterException - 필수 파라미터 누락</li>
 *   <li>MethodArgumentTypeMismatchException - 파라미터 타입 불일치</li>
 *   <li>IllegalArgumentException - 잘못된 인자</li>
 *   <li>IllegalStateException - 잘못된 상태</li>
 *   <li>NoResourceFoundException - 리소스 없음 (404)</li>
 *   <li>Exception - 기타 모든 예외 (500)</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 직접 정의한 비즈니스 예외({@link CustomBusinessException})를 처리합니다.
     * 가장 구체적인 예외이므로 최우선으로 처리됩니다.
     *
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
     * @Valid 어노테이션을 통한 RequestBody 검증 실패 시 발생하는 예외를 처리합니다.
     *
     * @param e 발생한 MethodArgumentNotValidException 객체
     * @param request HTTP 요청 객체 (경로 정보 추출용)
     * @return 400 Bad Request 상태 코드와 필드별 에러 상세를 포함한 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        log.warn("handleMethodArgumentNotValidException: {}", e.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(ErrorResponse.FieldError::from)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                CommonErrorCode.INVALID_INPUT_VALUE.getMessage(),
                request.getRequestURI(),
                fieldErrors
        );

        return new ResponseEntity<>(
                ApiResponse.errorWithDetails(errorResponse),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * @Validated 어노테이션을 통한 경로변수/쿼리파라미터 검증 실패 시 발생하는 예외를 처리합니다.
     *
     * @param e 발생한 ConstraintViolationException 객체
     * @param request HTTP 요청 객체 (경로 정보 추출용)
     * @return 400 Bad Request 상태 코드와 검증 위반 상세를 포함한 ResponseEntity
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request) {
        log.warn("handleConstraintViolationException: {}", e.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = e.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldError(
                        extractFieldName(violation),
                        violation.getMessage(),
                        violation.getInvalidValue()
                ))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                CommonErrorCode.INVALID_INPUT_VALUE.getMessage(),
                request.getRequestURI(),
                fieldErrors
        );

        return new ResponseEntity<>(
                ApiResponse.errorWithDetails(errorResponse),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * JSON 파싱 오류 시 발생하는 예외를 처리합니다.
     *
     * @param e 발생한 HttpMessageNotReadableException 객체
     * @return 400 Bad Request 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.warn("handleHttpMessageNotReadableException: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                "Invalid request body format"
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 필수 요청 파라미터가 누락된 경우 발생하는 예외를 처리합니다.
     *
     * @param e 발생한 MissingServletRequestParameterException 객체
     * @return 400 Bad Request 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("handleMissingServletRequestParameterException: {}", e.getMessage());
        String message = String.format("Required parameter '%s' is missing", e.getParameterName());
        ApiResponse<Object> response = ApiResponse.error(
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                message
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 요청 파라미터 타입이 일치하지 않는 경우 발생하는 예외를 처리합니다.
     *
     * @param e 발생한 MethodArgumentTypeMismatchException 객체
     * @return 400 Bad Request 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("handleMethodArgumentTypeMismatchException: {}", e.getMessage());
        String message = String.format("Parameter '%s' should be of type '%s'",
                e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        ApiResponse<Object> response = ApiResponse.error(
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                message
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 잘못된 인자가 전달된 경우 발생하는 예외를 처리합니다.
     * 주로 비즈니스 로직에서 검증 실패 시 발생합니다.
     *
     * @param e 발생한 IllegalArgumentException 객체
     * @return 400 Bad Request 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("handleIllegalArgumentException: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                e.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 잘못된 상태에서 작업이 수행된 경우 발생하는 예외를 처리합니다.
     * 주로 권한 부족이나 상태 전이 오류 시 발생합니다.
     *
     * @param e 발생한 IllegalStateException 객체
     * @return 403 Forbidden 상태 코드와 메시지를 포함한 ResponseEntity
     */
    @ExceptionHandler(IllegalStateException.class)
    protected ResponseEntity<ApiResponse<Object>> handleIllegalStateException(IllegalStateException e) {
        log.warn("handleIllegalStateException: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                CommonErrorCode.FORBIDDEN.getCode(),
                e.getMessage()
        );
        return new ResponseEntity<>(response, CommonErrorCode.FORBIDDEN.getStatus());
    }

    /**
     * 잘못된 URL 요청 시 발생하는 {@link NoResourceFoundException}을 처리합니다.
     *
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
     *
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

    /**
     * ConstraintViolation에서 필드명을 추출합니다.
     * 경로에서 마지막 부분(실제 필드명)만 추출합니다.
     */
    private String extractFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDotIndex = propertyPath.lastIndexOf('.');
        return lastDotIndex >= 0 ? propertyPath.substring(lastDotIndex + 1) : propertyPath;
    }
}
