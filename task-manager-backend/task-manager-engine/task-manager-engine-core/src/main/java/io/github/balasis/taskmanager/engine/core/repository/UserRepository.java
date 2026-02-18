package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByAzureKey(String azureKey);

    Optional<User> findByEmail(String email);

    boolean existsByAzureKey(String azureKey);

    Optional<User> findByInviteCode(String inviteCode);

    @Query("""
    select u
    from User u
    where (:q IS NULL OR lower(u.name) like lower(concat('%',:q,'%') )
              OR lower(u.email) like lower(concat('%',:q,'%')))
    """)
    Page<User> searchUser(@Param("q") String q, Pageable pageable);

    @Query("""
    select u
    from User u
    where (:q IS NULL OR lower(u.name) like lower(concat('%',:q,'%'))
              OR lower(u.email) like lower(concat('%',:q,'%')))
      and not exists (
          select gm.id from GroupMembership gm where gm.user = u and gm.group.id = :groupId
      )
      and (:tenantId IS NULL OR u.tenantId = :tenantId)
    """)
    Page<User> searchUserForInvites(@Param("groupId") Long groupId, @Param("q") String q, @Param("tenantId") String tenantId, Pageable pageable);
}
