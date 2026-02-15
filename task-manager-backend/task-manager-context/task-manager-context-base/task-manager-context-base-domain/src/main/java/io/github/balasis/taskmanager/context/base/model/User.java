package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users")
public class User extends BaseModel{
    @Column(nullable = false , unique = true)
    private String azureKey;

    @Column
    private String tenantId;

    @Column
    private String defaultImgUrl;

    @Column
    private String imgUrl;

    @Column
    private boolean isOrg = false;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column
    @Builder.Default
    private Boolean allowEmailNotification = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskParticipant> taskParticipants  = new HashSet<>();

    @Column
    private Instant lastSeenInvites;

    @PrePersist
    protected void onCreate(){
        lastSeenInvites = Instant.now();
    }


}
