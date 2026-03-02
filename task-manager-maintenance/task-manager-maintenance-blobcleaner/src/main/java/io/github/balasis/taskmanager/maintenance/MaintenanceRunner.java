package io.github.balasis.taskmanager.maintenance;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.service.AcrCleanerService;
import io.github.balasis.taskmanager.maintenance.service.AssetCleanerService;
import io.github.balasis.taskmanager.maintenance.service.BlobCleanerService;
import io.github.balasis.taskmanager.maintenance.service.DatabaseCleanupService;
import io.github.balasis.taskmanager.maintenance.service.UserCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceRunner extends BaseComponent implements CommandLineRunner {

    private final BlobCleanerService blobCleanerService;
    private final UserCleanupService userCleanupService;
    private final AssetCleanerService assetCleanerService;
    private final DatabaseCleanupService databaseCleanupService;
    private final AcrCleanerService acrCleanerService;

    @Autowired
    public MaintenanceRunner(
            BlobCleanerService blobCleanerService,
            UserCleanupService userCleanupService,
            AssetCleanerService assetCleanerService,
            DatabaseCleanupService databaseCleanupService,
            @Autowired(required = false) AcrCleanerService acrCleanerService
    ) {
        this.blobCleanerService = blobCleanerService;
        this.userCleanupService = userCleanupService;
        this.assetCleanerService = assetCleanerService;
        this.databaseCleanupService = databaseCleanupService;
        this.acrCleanerService = acrCleanerService;
    }

    @Override
    public void run(String... args) {
        logger.info("Maintenance run starting...");
        runAllJobs();
        logger.info("Maintenance run finished. Container will exit.");
    }

    private void runAllJobs() {
        for (BlobContainerType type : BlobContainerType.values()) {
            blobCleanerService.clean(type);
        }
        userCleanupService.cleanInactiveUsers();
        assetCleanerService.cleanOldAssets();
        databaseCleanupService.cleanStaleRows();

        if (acrCleanerService != null) {
            acrCleanerService.cleanOldImages();
        } else {
            logger.info("ACR cleanup: skipped (not available in this profile).");
        }
    }
}
