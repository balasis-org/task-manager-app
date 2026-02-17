package io.github.balasis.taskmanager.context.base.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RefreshTokens")
public class RefreshToken extends BaseModel{
    @Column(nullable = false, length = 128)
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
