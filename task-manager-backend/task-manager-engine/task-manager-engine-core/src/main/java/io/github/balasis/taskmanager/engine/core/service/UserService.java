package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User getMyProfile();
    User findCurrentUser();
    User patchMyProfile(User user);
    User updateProfileImage(MultipartFile file);
    Page<User> searchUser(String q, Pageable pageable);
    Page<User> searchUserForInvites(Long groupId, String q, boolean sameOrgOnly, Pageable pageable);
}
