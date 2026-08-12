package com.fitagain.global.common.exception;

import com.fitagain.global.common.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final BaseErrorCode Code;

    public CustomException(BaseErrorCode code) {
        this.Code = code;
    }
}