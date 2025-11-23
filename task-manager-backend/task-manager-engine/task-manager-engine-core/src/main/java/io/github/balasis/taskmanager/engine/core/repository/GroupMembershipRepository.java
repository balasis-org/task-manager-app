package io.github.balasis.taskmanager.engine.core.repository;

import com.azure.core.http.HttpHeaders;
import io.github.balasis.taskmanager.context.base.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership,Long> {

    Optional<GroupMembership> findByUserIdAndGroupId(Long userId, Long groupId);

    Optional<GroupMembership> findByUserId(Long userId);
}
