# Palaz SSO (Keycloak) — فاز ۱

Keycloak + PostgreSQL، متصل به Active Directory دامنه `palaz.local` از طریق LDAPS.

## ساختار

| مسیر | توضیح |
|---|---|
| `docker-compose.yml` | Keycloak + PostgreSQL |
| `.env.example` | متغیرها؛ به `.env` کپی و پر شود |
| `realm/palaz-realm.json` | Realm `palaz` با User Federation به AD، mapperها و تنظیمات امنیتی |
| `certs/` | گواهی CA دامنه برای اعتماد به LDAPS (commit نمی‌شود) |
| `scripts/get-ad-ca.ps1` | گرفتن گواهی CA از DC |

## پیش‌نیازهای AD (با تیم شبکه/زیرساخت)

1. **Service Account** فقط‌خواندنی، مثلاً `svc-keycloak`
   - عضو Domain Users کافی است (خواندن کاربران و گروه‌ها)
   - رمز قوی، `Password never expires`، بدون اجازه Logon تعاملی
2. **گروه‌های SSO** با پیشوند `SSO-` (فقط همین گروه‌ها وارد Keycloak می‌شوند). پیشنهاد:
   - `SSO-CRM-Admins`، `SSO-CRM-Supervisors`، `SSO-CRM-Agents`، `SSO-Tender-Users`
   - گروه تودرتو (nested) پشتیبانی می‌شود
3. **فیلد `mobile`** کاربران در AD پر باشد (برای OTP پیامکی فاز بعد)
4. محدوده `SSO_LDAP_USERS_DN` را به OU کاربران واقعی محدود کنید تا حساب‌های سرویس/سیستمی وارد نشوند
   (کاربران غیرفعال AD به‌صورت خودکار فیلتر می‌شوند)

## اجرا

```powershell
cd sso
copy .env.example .env          # و مقادیر را پر کنید
.\scripts\get-ad-ca.ps1         # certs\ad-ca.pem
docker compose up -d
docker compose logs -f keycloak
```

- پنل ادمین: http://localhost:8080/admin (با `KC_BOOTSTRAP_ADMIN_*`)
- سلامت: http://localhost:9000/health/ready

## بعد از اولین اجرا

1. **ادمین دائمی:** در Realm `master` یک کاربر ادمین واقعی بسازید و `bootstrap-admin` را حذف کنید.
2. **تست اتصال AD:** Realm `palaz` ← User federation ← `palaz-ad`
   - `Test connection` و `Test authentication`
   - Action ← `Sync all users`
3. **بررسی Mapperها:** تب Mappers؛ اگر Mapper تکراری (پیش‌فرض‌های Keycloak) ساخته شده، نسخه اضافه را حذف کنید.
4. **گروه‌ها:** mapper `ad-groups` ← `Sync LDAP groups to Keycloak`؛ گروه‌ها زیر `/ad` ظاهر می‌شوند.
5. **تست لاگین کاربر AD:** http://localhost:8080/realms/palaz/account

> `--import-realm` فقط وقتی Realm وجود ندارد اجرا می‌شود. برای اعمال دوباره فایل JSON باید Realm حذف شود؛
> تغییرات بعدی را از پنل انجام دهید و با `kc.sh export` خروجی بگیرید.

## Production

- پشت reverse proxy (IIS/Nginx) با HTTPS، مثلاً `https://sso.palaz.local`؛ `KC_HOSTNAME` همان آدرس باشد
- پورت `9000` (health/metrics) فقط داخلی باشد
- Backup منظم volume `pgdata`
- HA: دو نود Keycloak روی یک PostgreSQL

## فازهای بعد

- [ ] Kerberos/SPNEGO (SPN + keytab + GPO مرورگرها)
- [ ] تم فارسی/RTL
- [ ] ثبت‌نام کاربران خارجی + SPI پیامک
- [ ] Clientها و اتصال OIDC سامانه‌ها (پایلوت: SupervisorDashboard)
