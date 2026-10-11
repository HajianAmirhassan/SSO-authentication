package io.github.hossein_i.keycloak.starter.phone_auth.sms;

import org.keycloak.provider.Provider;

public interface SmsProvider extends Provider {

    void send(SmsMessage message);
}
