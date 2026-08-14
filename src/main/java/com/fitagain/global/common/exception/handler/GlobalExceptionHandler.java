package com.fitagain.global.common.exception.handler;

import com.fitagain.domain.recommend.exception.TaskException;
import com.fitagain.global.common.CustomResponse;
import com.fitagain.global.common.code.BaseErrorCode;
import com.fitagain.global.common.code.GeneralErrorCode;
import com.fitagain.global.common.exception.CustomException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 애플리케이션에서 발생하는 커스텀 예외를 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CustomResponse<Void>> handleCustomException(CustomException ex) {
        log.warn("[ CustomException ]: {}", ex.getCode().getMessage());
        return ResponseEntity.status(ex.getCode().getHttpStatus())
                .body(ex.getCode().getErrorResponse());
    }

    // TaskException 처리 (CustomException을 상속받지 않으므로 별도 유지 필요)
    @ExceptionHandler(TaskException.class)
    public ResponseEntity<CustomResponse<Object>> handleTaskException(TaskException e) {
        log.warn("[ TaskException ]: {}", e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(CustomResponse.onFailure(e.getCode(), e.getMessage()));
    }

    // @Valid 또는 @Validated 바인딩 에러 발생 시 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("[WARNING] Validation Error : {} ", ex.getMessage());
        String errorMessage = ex.getBindingResult().getFieldError() != null 
                ? ex.getBindingResult().getFieldError().getDefaultMessage() 
                : GeneralErrorCode.VALIDATION_FAILED.getMessage();
        
        CustomResponse<String> errorResponse = CustomResponse.onFailure(
                GeneralErrorCode.VALIDATION_FAILED.getCode(),
                errorMessage,
                null
        );
        return ResponseEntity
                .status(GeneralErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<CustomResponse<String>> handleBindException(BindException ex) {
        log.error("[WARNING] Bind Error : {} ", ex.getMessage());
        CustomResponse<String> errorResponse = CustomResponse.onFailure(
                GeneralErrorCode.VALIDATION_FAILED.getCode(),
                GeneralErrorCode.VALIDATION_FAILED.getMessage(),
                null
        );
        return ResponseEntity
                .status(GeneralErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(errorResponse);
    }

    // 지원하지 않는 HTTP method 호출 할 경우 발생
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CustomResponse<String>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.error("[WARNING] HttpRequestMethodNotSupported : {} ", ex.getMessage());
        BaseErrorCode errorCode = GeneralErrorCode.BAD_REQUEST_400; 
        CustomResponse<String> response = CustomResponse.onFailure(
                errorCode.getCode(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    // 그 외의 정의되지 않은 모든 예외 처리
    @ExceptionHandler({Exception.class})
    public ResponseEntity<CustomResponse<String>> handleAllException(Exception ex) {
        log.error("[WARNING] Internal Server Error : {} ", ex.getMessage());
        ex.printStackTrace(); // 500 에러 추적을 위해 스택트레이스 유지
        BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR_500;
        CustomResponse<String> errorResponse = CustomResponse.onFailure(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(errorResponse);
    }
}