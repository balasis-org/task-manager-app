package io.github.balasis.taskmanager.maintenance;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import io.github.balasis.taskmanager.maintenance.repository.MaintenanceRepository;
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

    private static final String MODE_TASK_BLOBS = "task-blobs";
    private static final String MODE_FULL = "full";

    private final BlobCleanerService blobCleanerService;
    private final UserCleanupService userCleanupService;
    private final AssetCleanerService assetCleanerService;
    private final DatabaseCleanupService databaseCleanupService;
    private final AcrCleanerService acrCleanerService;
    private final MaintenanceRepository maintenanceRepository;

    @Autowired
    public MaintenanceRunner(
            BlobCleanerService blobCleanerService,
            UserCleanupService userCleanupService,
            AssetCleanerService assetCleanerService,
            DatabaseCleanupService databaseCleanupService,
            MaintenanceRepository maintenanceRepository,
            @Autowired(required = false) AcrCleanerService acrCleanerService
    ) {
        this.blobCleanerService = blobCleanerService;
        this.userCleanupService = userCleanupService;
        this.assetCleanerService = assetCleanerService;
        this.databaseCleanupService = databaseCleanupService;
        this.maintenanceRepository = maintenanceRepository;
        this.acrCleanerService = acrCleanerService;
    }

    @Override
    public void run(String... args) {
        String mode = resolveMode(args);
        logger.info("Maintenance run starting in [{}] mode...", mode);

        if (MODE_TASK_BLOBS.equals(mode)) {
            runTaskBlobsOnly();
        } else {
            runFull();
        }

        logger.info("Maintenance run finished (mode={}). Container will exit.", mode);
    }

    private String resolveMode(String[] args) {
        String envMode = System.getenv("MAINTENANCE_MODE");
        if (envMode != null && !envMode.isBlank()) {
            return envMode.trim().toLowerCase();
        }
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return args[0].trim().toLowerCase();
        }
        return MODE_FULL;
    }

    /**
     * Short-interval mode (every 30-45 min).
     * Only scans task-file containers where orphans accumulate fastest.
     */
    private void runTaskBlobsOnly() {
        int totalOrphans = 0;
        int totalScanned = 0;

        blobCleanerService.clean(BlobContainerType.TASK_FILES);
        blobCleanerService.clean(BlobContainerType.TASK_ASSIGNEE_FILES);

        maintenanceRepository.upsertMaintenanceStatus(false, totalOrphans, totalScanned, 0);
    }

    /**
     * Full daily mode.
     * Scans ALL blob containers (task files, images, group images),
     * cleans stale assets, purges inactive users, runs DB hygiene,
     * resets group-creation counters and optionally cleans ACR images.
     */
    private void runFull() {
        for (BlobContainerType type : BlobContainerType.values()) {
            blobCleanerService.clean(type);
        }

        assetCleanerService.cleanOldAssets();
        userCleanupService.cleanInactiveUsers();
        databaseCleanupService.cleanStaleRows();

        int resetCount = maintenanceRepository.resetGroupCreationCounters();
        logger.info("Reset group-creation counters for {} user(s).", resetCount);

        int reconciled = maintenanceRepository.reconcileStorageBudgets();
        logger.info("Reconciled storage budgets for {} user(s).", reconciled);

        int monthlyReset = maintenanceRepository.resetMonthlyBudgets();
        logger.info("Reset monthly download + email counters for {} user(s).", monthlyReset);

        if (acrCleanerService != null) {
            acrCleanerService.cleanOldImages();
        } else {
            logger.info("ACR cleanup: skipped (not available in this profile).");
        }

        String intervalStr = System.getenv("MAINTENANCE_FULL_INTERVAL_HOURS");
        long intervalHours = 24;
        if (intervalStr != null && !intervalStr.isBlank()) {
            try { intervalHours = Long.parseLong(intervalStr.trim()); }
            catch (NumberFormatException ignored) { }
        }

        maintenanceRepository.upsertMaintenanceStatus(true, 0, 0, intervalHours);
    }
}
