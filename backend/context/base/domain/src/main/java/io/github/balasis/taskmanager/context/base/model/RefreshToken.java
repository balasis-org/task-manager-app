package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

// JWT refresh tokens. we use short-lived access tokens (from the JWT service)
// paired with longer-lived refresh tokens stored in the DB. the refresh code
// is stored as a SHA-256 hex digest (64 chars); the raw code lives only in the
// browser cookie. expires 24 hours after creation. the user can have multiple
// active refresh tokens (one per device/session).
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RefreshTokens", indexes = {
        @Index(name = "idx_rt_user", columnList = "user_id")
})
public class RefreshToken extends BaseModel{
    @Version
    private Long version;

    @Column(nullable = false, length = 64)
    private String refreshCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User user;
    @Column(updatable = false)
    private Instant createdAt;
    @Column(updatable = false)
    private Instant expiresAt;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
        expiresAt = createdAt.plusSeconds(60 * 60 * 24);
    }

}
