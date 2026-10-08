package vn.ticketscenter.config;

import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import vn.ticketscenter.transaction.PrincipalKind;

/** Validates environment configuration without exposing values in diagnostics. */
public final class ApplicationConfig {
  private final Map<String, String> env;
  private final String appEnv;
  private final URI baseUrl;

  private ApplicationConfig(Map<String, String> environment) {
    env = Map.copyOf(environment);
    required("TC_SQL_HOST");
    if (!required("TC_DATABASE").matches("[a-zA-Z0-9_]{1,128}")) throw invalid("TC_DATABASE");
    appEnv = value("TC_APP_ENV", "development");
    if (!Set.of("development", "test", "production").contains(appEnv)) throw invalid("TC_APP_ENV");
    try {
      baseUrl = URI.create(required("TC_APP_BASE_URL"));
    } catch (IllegalArgumentException ex) {
      throw invalid("TC_APP_BASE_URL");
    }
    if (baseUrl.getScheme() == null
        || !Set.of("http", "https").contains(baseUrl.getScheme())
        || baseUrl.getHost() == null
        || baseUrl.getUserInfo() != null
        || baseUrl.getQuery() != null
        || baseUrl.getFragment() != null
        || (isProduction() && !"https".equals(baseUrl.getScheme())))
      throw invalid("TC_APP_BASE_URL");
    number("TC_SQL_PORT", 1433, 1, 65535);
    number("TC_DB_POOL_SIZE", 5, 1, 5);
    number("TC_DB_TIMEOUT_MS", 2000, 250, 10000);
    number("TC_MAIL_PORT", 587, 1, 65535);
    number("TC_MAIL_TIMEOUT_MS", 10000, 1, 30000);
    number("TC_SESSION_TIMEOUT_MINUTES", 30, 1, 120);
    flag("TC_DEMO_ADMIN_ENABLED", false);
    flag("TC_DB_TRUST_SERVER_CERTIFICATE", false);
    if (isProduction() && (isDemoMode() || trustServerCertificate())) throw invalid("TC_APP_ENV");
    if (getDbUser() != null) required("TC_DB_PASSWORD");
    try {
      if (Base64.getDecoder().decode(required("TC_OTP_HMAC_SECRET")).length < 32)
        throw invalid("TC_OTP_HMAC_SECRET");
    } catch (IllegalArgumentException ex) {
      throw invalid("TC_OTP_HMAC_SECRET");
    }
    for (PrincipalKind principal : PrincipalKind.values()) {
      if (!getDatabasePrincipal(principal).matches("[a-zA-Z0-9_]{1,128}"))
        throw invalid("TC_PRINCIPAL_" + principal.name());
    }
  }

  public static ApplicationConfig from(Map<String, String> environment) {
    return new ApplicationConfig(environment);
  }

  public static ApplicationConfig getInstance() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    private static final ApplicationConfig INSTANCE = from(System.getenv());
  }

  private String required(String name) {
    String result = env.get(name);
    if (result == null || result.isBlank()) throw invalid(name);
    return result.strip();
  }

  private String value(String name, String fallback) {
    String result = env.get(name);
    return result == null || result.isBlank() ? fallback : result.strip();
  }

  private int number(String name, int fallback, int minimum, int maximum) {
    try {
      int result = Integer.parseInt(value(name, Integer.toString(fallback)));
      if (result < minimum || result > maximum) throw invalid(name);
      return result;
    } catch (NumberFormatException ex) {
      throw invalid(name);
    }
  }

  private boolean flag(String name, boolean fallback) {
    String result = value(name, Boolean.toString(fallback));
    if (!Set.of("true", "false").contains(result)) throw invalid(name);
    return Boolean.parseBoolean(result);
  }

  private static IllegalStateException invalid(String name) {
    return new IllegalStateException("Missing or invalid environment variable: " + name);
  }

  public String getDbHost() {
    return required("TC_SQL_HOST");
  }

  public String getDbName() {
    return required("TC_DATABASE");
  }

  public String getDbUser() {
    return value("TC_DB_USER", null);
  }

  public String getDbPassword() {
    return env.get("TC_DB_PASSWORD");
  }

  public String getDbPort() {
    return value("TC_SQL_PORT", "1433");
  }

  public int getDbPoolSize() {
    return number("TC_DB_POOL_SIZE", 5, 1, 5);
  }

  public int getDbTimeoutMs() {
    return number("TC_DB_TIMEOUT_MS", 2000, 250, 10000);
  }

  public boolean trustServerCertificate() {
    return flag("TC_DB_TRUST_SERVER_CERTIFICATE", false);
  }

  public String getAppBaseUrl() {
    return baseUrl.toString().replaceAll("/$", "");
  }

  public String getAppEnv() {
    return appEnv;
  }

  public boolean isDemoMode() {
    return flag("TC_DEMO_ADMIN_ENABLED", false);
  }

  public boolean isProduction() {
    return "production".equals(appEnv);
  }

  public String getMailHost() {
    return value("TC_MAIL_HOST", "");
  }

  public String getMailPort() {
    return value("TC_MAIL_PORT", "587");
  }

  public String getMailUser() {
    return value("TC_MAIL_USER", null);
  }

  public String getMailFrom() {
    return value("TC_MAIL_FROM", "");
  }

  public int getMailTimeoutMs() {
    return number("TC_MAIL_TIMEOUT_MS", 10000, 1, 30000);
  }

  public String getOtpHmacSecret() {
    return required("TC_OTP_HMAC_SECRET");
  }

  public int getSessionTimeoutMinutes() {
    return number("TC_SESSION_TIMEOUT_MINUTES", 30, 1, 120);
  }

  public String getDatabasePrincipal(PrincipalKind principal) {
    String fallback =
        switch (principal) {
          case BUYER -> "tc_buyer";
          case MANAGER -> "tc_manager";
          case CHECK_IN -> "tc_checkin";
          case PLATFORM_ADMIN -> "tc_platform_admin";
          case AUTH_TECH -> "tc_auth_tech";
          case WORKER_TECH -> "tc_worker_tech";
        };
    return value("TC_PRINCIPAL_" + principal.name(), fallback);
  }
}
