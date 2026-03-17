package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// Singleton row (id = 1) tracking the last successful run of each maintenance mode.
// The maintenance job upserts this after every run; the backend reads it
// to detect stale maintenance (fallback alerting).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MaintenanceStatus")
public class MaintenanceStatus {

    @Id
    private Long id;

    @Column
    private Instant lastBlobCleanupAt;

    @Column
    private Instant lastFullCleanupAt;

    @Column
    @Builder.Default
    private int lastOrphanCount = 0;

    @Column
    @Builder.Default
    private int lastBlobsScanned = 0;

    @Column
    private Instant nextResetAt;
}
