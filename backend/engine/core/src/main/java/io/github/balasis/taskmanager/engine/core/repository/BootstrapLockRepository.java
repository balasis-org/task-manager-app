package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.BootstrapLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BootstrapLockRepository extends JpaRepository<BootstrapLock, Long> {

    Optional<BootstrapLock> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BootstrapLock b WHERE b.name = :name")
    Optional<BootstrapLock> findByNameForUpdate(@Param("name") String name);
}
