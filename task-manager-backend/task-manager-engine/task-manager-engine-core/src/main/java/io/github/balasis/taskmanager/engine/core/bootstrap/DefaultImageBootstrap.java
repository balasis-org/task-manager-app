package io.github.balasis.taskmanager.engine.core.bootstrap;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.model.DefaultImage;
import io.github.balasis.taskmanager.contracts.enums.BlobDefaultImageContainer;
import io.github.balasis.taskmanager.engine.core.repository.DefaultImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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
                                .type(container.getTypeColumn()) // from enum
                                .fileName(fileName)
                                .build()
                );
            }
        }
    }
}
