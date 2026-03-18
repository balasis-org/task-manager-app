package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

// refresh tokens are stored in DB so we can revoke them on logout/ban.
// deleteAllByUser_Id wipes all sessions when a user is banned or deleted.
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    @Modifying
    void deleteAllByUser_Id(Long userId);
}
