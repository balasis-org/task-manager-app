package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.enumeration.SystemRole;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.context.web.mapper.outbound.UserOutboundMapper;
import io.github.balasis.taskmanager.engine.core.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(mapUserFull(adminService.getUser(userId)));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        SystemRole systemRole = body.containsKey("systemRole")
                ? SystemRole.valueOf((String) body.get("systemRole")) : null;
        SubscriptionPlan plan = body.containsKey("subscriptionPlan")
                ? SubscriptionPlan.valueOf((String) body.get("subscriptionPlan")) : null;
        Boolean allowEmail = body.containsKey("allowEmailNotification")
                ? (Boolean) body.get("allowEmailNotification") : null;
        return ResponseEntity.ok(mapUserFull(
                adminService.updateUser(userId, name, email, systemRole, plan, allowEmail)));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /* ───── groups ───── */

    @GetMapping("/groups")
    public ResponseEntity<Page<Map<String, Object>>> listGroups(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        Page<Group> page = adminService.listGroups(q, pageable);
        return ResponseEntity.ok(page.map(this::mapGroup));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(mapGroupFull(adminService.getGroup(groupId)));
    }

    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        String announcement = (String) body.get("announcement");
        Boolean allowEmail = body.containsKey("allowEmailNotification")
                ? (Boolean) body.get("allowEmailNotification") : null;
        return ResponseEntity.ok(mapGroupFull(
                adminService.updateGroup(groupId, name, description, announcement, allowEmail)));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
        adminService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    /* ───── tasks ───── */

    @GetMapping("/tasks")
    public ResponseEntity<Page<Map<String, Object>>> listTasks(
            @RequestParam(required = false) String q,
            Pageable pageable) {
        Page<Task> page = adminService.listTasks(q, pageable);
        return ResponseEntity.ok(page.map(this::mapTask));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(mapTaskFull(adminService.getTask(taskId)));
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> updateTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        TaskState taskState = body.containsKey("taskState")
                ? TaskState.valueOf((String) body.get("taskState")) : null;
        Integer priority = body.containsKey("priority") && body.get("priority") != null
                ? ((Number) body.get("priority")).intValue() : null;
        Instant dueDate = body.containsKey("dueDate") && body.get("dueDate") != null
                ? Instant.parse((String) body.get("dueDate")) : null;
        return ResponseEntity.ok(mapTaskFull(
                adminService.updateTask(taskId, title, description, taskState, priority, dueDate)));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        adminService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    /* ───── comments ───── */

    @GetMapping("/comments")
    public ResponseEntity<Page<Map<String, Object>>> listComments(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long creatorId,
            Pageable pageable) {
        Page<TaskComment> page = adminService.listComments(taskId, groupId, creatorId, pageable);
        return ResponseEntity.ok(page.map(this::mapComment));
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> getComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(mapCommentFull(adminService.getComment(commentId)));
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> body) {
        String comment = (String) body.get("comment");
        return ResponseEntity.ok(mapCommentFull(
                adminService.updateComment(commentId, comment)));
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

    private Map<String, Object> mapUserFull(User u) {
        Map<String, Object> m = mapUser(u);
        m.put("allowEmailNotification", u.getAllowEmailNotification());
        m.put("tenantId", u.getTenantId());
        m.put("lastSeenInvites", u.getLastSeenInvites());
        m.put("lastInviteReceivedAt", u.getLastInviteReceivedAt());
        m.put("createdAt", u.getLastActiveAt()); // closest proxy
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

    private Map<String, Object> mapGroupFull(Group g) {
        Map<String, Object> m = mapGroup(g);
        m.put("announcement", g.getAnnouncement());
        m.put("allowEmailNotification", g.getAllowEmailNotification());
        m.put("lastGroupEventDate", g.getLastGroupEventDate());
        // members detail
        if (g.getMemberships() != null) {
            m.put("members", g.getMemberships().stream().map(gm -> {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("id", gm.getId());
                mm.put("userId", gm.getUser() != null ? gm.getUser().getId() : null);
                mm.put("userName", gm.getUser() != null ? gm.getUser().getName() : null);
                mm.put("role", gm.getRole());
                return mm;
            }).collect(Collectors.toList()));
        }
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

    private Map<String, Object> mapTaskFull(Task t) {
        Map<String, Object> m = mapTask(t);
        m.put("description", t.getDescription());
        m.put("reviewersDecision", t.getReviewersDecision());
        m.put("reviewComment", t.getReviewComment());
        m.put("reviewedBy", t.getReviewedBy() != null ? t.getReviewedBy().getName() : null);
        // participants
        if (t.getTaskParticipants() != null) {
            m.put("participants", t.getTaskParticipants().stream().map(tp -> {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("id", tp.getId());
                pm.put("userId", tp.getUser() != null ? tp.getUser().getId() : null);
                pm.put("userName", tp.getUser() != null ? tp.getUser().getName() : null);
                pm.put("role", tp.getTaskParticipantRole());
                return pm;
            }).collect(Collectors.toList()));
        }
        // creator files
        if (t.getCreatorFiles() != null) {
            m.put("creatorFiles", t.getCreatorFiles().stream().map(f -> {
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("id", f.getId());
                fm.put("name", f.getName());
                fm.put("fileUrl", f.getFileUrl());
                return fm;
            }).collect(Collectors.toList()));
        }
        // assignee files
        if (t.getAssigneeFiles() != null) {
            m.put("assigneeFiles", t.getAssigneeFiles().stream().map(f -> {
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("id", f.getId());
                fm.put("name", f.getName());
                fm.put("fileUrl", f.getFileUrl());
                return fm;
            }).collect(Collectors.toList()));
        }
        return m;
    }

    private Map<String, Object> mapComment(TaskComment c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("comment", c.getComment());
        m.put("creatorName", c.getCreator() != null ? c.getCreator().getName() : c.getCreatorNameSnapshot());
        m.put("creatorEmail", c.getCreator() != null ? c.getCreator().getEmail() : null);
        m.put("creatorId", c.getCreator() != null ? c.getCreator().getId() : null);
        m.put("taskId", c.getTask() != null ? c.getTask().getId() : null);
        m.put("taskTitle", c.getTask() != null ? c.getTask().getTitle() : null);
        m.put("groupId", c.getTask() != null && c.getTask().getGroup() != null ? c.getTask().getGroup().getId() : null);
        m.put("groupName", c.getTask() != null && c.getTask().getGroup() != null ? c.getTask().getGroup().getName() : null);
        m.put("createdAt", c.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapCommentFull(TaskComment c) {
        Map<String, Object> m = mapComment(c);
        return m;
    }
}
