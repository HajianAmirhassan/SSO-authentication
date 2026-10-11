package io.github.hossein_i.keycloak.starter.phone_auth.otp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OtpServiceTest {

    private final OtpService service =
            new OtpService();

    @Test
    void shouldGenerateSixDigitOtp() {
        String otp = service.generate();

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void shouldGenerateDifferentValues() {
        String first = service.generate();
        String second = service.generate();

        assertNotEquals(first, second);
    }

    @Test
    void shouldHashAndVerifyOtp() {
        String otp = service.generate();

        String hash = service.hash(otp);

        assertNotNull(hash);
        assertNotEquals(otp, hash);

        assertTrue(
                service.matches(otp, hash)
        );
    }

    @Test
    void shouldRejectWrongOtp() {
        String otp = service.generate();

        String hash = service.hash(otp);

        assertFalse(
                service.matches("000000", hash)
        );
    }
}