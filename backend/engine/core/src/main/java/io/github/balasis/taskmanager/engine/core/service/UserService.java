package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.shared.enums.BlobContainerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// user profile contract. thin wrapper -- most of the interesting
// logic lives in UserServiceImpl (image upload pipeline, invite codes, etc.)
public interface UserService {
    // getMyProfile has side-effects (lastActiveAt, cache key rotation)
    User getMyProfile();
    User findCurrentUser();
    User patchMyProfile(User user);
    User updateProfileImage(MultipartFile file);
    Page<User> searchUser(String q, Pageable pageable);
    Page<User> searchUserForInvites(Long groupId, String q, boolean sameOrgOnly, Pageable pageable);
    User refreshInviteCode();
    List<String> findDefaultImages(BlobContainerType type);
    User pickDefaultProfileImage(String fileName);
    User pickMicrosoftProfilePhoto();
    void storeMicrosoftPhoto(User user, byte[] photoBytes);
}
