package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// a workspace that users collaborate in. the owner (who created it) is the one whose
// subscription plan limits apply to the whole group (member caps, storage budget, etc.)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Groups" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name","owner_id"})
})
public class Group extends BaseModel{

    @Column(name="name", nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 150)
    private String announcement;

    @Column(length = 500)
    private String defaultImgUrl;

    @Column(length = 500)
    private String imgUrl;

    // LAZY because we only need the owner when checking plan limits or mapping
    // to the DTO. fetched explicitly via JOIN FETCH in repo queries when needed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id",nullable = false)
    private User owner;

    // cascade ALL + orphanRemoval on all children so deleting a group
    // takes everything with it in one transaction
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupEvent> groupEvent = new HashSet<>();

    @Column
    private Instant lastGroupEventDate;

    @Column
    @Builder.Default
    private Boolean allowEmailNotification = true;

    @Column
    @Builder.Default
    private Boolean allowAssigneeEmailNotification = false;

    // these timestamps are the backbone of poll-based sync. the frontend calls
    // refreshGroup and only gets new data when lastChangeInGroup has advanced.
    // we split them so the poller can tell if it needs to refetch tasks vs members.
    // lastChangeInGroupNoJoins is for lightweight changes that dont touch child entities.
    @Column
    private Instant lastChangeInGroup;

    @Column
    private Instant lastChangeInGroupNoJoins;

    // set when a task gets deleted, so the refresh endpoint can send tombstones
    @Column
    private Instant lastDeleteTaskDate;

    // set when a member joins/leaves/changes role, triggers member list refresh
    @Column
    private Instant lastMemberChangeDate;

    // set by the maintenance job after cleanup, used for staleness detection
    @Column
    private Instant lastMaintenanceDate;

    @Column
    private Instant createdAt;

    // group-level overrides for file limits. when these are null the system
    // falls back to the owner's plan defaults from PlanLimits.
    // if the task also has overrides, task wins over group.
    // this 3-tier cascade (plan -> group -> task) lets admins fine-tune per context.
    @Column
    private Integer maxCreatorFilesPerTask;

    @Column
    private Integer maxAssigneeFilesPerTask;

    @Column
    private Long maxFileSizeBytes;

    // when true, downloads from this group count against the owner's monthly budget
    @Column
    @Builder.Default
    private Boolean dailyDownloadCapEnabled = true;

    // when true the maintenance DowngradeCleanupService skips this group
    // even if the owner downgraded. admin can set this for special cases.
    @Column
    @Builder.Default
    private Boolean downgradeShielded = false;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
    }
}
