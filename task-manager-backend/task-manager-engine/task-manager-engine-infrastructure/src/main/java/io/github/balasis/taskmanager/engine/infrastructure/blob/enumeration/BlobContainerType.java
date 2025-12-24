package io.github.balasis.taskmanager.engine.infrastructure.blob.enumeration;

import lombok.Getter;

@Getter
public enum BlobContainerType {

    TASK_FILES("task-files", false),
    PROFILE_IMAGES("profile-images", true),
    GROUP_IMAGES("group-images", true),
    TASK_IMAGES("task-images", true);

    private final String containerName;
    private final boolean isPublic;

    BlobContainerType(String containerName, boolean isPublic) {
        this.containerName = containerName;
        this.isPublic = isPublic;
    }

}
