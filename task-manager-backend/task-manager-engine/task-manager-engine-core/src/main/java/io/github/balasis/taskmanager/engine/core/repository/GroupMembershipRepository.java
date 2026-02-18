package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    Page<GroupMembership> findByGroup_Id(Long groupId, Pageable pageable);

        @EntityGraph(attributePaths = {"user"})
        @Query("""
        select gm
        from GroupMembership gm
        where gm.group.id = :groupId
            and (:q IS NULL OR lower(gm.user.name) like lower(concat('%', :q, '%')) OR lower(gm.user.email) like lower(concat('%', :q, '%')))
""")
        Page<GroupMembership> searchByGroupIdAndUser(@Param("groupId") Long groupId, @Param("q") String q, Pageable pageable);


    @Modifying
    @Query("delete from GroupMembership gm where gm.group.id = :groupId and gm.user.id = :userId")
    void deleteByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);

    Optional<GroupMembership> findByGroup_IdAndRole(Long groupId, Role role);

    GroupMembership findByUser_IdAndGroup_Id(Long currentUserId, Long groupId);

    long countByUser_Id(Long userId);
}
