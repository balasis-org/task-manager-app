package io.github.balasis.taskmanager.contracts.enums;

import lombok.Getter;

// config for the default image seeding at startup. we keep 4 profile defaults
// and 4 group defaults in the "default-images" container. DefaultImageBootstrap
// reads these from local disk and uploads them to blob storage, then saves
// their blob names in the default_images table.
// filePrefix distinguishes profile vs group images in the container.
@Getter
public enum BlobDefaultImageContainer {

    PROFILE_IMAGES(
            "default-images",
            "default_images",
            BlobContainerType.PROFILE_IMAGES,
            "fileName",
            false,
            "profile",
            4
    ),
    GROUP_IMAGES(
            "default-images",
            "default_images",
            BlobContainerType.GROUP_IMAGES,
            "fileName",
            false,
            "group",
            4
    );

    private final String containerName;
    private final String tableName;
    private final BlobContainerType  typeColumn;
    private final String fileNameColumn;
    private final boolean isPublic;
    private final String filePrefix;
    private final int maxCount;

    BlobDefaultImageContainer(
            String containerName,
            String tableName,
            BlobContainerType typeColumn,
            String fileNameColumn,
            boolean isPublic,
            String filePrefix,
            int maxCount
    ) {
        this.containerName = containerName;
        this.tableName = tableName;
        this.typeColumn = typeColumn;
        this.fileNameColumn = fileNameColumn;
        this.isPublic = isPublic;
        this.filePrefix = filePrefix;
        this.maxCount = maxCount;
    }
}
