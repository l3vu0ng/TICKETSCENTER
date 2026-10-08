# Build and run

## Requirements

JDK 25, Maven 3.10+, Tomcat 11.0.25. Set JAVA_HOME to JDK 25; JDK 24 cannot compile
release 25. Export variables from `.env.example` in the application launcher.
Copying the file alone does not load it.

```powershell
mvn -B verify
# target/ticketscenter.war
mvn -B spotless:apply
```

`verify` includes the Java formatter check and fails if no unit tests exist.

## Standalone WAR verification

This profile deploys the packaged WAR twice to an embedded Tomcat 11.0.25 and
checks health, CSRF, static assets, JSP escaping and pool shutdown. It does not prove
SQL Server transactions, grants, browser behavior or the business flows of M1.

```powershell
$env:TC_APP_ENV = 'test'
$env:TC_SQL_HOST = '127.0.0.1'
$env:TC_SQL_PORT = '1'
$env:TC_DATABASE = 'ticketscenter_test'
$env:TC_APP_BASE_URL = 'http://localhost:8080/ticketscenter'
$env:TC_OTP_HMAC_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
mvn -B -Phttp-it verify
```

The unavailable SQL endpoint is deliberate: live must be 200, ready must be 503.

To also check the shared frame in a real browser, provide an installed Node/Playwright
runtime and browser executable. This smoke check does not configure Vương's browser-it
profile or accept the M1 pages/membership flows:

```powershell
$env:TC_BROWSER_NODE = 'C:\path\to\node.exe'
$env:TC_PLAYWRIGHT_MODULE = 'C:\path\to\node_modules\playwright'
$env:TC_BROWSER_EXECUTABLE = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
$env:TC_BROWSER_EVIDENCE_DIR = Join-Path $PWD 'docs/evidence/khanh/layout'
mvn -B -Phttp-it verify
& $env:TC_BROWSER_NODE --test src/test/js/shared-client.test.cjs
```

The browser script runs while the test-only layout route is available. Browser failure
fails http-it; no browser runtime is downloaded by Maven or bundled into the WAR.

On this OneDrive workspace, `clean` cannot remove the old `target/classes/META-INF`
directory. Use an isolated output directory instead of suppressing clean failures:

```powershell
$taskBuildPath = Join-Path ([IO.Path]::GetTempPath()) 'ticketscenter-m0-build'
mvn -B -Phttp-it "-Dtc.build.directory=$taskBuildPath" clean verify
```

## SQL Server integration

Set TC_SQL_HOST, TC_TEST_DATABASE ending in `_test`, TC_APP_BASE_URL and the remaining
application variables. Apply migrations through Vương's manifest; add 0011 after
0010. Load Vương's fixture registry and principals, then prepare the test-only SP
using `database/tests/khanh/KHANH-03.sql`.

```powershell
sqlcmd -S "$env:TC_SQL_HOST" -d "$env:TC_TEST_DATABASE" -E -b -i database/tests/khanh/KHANH-03.sql
sqlcmd -S "$env:TC_SQL_HOST" -d "$env:TC_TEST_DATABASE" -E -b -i database/tests/khanh/KHANH-04.sql
mvn -B -Psqlserver-it verify
```

SQL auth for sqlcmd uses `-U` with SQLCMDPASSWORD from the environment. JDBC uses
TC_DB_USER / TC_DB_PASSWORD for the reviewed broker. Windows JDBC integrated auth
requires the Microsoft driver's matching native authentication DLL on java.library.path;
a working `sqlcmd -E` does not itself configure JDBC authentication.

## Deploy

Copy the WAR to Tomcat webapps, export configuration in setenv.bat/the service launcher
and start Tomcat. Test `/ticketscenter/health/live` and `/health/ready`.
Readiness reports UP only for a successful bounded SQL connection probe.

M0 currently exposes CSRF and health. Account mutations and personal pages return
NOT_IMPLEMENTED until the M1/integration tasks are supplied.
