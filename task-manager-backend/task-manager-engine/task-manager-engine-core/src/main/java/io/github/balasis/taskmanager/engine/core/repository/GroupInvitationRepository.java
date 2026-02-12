package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.model.GroupInvitation;
import io.github.balasis.taskmanager.context.base.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

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
    Set<GroupInvitation> findByUser_IdAndInvitationStatus(Long userId, InvitationStatus status);
    boolean existsByUser_IdAndGroup_IdAndInvitationStatus(Long userId, Long groupId, InvitationStatus status);
}
