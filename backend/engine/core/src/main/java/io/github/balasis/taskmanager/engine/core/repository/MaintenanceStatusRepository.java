package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

// tracks the last run timestamp and status of the maintenance module.
// there's only ever one row in this table; it acts as a singleton config.
public interface MaintenanceStatusRepository extends JpaRepository<MaintenanceStatus, Long> {
}
