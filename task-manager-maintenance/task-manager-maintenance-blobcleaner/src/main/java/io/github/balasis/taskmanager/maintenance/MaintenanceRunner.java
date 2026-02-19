package io.github.balasis.taskmanager.maintenance;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.service.BlobCleanerService;
import io.github.balasis.taskmanager.maintenance.service.UserCleanupService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@EnableScheduling
public class MaintenanceRunner extends BaseComponent implements CommandLineRunner {

    private final BlobCleanerService blobCleanerService;
    private final UserCleanupService userCleanupService;

    @Override
    public void run(String... args) {
        logger.trace("Maintenance test run starting...");
        runAllJobs();
        logger.trace("Test run finished.");
    }

    @Scheduled(cron = "0 0 3 * * ?", zone = "UTC")
    public void scheduledRun() {
        logger.trace("Maintenance scheduled run starting...");
        runAllJobs();
        logger.trace("Scheduled run finished.");
    }

    private void runAllJobs() {
        for (BlobContainerType type : BlobContainerType.values()) {
            blobCleanerService.clean(type);
        }
        userCleanupService.cleanInactiveUsers();
    }
}
