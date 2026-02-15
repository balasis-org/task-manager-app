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


@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public class UserServiceImpl extends BaseComponent implements UserService {
    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final BlobStorageService blobStorageService;

    @Override
    @Transactional(readOnly = true)
    public User getMyProfile() {
        return userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));
    }

    @Override
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
    public User updateProfileImage(MultipartFile file){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));
        String blobName = blobStorageService.uploadProfileImage(file, user.getId());
        user.setImgUrl(blobName);
        return user;
    }

}
