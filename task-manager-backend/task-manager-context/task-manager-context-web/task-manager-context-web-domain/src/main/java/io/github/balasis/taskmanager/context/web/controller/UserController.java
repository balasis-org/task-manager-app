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
import io.github.balasis.taskmanager.engine.core.service.UserService;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

}
