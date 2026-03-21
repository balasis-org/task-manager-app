package io.github.balasis.taskmanager.engine.core.bootstrap;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.model.BootstrapLock;
import io.github.balasis.taskmanager.context.base.model.DefaultImage;
import io.github.balasis.taskmanager.shared.enums.BlobDefaultImageContainer;
import io.github.balasis.taskmanager.engine.core.repository.BootstrapLockRepository;
import io.github.balasis.taskmanager.engine.core.repository.DefaultImageRepository;
import io.github.balasis.taskmanager.engine.infrastructure.bootstrap.StartupGate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// seeds the default_images table with filenames for profile and group avatars.
// runs at HIGHEST_PRECEDENCE +10 so it finishes before DataLoader starts assigning
// images to seed users. the actual PNG files live in blob storage (Azurite locally,
// Azure Blob in prod) — this just ensures the DB rows exist.
// multi-instance safe: a database pessimistic lock (SELECT FOR UPDATE on
// BootstrapLocks) ensures only one instance seeds; the other blocks and skips.
@Component
@RequiredArgsConstructor
public class DefaultImageBootstrap extends BaseComponent {

    private static final String LOCK_NAME = "DEFAULT_IMAGES";

    private final DefaultImageRepository defaultImageRepository;
    private final BootstrapLockRepository bootstrapLockRepository;
    private final StartupGate startupGate;
    private final PlatformTransactionManager transactionManager;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public void init() {
        logger.info("DefaultImageBootstrap initiated");
        try {
            ensureLockRowExists();

            new TransactionTemplate(transactionManager).execute(status -> {
                BootstrapLock lock = bootstrapLockRepository
                        .findByNameForUpdate(LOCK_NAME).orElseThrow();

                if (lock.isCompleted()) {
                    logger.info("Another instance already seeded default images; skipping.");
                    return null;
                }

                for (BlobDefaultImageContainer container : BlobDefaultImageContainer.values()) {
                    logger.info("DefaultImageBootstrap seeding {}", container.getContainerName());
                    seed(container);
                }

                lock.setCompleted(true);
                return null;
            });
        } finally {
            startupGate.markImagesReady();
        }
        logger.info("DefaultImageBootstrap finished");
    }

    private void ensureLockRowExists() {
        if (bootstrapLockRepository.findByName(LOCK_NAME).isPresent()) {
            return;
        }
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    bootstrapLockRepository.saveAndFlush(
                            BootstrapLock.builder().name(LOCK_NAME).completed(false).build()));
        } catch (DataIntegrityViolationException ignored) {
            // another instance created the row concurrently
        }
    }

    private void seed(BlobDefaultImageContainer container) {
        for (int i = 1; i <= container.getMaxCount(); i++) {
            String fileName = container.getFilePrefix() + i + ".png";

            if (!defaultImageRepository.existsByTypeAndFileName(container.getTypeColumn(), fileName)) {
                defaultImageRepository.save(
                        DefaultImage.builder()
                                .type(container.getTypeColumn())
                                .fileName(fileName)
                                .build()
                );
            }
        }
    }
}
