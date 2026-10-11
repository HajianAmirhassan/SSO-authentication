package io.github.hossein_i.keycloak.starter.phone_auth.authentication;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public final class PhoneAuthAuthenticatorFactory
        implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "phone-auth";

    public static final String CONFIG_DEFAULT_REGION = "defaultRegion";
    public static final String CONFIG_CREATE_USERS = "createUsers";
    public static final String CONFIG_SMS_TEMPLATE = "smsTemplate";

    public static final String DEFAULT_REGION = "IR";
    public static final String DEFAULT_SMS_TEMPLATE = "کد ورود شما: %s";

    public PhoneAuthAuthenticatorFactory() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Phone Authentication";
    }

    @Override
    public String getReferenceCategory() {
        return "phone";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Authenticates users with a phone number and an SMS one-time code.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(
                        CONFIG_DEFAULT_REGION,
                        "Default region",
                        "ISO country code used to parse numbers entered without a country prefix.",
                        ProviderConfigProperty.STRING_TYPE,
                        DEFAULT_REGION
                ),
                new ProviderConfigProperty(
                        CONFIG_CREATE_USERS,
                        "Create unknown users",
                        "Create a local user for phone numbers that match no existing user. "
                                + "Keep OFF for employee-only realms (AD users are matched by their mobile).",
                        ProviderConfigProperty.BOOLEAN_TYPE,
                        "false"
                ),
                new ProviderConfigProperty(
                        CONFIG_SMS_TEMPLATE,
                        "SMS template",
                        "Message body; %s is replaced by the code.",
                        ProviderConfigProperty.STRING_TYPE,
                        DEFAULT_SMS_TEMPLATE
                )
        );
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return PhoneAuthAuthenticator.INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
