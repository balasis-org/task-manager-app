package io.github.balasis.taskmanager.context.web.mapper.outbound;

import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.model.Task;
import io.github.balasis.taskmanager.context.base.model.TaskParticipant;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.resource.task.outbound.TaskOutboundResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {TaskFileOutboundMapper.class,UserOutboundMapper.class})
public interface TaskOutboundMapper extends BaseOutboundMapper<Task, TaskOutboundResource> {


    @Mapping(source = "taskParticipants", target = "assignees", qualifiedByName = "assignees")
    @Mapping(source = "taskParticipants", target = "reviewers", qualifiedByName = "reviewers")
    TaskOutboundResource toResource(Task task);

    @Named("assignees")
    default Set<User> assignees(Set<TaskParticipant> participants) {
        return participants == null ? Set.of() :
                participants.stream()
                        .filter(p -> p.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE)
                        .map(TaskParticipant::getUser)
                        .collect(Collectors.toSet());
    }

    @Named("reviewers")
    default Set<User> reviewers(Set<TaskParticipant> participants) {
        return participants == null ? Set.of() :
                participants.stream()
                        .filter(p -> p.getTaskParticipantRole() == TaskParticipantRole.REVIEWER)
                        .map(TaskParticipant::getUser)
                        .collect(Collectors.toSet());
    }

}

