package vn.ticketscenter.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public final class PersistenceListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent event) {
    var context = event.getServletContext();
    var config = (ApplicationConfig) context.getAttribute("appConfig");
    var factory = new PersistenceFactory(config);
    context.setAttribute(PersistenceFactory.ATTRIBUTE, factory);
    context.setAttribute("transactionRunner", factory.transactionRunner());
    var users = vn.ticketscenter.repository.identity.UserRepository.getInstance();
    context.setAttribute(
        "authService",
        new vn.ticketscenter.service.identity.AuthService(factory.transactionRunner(), users));
    context.setAttribute(
        "authorizationService",
        new vn.ticketscenter.service.identity.AuthorizationService(users, null));
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    var context = event.getServletContext();
    var factory = (PersistenceFactory) context.getAttribute(PersistenceFactory.ATTRIBUTE);
    if (factory != null) factory.close();
    context.removeAttribute(PersistenceFactory.ATTRIBUTE);
    context.removeAttribute("transactionRunner");
    context.removeAttribute("authService");
    context.removeAttribute("authorizationService");
    // Only drivers loaded by this WAR belong to this listener.
    java.sql.DriverManager.drivers()
        .filter(driver -> driver.getClass().getClassLoader() == getClass().getClassLoader())
        .forEach(
            driver -> {
              try {
                java.sql.DriverManager.deregisterDriver(driver);
              } catch (java.sql.SQLException ex) {
                throw new IllegalStateException("Cannot release JDBC driver", ex);
              }
            });
  }
}
