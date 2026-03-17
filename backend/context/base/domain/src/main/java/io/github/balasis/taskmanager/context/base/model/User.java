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
@Table(name = "Users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
public class User extends BaseModel{
    // the subject claim (oid) from Azure Entra ID tokens, this is how we match
    // a JWT to a user row. unique per tenant+user combination
    @Column(nullable = false, unique = true, length = 128)
    private String azureKey;

    // Entra ID tenant, stored so we can tell apart personal vs org accounts
    @Column(length = 64)
    private String tenantId;

    // defaultImgUrl is the Microsoft Graph profile photo we fetch at first login
    // imgUrl is what the user actually uses (could be a custom upload)
    @Column(length = 500)
    private String defaultImgUrl;

    @Column(length = 500)
    private String imgUrl;

    // true if the user authenticated via an organizational Entra ID tenant
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

    // cascade ALL + orphanRemoval because when a user is deleted we want
    // all their memberships and task participations cleaned up in one shot
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskParticipant> taskParticipants  = new HashSet<>();

    // tracks when user last checked their invitations page, used to show
    // the "new invites" badge in the frontend
    @Column
    private Instant lastSeenInvites;

    @Column
    private Instant lastInviteReceivedAt;

    @Column
    private Instant lastActiveAt;

    // cacheKey is a random string the frontend uses as part of its AES encryption
    // key for the local encrypted cache (IndexedDB). when we rotate it the frontend
    // cache becomes unreadable and the user gets a fresh state. rotated on login.
    @Column(length = 64)
    private String cacheKey;

    @Column
    private Instant cacheKeyCreatedAt;

    // 8-char alphanumeric code users can share to get invited to groups
    // excludes ambiguous chars (0, O, I, 1, L) for readability
    @Column(unique = true, length = 8)
    private String inviteCode;

    @Column
    private Instant inviteCodeCreatedAt;

    // totalGroupsCreated is a rolling counter reset by maintenance.
    // prevents delete+recreate abuse that would pile up orphan blobs
    @Column
    @Builder.Default
    private int totalGroupsCreated = 0;

    // maintenance resets this timestamp and totalGroupsCreated together
    @Column
    private Instant groupCreationBudgetResetAt;

    // running total of bytes this user's files occupy across all groups they own.
    // incremented on upload, decremented on file removal or maintenance cleanup.
    @Column
    @Builder.Default
    private long usedStorageBytes = 0;

    // monthly budget counters, all reset by the maintenance full-cleanup job.
    // download bytes are charged to the group owner when any member downloads a file.
    @Column
    @Builder.Default
    private long usedDownloadBytesMonth = 0;

    @Column
    @Builder.Default
    private int usedEmailsMonth = 0;

    // how many images this user sent through Azure Content Safety this month
    @Column
    @Builder.Default
    private int usedImageScansMonth = 0;

    // AI text analysis credits used this month (sentiment, key phrases, PII, summary)
    @Column
    @Builder.Default
    private int usedTaskAnalysisCreditsMonth = 0;

    @Column(length = 500)
    private String msProfilePhotoUrl;

    // when a user downgrades their plan we don't nuke excess resources immediately.
    // they get a grace window (set by maintenance) to clean up or re-upgrade.
    // after the deadline the maintenance DowngradeCleanupService trims excess files.
    @Column
    private Instant downgradeGraceDeadline;

    // remembers what plan they had before downgrading so maintenance knows
    // which limits applied and how much to trim
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SubscriptionPlan previousPlan;

    // 4-level escalation when content safety rejects an image upload:
    // strike 1 = 1h upload ban, strike 2 = 24h, strike 3 = 7d, strike 4 = permanent account ban.
    // uploadBannedUntil blocks new image uploads, accountBannedUntil blocks all API access.
    // the AccountBanInterceptor checks accountBannedUntil on every request.
    @Column
    private Instant uploadBannedUntil;

    @Column
    private Instant accountBannedUntil;

    // how many times content safety has rejected uploads from this user (never resets)
    @Column
    @Builder.Default
    private int uploadBanCount = 0;

    @Column
    private Instant lastUploadBanAt;

    @PrePersist
    protected void onCreate(){
        lastSeenInvites = Instant.now();
        lastActiveAt = Instant.now();
        rotateCacheKey();
        if (inviteCode == null) refreshInviteCode();
    }

    // called on every login, the new key invalidates the frontend encrypted cache
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
