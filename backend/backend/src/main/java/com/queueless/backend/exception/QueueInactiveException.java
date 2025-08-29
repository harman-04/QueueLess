package com.queueless.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409
public class QueueInactiveException extends RuntimeException {
    public QueueInactiveException(String message) {
        super(message);
    }
}
