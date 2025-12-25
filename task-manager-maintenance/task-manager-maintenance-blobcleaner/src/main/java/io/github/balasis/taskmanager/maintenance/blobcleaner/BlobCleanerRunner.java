package io.github.balasis.taskmanager.maintenance.blobcleaner;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.blobcleaner.service.BlobCleanerService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@EnableScheduling
public class BlobCleanerRunner implements CommandLineRunner {

    private final BlobCleanerService blobCleanerService;

    @Override
    public void run(String... args) {
        System.out.println("Blob Cleaner test run starting...");
        cleanAllContainers();
        System.out.println("Test run finished.");
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledRun() {
        System.out.println("Blob Cleaner scheduled run starting...");
        cleanAllContainers();
        System.out.println("Scheduled run finished.");
    }

    private void cleanAllContainers() {
        for (BlobContainerType type : BlobContainerType.values()) {
            blobCleanerService.clean(type);
        }
    }
}
