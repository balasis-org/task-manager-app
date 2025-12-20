package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.web.mapper.outbound.GroupInvitationOutboundMapper;
import io.github.balasis.taskmanager.context.web.resource.groupinvitation.outbound.GroupInvitationOutboundResource;
import io.github.balasis.taskmanager.engine.core.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group-invitations")
public class GroupInvitationController {
    private final GroupInvitationOutboundMapper groupInvitationOutboundMapper;
    private final GroupService groupService;


    @PostMapping("/{groupInvitationId}/accept")
    public ResponseEntity<GroupInvitationOutboundResource> acceptInvitation(
            @PathVariable("groupInvitationId") Long groupInvitationId
    ) {
        return ResponseEntity.ok(
                groupInvitationOutboundMapper.toResource(
                        groupService.acceptInvitation(groupInvitationId)
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Set<GroupInvitationOutboundResource>> findMyGroupInvitations(){
        return ResponseEntity.ok(groupInvitationOutboundMapper.toResources(
                groupService.findMyGroupInvitations()
        ));
    }

}
