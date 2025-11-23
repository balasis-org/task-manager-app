package io.github.balasis.taskmanager.engine.core.controller;

import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.web.resource.GroupResource;
import io.github.balasis.taskmanager.context.web.resource.TaskResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.engine.core.mapper.GroupMapper;
import io.github.balasis.taskmanager.engine.core.mapper.TaskMapper;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import io.github.balasis.taskmanager.engine.core.service.authorization.AuthorizationService;
import io.github.balasis.taskmanager.engine.core.validation.GroupValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/groups")
public class GroupController{
    private final ResourceDataValidator resourceDataValidator;
    private final GroupValidator groupValidator;//Do not remove
    private final GroupService groupService;
    private final GroupMapper groupMapper;
    private final TaskMapper taskMapper;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<GroupResource> create(@RequestBody final GroupResource groupResource){
        groupResource.setId(null);
        var group =  groupValidator.validate(groupMapper.toDomain(groupResource));
        return ResponseEntity.ok(groupMapper.toResource(
                groupService.create(group)
        ));
    }

    @GetMapping
    public ResponseEntity<List<GroupResource>> findAllByCurrentUser() {
        return ResponseEntity.ok(groupMapper.toResources(groupService.findAllByCurrentUser()));
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
