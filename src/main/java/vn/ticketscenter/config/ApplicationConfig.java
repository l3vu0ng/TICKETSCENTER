package vn.ticketscenter.config;

import java.util.logging.Logger;

/**
 * Reads and validates all application configuration from environment variables.
 * Throws IllegalStateException at startup if required variables are missing.
 * Never logs secret values.
 * Owner: Khánh (KHANH-01)
 */
public final class ApplicationConfig {

    private static final Logger log = Logger.getLogger(ApplicationConfig.class.getName());

    private final String dbHost;
    private final String dbName;
    private final String dbUser;
    private final String dbPort;
    private final int dbPoolSize;
    private final String appBaseUrl;
    private final String appEnv;
    private final boolean demoMode;
    private final String mailHost;
    private final String mailPort;
    private final String mailUser;
    private final String mailFrom;
    private final int mailTimeoutMs;
    private final String otpHmacSecret;
    private final int sessionTimeoutMinutes;

    private static volatile ApplicationConfig instance;

    private ApplicationConfig() {
        this.dbHost = requireEnv("TC_SQL_HOST");
        this.dbName = requireEnv("TC_DATABASE");
        this.dbUser = System.getenv("TC_DB_USER");
        this.dbPort = System.getenv().getOrDefault("TC_SQL_PORT", "1433");
        this.dbPoolSize = parseIntEnv("TC_DB_POOL_SIZE", 5);
        this.appBaseUrl = requireEnv("TC_APP_BASE_URL");
        this.appEnv = System.getenv().getOrDefault("TC_APP_ENV", "development");
        this.demoMode = Boolean.parseBoolean(
                System.getenv().getOrDefault("TC_DEMO_ADMIN_ENABLED", "false"));
        this.mailHost = System.getenv().getOrDefault("TC_MAIL_HOST", "");
        this.mailPort = System.getenv().getOrDefault("TC_MAIL_PORT", "587");
        this.mailUser = System.getenv("TC_MAIL_USER");
        this.mailFrom = System.getenv().getOrDefault("TC_MAIL_FROM", "");
        this.mailTimeoutMs = parseIntEnv("TC_MAIL_TIMEOUT_MS", 10000);
        this.otpHmacSecret = requireEnv("TC_OTP_HMAC_SECRET");
        this.sessionTimeoutMinutes = parseIntEnv("TC_SESSION_TIMEOUT_MINUTES", 30);
        log.info("Config loaded: env=" + appEnv + " host=" + dbHost + " demo=" + demoMode);
    }

    public static ApplicationConfig getInstance() {
        if (instance == null) {
            synchronized (ApplicationConfig.class) {
                if (instance == null) instance = new ApplicationConfig();
            }
        }
        return instance;
    }

    public String getDbHost() { return dbHost; }
    public String getDbName() { return dbName; }
    public String getDbUser() { return dbUser; }
    public String getDbPort() { return dbPort; }
    public int getDbPoolSize() { return dbPoolSize; }
    public String getAppBaseUrl() { return appBaseUrl; }
    public String getAppEnv() { return appEnv; }
    public boolean isDemoMode() { return demoMode; }
    public boolean isProduction() { return "production".equalsIgnoreCase(appEnv); }
    public String getMailHost() { return mailHost; }
    public String getMailPort() { return mailPort; }
    public String getMailUser() { return mailUser; }
    public String getMailFrom() { return mailFrom; }
    public int getMailTimeoutMs() { return mailTimeoutMs; }
    public String getOtpHmacSecret() { return otpHmacSecret; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }

    static String requireEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank())
            throw new IllegalStateException("Required env var not set: " + name);
        return v.trim();
    }

    static int parseIntEnv(String name, int def) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank()) return def;
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException e) {
            throw new IllegalStateException("Env var " + name + " must be integer, got: " + raw);
        }
    }
}
