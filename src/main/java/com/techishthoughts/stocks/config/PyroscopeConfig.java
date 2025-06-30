package com.techishthoughts.stocks.config;

import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration for Grafana Pyroscope continuous profiling.
 *
 * Provides CPU profiling, memory allocation tracking, and method-level performance insights
 * for the Spring Boot + Pekko Actors application.
 *
 * @see <a href="https://grafana.com/docs/pyroscope/latest/configure-client/language-sdks/java/">Grafana Pyroscope Java SDK</a>
 */
@Configuration
public class PyroscopeConfig {

    private static final Logger log = LoggerFactory.getLogger(PyroscopeConfig.class);

    @Value("${pyroscope.enabled:true}")
    private boolean enabled;

    @Value("${pyroscope.application-name:${spring.application.name:stocks-service}}")
    private String applicationName;

    @Value("${pyroscope.server-address:http://localhost:4040}")
    private String serverAddress;

    @Value("${pyroscope.basic-auth-user:}")
    private String basicAuthUser;

    @Value("${pyroscope.basic-auth-password:}")
    private String basicAuthPassword;

    @Value("${pyroscope.tenant-id:}")
    private String tenantId;

    @Value("${pyroscope.profiling-interval:10ms}")
    private String profilingInterval;

    @Value("${pyroscope.profiling-alloc:512k}")
    private String profilingAlloc;

    @Value("${pyroscope.profiling-lock:10ms}")
    private String profilingLock;

    @Value("${pyroscope.format:jfr}")
    private String format;

    @Value("${pyroscope.log-level:info}")
    private String logLevel;

    /**
     * Initialize Pyroscope profiling after the application context is fully loaded.
     * This ensures all beans are initialized before profiling starts.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initializePyroscope() {
        if (!enabled) {
            log.info("Pyroscope profiling is disabled");
            return;
        }

        try {
            log.info("Starting Pyroscope profiling for application: {}", applicationName);

            Config.Builder configBuilder = new Config.Builder()
                    .setApplicationName(applicationName)
                    .setProfilingEvent(EventType.ITIMER)
                    .setServerAddress(serverAddress);

            // Configure authentication if provided (for Grafana Cloud)
            if (!basicAuthUser.isEmpty() && !basicAuthPassword.isEmpty()) {
                configBuilder
                        .setBasicAuthUser(basicAuthUser)
                        .setBasicAuthPassword(basicAuthPassword);
                log.info("Pyroscope authentication configured for Grafana Cloud");
            }

            // Configure tenant ID if provided (for multi-tenancy)
            if (!tenantId.isEmpty()) {
                configBuilder.setTenantID(tenantId);
                log.info("Pyroscope tenant ID configured: {}", tenantId);
            }

            PyroscopeAgent.start(configBuilder.build());

            log.info("Pyroscope profiling started successfully");
            log.info("  - Application: {}", applicationName);
            log.info("  - Server: {}", serverAddress);
            log.info("  - Event: ITIMER (CPU profiling)");

        } catch (Exception e) {
            log.error("Failed to start Pyroscope profiling", e);
            // Don't fail the application startup if profiling fails
        }
    }
}
