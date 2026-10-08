package vn.ticketscenter.support;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import vn.ticketscenter.config.ApplicationConfig;

public final class SqlServerTestSupport {
  private SqlServerTestSupport() {}

  public static String required(String name) {
    String result = System.getProperty(name);
    if (result == null || result.isBlank() || result.startsWith("${")) result = System.getenv(name);
    if (result == null || result.isBlank())
      throw new IllegalStateException("Integration environment is missing " + name);
    return result;
  }

  public static ApplicationConfig config() {
    Map<String, String> env = new HashMap<>(System.getenv());
    env.put("TC_SQL_HOST", required("TC_SQL_HOST"));
    String database = required("TC_TEST_DATABASE");
    if (!database.matches("[A-Za-z0-9_]+_test"))
      throw new IllegalStateException("TC_TEST_DATABASE must end with _test");
    env.put("TC_DATABASE", database);
    env.put("TC_APP_BASE_URL", required("TC_APP_BASE_URL"));
    env.put("TC_OTP_HMAC_SECRET", required("TC_OTP_HMAC_SECRET"));
    return ApplicationConfig.from(env);
  }

  public static Connection open() throws Exception {
    var config = config();
    var source = new SQLServerDataSource();
    String[] host = config.getDbHost().split("\\\\", 2);
    source.setServerName(host[0]);
    if (host.length == 2) source.setInstanceName(host[1]);
    else source.setPortNumber(Integer.parseInt(config.getDbPort()));
    source.setDatabaseName(config.getDbName());
    source.setEncrypt("true");
    source.setTrustServerCertificate(config.trustServerCertificate());
    source.setLoginTimeout(3);
    if (config.getDbUser() == null) source.setIntegratedSecurity(true);
    else {
      source.setUser(config.getDbUser());
      source.setPassword(config.getDbPassword());
    }
    return source.getConnection();
  }
}
