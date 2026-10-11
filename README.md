# Palaz SSO (Keycloak) — فاز ۱

Keycloak + PostgreSQL، متصل به Active Directory دامنه `palaz.local` از طریق LDAPS.

## ساختار

| مسیر | توضیح |
|---|---|
| `docker-compose.yml` | Keycloak (image سفارشی) + PostgreSQL |
| `Dockerfile` | build چندمرحله‌ای: Maven ← jar providerها ← `kc.sh build` |
| `pom.xml` | پروژه Maven والد؛ `keycloak.version` اینجاست |
| `providers/phone-auth/` | Authenticator ورود با موبایل + OTP پیامکی و SPI پیامک (`sms`) |
| `themes/palaz/login/` | تم login (فارسی؛ فرم‌های `phone.ftl` و `otp.ftl`) |
| `.env.example` | متغیرها؛ به `.env` کپی و پر شود |
| `realm/palaz-realm.json` | Realm `palaz` با User Federation به AD، mapperها و تنظیمات امنیتی |
| `certs/` | گواهی CA دامنه برای اعتماد به LDAPS (commit نمی‌شود) |
| `scripts/get-ad-ca.ps1` | گرفتن گواهی CA از DC |

> ساختار providers/themes و Dockerfile از
> [Hossein-i/keycloak-starter-kit](https://github.com/Hossein-i/keycloak-starter-kit) گرفته و با این پروژه تطبیق داده شده است
> (جزئیات در بخش «ورود با موبایل»).

## پیش‌نیازهای AD (با تیم شبکه/زیرساخت)

1. **Service Account** فقط‌خواندنی، مثلاً `svc-keycloak`
   - عضو Domain Users کافی است (خواندن کاربران و گروه‌ها)
   - رمز قوی، `Password never expires`، بدون اجازه Logon تعاملی
2. **گروه‌های SSO** با پیشوند `SSO-` (فقط همین گروه‌ها وارد Keycloak می‌شوند). پیشنهاد:
   - `SSO-CRM-Admins`، `SSO-CRM-Supervisors`، `SSO-CRM-Agents`، `SSO-Tender-Users`
   - گروه تودرتو (nested) پشتیبانی می‌شود
3. **فیلد `mobile`** کاربران در AD پر باشد (برای OTP پیامکی)، بدون فاصله و خط تیره:
   `09121234567` یا `+989121234567`
4. محدوده `SSO_LDAP_USERS_DN` را به OU کاربران واقعی محدود کنید تا حساب‌های سرویس/سیستمی وارد نشوند
   (کاربران غیرفعال AD به‌صورت خودکار فیلتر می‌شوند)

## اجرا

```powershell
cd sso
copy .env.example .env          # و مقادیر را پر کنید
.\scripts\get-ad-ca.ps1         # certs\ad-ca.pem
docker compose up -d --build   # build اول چند دقیقه طول می‌کشد (دانلود Maven و وابستگی‌ها)
docker compose logs -f keycloak
```

بعد از هر تغییر در `providers/` یا `themes/` دوباره `docker compose up -d --build` بزنید.
برای ارتقای Keycloak، `KC_VERSION` در `.env` و `keycloak.version` در `pom.xml` را با هم عوض کنید.

- پنل ادمین: http://localhost:8080/admin (با `KC_BOOTSTRAP_ADMIN_*`)
- سلامت: http://localhost:9000/health/ready

## بعد از اولین اجرا

1. **ادمین دائمی:** در Realm `master` یک کاربر ادمین واقعی بسازید و `bootstrap-admin` را حذف کنید.
2. **تست اتصال AD:** Realm `palaz` ← User federation ← `palaz-ad`
   - اگر `SSO_LDAP_BIND_PASSWORD` خالی بود، Bind DN و Bind credentials را همین‌جا وارد و Save کنید
   - `Test connection` و `Test authentication`
   - Action ← `Sync all users`
3. **بررسی Mapperها:** تب Mappers؛ اگر Mapper تکراری (پیش‌فرض‌های Keycloak) ساخته شده، نسخه اضافه را حذف کنید.
4. **گروه‌ها:** mapper `ad-groups` ← `Sync LDAP groups to Keycloak`؛ گروه‌ها زیر `/ad` ظاهر می‌شوند.
5. **تست لاگین کاربر AD:** http://localhost:8080/realms/palaz/account

> تا Bind credentials وارد نشود، هر جستجوی کاربر (حتی ساخت کاربر محلی یا ورود با موبایل) با خطای
> `successful bind must be completed` از AD شکست می‌خورد.

**User profile Realm:** ایمیل اجباری نیست (بعضی کاربران AD `mail` ندارند و چون federation فقط‌خواندنی است،
در صفحه «به‌روزرسانی اطلاعات حساب» گیر می‌کردند). نام و نام خانوادگی اجباری‌اند.
`phoneNumber` و `phoneNumberVerified` فقط برای ادمین قابل مشاهده/ویرایش‌اند.

> `--import-realm` فقط وقتی Realm وجود ندارد اجرا می‌شود. برای اعمال دوباره فایل JSON باید Realm حذف شود؛
> تغییرات بعدی را از پنل انجام دهید و با `kc.sh export` خروجی بگیرید.

## ورود با موبایل (OTP پیامکی) — در حال توسعه

Provider `phone-auth` در image هست ولی به هیچ flowی وصل نیست؛ تا وقتی آن را bind نکنید رفتار لاگین تغییری نمی‌کند.

**روند کار:** کاربر شماره را وارد می‌کند ← شماره به E.164 نرمال می‌شود ← کاربری با attribute `phoneNumber`
پیدا می‌شود (برای کاربران AD همان `mobile`؛ شکل‌های `0912…`، `98912…` و `+98912…` هر سه جستجو می‌شوند)
← کد ۶ رقمی ۲ دقیقه‌ای ارسال می‌شود ← حداکثر ۵ تلاش، ارسال دوباره بعد از ۶۰ ثانیه.
اگر شماره به کاربری نخورد، صفحه کد همان‌طور نمایش داده می‌شود ولی پیامکی ارسال نمی‌شود (افشای شماره‌ها و هزینه پیامک نه).

**تست روی محیط توسعه:**

1. Realm `palaz` ← Authentication ← `browser` را Duplicate کنید (مثلاً `browser-phone`)
2. در subflow `forms`، step `Phone Authentication` را اضافه و `Alternative` کنید (کنار Username Password Form)
   — یا برای تست جدا، یک flow فقط با همین step بسازید
3. با چرخ‌دنده step تنظیمات را ببینید: `Default region`=`IR`، `Create unknown users`=خاموش، `SMS template`
4. Action ← `Bind flow` ← Browser flow (یا فقط روی یک client تست از تب Advanced ← Authentication flow overrides)
5. Realm settings ← Themes ← Login theme باید `palaz` باشد (در import جدید خودکار است)
6. کد OTP فعلاً با provider `console` فقط در لاگ چاپ می‌شود: `docker compose logs keycloak | findstr console-sms`

**تغییرات نسبت به starter kit:**

- SPI پیامک ثبت و قابل استفاده شد (`SmsProviderSpi` + `ConsoleSmsProviderFactory`)؛ در نسخه اصلی ثبت نشده بود و OTP با `System.out` چاپ می‌شد
- جستجوی کاربر با چند شکل شماره، تا کاربر AD با `mobile` غیر E.164 تکراری ساخته نشود
- ساخت خودکار کاربر برای شماره ناشناس قابل تنظیم و پیش‌فرض خاموش (Realm فعلاً فقط کارمندان AD است)
- «ارسال دوباره» دیگر شمارنده تلاش‌ها را صفر نمی‌کند (جلوگیری از حدس نامحدود)، و بعد از انقضای کد کار می‌کند
- کاربر غیرفعال پیامک نمی‌گیرد؛ روی کاربر AD (فقط‌خواندنی) attribute نوشته نمی‌شود
- پیام‌ها با کلید ترجمه و فارسی/انگلیسی در تم `palaz` (parent: `keycloak.v2`)
- نسخه Keycloak در یک جا (`keycloak.version` / `KC_VERSION`) و گزینه‌های build-time در Dockerfile

**قبل از Production لازم است:**

- [ ] Provider واقعی پیامک (درگاه پیامک سازمان) به‌جای `console` — یک `SmsProviderFactory` جدید با id خودش؛
      با بیش از یک provider، انتخاب با `KC_SPI_SMS__PROVIDER=<id>`
- [x] محدودیت ارسال به‌ازای شماره بین sessionها: حداقل ۶۰ ثانیه فاصله و حداکثر ۵ پیامک در ساعت
      (`OtpRateLimiter`، روی store مشترک Keycloak ← در cluster هم معتبر است). محدودیت IP عمداً اضافه نشد
      چون کاربران داخلی پشت NAT یک IP دارند؛ در صورت نیاز روی reverse proxy
- [ ] تصمیم سیاستی: آیا کاربر AD فقط با OTP (بدون رمز) وارد شود یا OTP فقط عامل دوم باشد
- [ ] تست‌های یکپارچه flow روی Keycloak واقعی

## Production

- پشت reverse proxy (IIS/Nginx) با HTTPS، مثلاً `https://sso.palaz.local`؛ `KC_HOSTNAME` همان آدرس باشد
- پورت `9000` (health/metrics) فقط داخلی باشد
- Backup منظم volume `pgdata`
- HA: دو نود Keycloak روی یک PostgreSQL

## فازهای بعد

- [ ] Kerberos/SPNEGO (SPN + keytab + GPO مرورگرها)
- [x] زیرساخت build برای provider و تم سفارشی (Dockerfile + Maven)
- [ ] تم فارسی/RTL — پایه در `themes/palaz` آماده است؛ ظاهر و فونت مانده
- [ ] ورود با موبایل/OTP — نسخه اولیه در `providers/phone-auth`؛ موارد «قبل از Production» بالا
- [ ] ثبت‌نام کاربران خارجی (روی همان `phone-auth` با `Create unknown users`، احتمالاً در Realm جدا)
- [ ] Clientها و اتصال OIDC سامانه‌ها — پایلوت SupervisorDashboard انجام شد (پایین)

## اتصال یک سامانه (Client جدید)

```powershell
.\scripts\new-oidc-client.ps1 -ClientId crm-supervisor-dashboard -Name "CRM Supervisor Dashboard" `
    -BaseUrls "https://localhost:5003,http://localhost:6003" `
    -UserSecretsProject D:\src\crm\Presentation\PalazCrm.SupervisorDashboard
```

- Client از نوع confidential با Authorization Code + PKCE؛ Redirect: `<BaseUrl>/signin-oidc`
- اجرای دوباره، Client موجود را به‌روز می‌کند (برای اضافه کردن آدرس Production هم همین)
- با `-UserSecretsProject`، Client Secret مستقیم در `Sso:ClientSecret` همان پروژه ذخیره می‌شود؛ در Production از متغیر محیطی `Sso__ClientSecret`

**پایلوت SupervisorDashboard (ریپوی CRM):** دکمه «ورود با حساب سازمانی (SSO)» در `/Account/LoginS`.
بعد از ورود در Keycloak، کاربر با `preferred_username` = `Users.UserName` در CRM پیدا می‌شود (فعال و با نقش Supervisor)
و همان توکن فعلی CRM صادر می‌شود؛ APIها و JS بدون تغییر کار می‌کنند. خروج، Session Keycloak را هم می‌بندد.
