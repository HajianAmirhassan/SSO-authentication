package io.github.hossein_i.keycloak.starter.phone_auth.sms;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public final class SmsProviderSpi
        implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "sms";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SmsProvider.class;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SmsProviderFactory.class;
    }
}
