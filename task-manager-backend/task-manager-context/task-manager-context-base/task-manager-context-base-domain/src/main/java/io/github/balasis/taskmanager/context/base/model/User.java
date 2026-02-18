package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.enumeration.SystemRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.security.SecureRandom;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private SystemRole systemRole = SystemRole.USER;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskParticipant> taskParticipants  = new HashSet<>();

    @Column
    private Instant lastSeenInvites;

    @Column
    private Instant lastInviteReceivedAt;

    @Column
    private Instant lastActiveAt;

    @Column(length = 64)
    private String cacheKey;

    @Column
    private Instant cacheKeyCreatedAt;

    @Column(unique = true, length = 8)
    private String inviteCode;

    @Column
    private Instant inviteCodeCreatedAt;

    @PrePersist
    protected void onCreate(){
        lastSeenInvites = Instant.now();
        lastActiveAt = Instant.now();
        rotateCacheKey();
        if (inviteCode == null) refreshInviteCode();
    }

    public void rotateCacheKey() {
        this.cacheKey = UUID.randomUUID().toString().replace("-", "");
        this.cacheKeyCreatedAt = Instant.now();
    }

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom CODE_RNG = new SecureRandom();
    private static final int CODE_LEN = 8;

    public void refreshInviteCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CODE_CHARS.charAt(CODE_RNG.nextInt(CODE_CHARS.length())));
        }
        this.inviteCode = sb.toString();
        this.inviteCodeCreatedAt = Instant.now();
    }


}
