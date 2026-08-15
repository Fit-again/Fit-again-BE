package com.fitagain.domain.consultation.exception;

import com.fitagain.global.common.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ConsultationErrorCode implements BaseErrorCode {
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "CONSULT404_1", "존재하지 않는 작업(Task) ID입니다."),
    ALREADY_EXISTS(HttpStatus.CONFLICT, "CONSULT409_1", "해당 작업에 대해 이미 상담 신청 내역이 존재합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
