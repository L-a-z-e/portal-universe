package com.portal.universe.commonlibrary.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // Null인 필드는 JSON에서 제외
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }
}
