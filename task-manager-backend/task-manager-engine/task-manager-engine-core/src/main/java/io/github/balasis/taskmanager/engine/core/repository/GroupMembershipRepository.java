package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    boolean existsByGroupIdAndUserId(Long groupId, Long assignedId);

    Optional<GroupMembership> findByGroupIdAndUserId(Long groupId, Long reviewerId);
}
