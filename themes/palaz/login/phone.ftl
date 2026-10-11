<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true displayInfo=false; section>
    <#if section = "header">
        ${msg("phoneAuthTitle")}
    <#elseif section = "form">
        <form id="kc-phone-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <label for="phone" class="${properties.kcLabelClass!}">${msg("phoneAuthPhoneLabel")}</label>
                <input id="phone" name="phone" type="tel" dir="ltr" autocomplete="tel"
                       placeholder="09xxxxxxxxx" class="${properties.kcInputClass!}" autofocus required/>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}"
                       type="submit" value="${msg("phoneAuthContinue")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
