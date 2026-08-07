package com.pizzastudio.eum.common.exception;

/**
 * 업무 규칙 위반. 사용자에게 그대로 보여 줄 메시지를 담는다.
 */
public class BusinessMessageException extends RuntimeException {

    public BusinessMessageException(String message) {
        super(message);
    }
}
