package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.web.mapper.inbound.GroupInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.inbound.TaskInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.GroupOutboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.TaskOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundPatchResource;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundResource;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.core.service.authorization.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController{
    private final ResourceDataValidator resourceDataValidator;
    private final GroupOutboundMapper groupOutboundMapper;
    private final GroupInboundMapper groupInboundMapper;
    private final TaskOutboundMapper taskOutboundMapper;
    private final TaskInboundMapper taskInboundMapper;
    private final GroupService groupService;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<GroupOutboundResource> create(@RequestBody final GroupInboundResource groupInboundResource){
        resourceDataValidator.validateResourceData(groupInboundResource);
        return ResponseEntity.ok(
                groupOutboundMapper.toResource(
                groupService.create(groupInboundMapper.toDomain(groupInboundResource)
                )
        ));
    }

    @GetMapping
    public ResponseEntity<List<GroupOutboundResource>> findAllByCurrentUser() {
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
        authorizationService.requireRoleIn(groupId,Set.of(
                Role.GROUP_LEADER
        ));
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
        authorizationService.requireRoleIn(groupId,Set.of(Role.GROUP_LEADER));
        groupService.delete(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{groupId}/tasks",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> createTask(
            @PathVariable Long groupId,
            @RequestPart("data") TaskInboundResource inbound,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        authorizationService.requireRoleIn(groupId, Set.of(
                Role.GROUP_LEADER,Role.TASK_MANAGER));
        var partialTask = taskInboundMapper.toDomain(inbound);
        return ResponseEntity.ok(taskOutboundMapper.toResource(
                groupService.createTask(groupId, partialTask, inbound.getAssignedId(),inbound.getReviewerId(),files)));
    }

}
