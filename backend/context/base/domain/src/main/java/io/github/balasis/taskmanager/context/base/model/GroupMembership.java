package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// join table between User and Group. every user in a group has exactly one of these.
// the role determines what they can do (see RolePolicyService for the permission matrix).
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GroupMemberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","group_id"}),
        indexes = @Index(name = "idx_gm_group", columnList = "group_id")
)
public class GroupMembership extends BaseModel{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // tracks when this member last viewed the group events log.
    // compared against the latest event timestamp to show unread badge in UI.
    @Column
    private Instant lastSeenGroupEvents;

    @PrePersist
    protected void onCreate(){
        lastSeenGroupEvents = Instant.now();
    }

}
