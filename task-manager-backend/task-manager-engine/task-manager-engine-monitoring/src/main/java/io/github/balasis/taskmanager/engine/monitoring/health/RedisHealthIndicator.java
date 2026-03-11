package io.github.balasis.taskmanager.engine.monitoring.health;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

// Health indicator for Redis - sends PING and reports UP/DOWN for /actuator/health.
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final StatefulRedisConnection<byte[], byte[]> redisConnection;

    public RedisHealthIndicator(StatefulRedisConnection<byte[], byte[]> redisConnection) {
        this.redisConnection = redisConnection;
    }

    @Override
    public Health health() {
        try {
            RedisCommands<byte[], byte[]> commands = redisConnection.sync();
            String pong = commands.ping();
            if ("PONG".equals(pong)) {
                return Health.up().build();
            }
            return Health.down().withDetail("response", pong).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
