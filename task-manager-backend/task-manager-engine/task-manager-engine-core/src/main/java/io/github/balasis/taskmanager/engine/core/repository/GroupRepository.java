package io.github.balasis.taskmanager.engine.core.repository;


import io.github.balasis.taskmanager.context.base.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group,Long> {
    boolean existsByNameAndOwner_Id(String name, Long ownerId);
    boolean existsByNameAndOwner_IdAndIdNot(String name, Long ownerId, Long id);

    @Query("""
        SELECT DISTINCT g
        FROM Group g
        LEFT JOIN FETCH g.owner
        LEFT JOIN FETCH g.tasks t
        LEFT JOIN FETCH t.taskParticipants tp
        LEFT JOIN FETCH tp.user
        WHERE g.id = :groupId
    """)
    Optional<Group> findByIdWithTasksAndParticipants(@Param("groupId") Long groupId);
}
