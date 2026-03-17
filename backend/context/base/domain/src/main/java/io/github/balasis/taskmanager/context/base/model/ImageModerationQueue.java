package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// async content safety moderation queue. when a user uploads a profile or group image
// we save the blob immediately (so the UI feels instant) then insert a row here.
// the ImageModerationDrainer picks it up every 10 seconds, downloads the blob,
// sends it to Azure Content Safety, and if rejected: deletes the blob, reverts
// the entity's imgUrl to its previous value, refunds the scan credit, and applies
// escalation bans based on how many times the user has been caught.
@Entity
@Table(name = "ImageModerationQueue")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImageModerationQueue extends BaseModel {

    @Column(nullable = false)
    private Long userId;

    // entityType is "USER" or "GROUP" and entityId is the PK of whichever
    // entity the image belongs to. we need both to know where to revert on rejection.
    @Column(nullable = false, length = 20)
    private String entityType; // USER or GROUP

    @Column(nullable = false)
    private Long entityId;

    // the blob name of the newly uploaded image that needs scanning
    @Column(nullable = false, length = 500)
    private String newBlobName;

    // the blob name of whatever image was there before (so we can revert on rejection)
    // null if there was no previous custom image
    @Column(length = 500)
    private String previousBlobName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 30)
    private String rejectedCategory;

    private Integer rejectedSeverity;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    @PrePersist
    void defaults() {
        if (status == null) status = "PENDING";
        if (createdAt == null) createdAt = Instant.now();
    }
}
