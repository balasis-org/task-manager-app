package io.github.balasis.taskmanager.engine.core.repository;

import io.github.balasis.taskmanager.context.base.model.GroupEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

// audit log for group-level events (member joined, role changed, etc.)
// paginated because groups can accumulate a lot of events over time
@Repository
public interface GroupEventRepository extends JpaRepository<GroupEvent,Long> {
    Page<GroupEvent> findAllByGroup_Id(Long groupId, Pageable pageable);

    @Modifying
    void deleteAllByGroup_Id(Long groupId);
}
