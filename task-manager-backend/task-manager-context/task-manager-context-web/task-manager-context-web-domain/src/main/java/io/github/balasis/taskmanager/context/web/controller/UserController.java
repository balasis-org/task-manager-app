package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.blob.upload.BlobUploadImageException;
import io.github.balasis.taskmanager.context.web.mapper.inbound.UserInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.UserMiniForDropdownOutboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.UserOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.user.inbound.UserInboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserMiniForDropdownOutboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import io.github.balasis.taskmanager.context.web.validation.ResourceDataValidator;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.engine.core.service.UserService;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController extends BaseComponent {
    private final UserService userService;
    private final UserOutboundMapper userOutboundMapper;
    private final UserMiniForDropdownOutboundMapper userMiniForDropdownOutboundMapper;
    private final UserInboundMapper userInboundMapper;
    private final ResourceDataValidator resourceDataValidator;

    @GetMapping("/me")
    public ResponseEntity<UserOutboundResource> getMyProfile(){
        return ResponseEntity.ok( userOutboundMapper.toResource(
                userService.getMyProfile()
        ));
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
        return ResponseEntity.ok( userOutboundMapper.toResource(
                userService.patchMyProfile(
                    userInboundMapper.toDomain(userInboundResource)
                )
        ));
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

    /**
     * Lists all seeded default images of a given type as full blob paths
     * (e.g. {@code "default-images/profile1.png"}) that the frontend can pass
     * directly to {@code blobUrl()}.
     *
     * @param type {@code PROFILE_IMAGES} or {@code GROUP_IMAGES}
     */
    @GetMapping("/me/default-images")
    public ResponseEntity<List<String>> getDefaultImages(@RequestParam BlobContainerType type) {
        return ResponseEntity.ok(userService.findDefaultImages(type));
    }

    /**
     * Applies a seeded default image as the user's profile picture.
     * Clears any previously uploaded custom image and sets the chosen
     * default as {@code defaultImgUrl}.
     *
     * @param fileName bare file name returned by {@code /me/default-images}
     *                 (e.g. {@code "profile2.png"} — without the container prefix)
     */
    @PatchMapping("/me/profile-image/pick-default")
    public ResponseEntity<UserOutboundResource> pickDefaultProfileImage(@RequestParam String fileName) {
        return ResponseEntity.ok(userOutboundMapper.toResource(
                userService.pickDefaultProfileImage(fileName)
        ));
    }

}
