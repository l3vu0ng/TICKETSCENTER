package vn.ticketscenter.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/** Loads configuration before the persistence listener. */
public final class ApplicationLifecycleListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        var config = ApplicationConfig.from(System.getenv());
        var context = event.getServletContext();
        context.setAttribute("appConfig", config);
        context.setAttribute("clockProvider", SystemClockProvider.getInstance());
        context.setSessionTimeout(config.getSessionTimeoutMinutes());
        var cookie = context.getSessionCookieConfig();
        cookie.setHttpOnly(true);
        cookie.setSecure(config.isProduction() || config.getAppBaseUrl().startsWith("https://"));
        cookie.setAttribute("SameSite", "Lax");
    }
}
