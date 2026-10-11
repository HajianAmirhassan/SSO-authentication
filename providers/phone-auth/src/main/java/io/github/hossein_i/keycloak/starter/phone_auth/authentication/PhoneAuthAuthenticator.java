package io.github.hossein_i.keycloak.starter.phone_auth.authentication;

import io.github.hossein_i.keycloak.starter.phone_auth.otp.OtpConstants;
import io.github.hossein_i.keycloak.starter.phone_auth.otp.OtpRateLimiter;
import io.github.hossein_i.keycloak.starter.phone_auth.otp.OtpService;
import io.github.hossein_i.keycloak.starter.phone_auth.otp.OtpSessionService;
import io.github.hossein_i.keycloak.starter.phone_auth.phone.InvalidPhoneNumberException;
import io.github.hossein_i.keycloak.starter.phone_auth.phone.PhoneNumberService;
import io.github.hossein_i.keycloak.starter.phone_auth.sms.SmsMessage;
import io.github.hossein_i.keycloak.starter.phone_auth.sms.SmsProvider;
import io.github.hossein_i.keycloak.starter.phone_auth.user.PhoneUserConstants;
import io.github.hossein_i.keycloak.starter.phone_auth.user.PhoneUserCreationService;
import io.github.hossein_i.keycloak.starter.phone_auth.user.PhoneUserLookupService;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;

public final class PhoneAuthAuthenticator implements Authenticator {

    public static final PhoneAuthAuthenticator INSTANCE =
            new PhoneAuthAuthenticator();

    private final PhoneNumberService phoneNumberService =
            new PhoneNumberService();

    private final OtpService otpService =
            new OtpService();

    private final OtpSessionService otpSessionService =
            new OtpSessionService(otpService);

    private PhoneAuthAuthenticator() {  }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(
                context.form()
                        .createForm("phone.ftl")
        );
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData =
                context.getHttpRequest()
                        .getDecodedFormParameters();

        String resend =
                formData.getFirst("resend");

        if ("true".equals(resend)) {
            handleResend(context);
            return;
        }

        String stage =
                context.getAuthenticationSession()
                        .getAuthNote(OtpConstants.NOTE_STAGE);

        if (OtpConstants.STAGE_OTP.equals(stage)) {
            handleOtp(context, formData);
            return;
        }

        handlePhone(context, formData);
    }

    private void handlePhone(
            AuthenticationFlowContext context,
            MultivaluedMap<String, String> formData
    ) {
        String phone = formData.getFirst("phone");

        try {
            String normalizedPhone =
                    phoneNumberService.normalize(
                            phone,
                            config(context,
                                    PhoneAuthAuthenticatorFactory.CONFIG_DEFAULT_REGION,
                                    PhoneAuthAuthenticatorFactory.DEFAULT_REGION)
                    );

            PhoneUserLookupService userLookupService =
                    new PhoneUserLookupService(
                            context.getSession()
                    );

            UserModel user =
                    userLookupService.findByPhone(
                            context.getRealm(),
                            phoneNumberService.lookupCandidates(normalizedPhone)
                    );

            if (user == null
                    && Boolean.parseBoolean(config(context,
                    PhoneAuthAuthenticatorFactory.CONFIG_CREATE_USERS, "false"))) {
                PhoneUserCreationService userCreationService =
                        new PhoneUserCreationService(
                                context.getSession()
                        );

                user =
                        userCreationService.create(
                                context.getRealm(),
                                normalizedPhone
                        );
            }

            // Checked for unknown numbers too, so the answer is the same either way
            if (!new OtpRateLimiter(context.getSession())
                    .tryAcquire(normalizedPhone)) {
                context.failureChallenge(
                        AuthenticationFlowError.INVALID_USER,
                        context.form()
                                .setError(OtpConstants.ERROR_RATE_LIMITED)
                                .createForm("phone.ftl")
                );
                return;
            }

            String otp =
                    otpSessionService.createOtp(
                            context.getAuthenticationSession(),
                            normalizedPhone
                    );

            // Unknown or disabled user: show the same OTP page without sending
            // anything, so the form does not reveal which numbers are registered
            // and no SMS is spent. Verification then simply fails.
            if (user != null && user.isEnabled()) {
                context.setUser(user);
                sendOtp(context, normalizedPhone, otp);
            }

            context.challenge(
                    context.form()
                            .createForm("otp.ftl")
            );

        } catch (InvalidPhoneNumberException exception) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_USER,
                    context.form()
                            .setError(exception.getMessage())
                            .createForm("phone.ftl")
            );
        }
    }

    private void handleResend(
            AuthenticationFlowContext context
    ) {
        AuthenticationSessionModel authenticationSession =
                context.getAuthenticationSession();

        if (!OtpConstants.STAGE_OTP.equals(
                authenticationSession.getAuthNote(
                        OtpConstants.NOTE_STAGE
                )
        )) {
            context.failure(
                    AuthenticationFlowError.INTERNAL_ERROR
            );
            return;
        }

        if (!otpSessionService.isResendAllowed(
                authenticationSession
        )) {

            long remaining =
                    otpSessionService.getResendCooldownSeconds(
                            authenticationSession
                    );

            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form()
                            .setError(
                                    OtpConstants.ERROR_RESEND_WAIT,
                                    remaining
                            )
                            .createForm("otp.ftl")
            );

            return;
        }

        String phone =
                authenticationSession.getAuthNote(
                        OtpConstants.NOTE_PHONE
                );

        if (phone == null || phone.isBlank()) {
            context.failure(
                    AuthenticationFlowError.INTERNAL_ERROR
            );
            return;
        }

        if (!new OtpRateLimiter(context.getSession())
                .tryAcquire(phone)) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form()
                            .setError(OtpConstants.ERROR_RATE_LIMITED)
                            .createForm("otp.ftl")
            );
            return;
        }

        String otp =
                otpSessionService.createOtp(
                        authenticationSession,
                        phone
                );

        if (context.getUser() != null) {
            sendOtp(context, phone, otp);
        }

        context.challenge(
                context.form()
                        .createForm("otp.ftl")
        );
    }

    private void sendOtp(
            AuthenticationFlowContext context,
            String phoneNumber,
            String otp
    ) {
        SmsProvider smsProvider =
                context.getSession()
                        .getProvider(SmsProvider.class);

        String template =
                config(context,
                        PhoneAuthAuthenticatorFactory.CONFIG_SMS_TEMPLATE,
                        PhoneAuthAuthenticatorFactory.DEFAULT_SMS_TEMPLATE);

        smsProvider.send(
                new SmsMessage(
                        phoneNumber,
                        String.format(template, otp)
                )
        );
    }

    private void handleOtp(
            AuthenticationFlowContext context,
            MultivaluedMap<String, String> formData
    ) {
        String otp = formData.getFirst("otp");

        if (otp == null || otp.isBlank()) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form()
                            .setError(OtpConstants.ERROR_REQUIRED)
                            .createForm("otp.ftl")
            );
            return;
        }

        AuthenticationSessionModel authenticationSession =
                context.getAuthenticationSession();

        // Not cleared here: the stage must stay OTP so "resend" keeps working
        if (otpSessionService.isExpired(authenticationSession)) {
            context.failureChallenge(
                    AuthenticationFlowError.EXPIRED_CODE,
                    context.form()
                            .setError(OtpConstants.ERROR_EXPIRED)
                            .createForm("otp.ftl")
            );

            return;
        }

        int attempts =
                otpSessionService.getAttempts(
                        authenticationSession
                );

        if (attempts >= OtpConstants.MAX_ATTEMPTS) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form()
                            .setError(OtpConstants.ERROR_TOO_MANY_ATTEMPTS)
                            .createForm("otp.ftl")
            );
            return;
        }

        UserModel user = context.getUser();

        if (user == null
                || !otpSessionService.verify(
                authenticationSession,
                otp
        )) {
            int currentAttempts =
                    otpSessionService.incrementAttempts(
                            authenticationSession
                    );

            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form()
                            .setError(
                                    currentAttempts >= OtpConstants.MAX_ATTEMPTS
                                            ? OtpConstants.ERROR_TOO_MANY_ATTEMPTS
                                            : OtpConstants.ERROR_INVALID
                            )
                            .createForm("otp.ftl")
            );
            return;
        }

        // Federated (AD) users are read-only and their mobile comes from AD
        if (user.getFederationLink() == null) {
            user.setSingleAttribute(
                    PhoneUserConstants.PHONE_NUMBER_VERIFIED_ATTRIBUTE,
                    "true"
            );
        }

        otpSessionService.clearOtp(
                authenticationSession
        );

        context.success();
    }

    private static String config(
            AuthenticationFlowContext context,
            String key,
            String defaultValue
    ) {
        AuthenticatorConfigModel model =
                context.getAuthenticatorConfig();

        Map<String, String> values =
                model == null ? null : model.getConfig();

        String value =
                values == null ? null : values.get(key);

        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(
            KeycloakSession session,
            RealmModel realm,
            UserModel user
    ) {
        return true;
    }

    @Override
    public void setRequiredActions(
            KeycloakSession session,
            RealmModel realm,
            UserModel user
    ) {
    }

    @Override
    public void close() {
    }
}
