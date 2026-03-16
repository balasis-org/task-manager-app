package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.ImageModerationQueue;
import io.github.balasis.taskmanager.engine.core.repository.ImageModerationQueueRepository;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ImageModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"prod-h2", "prod-azuresql", "dev-h2", "dev-mssql", "dev-flyway-mssql"})
@RequiredArgsConstructor
public class JpaImageModerationService implements ImageModerationService {

    private final ImageModerationQueueRepository repository;

    @Override
    @Transactional
    public void enqueue(Long userId, String entityType, Long entityId,
                        String newBlobName, String previousBlobName) {
        repository.save(ImageModerationQueue.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .newBlobName(newBlobName)
                .previousBlobName(previousBlobName)
                .status("PENDING")
                .retryCount(0)
                .build());
    }
}
