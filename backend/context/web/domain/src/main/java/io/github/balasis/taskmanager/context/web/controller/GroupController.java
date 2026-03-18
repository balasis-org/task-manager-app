package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.AnalysisType;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.model.TaskAnalysisSnapshot;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.base.utils.StringSanitizer;
import io.github.balasis.taskmanager.context.web.mapper.inbound.GroupInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.inbound.TaskInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.*;
import io.github.balasis.taskmanager.context.web.resource.filereview.inbound.FileReviewInboundResource;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundResource;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupMiniForDropdownResource;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskanalysis.outbound.TaskAnalysisEstimateResource;
import io.github.balasis.taskmanager.context.web.resource.taskanalysis.outbound.TaskAnalysisSnapshotResource;
import io.github.balasis.taskmanager.engine.core.dto.AnalysisEstimateDto;
import io.github.balasis.taskmanager.engine.core.dto.FileReviewInfoDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupFileDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupRefreshDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupWithPreviewDto;
import io.github.balasis.taskmanager.engine.core.dto.TaskPreviewDto;
import io.github.balasis.taskmanager.context.web.resource.groupevent.outbound.GroupEventOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.inbound.GroupInvitationInboundResource;
import io.github.balasis.taskmanager.context.web.resource.groupmembership.outbound.GroupMembershipOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskPreviewOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskcomment.inbound.TaskCommentInboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskcomment.outbound.TaskCommentOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskparticipant.inbound.TaskParticipantInboundResource;
import io.github.balasis.taskmanager.context.web.throttle.DownloadGate;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.core.service.UserService;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import io.github.balasis.taskmanager.engine.infrastructure.redis.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// the big REST controller — covers groups, tasks, comments, file uploads/downloads,
// invitations, member management, comment intelligence (analysis + summary), file reviews.
// downloads use DownloadGate for per-user concurrency limiting and ETag-based 304s.
// smart-poll endpoints (hasGroupChanged etc.) return 409/204 so the frontend avoids refetching.
@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController extends BaseComponent {
    private final ResourceDataValidator resourceDataValidator;
    private final GroupOutboundMapper groupOutboundMapper;
    private final GroupInboundMapper groupInboundMapper;
    private final GroupInvitationOutboundMapper groupInvitationOutboundMapper;
    private final GroupEventOutboundMapper groupEventOutboundMapper;
    private final TaskOutboundMapper taskOutboundMapper;
        private final TaskCommentOutboundMapper taskCommentOutboundMapper;
    private final TaskPreviewOutboundMapper taskPreviewOutboundMapper;
    private final TaskInboundMapper taskInboundMapper;
    private final GroupService groupService;
    private final GroupMembershipOutboundMapper groupMembershipOutboundMapper;
    private final GroupMiniForDropdownOutboundMapper groupMiniForDropdownOutboundMapper;
        private final UserMiniForDropdownOutboundMapper userMiniForDropdownOutboundMapper;
        private final UserService userService;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final PresenceService presenceService;
    private final PlanLimits planLimits;
    private final DownloadGate downloadGate;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {};
    private static final TypeReference<List<TaskAnalysisSnapshotResource.CommentAnalysisView>> COMMENT_VIEW_LIST =
            new TypeReference<>() {};

    @PostMapping
    public ResponseEntity<GroupOutboundResource> create(@RequestBody final GroupInboundResource groupInboundResource){
        resourceDataValidator.validateResourceData(groupInboundResource);
                return ResponseEntity.ok(
                                groupOutboundMapper.toResource(
                                groupService.create(groupInboundMapper.toDomain(groupInboundResource)
                                )
                ));
    }

    @PostMapping("/{groupId}/image")
    public ResponseEntity<GroupOutboundResource> updateGroupImage(
            @PathVariable Long groupId,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(groupOutboundMapper.toResource(
                groupService.updateGroupImage(groupId, file)));
    }

    @PatchMapping("/{groupId}/image/pick-default")
    public ResponseEntity<GroupOutboundResource> pickDefaultGroupImage(
            @PathVariable Long groupId,
            @RequestParam String fileName) {

        return ResponseEntity.ok(groupOutboundMapper.toResource(
                groupService.pickDefaultGroupImage(groupId, fileName)));
    }

    @GetMapping
    public ResponseEntity<Set<GroupMiniForDropdownResource>> findAllByCurrentUser() {
        return ResponseEntity.ok(
                groupMiniForDropdownOutboundMapper.toResources(
                        groupService.findAllByCurrentUser()
                ));
    }

    @GetMapping(path = "/{groupId}/has-changed")
    public ResponseEntity<Void> hasGroupChanged(
            @PathVariable Long groupId,
            @RequestParam Instant lastSeen
    ) {
        boolean changed = groupService.hasGroupChanged(groupId, lastSeen);

        // Piggyback a presence heartbeat, the user is actively viewing this group.
        // Best-effort: PresenceService swallows Redis failures internally.
        try {
            presenceService.heartbeat(groupId, effectiveCurrentUser.getUserId());
        } catch (Exception ignored) { }

        return changed
                ? ResponseEntity.status(409).build()
                : ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/{groupId}")
    public ResponseEntity<GroupWithPreviewDto> getGroupWithPreviewTasks(
            @PathVariable Long groupId
    ){
        return ResponseEntity.ok(groupService.findGroupWithPreviewTasks(groupId));
    }

    @GetMapping(path = "/{groupId}/presence")
    public ResponseEntity<List<Long>> getGroupPresence(@PathVariable Long groupId) {
        groupService.checkMembership(groupId);
        return ResponseEntity.ok(presenceService.getPresent(groupId));
    }

    @GetMapping(path = "/{groupId}/refresh")
    public ResponseEntity<GroupRefreshDto> refreshGroup(
            @PathVariable Long groupId,
            @RequestParam Instant lastSeen
    ) {
        return ResponseEntity.ok(groupService.refreshGroup(groupId, lastSeen));
    }

    @GetMapping(path = "/{groupId}/task/{taskId}/has-changed")
    public ResponseEntity<Void> hasTaskChanged(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestParam Instant since
    ) {
        if (groupService.hasTaskChanged(groupId, taskId, since)) {
            return ResponseEntity.status(409).build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/{groupId}/task/{taskId}/comments/has-changed")
    public ResponseEntity<Void> hasCommentsChanged(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestParam Instant since
    ) {
        if (groupService.hasCommentsChanged(groupId, taskId, since)) {
            return ResponseEntity.status(409).build();
        }
        return ResponseEntity.noContent().build();
    }



    @GetMapping("/{groupId}/groupMemberships")
    public ResponseEntity<Page<GroupMembershipOutboundResource>> getAllGroupMembers(
            @PathVariable Long groupId,
            Pageable pageable
    ){
        User me = userService.findCurrentUser();
        return ResponseEntity.ok(
                groupService.getAllGroupMembers(groupId,pageable).map(gm -> {
                    var res = groupMembershipOutboundMapper.toResource(gm);
                    if (res.getUser() != null) {
                        res.getUser().setSameOrg(
                                me.isOrg() && gm.getUser().isOrg()
                                && me.getTenantId() != null && me.getTenantId().equals(gm.getUser().getTenantId())
                        );
                    }
                    return res;
                })
        );
    }

    @GetMapping("/{groupId}/groupMemberships/search")
    public ResponseEntity<Page<GroupMembershipOutboundResource>> searchGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(required = false) String q,
            Pageable pageable
    ){
        User me = userService.findCurrentUser();
        return ResponseEntity.ok(
                groupService.searchGroupMembers(groupId, q, pageable).map(gm -> {
                    var res = groupMembershipOutboundMapper.toResource(gm);
                    if (res.getUser() != null) {
                        res.getUser().setSameOrg(
                                me.isOrg() && gm.getUser().isOrg()
                                && me.getTenantId() != null && me.getTenantId().equals(gm.getUser().getTenantId())
                        );
                    }
                    return res;
                })
        );
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<GroupOutboundResource> patch(
            @PathVariable Long groupId,
            @RequestBody GroupInboundPatchResource groupInboundPatchResource
    ) {

        resourceDataValidator.validateResourceData(groupInboundPatchResource);

        return ResponseEntity.ok(
                groupOutboundMapper.toResource(
                        groupService.patch(
                                groupId,
                                groupInboundMapper.toDomain(groupInboundPatchResource)
                        )
                )
        );
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(@PathVariable Long groupId) {
        groupService.delete(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path="/{groupId}/invite")
    public ResponseEntity<Void> inviteToGroup(
            @PathVariable(name = "groupId") Long groupId,
            @RequestBody GroupInvitationInboundResource groupInvitationInboundResource
    ){
        resourceDataValidator.validateResourceData(groupInvitationInboundResource);
        groupService.createGroupInvitation(groupId,groupInvitationInboundResource.getInviteCode(),
                        groupInvitationInboundResource.getUserToBeInvitedRole(),
                        groupInvitationInboundResource.getComment(),
                        Boolean.TRUE.equals(groupInvitationInboundResource.getSendEmail()));
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/{groupId}/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> createTask(
            @PathVariable Long groupId,
            @RequestPart("data") TaskInboundResource inbound,
            @RequestPart(value = "files", required = false) List<MultipartFile> files

    ) {
        resourceDataValidator.validateResourceData(inbound);
        Set<MultipartFile> filesSet = files == null ? Collections.emptySet() : new HashSet<>(files);
        var partialTask = taskInboundMapper.toDomain(inbound);
        var task = groupService.createTask(groupId, partialTask, inbound.getAssignedIds(),
                        inbound.getReviewerIds(), filesSet);
        return ResponseEntity.ok(mapAndEnrich(task, groupId));
    }

    @GetMapping(path = "/{groupId}/tasks/search")
    public ResponseEntity<Set<TaskPreviewDto>> findTasksWithFilters(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Boolean creatorIsMe,
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) Boolean reviewerIsMe,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean assigneeIsMe,
            @RequestParam(required = false) Instant dueDateBefore
    ) {
        return ResponseEntity.ok(
                groupService.findTasksWithPreviewByFilters(
                        groupId,
                        creatorId,
                        creatorIsMe,
                        reviewerId,
                        reviewerIsMe,
                        assigneeId,
                        assigneeIsMe,
                        dueDateBefore
                )
        );
    }

    @GetMapping(path = "/{groupId}/tasks/accessible-ids")
    public ResponseEntity<Set<Long>> findAccessibleTaskIds(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.findAccessibleTaskIds(groupId));
    }

    @GetMapping(path = "/{groupId}/tasks/filter-ids")
    public ResponseEntity<Set<Long>> findFilteredTaskIds(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Boolean creatorIsMe,
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) Boolean reviewerIsMe,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean assigneeIsMe,
            @RequestParam(required = false) Instant dueDateBefore,
            @RequestParam(required = false) Integer priorityMin,
            @RequestParam(required = false) Integer priorityMax,
            @RequestParam(required = false) TaskState taskState,
            @RequestParam(required = false) Boolean hasFiles
    ) {
        return ResponseEntity.ok(
                groupService.findFilteredTaskIds(
                        groupId,
                        creatorId,
                        creatorIsMe,
                        reviewerId,
                        reviewerIsMe,
                        assigneeId,
                        assigneeIsMe,
                        dueDateBefore,
                        priorityMin,
                        priorityMax,
                        taskState,
                        hasFiles
                )
        );
    }

    @GetMapping(path="/{groupId}/task")
    public ResponseEntity<Set<TaskPreviewOutboundResource>> findMyTasks(
            @PathVariable Long groupId,
            @RequestParam(required = false) Boolean reviewer,
            @RequestParam(required = false) Boolean assigned,
            @RequestParam(required = false) TaskState taskState
    ){
       return ResponseEntity.ok(
               taskPreviewOutboundMapper.toResources(
               groupService.findMyTasks(groupId,reviewer,assigned,taskState)
            )
       );
    }

    @GetMapping("/{groupId}/events")
    public ResponseEntity<Page<GroupEventOutboundResource>> findAllGroupEvents(
            @PathVariable Long groupId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                groupService.findAllGroupEvents(groupId, pageable)
                        .map(groupEventOutboundMapper::toResource)
        );
    }

    @DeleteMapping("/{groupId}/events")
    public ResponseEntity<Void> deleteAllGroupEvents(
            @PathVariable Long groupId
    ) {
        groupService.deleteAllGroupEvents(groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/{groupId}/task/{taskId}")
    public ResponseEntity<TaskOutboundResource> getTask(
            @PathVariable Long groupId,
            @PathVariable Long taskId
    ){
        var task = groupService.getTask(groupId, taskId);
        var resource = mapAndEnrich(task, groupId);
        enrichFileReviews(task, resource);
        return ResponseEntity.ok(resource);
    }

    @PatchMapping(path="/{groupId}/task/{taskId}")
    public ResponseEntity<TaskOutboundResource> patchTask(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestBody TaskInboundPatchResource taskInboundPatchResource
            ){
        resourceDataValidator.validateResourceData(taskInboundPatchResource);
        var task = groupService.patchTask(groupId, taskId,
                taskInboundMapper.toDomain(taskInboundPatchResource));
        return ResponseEntity.ok(mapAndEnrich(task, groupId));
    }

    @DeleteMapping("/{groupId}/task/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long groupId,
            @PathVariable Long taskId
    ){
        groupService.deleteTask(groupId,taskId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/review")
    public ResponseEntity<TaskOutboundResource> reviewTask(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestBody TaskInboundPatchResource taskInboundPatchResource
    ){
        resourceDataValidator.validateResourceData(taskInboundPatchResource);
        var task = groupService.reviewTask(groupId, taskId, taskInboundMapper.toDomain(taskInboundPatchResource));
        return ResponseEntity.ok(mapAndEnrich(task, groupId));
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/to-be-reviewed")
    public ResponseEntity<TaskOutboundResource> markTaskToBeReviewed(
            @PathVariable Long groupId,
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(
                mapAndEnrich(groupService.markTaskToBeReviewed(groupId, taskId), groupId)
        );
    }

    @PostMapping(path="/{groupId}/task/{taskId}/taskParticipants")
    public ResponseEntity<TaskOutboundResource> addParticipant(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestBody TaskParticipantInboundResource taskParticipantInboundResource
            ) {
        resourceDataValidator.validateResourceData(taskParticipantInboundResource);
       return ResponseEntity.ok(
               mapAndEnrich(groupService.addTaskParticipant(groupId, taskId,
                        taskParticipantInboundResource.getUserId(),
                        taskParticipantInboundResource.getTaskParticipantRole()), groupId)
       );
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> addTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestPart("file") MultipartFile file
    ) {
           return ResponseEntity.ok(mapAndEnrich(
                groupService.addTaskFile(groupId, taskId, file), groupId
        ));
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/assignee-files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> addAssigneeTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(mapAndEnrich(
                groupService.addAssigneeTaskFile(groupId, taskId, file), groupId
        ));
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/comments")
    public ResponseEntity<TaskCommentOutboundResource> addTaskComment(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestBody TaskCommentInboundResource inbound
    ) {
        resourceDataValidator.validateResourceData(inbound);

        return ResponseEntity.ok(
                taskCommentOutboundMapper.toResource(
                        groupService.addTaskComment(groupId, taskId, inbound.getComment())
                )
        );
    }

    @GetMapping(path = "/{groupId}/task/{taskId}/comments")
    public ResponseEntity<Page<TaskCommentOutboundResource>> findAllTaskComments(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            Pageable pageable
    ){

        return ResponseEntity.ok(
                groupService.findAllTaskComments(groupId,taskId,pageable).map(
                        taskCommentOutboundMapper::toResource
                )
        );
    }

    @PatchMapping(path = "/{groupId}/task/{taskId}/comments/{commentId}")
    public ResponseEntity<TaskCommentOutboundResource> patchTaskComment(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestBody TaskCommentInboundResource inbound
    ) {
        resourceDataValidator.validateResourceData(inbound);
        return ResponseEntity.ok(
                taskCommentOutboundMapper.toResource(
                        groupService.patchTaskComment(groupId, taskId, commentId, inbound.getComment())
                )
        );
    }

    @DeleteMapping(path = "/{groupId}/task/{taskId}/comments/{commentId}")
    public ResponseEntity<Void> deleteTaskComment(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long commentId
    ) {
        groupService.deleteTaskComment(groupId, taskId, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/task/{taskId}/files/{fileId}/download")
    public ResponseEntity<StreamingResponseBody> downloadTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long fileId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        String etag = "\"tf-" + fileId + "\"";

        // ETag hit — verify membership only (no blob, no budget charge)
        if (etag.equals(ifNoneMatch)) {
            groupService.checkMembership(groupId);
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        User currentUser = userService.findCurrentUser();
        long userId = currentUser.getId();

        // reserve a slot before we even touch blob storage
        downloadGate.acquire(userId);
        try {
            TaskFileDownload download = groupService.downloadTaskFile(groupId, taskId, fileId);
            long timeoutMs = planLimits.computeDownloadTimeoutMs(download.size(), currentUser.getSubscriptionPlan());

            StreamingResponseBody body = out -> {
                try (var in = download.content()) {
                    transferWithTimeout(in, out, timeoutMs);
                } finally {
                    downloadGate.release(userId);
                }
            };
            return ResponseEntity.ok()
                    .eTag(etag)
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + StringSanitizer.sanitizeFilenameForHeader(download.filename()) + "\"")
                    .contentLength(download.size())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body);
        } catch (Exception e) {
            // if anything blows up before the StreamingResponseBody runs, free the slot
            downloadGate.release(userId);
            throw e;
        }
    }

    @GetMapping("/{groupId}/task/{taskId}/assignee-files/{fileId}/download")
    public ResponseEntity<StreamingResponseBody> downloadAssigneeTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long fileId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        String etag = "\"af-" + fileId + "\"";

        if (etag.equals(ifNoneMatch)) {
            groupService.checkMembership(groupId);
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }

        User currentUser = userService.findCurrentUser();
        long userId = currentUser.getId();

        downloadGate.acquire(userId);
        try {
            TaskFileDownload download = groupService.downloadAssigneeTaskFile(groupId, taskId, fileId);
            long timeoutMs = planLimits.computeDownloadTimeoutMs(download.size(), currentUser.getSubscriptionPlan());

            StreamingResponseBody body = out -> {
                try (var in = download.content()) {
                    transferWithTimeout(in, out, timeoutMs);
                } finally {
                    downloadGate.release(userId);
                }
            };
            return ResponseEntity.ok()
                    .eTag(etag)
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + StringSanitizer.sanitizeFilenameForHeader(download.filename()) + "\"")
                    .contentLength(download.size())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body);
        } catch (Exception e) {
            downloadGate.release(userId);
            throw e;
        }
    }

    @DeleteMapping("/{groupId}/task/{taskId}/taskParticipant/{taskParticipantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long taskParticipantId
    ){
        groupService.removeTaskParticipant(groupId,taskId,taskParticipantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/task/{taskId}/notify/{userId}")
    public ResponseEntity<Void> notifyTaskParticipant(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long userId,
            @RequestBody(required = false) java.util.Map<String, String> body
    ){
        String customNote = (body != null) ? body.get("customNote") : null;
        groupService.notifyTaskParticipant(groupId,taskId,userId, customNote);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/task/{taskId}/notify-bulk")
    public ResponseEntity<Void> notifyTaskParticipants(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestBody java.util.Map<String, Object> body
    ){
        @SuppressWarnings("unchecked")
        java.util.List<Number> rawIds = (java.util.List<Number>) body.get("userIds");
        java.util.Set<Long> userIds = rawIds.stream().map(Number::longValue).collect(java.util.stream.Collectors.toSet());
        String customNote = body.get("customNote") != null ? body.get("customNote").toString() : null;
        groupService.notifyTaskParticipants(groupId, taskId, userIds, customNote);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/task/{taskId}/files/{fileId}")
    public ResponseEntity<Void> removeTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long fileId
    ) {
        groupService.removeTaskFile(groupId, taskId, fileId);
        return ResponseEntity.noContent().build();
    }

        @DeleteMapping("/{groupId}/task/{taskId}/assignee-files/{fileId}")
        public ResponseEntity<Void> removeAssigneeTaskFile(
                        @PathVariable Long groupId,
                        @PathVariable Long taskId,
                        @PathVariable Long fileId
        ) {
                groupService.removeAssigneeTaskFile(groupId, taskId, fileId);
                return ResponseEntity.noContent().build();
        }

    @DeleteMapping("/{groupId}/groupMembership/{groupMembershipId}")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable Long groupId,
            @PathVariable Long groupMembershipId
    ) {
        groupService.removeGroupMember(groupId, groupMembershipId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/groupMembership/{groupMembershipId}/role")
    public ResponseEntity<GroupMembershipOutboundResource> changeGroupMembershipRole(
            @PathVariable Long groupId,
            @PathVariable Long groupMembershipId,
            @RequestParam("role") Role role
    ) {
        return ResponseEntity.ok(
                groupMembershipOutboundMapper.toResource(
                        groupService.changeGroupMembershipRole(groupId, groupMembershipId, role)
                )
        );
    }

    // ── File Gallery & Per-File Review ─────────────────────────

    @GetMapping("/{groupId}/files")
    public ResponseEntity<List<GroupFileDto>> getGroupFiles(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupFiles(groupId));
    }

    @PostMapping("/{groupId}/task/{taskId}/files/{fileId}/review")
    public ResponseEntity<Void> reviewTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long fileId,
            @RequestBody FileReviewInboundResource inbound
    ) {
        resourceDataValidator.validateResourceData(inbound);
        groupService.reviewTaskFile(groupId, taskId, fileId, inbound.getStatus(), inbound.getNote());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/task/{taskId}/assignee-files/{fileId}/review")
    public ResponseEntity<Void> reviewAssigneeFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long fileId,
            @RequestBody FileReviewInboundResource inbound
    ) {
        resourceDataValidator.validateResourceData(inbound);
        groupService.reviewAssigneeFile(groupId, taskId, fileId, inbound.getStatus(), inbound.getNote());
        return ResponseEntity.ok().build();
    }

    // ── Comment Intelligence ─────────────────────────────────────

    @GetMapping("/{groupId}/task/{taskId}/analysis-estimate")
    public ResponseEntity<TaskAnalysisEstimateResource> getAnalysisEstimate(
            @PathVariable Long groupId,
            @PathVariable Long taskId) {
        AnalysisEstimateDto dto = groupService.getAnalysisEstimate(groupId, taskId);
        TaskAnalysisSnapshot s = dto.snapshot();

        int fullCredits = s.getEstimatedAnalysisCredits()
                        + s.getEstimatedSummaryCredits()
                        + s.getEstimatedEgressCredits();
        boolean stale = s.getEstimateChangeMarker() != null
                     && s.getEstimatedAt() != null
                     && s.getEstimateChangeMarker().isAfter(s.getEstimatedAt());

        TaskAnalysisEstimateResource r = new TaskAnalysisEstimateResource();
        r.setCommentCount(s.getEstimatedCommentCount());
        r.setTotalChars(s.getEstimatedTotalChars());
        r.setAnalysisCredits(s.getEstimatedAnalysisCredits());
        r.setSummaryCredits(s.getEstimatedSummaryCredits());
        r.setEgressCredits(s.getEstimatedEgressCredits());
        r.setFullCredits(fullCredits);
        r.setBudgetUsed(dto.budgetUsed());
        r.setBudgetMax(dto.budgetMax());
        r.setBudgetRemaining(dto.budgetMax() - dto.budgetUsed());
        r.setEstimatedAt(s.getEstimatedAt());
        r.setStale(stale);
        return ResponseEntity.ok(r);
    }

    @PostMapping("/{groupId}/task/{taskId}/analyze")
    public ResponseEntity<Map<String, Integer>> requestAnalysis(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestParam AnalysisType type) {
        int credits = groupService.requestAnalysis(groupId, taskId, type);
        return ResponseEntity.accepted().body(Map.of("creditsCharged", credits));
    }

    @GetMapping("/{groupId}/task/{taskId}/analysis")
    public ResponseEntity<TaskAnalysisSnapshotResource> getAnalysisSnapshot(
            @PathVariable Long groupId,
            @PathVariable Long taskId) {
        TaskAnalysisSnapshot snap = groupService.getAnalysisSnapshot(groupId, taskId);
        if (snap == null || (snap.getAnalyzedAt() == null && snap.getSummarizedAt() == null)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toSnapshotResource(snap));
    }

    @DeleteMapping("/{groupId}/task/{taskId}/comments/bulk")
    public ResponseEntity<Map<String, Integer>> bulkDeleteComments(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestParam Instant before) {
        int deleted = groupService.bulkDeleteCommentsBefore(groupId, taskId, before);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    // ── private helpers ──────────────────────────────────────────

    private TaskOutboundResource mapAndEnrich(io.github.balasis.taskmanager.context.base.model.Task task, Long groupId) {
        var resource = taskOutboundMapper.toResource(task);
        var limits = groupService.resolveEffectiveFileLimits(groupId, task);
        resource.setEffectiveMaxCreatorFiles(limits.maxCreatorFiles());
        resource.setEffectiveMaxAssigneeFiles(limits.maxAssigneeFiles());
        resource.setEffectiveMaxFileSizeBytes(limits.maxFileSizeBytes());
        return resource;
    }

    private void enrichFileReviews(io.github.balasis.taskmanager.context.base.model.Task task,
                                   TaskOutboundResource resource) {
        Set<Long> cIds = task.getCreatorFiles().stream()
                .map(f -> f.getId()).collect(java.util.stream.Collectors.toSet());
        Set<Long> aIds = task.getAssigneeFiles().stream()
                .map(f -> f.getId()).collect(java.util.stream.Collectors.toSet());

        if (cIds.isEmpty() && aIds.isEmpty()) return;

        Map<Long, List<FileReviewInfoDto>> reviews = groupService.getFileReviews(cIds, aIds);

        if (resource.getFiles() != null) {
            for (var f : resource.getFiles()) {
                f.setReviews(reviews.getOrDefault(f.getId(), List.of()));
            }
        }
        if (resource.getAssigneeFiles() != null) {
            for (var f : resource.getAssigneeFiles()) {
                f.setReviews(reviews.getOrDefault(f.getId(), List.of()));
            }
        }
    }

    private TaskAnalysisSnapshotResource toSnapshotResource(TaskAnalysisSnapshot snap) {
        TaskAnalysisSnapshotResource r = new TaskAnalysisSnapshotResource();

        if (snap.getOverallSentiment() != null) {
            r.setOverallSentiment(snap.getOverallSentiment().name());
        }
        r.setOverallConfidence(snap.getOverallConfidence());
        r.setPositiveCount(snap.getPositiveCount());
        r.setNeutralCount(snap.getNeutralCount());
        r.setNegativeCount(snap.getNegativeCount());
        r.setPiiDetectedCount(snap.getPiiDetectedCount());
        r.setAnalysisCommentCount(snap.getAnalysisCommentCount());
        r.setAnalyzedAt(snap.getAnalyzedAt());

        if (snap.getKeyPhrases() != null) {
            try { r.setKeyPhrases(objectMapper.readValue(snap.getKeyPhrases(), STRING_LIST)); }
            catch (Exception ignored) { /* non-critical */ }
        }
        if (snap.getCommentResults() != null) {
            try { r.setCommentResults(objectMapper.readValue(snap.getCommentResults(), COMMENT_VIEW_LIST)); }
            catch (Exception ignored) { /* non-critical */ }
        }

        boolean analysisStale = snap.getAnalysisChangeMarker() != null
                             && snap.getAnalyzedAt() != null
                             && snap.getAnalysisChangeMarker().isAfter(snap.getAnalyzedAt());
        r.setAnalysisStale(analysisStale);

        r.setSummaryText(snap.getSummaryText());
        r.setSummaryCommentCount(snap.getSummaryCommentCount());
        r.setSummarizedAt(snap.getSummarizedAt());

        boolean summaryStale = snap.getSummaryChangeMarker() != null
                            && snap.getSummarizedAt() != null
                            && snap.getSummaryChangeMarker().isAfter(snap.getSummarizedAt());
        r.setSummaryStale(summaryStale);
        return r;
    }

}
