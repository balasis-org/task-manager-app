package io.github.balasis.taskmanager.engine.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// wires JPA: entities live in context.base.model (domain module),
// repositories live in engine.core.repository (core module).
// without this, Spring Boot's default scan wouldn't find them since
// they're in different Maven modules with different base packages.
@Configuration
@EntityScan(basePackages = "io.github.balasis.taskmanager.context.base.model")
@EnableJpaRepositories(basePackages = "io.github.balasis.taskmanager.engine.core.repository")
public class CoreDatabaseConfiguration {
}
