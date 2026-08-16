package com.fitagain.domain.recommend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TaskException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public TaskException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static TaskException notFound() {
        return new TaskException("TASK404", "존재하지 않는 작업 ID입니다.", HttpStatus.NOT_FOUND);
    }

    public static TaskException notDiagnosedYet() {
        return new TaskException(
                "TASK400",
                "진단 분석이 완료되지 않아 추천 결과를 요청할 수 없거나 잘못된 작업 ID입니다.",
                HttpStatus.BAD_REQUEST
        );
    }
}