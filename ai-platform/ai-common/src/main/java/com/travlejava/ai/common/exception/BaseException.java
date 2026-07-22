package com.travlejava.ai.common.exception;

import com.travlejava.ai.common.response.ResponseEnum;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final Integer responseCode;
    private final String responseMsg;

    public BaseException(ResponseEnum responseEnum) {
        super(responseEnum.getMessage());
        this.responseCode = responseEnum.getCode();
        this.responseMsg = responseEnum.getMessage();
    }

    public BaseException(ResponseEnum responseEnum, String message) {
        super(message);
        this.responseCode = responseEnum.getCode();
        this.responseMsg = message;
    }
}
