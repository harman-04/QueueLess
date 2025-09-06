package com.queueless.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyInQueueException extends RuntimeException {
    public UserAlreadyInQueueException(String message) {
        super(message);
    }
}