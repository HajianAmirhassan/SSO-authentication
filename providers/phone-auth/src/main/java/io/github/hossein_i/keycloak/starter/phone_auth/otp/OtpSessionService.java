package io.github.hossein_i.keycloak.starter.phone_auth.otp;

import org.keycloak.sessions.AuthenticationSessionModel;

public final class OtpSessionService {

    private final OtpService otpService;

    public OtpSessionService(OtpService otpService) {
        this.otpService = otpService;
    }

    public String createOtp(
            AuthenticationSessionModel authenticationSession,
            String normalizedPhone
    ) {
        String otp = otpService.generate();
        String otpHash = otpService.hash(otp);

        long now = System.currentTimeMillis();

        long expiresAt =
                now
                        + (OtpConstants.OTP_EXPIRATION_SECONDS * 1000);

        authenticationSession.setAuthNote(
                OtpConstants.NOTE_PHONE,
                normalizedPhone
        );

        authenticationSession.setAuthNote(
                OtpConstants.NOTE_OTP_HASH,
                otpHash
        );

        authenticationSession.setAuthNote(
                OtpConstants.NOTE_OTP_EXPIRES_AT,
                String.valueOf(expiresAt)
        );

        // Keep the counter across resends; it only starts at 0 for a new session
        authenticationSession.setAuthNote(
                OtpConstants.NOTE_OTP_ATTEMPTS,
                String.valueOf(getAttempts(authenticationSession))
        );

        authenticationSession.setAuthNote(
                OtpConstants.NOTE_OTP_SENT_AT,
                String.valueOf(now)
        );

        authenticationSession.setAuthNote(
                OtpConstants.NOTE_STAGE,
                OtpConstants.STAGE_OTP
        );

        return otp;
    }

    public boolean isResendAllowed(
            AuthenticationSessionModel authenticationSession
    ) {
        String sentAt =
                authenticationSession.getAuthNote(
                        OtpConstants.NOTE_OTP_SENT_AT
                );

        if (sentAt == null) {
            return true;
        }

        long cooldownMillis =
                OtpConstants.OTP_RESEND_COOLDOWN_SECONDS * 1000;

        return System.currentTimeMillis()
                >= Long.parseLong(sentAt) + cooldownMillis;
    }

    public boolean isExpired(
            AuthenticationSessionModel authenticationSession
    ) {
        String expiresAt =
                authenticationSession.getAuthNote(
                        OtpConstants.NOTE_OTP_EXPIRES_AT
                );

        if (expiresAt == null) {
            return true;
        }

        return System.currentTimeMillis()
                >= Long.parseLong(expiresAt);
    }

    public int getAttempts(
            AuthenticationSessionModel authenticationSession
    ) {
        String attempts =
                authenticationSession.getAuthNote(
                        OtpConstants.NOTE_OTP_ATTEMPTS
                );

        if (attempts == null) {
            return 0;
        }

        return Integer.parseInt(attempts);
    }

    public long getResendCooldownSeconds(
            AuthenticationSessionModel authenticationSession
    ) {
        String sentAt =
                authenticationSession.getAuthNote(
                        OtpConstants.NOTE_OTP_SENT_AT
                );

        if (sentAt == null) {
            return 0;
        }

        long cooldownMillis =
                OtpConstants.OTP_RESEND_COOLDOWN_SECONDS * 1000;

        long remaining =
                sentAt != null
                        ? Long.parseLong(sentAt)
                        + cooldownMillis
                        - System.currentTimeMillis()
                        : 0;

        if (remaining <= 0) {
            return 0;
        }

        return (long) Math.ceil(
                remaining / 1000.0
        );
    }

    public int incrementAttempts(
            AuthenticationSessionModel authenticationSession
    ) {
        int attempts =
                getAttempts(authenticationSession) + 1;

        authenticationSession.setAuthNote(
                OtpConstants.NOTE_OTP_ATTEMPTS,
                String.valueOf(attempts)
        );

        return attempts;
    }

    public boolean verify(
            AuthenticationSessionModel authenticationSession,
            String otp
    ) {
        if (isExpired(authenticationSession)) {
            return false;
        }

        String expectedHash =
                authenticationSession.getAuthNote(
                        OtpConstants.NOTE_OTP_HASH
                );

        return otpService.matches(
                otp,
                expectedHash
        );
    }

    public void clearOtp(
            AuthenticationSessionModel authenticationSession
    ) {
        authenticationSession.removeAuthNote(
                OtpConstants.NOTE_OTP_HASH
        );

        authenticationSession.removeAuthNote(
                OtpConstants.NOTE_OTP_EXPIRES_AT
        );

        authenticationSession.removeAuthNote(
                OtpConstants.NOTE_OTP_ATTEMPTS
        );

        authenticationSession.removeAuthNote(
                OtpConstants.NOTE_OTP_SENT_AT
        );

        authenticationSession.removeAuthNote(
                OtpConstants.NOTE_STAGE
        );

    }
}