package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.model.TaskComment;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.mapper.outbound.UserOutboundMapper;
import io.github.balasis.taskmanager.engine.core.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController extends BaseComponent {

    private final AdminService adminService;
    private final UserOutboundMapper userOutboundMapper;

    /* ───── users ───── */

    @GetMapping("/users")
    public ResponseEntity<Page<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        Page<User> page = adminService.listUsers(q, pageable);
        return ResponseEntity.ok(page.map(this::mapUser));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /* ───── groups ───── */

    @GetMapping("/groups")
    public ResponseEntity<Page<Map<String, Object>>> listGroups(Pageable pageable) {
        Page<Group> page = adminService.listGroups(pageable);
        return ResponseEntity.ok(page.map(this::mapGroup));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
        adminService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    /* ───── tasks ───── */

    @GetMapping("/tasks")
    public ResponseEntity<Page<Map<String, Object>>> listTasks(Pageable pageable) {
        Page<Task> page = adminService.listTasks(pageable);
        return ResponseEntity.ok(page.map(this::mapTask));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        adminService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    /* ───── comments ───── */

    @GetMapping("/comments")
    public ResponseEntity<Page<Map<String, Object>>> listComments(Pageable pageable) {
        Page<TaskComment> page = adminService.listComments(pageable);
        return ResponseEntity.ok(page.map(this::mapComment));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        adminService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    /* ───── private mappers ───── */

    private Map<String, Object> mapUser(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("systemRole", u.getSystemRole());
        m.put("subscriptionPlan", u.getSubscriptionPlan());
        m.put("isOrg", u.isOrg());
        m.put("lastActiveAt", u.getLastActiveAt());
        return m;
    }

    private Map<String, Object> mapGroup(Group g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("name", g.getName());
        m.put("description", g.getDescription());
        m.put("ownerName", g.getOwner() != null ? g.getOwner().getName() : null);
        m.put("ownerEmail", g.getOwner() != null ? g.getOwner().getEmail() : null);
        m.put("memberCount", g.getMemberships() != null ? g.getMemberships().size() : 0);
        m.put("taskCount", g.getTasks() != null ? g.getTasks().size() : 0);
        m.put("createdAt", g.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapTask(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("taskState", t.getTaskState());
        m.put("priority", t.getPriority());
        m.put("groupId", t.getGroup() != null ? t.getGroup().getId() : null);
        m.put("groupName", t.getGroup() != null ? t.getGroup().getName() : null);
        m.put("creatorNameSnapshot", t.getCreatorNameSnapshot());
        m.put("commentCount", t.getCommentCount());
        m.put("createdAt", t.getCreatedAt());
        m.put("dueDate", t.getDueDate());
        return m;
    }

    private Map<String, Object> mapComment(TaskComment c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("comment", c.getComment());
        m.put("creatorName", c.getCreator() != null ? c.getCreator().getName() : c.getCreatorNameSnapshot());
        m.put("creatorEmail", c.getCreator() != null ? c.getCreator().getEmail() : null);
        m.put("taskId", c.getTask() != null ? c.getTask().getId() : null);
        m.put("taskTitle", c.getTask() != null ? c.getTask().getTitle() : null);
        m.put("createdAt", c.getCreatedAt());
        return m;
    }
}
