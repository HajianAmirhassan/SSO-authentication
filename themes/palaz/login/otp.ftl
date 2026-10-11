<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true displayInfo=false; section>
    <#if section = "header">
        ${msg("phoneAuthOtpTitle")}
    <#elseif section = "form">
        <form id="kc-otp-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <label for="otp" class="${properties.kcLabelClass!}">${msg("phoneAuthOtpLabel")}</label>
                <input id="otp" name="otp" type="text" dir="ltr" inputmode="numeric" autocomplete="one-time-code"
                       maxlength="6" class="${properties.kcInputClass!}" autofocus required/>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}"
                       type="submit" value="${msg("phoneAuthVerify")}"/>
            </div>
        </form>

        <form action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <button type="submit" name="resend" value="true"
                        class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!}">
                    ${msg("phoneAuthResend")}
                </button>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
