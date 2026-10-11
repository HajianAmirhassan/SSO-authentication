package io.github.hossein_i.keycloak.starter.phone_auth.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class OtpService {

    private static final int OTP_LENGTH = 6;

    private final SecureRandom secureRandom;

    public OtpService() {
        this.secureRandom = new SecureRandom();
    }

    public String generate() {
        int upperBound = (int) Math.pow(10, OTP_LENGTH);

        int code = secureRandom.nextInt(upperBound);

        return String.format(
                "%0" + OTP_LENGTH + "d",
                code
        );
    }

    public String hash(String otp) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            otp.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result = new StringBuilder();

            for (byte value : hash) {
                result.append(
                        String.format("%02x", value)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    exception
            );
        }
    }

    public boolean matches(
            String otp,
            String expectedHash
    ) {
        if (otp == null || expectedHash == null) {
            return false;
        }

        String actualHash = hash(otp);

        return MessageDigest.isEqual(
                actualHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}