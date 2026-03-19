package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// the junction table between users and groups, with a Role column.
// findByUserIdWithGroup is the main query for the dashboard sidebar —
// it loads all groups a user belongs to in one shot with fetch joins.
@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership,Long> {

    Optional<GroupMembership> findByUserIdAndGroupId(Long userId, Long groupId);

    @Query("""
    select gm
    from GroupMembership gm
    join fetch gm.group g
    join fetch g.owner
    where gm.user.id = :userId
""")
    List<GroupMembership> findByUserIdWithGroup(@Param("userId") Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMembership> findByGroupIdAndUserId(Long groupId, Long reviewerId);

    @Modifying
    void deleteAllByGroup_Id(Long groupId);

    @EntityGraph(attributePaths = {"user"})
    Page<GroupMembership> findByGroup_Id(Long groupId, Pageable pageable);

    @Query("SELECT gm FROM GroupMembership gm JOIN FETCH gm.user WHERE gm.id = :id")
    Optional<GroupMembership> findByIdWithUser(@Param("id") Long id);

        @EntityGraph(attributePaths = {"user"})
        @Query("""
        select gm
        from GroupMembership gm
        where gm.group.id = :groupId
            and (:q IS NULL OR gm.user.name like concat('%', :q, '%') OR gm.user.email like concat('%', :q, '%'))
""")
        Page<GroupMembership> searchByGroupIdAndUser(@Param("groupId") Long groupId, @Param("q") String q, Pageable pageable);

    @Modifying
    @Query("delete from GroupMembership gm where gm.group.id = :groupId and gm.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    Optional<GroupMembership> findByGroup_IdAndRole(Long groupId, Role role);

    GroupMembership findByUser_IdAndGroup_Id(Long currentUserId, Long groupId);

    long countByUser_Id(Long userId);

    long countByGroup_Id(Long groupId);
}
