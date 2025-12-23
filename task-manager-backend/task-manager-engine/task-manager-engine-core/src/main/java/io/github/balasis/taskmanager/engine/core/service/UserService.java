package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.User;

public interface UserService {
    User getMyProfile();
    User patchMyProfile(User user);
}
