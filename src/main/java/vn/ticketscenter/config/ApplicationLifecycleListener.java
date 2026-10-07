package vn.ticketscenter.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initializes application-level resources at startup.
 * Registered in web.xml (not @WebListener — web.xml controls ordering).
 * Owner: Khánh (KHANH-01)
 */
public class ApplicationLifecycleListener implements ServletContextListener {

    private static final Logger log = Logger.getLogger(
            ApplicationLifecycleListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        log.info("TicketsCenter starting...");
        try {
            ApplicationConfig config = ApplicationConfig.getInstance();
            ctx.setAttribute("appConfig", config);
            ctx.setAttribute("appEnv", config.getAppEnv());
            ctx.setAttribute("appBaseUrl", config.getAppBaseUrl());
            // KHANH-03: PersistenceFactory.init(config) will be added here
            log.info("TicketsCenter started: env=" + config.getAppEnv());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Startup failed: " + e.getMessage(), e);
            throw new RuntimeException("Application startup failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("TicketsCenter shutting down...");
        // KHANH-03: PersistenceFactory.shutdown() will be added here
    }
}
