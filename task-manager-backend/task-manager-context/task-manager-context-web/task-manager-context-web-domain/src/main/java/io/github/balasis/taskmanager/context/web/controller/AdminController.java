package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.web.mapper.outbound.AdminOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.admin.outbound.*;
import io.github.balasis.taskmanager.engine.core.service.AdminService;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController extends BaseComponent {

    private final AdminService adminService;
    private final AdminOutboundMapper adminOutboundMapper;

    /* ───── users ───── */

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResource>> listUsers(
            @RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(adminService.listUsers(q, pageable)
                .map(adminOutboundMapper::toUserResource));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResource> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminOutboundMapper.toUserResource(adminService.getUser(userId)));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /* ───── groups ───── */

    @GetMapping("/groups")
    public ResponseEntity<Page<AdminGroupResource>> listGroups(
            @RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(adminService.listGroups(q, pageable)
                .map(adminOutboundMapper::toGroupListResource));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<AdminGroupResource> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(adminOutboundMapper.toGroupDetailResource(adminService.getGroup(groupId)));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
        adminService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    /* ───── tasks ───── */

    @GetMapping("/tasks")
    public ResponseEntity<Page<AdminTaskResource>> listTasks(
            @RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(adminService.listTasks(q, pageable)
                .map(adminOutboundMapper::toTaskListResource));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<AdminTaskResource> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(adminOutboundMapper.toTaskDetailResource(adminService.getTask(taskId)));
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        adminService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/{taskId}/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadTaskFile(
            @PathVariable Long taskId,
            @PathVariable Long fileId) {
        TaskFileDownload download = adminService.downloadTaskFile(taskId, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(download.content());
    }

    @GetMapping("/tasks/{taskId}/assignee-files/{fileId}/download")
    public ResponseEntity<byte[]> downloadAssigneeTaskFile(
            @PathVariable Long taskId,
            @PathVariable Long fileId) {
        TaskFileDownload download = adminService.downloadAssigneeTaskFile(taskId, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(download.content());
    }

    /* ───── comments ───── */

    @GetMapping("/comments")
    public ResponseEntity<Page<AdminCommentResource>> listComments(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long creatorId,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.listComments(taskId, groupId, creatorId, pageable)
                .map(adminOutboundMapper::toCommentResource));
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<AdminCommentResource> getComment(@PathVariable Long commentId) {
        return ResponseEntity.ok(adminOutboundMapper.toCommentResource(adminService.getComment(commentId)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        adminService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
