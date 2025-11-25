package io.github.balasis.taskmanager.context.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "io.github.balasis.taskmanager.engine.core",
        "io.github.balasis.taskmanager.context.base",
        "io.github.balasis.taskmanager.engine.monitoring",
        "io.github.balasis.taskmanager.engine.infrastructure",
        "io.github.balasis.taskmanager.context.web"
})
public class TaskManagerCore {

    public static void main(String[] args) {
      SpringApplication.run(TaskManagerCore.class, args);
    }
}
