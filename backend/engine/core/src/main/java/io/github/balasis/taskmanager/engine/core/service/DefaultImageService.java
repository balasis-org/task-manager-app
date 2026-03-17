package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.DefaultImage;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.DefaultImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// manages the pool of pre-seeded default images that live in Azure Blob Storage.
// these get loaded at startup by DefaultImageBootstrap and stored in the DB.
// groups and users get assigned a random default image at creation time.
@Service
@RequiredArgsConstructor
public class DefaultImageService {

    private final DefaultImageRepository defaultImageRepository;

    // not using a ThreadLocalRandom because this isnt performance-critical.
    // just picks a random default image for new groups/users.
    public String pickRandom(BlobContainerType type) {
        List<DefaultImage> images = defaultImageRepository.findByType(type);
        if (images.isEmpty()) return null;
        int idx = new Random().nextInt(images.size());
        return images.get(idx).getFileName();
    }

    // deterministic pick: alphabetically first. used by tests and bootstrap
    // so we get consistent results.
    public String pickFirst(BlobContainerType type) {
        List<DefaultImage> images = defaultImageRepository.findByType(type);
        if (images.isEmpty()) return null;
        return images.stream()
                .map(DefaultImage::getFileName)
                .min(String::compareTo)
                .orElse(null);
    }

    public List<String> findAll(BlobContainerType type) {
        return defaultImageRepository.findByType(type)
                .stream()
                .map(DefaultImage::getFileName)
                .collect(Collectors.toList());
    }
}
