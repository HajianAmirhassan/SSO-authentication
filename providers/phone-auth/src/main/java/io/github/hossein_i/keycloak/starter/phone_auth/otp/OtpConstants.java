package io.github.hossein_i.keycloak.starter.phone_auth.otp;

public final class OtpConstants {

    private OtpConstants() {
    }

    public static final String NOTE_PHONE =
            "phone-auth.phone";

    public static final String NOTE_OTP_HASH =
            "phone-auth.otp-hash";

    public static final String NOTE_OTP_EXPIRES_AT =
            "phone-auth.otp-expires-at";

    public static final String NOTE_OTP_ATTEMPTS =
            "phone-auth.otp-attempts";

    public static final String NOTE_STAGE =
            "phone-auth.stage";

    public static final String NOTE_OTP_SENT_AT =
            "phone-auth.otp-sent-at";

    public static final long OTP_RESEND_COOLDOWN_SECONDS = 60;

    public static final String STAGE_PHONE =
            "PHONE";

    public static final String STAGE_OTP =
            "OTP";

    public static final long OTP_EXPIRATION_SECONDS = 120;

    // Counted per authentication session and NOT reset by "resend",
    // otherwise resending would grant a fresh set of guesses every minute.
    public static final int MAX_ATTEMPTS = 5;

    // Across all sessions, per phone number (see OtpRateLimiter)
    public static final int MAX_SENDS_PER_HOUR = 5;

    // Message keys, resolved by the login theme (messages_*.properties)
    public static final String ERROR_REQUIRED = "phoneAuthOtpRequired";
    public static final String ERROR_INVALID = "phoneAuthOtpInvalid";
    public static final String ERROR_EXPIRED = "phoneAuthOtpExpired";
    public static final String ERROR_TOO_MANY_ATTEMPTS = "phoneAuthOtpTooManyAttempts";
    public static final String ERROR_RESEND_WAIT = "phoneAuthOtpResendWait";
    public static final String ERROR_RATE_LIMITED = "phoneAuthOtpRateLimited";

}
