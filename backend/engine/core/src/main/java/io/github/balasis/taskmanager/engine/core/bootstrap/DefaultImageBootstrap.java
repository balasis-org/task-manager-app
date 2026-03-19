package io.github.balasis.taskmanager.engine.core.bootstrap;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.model.DefaultImage;
import io.github.balasis.taskmanager.shared.enums.BlobDefaultImageContainer;
import io.github.balasis.taskmanager.engine.core.repository.DefaultImageRepository;
import io.github.balasis.taskmanager.engine.infrastructure.bootstrap.StartupGate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// seeds the default_images table with filenames for profile and group avatars.
// runs at HIGHEST_PRECEDENCE +10 so it finishes before DataLoader starts assigning
// images to seed users. the actual PNG files live in blob storage (Azurite locally,
// Azure Blob in prod) — this just ensures the DB rows exist.
@Component
@RequiredArgsConstructor
public class DefaultImageBootstrap extends BaseComponent {

    private final DefaultImageRepository defaultImageRepository;
    private final StartupGate startupGate;

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public void init() {
        logger.info("DefaultImageBootstrap initiated");
        for (BlobDefaultImageContainer container : BlobDefaultImageContainer.values()) {
            logger.info("DefaultImageBootstrap seeding {}", container.getContainerName());
            seed(container);
        }
        startupGate.markImagesReady();
        logger.info("DefaultImageBootstrap finished");
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
