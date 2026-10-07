# Build and run guide — TicketsCenter

## Prerequisites

- JDK 24+ (target 25 per spec; using 24 locally — see decisions.md DECISION-001)
- Apache Maven 3.10+
- Tomcat 11.0.25 (for local deploy)
- SQL Server (local or Azure SQL) — only required for integration tests
- Environment variables — copy `.env.example` to `.env` and fill in values

## Build

```bash
# Unit tests + WAR (no DB required)
mvn -B verify

# Output WAR at:
# target/ticketscenter.war
```

## Integration tests (SQL Server required)

Set environment variables:
```bash
$env:TC_SQL_HOST = "localhost\SQLEXPRESS"
$env:TC_TEST_DATABASE = "ticketscenter_test"
$env:TC_APP_BASE_URL = "http://localhost:8080/ticketscenter"
$env:TC_OTP_HMAC_SECRET = "<your-secret>"
# ... other vars from .env.example
```

Then run:
```bash
mvn -B -Psqlserver-it verify

# Run single IT class:
mvn -B -Psqlserver-it -Dit.test=KhanhHealthIT verify
```

## Deploy to Tomcat

1. Copy `target/ticketscenter.war` to `$CATALINA_HOME/webapps/`
2. Set environment variables on Tomcat (via setenv.sh/setenv.bat or container config)
3. Start Tomcat — app is at `/ticketscenter`
4. Health check: GET http://localhost:8080/ticketscenter/health/live

## SQL migrations

Run with sqlcmd (Windows Integrated Auth):
```powershell
sqlcmd -S "$env:TC_SQL_HOST" -d "$env:TC_TEST_DATABASE" -E -b -i database/migrations/0010_identity.sql
```

With SQL auth (password from environment, NOT -P flag):
```powershell
$env:SQLCMDPASSWORD = "<password>"
sqlcmd -S "$env:TC_SQL_HOST" -d "$env:TC_TEST_DATABASE" -U "$env:TC_TEST_LOGIN" -b -i database/migrations/0010_identity.sql
```

**NEVER** put the password in -P or print it to console.
