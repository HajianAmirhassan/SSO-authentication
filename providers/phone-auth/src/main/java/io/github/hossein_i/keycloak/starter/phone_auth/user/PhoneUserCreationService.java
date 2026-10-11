package io.github.hossein_i.keycloak.starter.phone_auth.user;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public final class PhoneUserCreationService {

    private final KeycloakSession session;

    public PhoneUserCreationService(KeycloakSession session) {
        this.session = session;
    }

    public UserModel create(
            RealmModel realm,
            String normalizedPhone
    ) {
        UserModel user =
                session.users().addUser(
                        realm,
                        normalizedPhone
                );

        user.setEnabled(true);

        user.setSingleAttribute(
                PhoneUserConstants.PHONE_NUMBER_ATTRIBUTE,
                normalizedPhone
        );

        return user;
    }
}
