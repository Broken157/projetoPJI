package com.portifolio.exception;

public class EmailDeliveryUnavailableException extends RuntimeException {
    public EmailDeliveryUnavailableException() {
        super("A confirmação por e-mail está temporariamente indisponível. Tente novamente mais tarde.");
    }
}
