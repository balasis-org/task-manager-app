package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.DefaultImage;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefaultImageRepository extends JpaRepository<DefaultImage,Long> {
    List<DefaultImage> findByType(BlobContainerType type);
    boolean existsByTypeAndFileName(BlobContainerType type, String fileName);
}
