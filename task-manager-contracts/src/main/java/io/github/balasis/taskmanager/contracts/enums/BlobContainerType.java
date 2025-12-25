package io.github.balasis.taskmanager.contracts.enums;

import lombok.Getter;

@Getter
public enum BlobContainerType {

    TASK_FILES("task-files", "TaskFiles", "fileUrl", false),
    PROFILE_IMAGES("profile-images", "Users", "profileImageUrl", true),
    GROUP_IMAGES("group-images", "Groups", "groupImageUrl", true),
    TASK_IMAGES("task-images", "Tasks", "taskImageUrl", true);

    private final String containerName;
    private final String tableName;
    private final String columnName;
    private final boolean isPublic;

    BlobContainerType(String containerName, String tableName, String columnName, boolean isPublic) {
        this.containerName = containerName;
        this.tableName = tableName;
        this.columnName = columnName;
        this.isPublic = isPublic;
    }
}
