package com.pizzastudio.eum.common.exception;

/**
 * 요청한 자료가 없을 때.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
