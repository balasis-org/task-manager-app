package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User getMyProfile();
    User patchMyProfile(User user);
    User updateProfileImage(MultipartFile file);
}
