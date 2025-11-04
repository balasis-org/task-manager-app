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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController extends BaseController<Task, TaskResource> {
    private final TaskService taskService;
    private final ResourceDataValidator resourceDataValidator;
    private final TaskValidator taskValidator;//Do not remove
    private final TaskMapper taskMapper;

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
