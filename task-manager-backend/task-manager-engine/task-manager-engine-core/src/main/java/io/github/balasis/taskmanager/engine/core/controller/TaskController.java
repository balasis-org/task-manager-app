package io.github.balasis.taskmanager.engine.core.controller;


import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.service.BaseService;
import io.github.balasis.taskmanager.context.web.controller.BaseController;
import io.github.balasis.taskmanager.context.web.mapper.BaseMapper;
import io.github.balasis.taskmanager.context.web.resource.TaskResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.engine.core.mapper.TaskMapper;
import io.github.balasis.taskmanager.engine.core.service.TaskService;
import io.github.balasis.taskmanager.engine.core.validation.TaskValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController extends BaseController<Task, TaskResource> {
    private final TaskService taskService;
    private final ResourceDataValidator resourceDataValidator;
    private final TaskValidator taskValidator;//Do not remove
    private final TaskMapper taskMapper;


    @PostMapping("/upload")
    public ResponseEntity<TaskResource> createWithFile(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("taskState") String taskState,
            @RequestPart(required = false) MultipartFile file){

//    if (file == null || file.isEmpty()) {
//        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
//    }

    var taskResource = new TaskResource(title,description,taskState,null);
    resourceDataValidator.validateResourceData(taskResource);
    return ResponseEntity.ok(
            taskMapper.toResource(
                    taskService.createWithFile( getMapper().toDomain(taskResource),file) )
    );
    }

    @GetMapping
    public ResponseEntity<List<TaskResource>> findAll() {
        return ResponseEntity.ok(taskMapper.toResources(taskService.findAll()));
    }

    @Override
    protected BaseService<Task, Long> getBaseService() {
        return taskService;
    }

    @Override
    protected BaseMapper<Task, TaskResource> getMapper() {
        return taskMapper;
    }

    @Override
    protected ResourceDataValidator resourceDatavalidator() {
        return resourceDataValidator;
    }
}
