package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.validation.UserValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;

import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public class UserServiceImpl extends BaseComponent implements UserService {
    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final BlobStorageService blobStorageService;

    @Override
    @Transactional
    public User getMyProfile() {
        User user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));

        // rotate cache encryption key every 7 days
        if (user.getCacheKey() == null || user.getCacheKeyCreatedAt() == null
                || Duration.between(user.getCacheKeyCreatedAt(), Instant.now()).toDays() >= 7) {
            user.rotateCacheKey();
        }

        return user;
    }

    @Override
    @Transactional
    public User patchMyProfile(User user) {
        var fetchedUser = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));
        userValidator.validateForPatch(effectiveCurrentUser.getUserId(),user);
        if (user.getName()!=null){
            fetchedUser.setName(user.getName());
        }
        if (user.getAllowEmailNotification() != null) {
            fetchedUser.setAllowEmailNotification(user.getAllowEmailNotification());
        }
        return fetchedUser;
    }


    @Override
    @Transactional
    public User updateProfileImage(MultipartFile file){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));
        String blobName = blobStorageService.uploadProfileImage(file, user.getId());
        user.setImgUrl(blobName);
        return user;
    }

}
