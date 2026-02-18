package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.mapper.inbound.GroupInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.inbound.TaskInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.*;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundResource;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupMiniForDropdownResource;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserMiniForDropdownOutboundResource;
import io.github.balasis.taskmanager.engine.core.dto.GroupRefreshDto;
import io.github.balasis.taskmanager.engine.core.dto.GroupWithPreviewDto;
import io.github.balasis.taskmanager.engine.core.dto.TaskPreviewDto;
import io.github.balasis.taskmanager.context.web.resource.groupevent.outbound.GroupEventOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.inbound.GroupInvitationInboundResource;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.outbound.GroupInvitationOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.groupmembership.outbound.GroupMembershipOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskPreviewOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskcomment.inbound.TaskCommentInboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskcomment.outbound.TaskCommentOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskparticipant.inbound.TaskParticipantInboundResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.core.service.UserService;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @GetMapping
    public ResponseEntity<Set<GroupMiniForDropdownResource>> findAllByCurrentUser() {
        return ResponseEntity.ok(
                groupMiniForDropdownOutboundMapper.toResources(
                        groupService.findAllByCurrentUser()
                ));
    }

    @GetMapping(path = "/{groupId}/refresh")
    public ResponseEntity<GroupRefreshDto> refreshGroup(
            @PathVariable Long groupId,
            @RequestParam Instant lastSeen
    ) {
        return ResponseEntity.ok(groupService.refreshGroup(groupId, lastSeen));
    }

    /**
     * Lightweight poll: has the task changed since the given timestamp?
     * Returns 204 if unchanged, 409 if changed.
     */
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

    /**
     * Lightweight poll: have comments changed on this task since the given timestamp?
     * Returns 204 if unchanged, 409 if changed.
     */
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

    @GetMapping(path = "/{groupId}")
    public ResponseEntity<GroupWithPreviewDto> getGroupWithPreviewTasks(
            @PathVariable Long groupId
    ){
        return ResponseEntity.ok(groupService.findGroupWithPreviewTasks(groupId));
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
    public ResponseEntity<GroupInvitationOutboundResource> inviteToGroup(
            @PathVariable(name = "groupId") Long groupId,
            @RequestBody GroupInvitationInboundResource groupInvitationInboundResource
    ){
        resourceDataValidator.validateResourceData(groupInvitationInboundResource);
        return ResponseEntity.ok(groupInvitationOutboundMapper.toResource(
                groupService.createGroupInvitation(groupId,groupInvitationInboundResource.getUserId(),
                        groupInvitationInboundResource.getUserToBeInvitedRole(),
                        groupInvitationInboundResource.getComment())
        ));
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

        return ResponseEntity.ok(taskOutboundMapper.toResource(
                groupService.createTask(groupId, partialTask, inbound.getAssignedIds(),
                        inbound.getReviewerIds(), filesSet)
        ));
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

    @GetMapping(path = "/{groupId}/searchForInvite")
    public ResponseEntity<Page<UserMiniForDropdownOutboundResource>> searchUsersForInvite(
            @PathVariable Long groupId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "false") boolean sameOrgOnly,
            Pageable pageable
    ){
                User me = userService.findCurrentUser();

                // If the requester asked for "same org only" but they themselves are not in an organisation,
                // return an empty page immediately (no results).
                if (sameOrgOnly && !me.isOrg()) {
                        return ResponseEntity.ok(Page.empty(pageable));
                }

                Page<UserMiniForDropdownOutboundResource> page = userService
                                .searchUserForInvites(groupId, q, sameOrgOnly, pageable)
                                .map(u -> {
                                        var res = userMiniForDropdownOutboundMapper.toResource(u);
                                        res.setSameOrg(
                                                        me.isOrg() && u.isOrg()
                                                                        && me.getTenantId() != null && me.getTenantId().equals(u.getTenantId())
                                        );
                                        return res;
                                });

                return ResponseEntity.ok(page);
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

    @GetMapping(path = "/{groupId}/task/{taskId}")
    public ResponseEntity<TaskOutboundResource> getTask(
            @PathVariable Long groupId,
            @PathVariable Long taskId
    ){
        return ResponseEntity.ok(
                taskOutboundMapper.toResource(groupService.getTask(groupId,taskId))
        );
    }


    @PatchMapping(path="/{groupId}/task/{taskId}")
    public ResponseEntity<TaskOutboundResource> patchTask(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestBody TaskInboundPatchResource taskInboundPatchResource
            ){
        resourceDataValidator.validateResourceData(taskInboundPatchResource);
        return ResponseEntity.ok(
                taskOutboundMapper.toResource(
                        groupService.patchTask(groupId,taskId,
                                taskInboundMapper.toDomain(taskInboundPatchResource))
                )
        );
    }

    @DeleteMapping("/groupId/{groupId}/task/{taskId}")
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
        return ResponseEntity.ok(
                taskOutboundMapper.toResource(
                        groupService.reviewTask(groupId, taskId, taskInboundMapper.toDomain(taskInboundPatchResource))
                )
        );
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/to-be-reviewed")
    public ResponseEntity<TaskOutboundResource> markTaskToBeReviewed(
            @PathVariable Long groupId,
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(
                taskOutboundMapper.toResource(
                        groupService.markTaskToBeReviewed(groupId, taskId)
                )
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
               taskOutboundMapper.toResource(
                groupService.addTaskParticipant(groupId, taskId,
                        taskParticipantInboundResource.getUserId(),
                        taskParticipantInboundResource.getTaskParticipantRole() )
        ));
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> addTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestPart("file") MultipartFile file
    ) {
           return ResponseEntity.ok(taskOutboundMapper.toResource(
                groupService.addTaskFile(groupId, taskId, file)
        ));
    }

    @PostMapping(path = "/{groupId}/task/{taskId}/assignee-files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> addAssigneeTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(taskOutboundMapper.toResource(
                groupService.addAssigneeTaskFile(groupId, taskId, file)
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
    public ResponseEntity<byte[]> downloadTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long fileId
    ) {
        TaskFileDownload download = groupService.downloadTaskFile(groupId, taskId, fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(download.content());
    }

    @GetMapping("/{groupId}/task/{taskId}/assignee-files/{fileId}/download")
    public ResponseEntity<byte[]> downloadAssigneeTaskFile(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @PathVariable Long fileId
    ) {
        TaskFileDownload download = groupService.downloadAssigneeTaskFile(groupId, taskId, fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(download.content());
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

}
