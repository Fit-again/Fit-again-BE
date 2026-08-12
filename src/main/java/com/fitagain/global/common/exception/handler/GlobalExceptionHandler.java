package com.fitagain.global.common.exception.handler;

import com.fitagain.domain.recommend.exception.TaskException;
import com.fitagain.global.common.CustomResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskException.class)
    public ResponseEntity<CustomResponse<Object>> handleTaskException(TaskException e) {
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(CustomResponse.onFailure(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponse<Object>> handleUnexpectedException(Exception e) {
        e.printStackTrace(); // 에러 원인 파악을 위한 로그 출력
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomResponse.onFailure("COMMON500", "서버 내부 오류가 발생했습니다."));
    }
}