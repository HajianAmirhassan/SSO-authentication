package io.github.hossein_i.keycloak.starter.phone_auth.sms;

import org.jboss.logging.Logger;

/**
 * Development-only provider: writes the message (including the OTP) to the
 * Keycloak log instead of sending it. Replace with a real gateway provider
 * before going to production.
 */
public final class ConsoleSmsProvider
        implements SmsProvider {

    private static final Logger LOG =
            Logger.getLogger(ConsoleSmsProvider.class);

    @Override
    public void send(SmsMessage message) {
        LOG.warnf(
                "[console-sms] DEV ONLY | phone=%s | message=%s",
                message.phoneNumber(),
                message.message()
        );
    }

    @Override
    public void close() {
    }
}
