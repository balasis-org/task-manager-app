package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.utils.StringSanitizer;
import io.github.balasis.taskmanager.context.web.mapper.outbound.AdminOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.admin.outbound.*;
import io.github.balasis.taskmanager.engine.core.service.AdminService;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

// admin panel endpoints: paginated browsing of users/groups/tasks/comments,
// CRUD + destructive ops (delete user/group/task/comment), plan changes,
// usage resets, and file downloads (bypasses normal ownership checks).
// authorization enforced at service layer (AdminService checks SystemRole.ADMIN).
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController extends BaseComponent {

    private final AdminService adminService;
    private final AdminOutboundMapper adminOutboundMapper;
    private final PlanLimits planLimits;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResource>> listUsers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.listUsers(q, pageable)
                .map(this::toUserResourceWithLimits));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResource> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(toUserResourceWithLimits(adminService.getUser(userId)));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/groups")
    public ResponseEntity<Page<AdminGroupResource>> listGroups(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
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

    @GetMapping("/tasks")
    public ResponseEntity<Page<AdminTaskResource>> listTasks(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
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
    public ResponseEntity<StreamingResponseBody> downloadTaskFile(
            @PathVariable Long taskId,
            @PathVariable Long fileId) {
        TaskFileDownload download = adminService.downloadTaskFile(taskId, fileId);
        long timeoutMs = planLimits.computeDownloadTimeoutMs(download.size(), SubscriptionPlan.TEAM);
        StreamingResponseBody body = out -> {
            try (var in = download.content()) {
                transferWithTimeout(in, out, timeoutMs);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + StringSanitizer.sanitizeFilenameForHeader(download.filename())  + "\"")
                .contentLength(download.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @GetMapping("/tasks/{taskId}/assignee-files/{fileId}/download")
    public ResponseEntity<StreamingResponseBody> downloadAssigneeTaskFile(
            @PathVariable Long taskId,
            @PathVariable Long fileId) {
        TaskFileDownload download = adminService.downloadAssigneeTaskFile(taskId, fileId);
        long timeoutMs = planLimits.computeDownloadTimeoutMs(download.size(), SubscriptionPlan.TEAM);
        StreamingResponseBody body = out -> {
            try (var in = download.content()) {
                transferWithTimeout(in, out, timeoutMs);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + StringSanitizer.sanitizeFilenameForHeader(download.filename()) + "\"")
                .contentLength(download.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @GetMapping("/comments")
    public ResponseEntity<Page<AdminCommentResource>> listComments(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long creatorId,
            @PageableDefault(size = 15, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
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

    @PatchMapping("/users/{userId}/plan")
    public ResponseEntity<AdminUserResource> updateUserPlan(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        SubscriptionPlan plan = SubscriptionPlan.valueOf(body.get("subscriptionPlan"));
        var updated = adminService.updateUser(userId, null, null, null, plan, null);
        return ResponseEntity.ok(toUserResourceWithLimits(updated));
    }

    @PostMapping("/users/{userId}/reset-email-usage")
    public ResponseEntity<AdminUserResource> resetEmailUsage(@PathVariable Long userId) {
        var updated = adminService.resetUserEmailUsage(userId);
        return ResponseEntity.ok(toUserResourceWithLimits(updated));
    }

    @PostMapping("/users/{userId}/reset-download-usage")
    public ResponseEntity<AdminUserResource> resetDownloadUsage(@PathVariable Long userId) {
        var updated = adminService.resetUserDownloadUsage(userId);
        return ResponseEntity.ok(toUserResourceWithLimits(updated));
    }

    private AdminUserResource toUserResourceWithLimits(io.github.balasis.taskmanager.context.base.model.User user) {
        AdminUserResource r = adminOutboundMapper.toUserResource(user);
        r.setStorageBudgetBytes(planLimits.storageBudgetBytes(user.getSubscriptionPlan()));
        r.setDownloadBudgetBytes(planLimits.downloadBudgetBytes(user.getSubscriptionPlan()));
        r.setEmailsPerMonth(planLimits.emailQuotaPerMonth(user.getSubscriptionPlan()));
        r.setImageScansPerMonth(planLimits.imageScansPerMonth(user.getSubscriptionPlan()));
        r.setTaskAnalysisCreditsPerMonth(planLimits.taskAnalysisCreditsPerMonth(user.getSubscriptionPlan()));
        return r;
    }
}
