package io.github.hossein_i.keycloak.starter.phone_auth.sms;

public final class SmsException extends RuntimeException {

    public SmsException(String message) {
        super(message);
    }

    public SmsException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}