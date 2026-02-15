package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.DefaultImage;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.DefaultImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DefaultImageService {

    private final DefaultImageRepository defaultImageRepository;

    public String pickRandom(BlobContainerType type) {
        List<DefaultImage> images = defaultImageRepository.findByType(type);
        if (images.isEmpty()) return null;
        int idx = new Random().nextInt(images.size());
        return images.get(idx).getFileName();
    }
}