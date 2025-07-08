package com.techishthoughts.stocks.config;

import org.apache.pekko.actor.typed.ActorSystem;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.techishthoughts.stocks.infrastructure.PekkoConfig;

/**
 * Basic application health indicator that checks core system components
 * without being blocked by background processes like stock data initialization.
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

    private final ActorSystem<PekkoConfig.Command> actorSystem;

    public ApplicationHealthIndicator(ActorSystem<PekkoConfig.Command> actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public Health health() {
        try {
            // Check if the actor system is available
            if (actorSystem != null) {
                return Health.up()
                        .withDetail("component", "Core Application")
                        .withDetail("actorSystem", "Available")
                        .withDetail("status", "Application is ready to serve requests")
                        .withDetail("systemName", actorSystem.name())
                        .build();
            } else {
                return Health.down()
                        .withDetail("component", "Core Application")
                        .withDetail("actorSystem", "Not available")
                        .withDetail("status", "Application is not ready")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("component", "Core Application")
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Error checking application health")
                    .build();
        }
    }
}
