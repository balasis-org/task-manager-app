package io.github.balasis.taskmanager.engine.core.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.SubscriptionPlan;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
import io.github.balasis.taskmanager.context.base.exception.business.LimitExceededException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.shared.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.repository.UserRepository;
import io.github.balasis.taskmanager.engine.core.validation.UserValidator;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;

import io.github.balasis.taskmanager.engine.infrastructure.blob.service.BlobStorageService;
import io.github.balasis.taskmanager.engine.infrastructure.contentsafety.ImageModerationService;
import io.github.balasis.taskmanager.engine.infrastructure.redis.ImageChangeLimiterService;
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
import java.util.List;

// user profile management: reads, patches, image uploads, invite codes.
// class-level @Transactional is readOnly by default; write methods override
// individually. this means spring skips the dirty-check flush on pure reads.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
public class UserServiceImpl extends BaseComponent implements UserService {
    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final BlobStorageService blobStorageService;
    private final DefaultImageService defaultImageService;
    private final PlanLimits planLimits;
    private final ImageChangeLimiterService imageChangeLimiterService;
    private final ImageModerationService imageModerationService;

    // getMyProfile does some housekeeping as side effects:
    // - bumps lastActiveAt so we know the user is alive
    // - rotates the cache key weekly (used for SAS URL cache busting)
    // - generates an invite code on first call (lazy init)
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public User getMyProfile() {
        User user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));

        user.setLastActiveAt(Instant.now());

        // Cache key is appended to blob SAS URLs so the browser knows to
        // re-download when the image changes. Rotating it weekly prevents
        // stale cached images from lingering forever.
        if (user.getCacheKey() == null || user.getCacheKeyCreatedAt() == null
                || Duration.between(user.getCacheKeyCreatedAt(), Instant.now()).toDays() >= 7) {
            user.rotateCacheKey();
        }

        if (user.getInviteCode() == null) {
            user.refreshInviteCode();
        }

        return user;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
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

    // profile image upload flow: ban check -> paid plan -> burst limiter ->
    // scan quota -> blob upload -> enqueue for AI moderation.
    // Keeps the old blob name so moderation can revert if the new one is flagged.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public User updateProfileImage(MultipartFile file){
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));

        // upload ban check
        if (user.getUploadBannedUntil() != null && Instant.now().isBefore(user.getUploadBannedUntil())) {
            throw new BusinessRuleException(
                    "Image uploads are temporarily prevented until " + user.getUploadBannedUntil() + ". "
                    + "A previous upload violated our content policy.");
        }

        SubscriptionPlan plan = user.getSubscriptionPlan();
        if (!planLimits.isPaid(plan)) {
            throw new BusinessRuleException(
                    "Custom image uploads require a paid plan. Choose a default image instead.");
        }

        imageChangeLimiterService.checkBurstLimit(user.getId());

        // incrementImageScanUsage is a @Modifying query with a WHERE guard
        // (usedImageScansMonth < maxScans). if it returns 0, the quota is exhausted.
        int maxScans = planLimits.imageScansPerMonth(plan);
        int updated = userRepository.incrementImageScanUsage(user.getId(), maxScans);
        if (updated == 0) {
            throw new LimitExceededException(
                    "Monthly image upload limit reached. Upgrade your plan for more.");
        }
        // @Modifying query bypasses persistence context; sync the in-memory entity
        user.setUsedImageScansMonth(user.getUsedImageScansMonth() + 1);

        String oldBlobName = user.getImgUrl();

        String blobName = blobStorageService.uploadProfileImage(file, user.getId());
        user.setImgUrl(blobName);

        // enqueue for async moderation — old blob kept as revert target
        imageModerationService.enqueue(
                user.getId(), "USER", user.getId(), blobName, oldBlobName);

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

    // sameOrgOnly uses the Microsoft Entra tenant ID to filter users
    // from the same organization. only works for org accounts (not personal).
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

    // 5 minute cooldown so users cant spam refreshes trying to brute-force
    // a short invite code
    private static final Duration INVITE_CODE_COOLDOWN = Duration.ofMinutes(5);

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
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

    @Override
    public List<String> findDefaultImages(BlobContainerType type) {
        return defaultImageService.findAll(type);
    }

    // switches to one of the pre-seeded default images and nukes the old
    // custom blob from Azure storage so it doesn't leak
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public User pickDefaultProfileImage(String fileName) {
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));

        String oldBlobName = user.getImgUrl();

        user.setDefaultImgUrl(fileName);
        user.setImgUrl(null);

        if (oldBlobName != null) {
            blobStorageService.deleteProfileImage(oldBlobName);
        }

        return userRepository.save(user);
    }

    // uses the Microsoft Graph profile photo that was synced during login.
    // doesn't go through moderation since it's already hosted by Microsoft.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public User pickMicrosoftProfilePhoto() {
        var user = userRepository.findById(effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Logged in user not found"));

        if (user.getMsProfilePhotoUrl() == null) {
            throw new BusinessRuleException("No Microsoft photo available for this account");
        }

        String oldBlobName = user.getImgUrl();
        user.setImgUrl(null);

        if (oldBlobName != null) {
            blobStorageService.deleteProfileImage(oldBlobName);
        }

        return userRepository.save(user);
    }

    // called during login when we fetch the users MS photo from Graph API.
    // uploads it as a "trusted" image (no content safety scan needed).
    // replaces the old one if there was already an MS photo blob.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void storeMicrosoftPhoto(User user, byte[] photoBytes) {
        String oldMsBlob = user.getMsProfilePhotoUrl();
        String blobName = blobStorageService.uploadTrustedProfileImage(photoBytes, user.getId());
        user.setMsProfilePhotoUrl(blobName);
        userRepository.save(user);
        if (oldMsBlob != null) {
            blobStorageService.deleteProfileImage(oldMsBlob);
        }
    }

}
