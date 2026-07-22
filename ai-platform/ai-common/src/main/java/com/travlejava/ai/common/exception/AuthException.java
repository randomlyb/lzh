package com.travlejava.ai.common.exception;

import com.travlejava.ai.common.response.ResponseEnum;

public class AuthException extends BaseException {
    public AuthException(ResponseEnum responseEnum) {
        super(responseEnum);
    }

    public AuthException(ResponseEnum responseEnum, String message) {
        super(responseEnum, message);
    }
}
