package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.limits.PlanLimits;
import io.github.balasis.taskmanager.context.base.model.User;
import io.github.balasis.taskmanager.context.web.mapper.inbound.UserInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.UserMiniForDropdownOutboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.UserOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.user.inbound.UserInboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserMiniForDropdownOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// thin REST controller for profile + account CRUD — delegates everything to UserService,
// enriches responses with plan-specific limits (storage budget, download budget, email quota)
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController extends BaseComponent {
    private final UserService userService;
    private final UserOutboundMapper userOutboundMapper;
    private final UserMiniForDropdownOutboundMapper userMiniForDropdownOutboundMapper;
    private final UserInboundMapper userInboundMapper;
    private final ResourceDataValidator resourceDataValidator;
    private final PlanLimits planLimits;

    @GetMapping("/me")
    public ResponseEntity<UserOutboundResource> getMyProfile(){
        User me = userService.getMyProfile();
        UserOutboundResource resource = userOutboundMapper.toResource(me);
        resource.setStorageBudgetBytes(planLimits.storageBudgetBytes(me.getSubscriptionPlan()));
        resource.setMaxGroups(planLimits.maxGroups(me.getSubscriptionPlan()));
        resource.setDownloadBudgetBytes(planLimits.downloadBudgetBytes(me.getSubscriptionPlan()));
        resource.setMaxMembersPerGroup(planLimits.maxMembersPerGroup(me.getSubscriptionPlan()));
        resource.setImageScansPerMonth(planLimits.imageScansPerMonth(me.getSubscriptionPlan()));
        resource.setEmailsPerMonth(planLimits.emailQuotaPerMonth(me.getSubscriptionPlan()));
        resource.setTaskAnalysisCreditsPerMonth(planLimits.taskAnalysisCreditsPerMonth(me.getSubscriptionPlan()));
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserMiniForDropdownOutboundResource>> searchUsers(
            @RequestParam(required = false) String q,
            Pageable pageable
    ){
        return ResponseEntity.ok(
                userService.searchUser(q,pageable).map(
                        userMiniForDropdownOutboundMapper::toResource
                )
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<UserOutboundResource> patchMyProfile(
           @RequestBody UserInboundResource userInboundResource){
        resourceDataValidator.validateResourceData(userInboundResource);
        User patched = userService.patchMyProfile(
                userInboundMapper.toDomain(userInboundResource)
        );
        UserOutboundResource resource = userOutboundMapper.toResource(patched);
        resource.setStorageBudgetBytes(planLimits.storageBudgetBytes(patched.getSubscriptionPlan()));
        resource.setMaxGroups(planLimits.maxGroups(patched.getSubscriptionPlan()));
        resource.setDownloadBudgetBytes(planLimits.downloadBudgetBytes(patched.getSubscriptionPlan()));
        resource.setMaxMembersPerGroup(planLimits.maxMembersPerGroup(patched.getSubscriptionPlan()));
        resource.setImageScansPerMonth(planLimits.imageScansPerMonth(patched.getSubscriptionPlan()));
        resource.setEmailsPerMonth(planLimits.emailQuotaPerMonth(patched.getSubscriptionPlan()));
        resource.setTaskAnalysisCreditsPerMonth(planLimits.taskAnalysisCreditsPerMonth(patched.getSubscriptionPlan()));
        return ResponseEntity.ok(resource);
    }

    @PostMapping("/me/profile-image")
    public ResponseEntity<UserOutboundResource> updateProfileImage(
            @RequestParam("file") MultipartFile file){
        return ResponseEntity.ok(userOutboundMapper.toResource(
                userService.updateProfileImage(file)
        ));
    }

    @PostMapping("/me/refresh-invite-code")
    public ResponseEntity<UserOutboundResource> refreshInviteCode() {
        return ResponseEntity.ok(userOutboundMapper.toResource(
                userService.refreshInviteCode()
        ));
    }

    @GetMapping("/me/default-images")
    public ResponseEntity<List<String>> getDefaultImages(@RequestParam BlobContainerType type) {
        return ResponseEntity.ok(userService.findDefaultImages(type));
    }

    @PatchMapping("/me/profile-image/pick-default")
    public ResponseEntity<UserOutboundResource> pickDefaultProfileImage(@RequestParam String fileName) {
        return ResponseEntity.ok(userOutboundMapper.toResource(
                userService.pickDefaultProfileImage(fileName)
        ));
    }

    @PatchMapping("/me/profile-image/pick-microsoft")
    public ResponseEntity<UserOutboundResource> pickMicrosoftProfilePhoto() {
        return ResponseEntity.ok(userOutboundMapper.toResource(
                userService.pickMicrosoftProfilePhoto()
        ));
    }

}
