package io.github.hossein_i.keycloak.starter.phone_auth.user;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Objects;

public final class PhoneUserLookupService {

    private final KeycloakSession session;

    public PhoneUserLookupService(KeycloakSession session) {
        this.session = session;
    }

    public UserModel findByPhone(
            RealmModel realm,
            List<String> candidates
    ) {
        return candidates.stream()
                .map(candidate -> session.users()
                        .searchForUserByUserAttributeStream(
                                realm,
                                PhoneUserConstants.PHONE_NUMBER_ATTRIBUTE,
                                candidate
                        )
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
