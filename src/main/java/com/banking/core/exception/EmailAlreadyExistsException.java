package com.banking.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Kayıt sırasında e-posta adresi zaten kullanılıyorsa fırlatılır.
 * HTTP 409 Conflict döner.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super(String.format("'%s' e-posta adresi zaten kayıtlı", email));
    }
}
