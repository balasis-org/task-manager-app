package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.TaskState;
import io.github.balasis.taskmanager.context.web.mapper.inbound.GroupInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.inbound.TaskInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.GroupInvitationOutboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.GroupOutboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.TaskOutboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.TaskPreviewOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundResource;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.inbound.GroupInvitationInboundResource;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.outbound.GroupInvitationOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskPreviewOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.taskparticipant.inbound.TaskParticipantInboundResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.core.transfer.TaskFileDownload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final TaskOutboundMapper taskOutboundMapper;
    private final TaskPreviewOutboundMapper taskPreviewOutboundMapper;
    private final TaskInboundMapper taskInboundMapper;
    private final GroupService groupService;


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
    public ResponseEntity<Set<GroupOutboundResource>> findAllByCurrentUser() {
        return ResponseEntity.ok(
                groupOutboundMapper.toResources(
                        groupService.findAllByCurrentUser()
                ));
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
        System.out.println(groupInvitationInboundResource.getUserToBeInvitedRole());
        return ResponseEntity.ok(groupInvitationOutboundMapper.toResource(
                groupService.createGroupInvitation(groupId,groupInvitationInboundResource.getUserId(),
                        groupInvitationInboundResource.getUserToBeInvitedRole())
        ));
    }


    @PostMapping(path = "/{groupId}/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> createTask(
            @PathVariable Long groupId,
            @RequestPart("data") TaskInboundResource inbound,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Set<MultipartFile> filesSet = files == null ? Collections.emptySet() : new HashSet<>(files);
        var partialTask = taskInboundMapper.toDomain(inbound);

        return ResponseEntity.ok(taskOutboundMapper.toResource(
                groupService.createTask(groupId, partialTask, inbound.getAssignedIds(), inbound.getReviewerIds(), filesSet)
        ));
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
        return ResponseEntity.ok(
                taskOutboundMapper.toResource(
                        groupService.patchTask(groupId,taskId,
                                taskInboundMapper.toDomain(taskInboundPatchResource))
                )
        );
    }
    
    @PostMapping(path="/{groupId}/task/{taskId}/taskParticipants")
    public ResponseEntity<TaskOutboundResource> addParticipant(
            @PathVariable Long groupId,
            @PathVariable Long taskId,
            @RequestBody TaskParticipantInboundResource taskParticipantInboundResource
            ) {
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

}
