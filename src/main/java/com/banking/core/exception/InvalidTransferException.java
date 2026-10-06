package com.banking.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Geçersiz transfer girişimi yapıldığında fırlatılır
 * (örn. aynı hesaba transfer, negatif tutar, farklı para birimi).
 * HTTP 400 Bad Request döner.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTransferException extends RuntimeException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
