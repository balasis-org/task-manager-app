package io.github.balasis.taskmanager.engine.core.test.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// not a runnable application — this class exists only to give @DataJpaTest
// a Spring Boot configuration to latch onto.
//
// the real application entry point (TaskManagerCore) lives in context-web-domain,
// which is a different Maven module and not on engine-core's test classpath.
// without this class, @DataJpaTest would fail with:
//   "Unable to find a @SpringBootConfiguration"
//
// the two scan annotations tell Spring where to find entities and repositories
// across the multi-module boundary — entities live in context-base-domain,
// repositories live here in engine-core.
@SpringBootApplication
@EntityScan(basePackages = "io.github.balasis.taskmanager.context.base.model")
@EnableJpaRepositories(basePackages = "io.github.balasis.taskmanager.engine.core.repository")
class TestApplication {
}
