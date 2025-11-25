package io.github.balasis.taskmanager.engine.core.repository;


import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group,Long> {
    boolean existsByNameAndOwner_Id(String name, Long ownerId);
    boolean existsByNameAndOwner_IdAndIdNot(String name, Long ownerId, Long id);
}
