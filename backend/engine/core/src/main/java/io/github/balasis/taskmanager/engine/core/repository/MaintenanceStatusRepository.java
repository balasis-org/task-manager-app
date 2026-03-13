package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceStatusRepository extends JpaRepository<MaintenanceStatus, Long> {
}
