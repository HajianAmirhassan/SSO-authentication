package io.github.hossein_i.keycloak.starter.phone_auth.phone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberServiceTest {

    private final PhoneNumberService service =
            new PhoneNumberService();

    @Test
    void shouldNormalizeIranianNationalNumber() {
        String result = service.normalize(
                "09121234567",
                "IR"
        );

        assertEquals("+989121234567", result);
    }

    @Test
    void shouldKeepIranianE164Number() {
        String result = service.normalize(
                "+989121234567",
                "IR"
        );

        assertEquals("+989121234567", result);
    }

    @Test
    void shouldRejectInvalidPhoneNumber() {
        assertThrows(
                InvalidPhoneNumberException.class,
                () -> service.normalize(
                        "123",
                        "IR"
                )
        );
    }

    @Test
    void shouldRejectBlankPhoneNumber() {
        assertThrows(
                InvalidPhoneNumberException.class,
                () -> service.normalize(
                        "   ",
                        "IR"
                )
        );
    }

    @Test
    void shouldRejectNullPhoneNumber() {
        assertThrows(
                InvalidPhoneNumberException.class,
                () -> service.normalize(
                        null,
                        "IR"
                )
        );
    }

    @Test
    void shouldIncludeFormatsStoredInActiveDirectory() {
        assertEquals(
                java.util.List.of(
                        "+989121234567",
                        "989121234567",
                        "00989121234567",
                        "09121234567",
                        "9121234567"
                ),
                service.lookupCandidates("+989121234567")
        );
    }
}
