package com.portal.universe.commonlibrary.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API 실패 응답 시, 에러 정보를 담는 DTO 클래스입니다.
 * {@link ApiResponse} 클래스 내부에서 사용됩니다.
 *
 * <p>필드 설명:</p>
 * <ul>
 *   <li>{@code code} - 에러 코드 (예: "C001", "A001")</li>
 *   <li>{@code message} - 사용자에게 표시할 에러 메시지</li>
 *   <li>{@code timestamp} - 에러 발생 시간 (디버깅 및 로그 추적용)</li>
 *   <li>{@code path} - 에러가 발생한 요청 경로 (선택적)</li>
 *   <li>{@code details} - 필드별 검증 에러 상세 (Validation 에러 시 사용)</li>
 * </ul>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
    private final String path;
    private final List<FieldError> details;

    /**
     * 기본 에러 응답 생성자입니다.
     * 단순한 비즈니스 에러에 사용됩니다.
     *
     * @param code 에러 코드
     * @param message 에러 메시지
     */
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.path = null;
        this.details = null;
    }

    /**
     * 상세 에러 응답 생성자입니다.
     * Validation 에러 등 상세 정보가 필요한 경우 사용됩니다.
     *
     * @param code 에러 코드
     * @param message 에러 메시지
     * @param path 요청 경로
     * @param details 필드별 에러 상세
     */
    public ErrorResponse(String code, String message, String path, List<FieldError> details) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.details = details;
    }

    /**
     * 필드별 검증 에러 정보를 담는 내부 클래스입니다.
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private final String field;
        private final String message;
        private final Object rejectedValue;

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        /**
         * Spring의 FieldError를 변환하는 팩토리 메서드입니다.
         */
        public static FieldError from(org.springframework.validation.FieldError fieldError) {
            return new FieldError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()
            );
        }
    }
}
