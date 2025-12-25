package io.github.balasis.taskmanager.maintenance.blobcleaner;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.blobcleaner.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.blobcleaner.service.BlobCleanerService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@EnableScheduling
public class BlobCleanerRunner extends BaseComponent implements CommandLineRunner {

    private final BlobCleanerService blobCleanerService;

    @Override
    public void run(String... args) {
        logger.trace("Blob Cleaner test run starting...");
        cleanAllContainers();
        logger.trace("Test run finished.");
    }

    @Scheduled(cron = "0 0 3 * * ?", zone = "UTC")
    public void scheduledRun() {
        logger.trace("Blob Cleaner scheduled run starting...");
        cleanAllContainers();
        logger.trace("Scheduled run finished.");
    }

    private void cleanAllContainers() {
        for (BlobContainerType type : BlobContainerType.values()) {
            blobCleanerService.clean(type);
        }
    }
}
