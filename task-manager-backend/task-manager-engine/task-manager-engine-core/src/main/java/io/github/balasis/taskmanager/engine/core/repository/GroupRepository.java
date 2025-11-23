package io.github.balasis.taskmanager.engine.core.repository;


import io.github.balasis.taskmanager.context.base.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group,Long> {
    boolean existsByName(String name);
}
