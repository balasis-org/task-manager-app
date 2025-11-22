package io.github.balasis.taskmanager.engine.core.service;


import io.github.balasis.taskmanager.context.base.model.Group;
import io.github.balasis.taskmanager.context.base.service.BaseService;

public interface GroupService extends BaseService{
    Group create(Group group);

}
