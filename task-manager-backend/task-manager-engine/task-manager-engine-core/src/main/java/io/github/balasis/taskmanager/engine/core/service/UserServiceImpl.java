package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.validation.UserValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;

import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        // keep last-active timestamp fresh (used by maintenance cleanup)
        user.setLastActiveAt(Instant.now());

        // rotate cache encryption key every 7 days
        if (user.getCacheKey() == null || user.getCacheKeyCreatedAt() == null
                || Duration.between(user.getCacheKeyCreatedAt(), Instant.now()).toDays() >= 7) {
            user.rotateCacheKey();
        }

        // generate invite code for existing users who don't have one yet
        if (user.getInviteCode() == null) {
            user.refreshInviteCode();
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

    @Override
    public Page<User> searchUser(String q, Pageable pageable) {
        var normalized = (q == null || q.isBlank()) ? null : q.trim();
        return userRepository.searchUser(normalized, pageable);
    }

    @Override
    public User findCurrentUser() {
        return userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));
    }

    @Override
    public Page<User> searchUserForInvites(Long groupId, String q, boolean sameOrgOnly, Pageable pageable) {
        var normalized = (q == null || q.isBlank()) ? null : q.trim();
        String tenantId = null;
        if (sameOrgOnly) {
            User me = findCurrentUser();
            if (me.isOrg()) {
                tenantId = me.getTenantId();
            }
        }
        return userRepository.searchUserForInvites(groupId, normalized, tenantId, pageable);
    }

    private static final Duration INVITE_CODE_COOLDOWN = Duration.ofMinutes(5);

    @Override
    @Transactional
    public User refreshInviteCode() {
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));

        if (user.getInviteCodeCreatedAt() != null
                && Duration.between(user.getInviteCodeCreatedAt(), Instant.now()).compareTo(INVITE_CODE_COOLDOWN) < 0) {
            throw new BusinessRuleException("You can refresh your invite code once every "
                    + INVITE_CODE_COOLDOWN.toMinutes() + " minutes");
        }

        user.refreshInviteCode();
        return userRepository.save(user);
    }

}
