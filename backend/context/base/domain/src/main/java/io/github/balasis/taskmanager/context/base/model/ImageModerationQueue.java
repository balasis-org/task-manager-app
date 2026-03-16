package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

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

    @Column(nullable = false, length = 20)
    private String entityType; // USER or GROUP

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 500)
    private String newBlobName;

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
