package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.web.mapper.inbound.UserInboundMapper;
import io.github.balasis.taskmanager.context.web.mapper.outbound.UserOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.user.inbound.UserInboundResource;
import io.github.balasis.taskmanager.context.web.resource.user.outbound.UserOutboundResource;
import io.github.balasis.taskmanager.engine.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController extends BaseComponent {
    private final UserService userService;
    private final UserOutboundMapper userOutboundMapper;
    private final UserInboundMapper userInboundMapper;

    @GetMapping("/me")
    public ResponseEntity<UserOutboundResource> getMyProfile(){
        return ResponseEntity.ok( userOutboundMapper.toResource(
                userService.getMyProfile()
        ));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserOutboundResource> patchMyProfile(
           @RequestBody UserInboundResource userInboundResource){
        return ResponseEntity.ok( userOutboundMapper.toResource(
                userService.patchMyProfile(
                    userInboundMapper.toDomain(userInboundResource)
                )
        ));
    }
}
