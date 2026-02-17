package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users")
public class User extends BaseModel{
    @Column(nullable = false, unique = true, length = 128)
    private String azureKey;

    @Column(length = 64)
    private String tenantId;

    @Column(length = 500)
    private String defaultImgUrl;

    @Column(length = 500)
    private String imgUrl;

    @Column
    private boolean isOrg = false;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
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

    @Column(length = 64)
    private String cacheKey;

    @Column
    private Instant cacheKeyCreatedAt;

    @PrePersist
    protected void onCreate(){
        lastSeenInvites = Instant.now();
        rotateCacheKey();
    }

    public void rotateCacheKey() {
        this.cacheKey = UUID.randomUUID().toString().replace("-", "");
        this.cacheKeyCreatedAt = Instant.now();
    }


}
