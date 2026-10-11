package io.github.hossein_i.keycloak.starter.phone_auth.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.List;

public final class PhoneNumberService {

    // Message keys, resolved by the login theme (messages_*.properties)
    public static final String ERROR_REQUIRED = "phoneAuthPhoneRequired";
    public static final String ERROR_INVALID = "phoneAuthPhoneInvalid";

    private final PhoneNumberUtil phoneNumberUtil;

    public PhoneNumberService() {
        this.phoneNumberUtil = PhoneNumberUtil.getInstance();
    }

    public String normalize(String phone, String defaultRegion) {
        if (phone == null || phone.isBlank()) {
            throw new InvalidPhoneNumberException(ERROR_REQUIRED);
        }

        try {
            Phonenumber.PhoneNumber parsed =
                    phoneNumberUtil.parse(phone.trim(), defaultRegion);

            if (!phoneNumberUtil.isValidNumber(parsed)) {
                throw new InvalidPhoneNumberException(ERROR_INVALID);
            }

            return phoneNumberUtil.format(
                    parsed,
                    PhoneNumberUtil.PhoneNumberFormat.E164
            );

        } catch (NumberParseException exception) {
            throw new InvalidPhoneNumberException(ERROR_INVALID);
        }
    }

    /**
     * Formats a stored phone attribute may have, for an already normalized
     * E.164 number. Users federated from AD keep {@code mobile} as typed by
     * the admins (e.g. {@code 09121234567}), so an exact E.164 lookup alone
     * would miss them and create a duplicate local user.
     */
    public List<String> lookupCandidates(String e164) {
        try {
            Phonenumber.PhoneNumber parsed =
                    phoneNumberUtil.parse(e164, null);

            String countryCode =
                    String.valueOf(parsed.getCountryCode());

            String national =
                    phoneNumberUtil.getNationalSignificantNumber(parsed);

            return List.of(
                    e164,
                    countryCode + national,
                    "00" + countryCode + national,
                    "0" + national,
                    national
            );

        } catch (NumberParseException exception) {
            return List.of(e164);
        }
    }
}
