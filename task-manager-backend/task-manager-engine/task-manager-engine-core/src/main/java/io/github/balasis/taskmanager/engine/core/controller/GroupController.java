package io.github.balasis.taskmanager.engine.core.controller;

import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.web.resource.group.inbound.GroupInboundResource;
import io.github.balasis.taskmanager.context.web.resource.group.outbound.GroupOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.inbound.TaskInboundResource;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.engine.core.mapper.inbound.GroupInboundMapper;
import io.github.balasis.taskmanager.engine.core.mapper.inbound.TaskInboundMapper;
import io.github.balasis.taskmanager.engine.core.mapper.outbound.GroupOutboundMapper;
import io.github.balasis.taskmanager.engine.core.mapper.outbound.TaskOutboundMapper;
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

    @PostMapping(path = "/{groupId}/tasks",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskOutboundResource> createTask(
            @PathVariable Long groupId,
            @RequestPart("data") TaskInboundResource inbound,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        authorizationService.requireRoleIn(groupId, Set.of(
                Role.GROUP_LEADER,Role.TASK_MANAGER));
        var partialTask = taskInboundMapper.toDomain(inbound);
        return ResponseEntity.ok(taskOutboundMapper.toResource(
                groupService.createTask(groupId, partialTask, inbound.getAssignedId(),inbound.getReviewerId(),file)));
    }

//    @PostMapping("/{id}/task/upload")
//    public ResponseEntity<TaskResource> createWithFile(
//            @PathVariable("id") String id,
//            @RequestParam("title") String title,
//            @RequestParam("description") String description,
//            @RequestParam("taskState") String taskState,
//            @RequestPart(required = false) MultipartFile file){
//
//        //    if (file == null || file.isEmpty()) {
//        //        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
//        //    }
//
//        var taskResource = new TaskResource(title,description,taskState,null);
//        resourceDataValidator.validateResourceData(taskResource);
//        return ResponseEntity.ok(
//                taskMapper.toResource(
//                        taskService.createWithFile( taskMapper.toDomain(taskResource),file) )
//        );
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<TaskResource> get(
//            @PathVariable("id") final Long id) {
//        return ResponseEntity.ok(
//                taskMapper.toResource(taskService.get(id))
//        );
//    }
//
//    @PostMapping
//    public ResponseEntity<TaskResource> create(
//            @RequestBody final TaskResource resource) {
//
//        resourceDataValidator.validateResourceData(resource);
//        resource.setId(null);
//        return ResponseEntity.ok(
//                taskMapper.toResource(
//                        taskService.create(taskMapper.toDomain(resource)))
//        );
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Void> update(
//            @PathVariable Long id,
//            @RequestBody final TaskResource resource) {
//
//        resourceDataValidator.validateResourceData(resource);
//        resource.setId(id);
//        Task domainObject = taskMapper.toDomain(resource);
//        domainObject.setId(id);
//        taskService.update(domainObject);
//        return ResponseEntity.noContent().build();
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(
//            @PathVariable Long id) {
//
//        taskService.delete(id);
//        return ResponseEntity.noContent().build();
//    }
//


}
