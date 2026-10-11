package io.github.hossein_i.keycloak.starter.phone_auth.sms;

public record SmsMessage(
        String phoneNumber,
        String message
) {
}