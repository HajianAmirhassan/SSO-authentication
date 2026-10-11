package io.github.hossein_i.keycloak.starter.phone_auth.sms;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public final class ConsoleSmsProviderFactory
        implements SmsProviderFactory {

    public static final String PROVIDER_ID = "console";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public SmsProvider create(KeycloakSession session) {
        return new ConsoleSmsProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
