package com.autoservice.billing.domain;

public class NegocioException extends RuntimeException {

    public NegocioException(final String message) {
        super(message);
    }
}
