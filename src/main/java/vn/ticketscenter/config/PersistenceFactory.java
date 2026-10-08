package vn.ticketscenter.config;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import org.hibernate.SessionFactory;
import vn.ticketscenter.dto.common.ActorContext;
import vn.ticketscenter.transaction.ActorSessionContext;
import vn.ticketscenter.transaction.PrincipalKind;
import vn.ticketscenter.transaction.TransactionRunner;

/** One bounded pool; database users are selected by server-side principal mapping. */
public final class PersistenceFactory implements AutoCloseable {
  public static final String ATTRIBUTE = PersistenceFactory.class.getName();
  private final ApplicationConfig config;
  private final HikariDataSource pool;
  private volatile EntityManagerFactory factory;
  private boolean closed;

  public PersistenceFactory(ApplicationConfig config) {
    this.config = config;
    SQLServerDataSource source = new SQLServerDataSource();
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
    HikariConfig settings = new HikariConfig();
    settings.setDataSource(source);
    settings.setPoolName("ticketscenter");
    settings.setMaximumPoolSize(config.getDbPoolSize());
    settings.setMinimumIdle(0);
    settings.setConnectionTimeout(config.getDbTimeoutMs());
    settings.setValidationTimeout(Math.min(1000, config.getDbTimeoutMs()));
    settings.setInitializationFailTimeout(-1);
    pool = new HikariDataSource(settings);
  }

  public TransactionRunner transactionRunner() {
    return new TransactionRunner(this::open);
  }

  public boolean isReady() {
    try (Connection connection = pool.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
      statement.setQueryTimeout(2);
      try (var rows = statement.executeQuery()) {
        return rows.next() && rows.getInt(1) == 1;
      }
    } catch (SQLException | IllegalStateException ex) {
      return false;
    }
  }

  private synchronized EntityManagerFactory factory() {
    if (closed) throw new IllegalStateException("Persistence factory is closed");
    if (factory == null) {
      factory =
          Persistence.createEntityManagerFactory(
              "ticketscenter",
              Map.of(
                  "jakarta.persistence.nonJtaDataSource", pool,
                  "hibernate.hbm2ddl.auto", "validate",
                  "hibernate.type.preferred_instant_jdbc_type", "TIMESTAMP",
                  "hibernate.type.preferred_uuid_jdbc_type", "UUID"));
    }
    return factory;
  }

  private TransactionRunner.Scope open(PrincipalKind principal, ActorContext actor) {
    SessionFactory sessions = factory().unwrap(SessionFactory.class);
    Connection connection = null;
    byte[] cookie = null;
    try {
      connection = pool.getConnection();
      rejectPrivilegedBroker(connection);
      cookie = impersonate(connection, config.getDatabasePrincipal(principal));
      ActorSessionContext.set(connection, actor);
      EntityManager em = sessions.withOptions().connection(connection).openSession();
      return new JdbcScope(connection, cookie, em);
    } catch (SQLException | RuntimeException ex) {
      if (connection != null) discard(connection);
      throw new IllegalStateException("Cannot open database transaction", ex);
    }
  }

  private static void rejectPrivilegedBroker(Connection connection) throws SQLException {
    try (var statement =
        connection.prepareStatement("SELECT IS_SRVROLEMEMBER('sysadmin'), IS_MEMBER('db_owner')")) {
      statement.setQueryTimeout(2);
      try (var rows = statement.executeQuery()) {
        if (!rows.next() || rows.getInt(1) == 1 || rows.getInt(2) == 1)
          throw new SQLException("Runtime connection must not use sysadmin or db_owner");
      }
    }
  }

  private static byte[] impersonate(Connection connection, String user) throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "DECLARE @principal sysname = ?, @cookie varbinary(8000); "
                + "EXECUTE AS USER = @principal WITH COOKIE INTO @cookie; SELECT @cookie;")) {
      statement.setString(1, user);
      statement.setQueryTimeout(2);
      try (var rows = statement.executeQuery()) {
        if (!rows.next() || rows.getBytes(1) == null)
          throw new SQLException("Principal switch failed");
        return rows.getBytes(1);
      }
    }
  }

  private void discard(Connection connection) {
    pool.evictConnection(connection);
    try {
      connection.close();
    } catch (SQLException ignored) {
      /* Already evicted. */
    }
  }

  private final class JdbcScope implements TransactionRunner.Scope {
    private final Connection connection;
    private final byte[] cookie;
    private final EntityManager em;

    JdbcScope(Connection connection, byte[] cookie, EntityManager em) {
      this.connection = connection;
      this.cookie = cookie;
      this.em = em;
    }

    @Override
    public EntityManager entityManager() {
      return em;
    }

    @Override
    public void close() {
      try {
        try {
          if (em.getTransaction().isActive()) em.getTransaction().rollback();
        } finally {
          em.close();
        }
        if (!connection.getAutoCommit()) {
          connection.rollback();
          connection.setAutoCommit(true);
        }
        // Revert even when an SP changed the execution context; never return a dirty connection.
        try (var statement =
            connection.prepareStatement(
                "DECLARE @cookie varbinary(8000)=?; REVERT WITH COOKIE=@cookie;")) {
          statement.setBytes(1, cookie);
          statement.setQueryTimeout(2);
          statement.execute();
        }
        ActorSessionContext.clear(connection);
        connection.close();
      } catch (SQLException | RuntimeException ex) {
        discard(connection);
        throw new IllegalStateException("Database connection cleanup failed", ex);
      }
    }
  }

  @Override
  public synchronized void close() {
    closed = true;
    try {
      if (factory != null) factory.close();
    } finally {
      pool.close();
    }
  }
}
