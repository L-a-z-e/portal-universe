package com.portal.universe.commonlibrary.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 모든 API 응답을 위한 표준 래퍼(Wrapper) 클래스입니다.
 * API 응답의 일관성을 유지하기 위해 사용됩니다.
 * @param <T> 응답 데이터의 타입
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // JSON으로 변환 시, null 값을 가진 필드는 제외합니다.
public class ApiResponse<T> {

    private final boolean success; // 요청 성공 여부
    private final T data;          // 성공 시 반환될 데이터
    private final ErrorResponse error; // 실패 시 반환될 에러 정보

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /**
     * 성공 응답을 생성하는 정적 팩토리 메서드입니다.
     * @param data 클라이언트에게 전달할 데이터
     * @param <T> 데이터의 타입
     * @return 데이터가 포함된 성공 ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 실패 응답을 생성하는 정적 팩토리 메서드입니다.
     * @param code 에러 코드
     * @param message 에러 메시지
     * @param <T> 데이터의 타입 (실패 시에는 항상 null)
     * @return 에러 정보가 포함된 실패 ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }
}