package io.github.balasis.taskmanager.contracts.enums;

import lombok.Getter;

// maps each Azure Blob Storage container to the DB table and column that
// references it. the maintenance orphan scanner uses this to check if a blob
// in a container still has a matching row in the DB. if not its an orphan
// and gets deleted. containerName is the actual Azure container name,
// tableName/columnName tell the scanner where to look.
// the contracts module is shared between backend and maintenance so both
// agree on the same container layout.
@Getter
public enum BlobContainerType {

    TASK_ASSIGNEE_FILES("task-assignee-files","TaskAssigneeFiles","fileUrl",false),
    TASK_FILES("task-files", "TaskFiles", "fileUrl", false),
    PROFILE_IMAGES("profile-images", "Users", "imgUrl", false),
    GROUP_IMAGES("group-images", "Groups", "imgUrl", false);

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
