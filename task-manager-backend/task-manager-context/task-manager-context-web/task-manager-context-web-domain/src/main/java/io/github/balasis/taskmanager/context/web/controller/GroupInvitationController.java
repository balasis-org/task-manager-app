package io.github.balasis.taskmanager.context.web.controller;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException;
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
public class GroupInvitationController extends BaseComponent {
    private final GroupInvitationOutboundMapper groupInvitationOutboundMapper;
    private final GroupService groupService;


    @PatchMapping("/{groupInvitationId}/status")
    public ResponseEntity<GroupInvitationOutboundResource> respondToInvitation(
            @PathVariable("groupInvitationId") Long groupInvitationId,
            @RequestParam("status") InvitationStatus status
    ) {
        return ResponseEntity.ok(
                groupInvitationOutboundMapper.toResource(
                        groupService.respondToInvitation(groupInvitationId, status)
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Set<GroupInvitationOutboundResource>> findMyGroupInvitations(){
        return ResponseEntity.ok(groupInvitationOutboundMapper.toResources(
                groupService.findMyGroupInvitations()
        ));
    }

    @GetMapping("/sent")
    public ResponseEntity<Set<GroupInvitationOutboundResource>> findInvitationsSentByMe() {
            return ResponseEntity.ok(groupInvitationOutboundMapper.toResources(
                            groupService.findInvitationsSentByMe()
            ));
    }

    /**
     * Lightweight poll endpoint.  Returns 204 when there are NO new invitations.
     * Throws a 409 (BusinessRuleException) when the user has unseen pending invitations,
     * so the frontend can catch the non-2xx status and show a notification badge.
     */
    @GetMapping("/check-new")
    public ResponseEntity<Void> checkNewInvitations() {
        if (groupService.hasNewInvitations()) {
            throw new BusinessRuleException("NEW_INVITATIONS");
        }
        return ResponseEntity.noContent().build();
    }

}
