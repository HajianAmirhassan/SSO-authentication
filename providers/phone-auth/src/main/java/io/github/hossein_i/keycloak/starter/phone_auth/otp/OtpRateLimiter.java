package io.github.hossein_i.keycloak.starter.phone_auth.otp;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;

/**
 * Per phone number SMS limit that holds across authentication sessions
 * (the cooldown in {@link OtpSessionService} only covers one session, so a
 * new login tab would otherwise allow unlimited SMS to the same number).
 * Backed by Keycloak's single-use object store, so it is shared by all
 * nodes of a cluster.
 */
public final class OtpRateLimiter {

    private static final String KEY_PREFIX = "phone-auth:";

    private final SingleUseObjectProvider store;

    public OtpRateLimiter(KeycloakSession session) {
        this.store = session.singleUseObjects();
    }

    /**
     * Reserves one send for the number. Returns false when the number is
     * still in cooldown or has used all of its hourly slots.
     */
    public boolean tryAcquire(String normalizedPhone) {
        if (!store.putIfAbsent(
                KEY_PREFIX + "cooldown:" + normalizedPhone,
                OtpConstants.OTP_RESEND_COOLDOWN_SECONDS
        )) {
            return false;
        }

        // Each slot is an entry that lives one hour; the first free slot wins
        for (int slot = 0; slot < OtpConstants.MAX_SENDS_PER_HOUR; slot++) {
            if (store.putIfAbsent(
                    KEY_PREFIX + "hour:" + normalizedPhone + ":" + slot,
                    3600
            )) {
                return true;
            }
        }

        return false;
    }
}
