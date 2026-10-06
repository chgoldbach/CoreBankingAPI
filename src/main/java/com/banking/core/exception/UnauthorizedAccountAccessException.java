package com.banking.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Kullanıcı başkasına ait kaynağa erişmeye çalıştığında fırlatılır.
 * HTTP 403 Forbidden döner.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAccountAccessException extends RuntimeException {

    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
