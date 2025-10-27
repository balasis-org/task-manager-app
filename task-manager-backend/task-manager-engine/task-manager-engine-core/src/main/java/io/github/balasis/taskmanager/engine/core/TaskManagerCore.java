package io.github.balasis.taskmanager.engine.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "io.github.balasis.taskmanager.context.base",
        "io.github.balasis.taskmanager.engine.core",
        "io.github.balasis.taskmanager.engine.monitoring",
})
public class TaskManagerCore {
    public static void main(String[] args) {
        SpringApplication.run(TaskManagerCore.class, args);
    }
}
