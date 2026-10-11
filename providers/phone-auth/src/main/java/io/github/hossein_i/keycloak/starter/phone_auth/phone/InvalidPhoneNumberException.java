package io.github.hossein_i.keycloak.starter.phone_auth.phone;

public final class InvalidPhoneNumberException extends RuntimeException {

    public InvalidPhoneNumberException(String message) {
        super(message);
    }
}