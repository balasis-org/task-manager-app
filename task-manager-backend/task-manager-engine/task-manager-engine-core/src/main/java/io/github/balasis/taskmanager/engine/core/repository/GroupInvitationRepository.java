package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.GroupInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation,Long> {
    boolean existsByUser_IdAndGroup_Id(Long id, Long id1);

    @Query("""
        SELECT gi
        FROM GroupInvitation gi
        JOIN FETCH gi.group
        WHERE gi.id = :id
        """)
    Optional<GroupInvitation> findByIdWithGroup(@Param("id") Long id);
}
