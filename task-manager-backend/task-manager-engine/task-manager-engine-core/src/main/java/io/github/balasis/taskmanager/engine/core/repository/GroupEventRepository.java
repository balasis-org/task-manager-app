package io.github.balasis.taskmanager.engine.core.repository;


import io.github.balasis.taskmanager.context.base.model.GroupEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupEventRepository extends JpaRepository<GroupEvent,Long> {
    Page<GroupEvent> findAllByGroup_Id(Long groupId, Pageable pageable);
}
