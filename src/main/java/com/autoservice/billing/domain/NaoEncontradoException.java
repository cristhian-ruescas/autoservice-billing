package com.autoservice.billing.domain;

public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(final String message) {
        super(message);
    }
}
