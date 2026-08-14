package com.fitagain.global.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class CustomResponse<T> {

    @JsonProperty("isSuccess")
    private Boolean isSuccess;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("result")
    private final T result;

    // 기본적으로 200 OK를 사용하는 성공 응답 생성 메서드
    public static <T> CustomResponse<T> onSuccess(T result) {
        return new CustomResponse<>(true, String.valueOf(HttpStatus.OK.value()), HttpStatus.OK.getReasonPhrase(), result);
    }

    // 상태 코드를 받아서 사용하는 성공 응답 생성 메서드
    public static <T> CustomResponse<T> onSuccess(HttpStatus status, T result) {
        return new CustomResponse<>(true, String.valueOf(status.value()), status.getReasonPhrase(), result);
    }

    // 커스텀 코드와 메시지를 받아서 사용하는 성공 응답 생성 메서드
    public static <T> CustomResponse<T> onSuccess(String code, String message, T result) {
        return new CustomResponse<>(true, code, message, result);
    }

    // 실패 응답 생성 메서드 (데이터 포함)
    public static <T> CustomResponse<T> onFailure(String code, String message, T result) {
        return new CustomResponse<>(false, code, message, result);
    }

    // 실패 응답 생성 메서드 (데이터 없음)
    public static <T> CustomResponse<T> onFailure(String code, String message) {
        return new CustomResponse<>(false, code, message, null);
    }
}