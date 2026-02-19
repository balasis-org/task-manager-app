package io.github.balasis.taskmanager.engine.core.validation;

import io.github.balasis.taskmanager.context.base.enumeration.InvitationStatus;
import io.github.balasis.taskmanager.context.base.enumeration.Role;
import io.github.balasis.taskmanager.context.base.enumeration.TaskParticipantRole;
import io.github.balasis.taskmanager.context.base.exception.authorization.InvalidRoleException;
import io.github.balasis.taskmanager.context.base.exception.authorization.NotAGroupMemberException;
import io.github.balasis.taskmanager.context.base.exception.authorization.UnauthorizedException;
import io.github.balasis.taskmanager.context.base.exception.business.InvalidMembershipRemovalException;
import io.github.balasis.taskmanager.context.base.exception.business.InvalidRoleAssignmentException;
import io.github.balasis.taskmanager.context.base.exception.duplicate.GroupDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.duplicate.GroupInviteDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.duplicate.GroupMemberDuplicateException;
import io.github.balasis.taskmanager.context.base.exception.notfound.TaskParticipantNotFoundException;
import io.github.balasis.taskmanager.context.base.exception.notfound.UserNotFoundException;
import io.github.balasis.taskmanager.context.base.exception.validation.InvalidFieldValueException;
import io.github.balasis.taskmanager.context.base.model.*;
import io.github.balasis.taskmanager.engine.core.repository.*;
import io.github.balasis.taskmanager.engine.core.service.authorization.RolePolicyService;
import io.github.balasis.taskmanager.engine.infrastructure.auth.loggedinuser.EffectiveCurrentUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GroupValidatorImpl implements GroupValidator{
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final EffectiveCurrentUser effectiveCurrentUser;
    private final RolePolicyService rolePolicyService;
    private final GroupInvitationRepository groupInvitationRepository;
    private final UserRepository userRepository;

    @Override
    public void validate(Group group) {
        doesGroupNameAlreadyExists(group);
    }

    @Override
    public void validateForPatch(Long id, Group group) {
        isGroupNameEmpty(group);
        doesAnyOtherGroupNameAlreadyExists(id,group);
    }

    @Override
    public void validateForCreateTask(Long groupId, Set<Long> assignedIds, Set<Long> reviewerIds) {
        validateToBeSetAssigneesForTask(groupId,assignedIds);
        validateToBeSetReviewersForTask(groupId, reviewerIds);
    }

    @Override
    public void validateForPatchTask(Long groupId, Long taskId, Task task) {
        if (task.getTitle() != null && task.getTitle().isBlank()){
            throw new InvalidFieldValueException("Task title cannot be empty");
        }
        if (task.getDescription() != null && task.getDescription().isBlank()){
            throw new InvalidFieldValueException("A minimum description is required");
        }
    }

    @Override
    public void validateAddAssigneeToTask(Task task, Long groupId, Long userId) {
        var membership = isUserToBeSetAssigneeMemberOfGroup(groupId,userId);
        isUserRoleEnoughToBeAnAssignee(membership);
        doesTaskBelongToGroup(task,groupId);
        isUserAlreadyAssigneeInTask(task,userId);
    }

    @Override
    public void validateAddReviewerToTask(Task task, Long groupId, Long userId) {
        var membership = isUserToBeSetReviewerMemberOfGroup(groupId,userId);
        isUserRoleEnoughToBeAReviewer(membership);
        doesTaskBelongToGroup(task,groupId);
        isUserAlreadyReviewerInTask(task,userId);
    }

    @Override
    public void validateAddTaskFile(Task task, Long groupId, MultipartFile file) {
        doesTaskBelongToGroup(task,groupId);
        var membership = groupMembershipRepository
                .findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not part of the group"));
        boolean isLeaderOrManager = membership.getRole() == Role.GROUP_LEADER || membership.getRole() == Role.TASK_MANAGER;
        if (!isLeaderOrManager) {
            throw new UnauthorizedException("You do not have the rights to upload in this task");
        }
        isFileEmpty(file);
        ensureMaxFiles(task.getCreatorFiles().size(), 3);
    }

    @Override
    public void validateAddAssigneeTaskFile(Task task, Long groupId, MultipartFile file) {
        doesTaskBelongToGroup(task, groupId);
        var membership = groupMembershipRepository
                .findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not part of the group"));

        boolean isLeaderOrManager = membership.getRole() == Role.GROUP_LEADER || membership.getRole() == Role.TASK_MANAGER;
        boolean isAssignee = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE
                        && tp.getUser().getId().equals(effectiveCurrentUser.getUserId()));

        boolean allowed = isLeaderOrManager || isAssignee;
        if (!allowed) {
            throw new UnauthorizedException("You do not have the rights to upload assignee files in this task");
        }

        isFileEmpty(file);
        ensureMaxFiles(task.getAssigneeFiles().size(), 3);
    }

    @Override
    public void validateDownloadTaskFile(Task task, Long groupId) {
        doesTaskBelongToGroup(task,groupId);
        var membership = groupMembershipRepository
                .findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not part of the group"));
        boolean allowed =
                membership.getRole() == Role.GROUP_LEADER ||
                membership.getRole() == Role.TASK_MANAGER ||
                task.getTaskParticipants().stream()
                            .anyMatch(tp -> tp.getUser().getId().equals(effectiveCurrentUser.getUserId()));

        if (!allowed) {
            throw new UnauthorizedException("You are not allowed to download this file");
        }
    }

    @Override
    public void validateRemoveTaskParticipant(Task task, Long groupId, Long taskParticipantId) {
        doesTaskBelongToGroup(task,groupId);
        doesTaskParticipantBelongsToTask(taskParticipantId,task);
    }

    @Override
    public void validateRemoveTaskFile(Task task, Long groupId, Long fileId) {
        doesTaskBelongToGroup(task,groupId);
        doesTheFileExistInGroup(task,fileId);

        var membership = groupMembershipRepository
                .findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not part of the group"));

        boolean isLeaderOrManager = membership.getRole() == Role.GROUP_LEADER || membership.getRole() == Role.TASK_MANAGER;
        if (!isLeaderOrManager) {
            throw new UnauthorizedException("You do not have the rights to remove creator files from this task");
        }
    }

    @Override
    public void validateRemoveAssigneeTaskFile(Task task, Long groupId, Long fileId) {
        doesTaskBelongToGroup(task, groupId);
        doesAssigneeFileExistInTask(task, fileId);

        var membership = groupMembershipRepository
                .findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not part of the group"));

        boolean isLeaderOrManager = membership.getRole() == Role.GROUP_LEADER || membership.getRole() == Role.TASK_MANAGER;
        boolean isAssignee = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE
                        && tp.getUser().getId().equals(effectiveCurrentUser.getUserId()));
        if (!(isLeaderOrManager || isAssignee)) {
            throw new UnauthorizedException("You do not have the rights to remove assignee files from this task");
        }
    }

    @Override
    public void validateAssigneeMarkTaskToBeReviewed(Task task, Long groupId) {
        doesTaskBelongToGroup(task, groupId);
        boolean isAssignee = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE
                        && tp.getUser().getId().equals(effectiveCurrentUser.getUserId()));
        if (!isAssignee) {
            throw new UnauthorizedException("Only assignees can move the task to TO_BE_REVIEWED");
        }
    }

    @Override
    public void validateReviewTask(Task task, Long groupId, Long userId) {
        doesTaskBelongToGroup(task, groupId);

        boolean isReviewerOnTask = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.REVIEWER
                        && tp.getUser().getId().equals(userId));

        if (isReviewerOnTask) {
            return;
        }

        var membershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, userId);
        if (membershipOpt.isEmpty()) {
            throw new NotAGroupMemberException("User is not part of the group and not a reviewer of the task");
        }
        var role = membershipOpt.get().getRole();
        boolean allowed = role == Role.GROUP_LEADER || role == Role.TASK_MANAGER;
        if (!allowed) {
            throw new UnauthorizedException("You are not authorized to review this task");
        }
    }

    @Override
    public void validateCreateGroupInvitation(GroupInvitation groupInvitation) {
        isToBeInvitedUserExists(groupInvitation);
        isToBeInvitedUserAlreadyInGroup(groupInvitation);
        isTheRoleChosenValidAccordingToAppLogic(groupInvitation);
        doesInvitationAlreadyExists(groupInvitation);
    }

    @Override
    public void validateInvitationRole(GroupInvitation groupInvitation) {
        isTheRoleChosenValidAccordingToAppLogic(groupInvitation);
    }

    @Override
    public void validateRespondToGroupInvitation(GroupInvitation invitation) {
        isUserTheReceiverOfTheInvitation(invitation);
        isTheInvitationOnPendingStatus(invitation);
    }

    @Override
    public void validateRemoveGroupMember(Long groupId, Long currentUserId, Long memberUserId, Optional<GroupMembership> currentMembershipOpt) {
        if (currentMembershipOpt.isEmpty()) {
            throw new NotAGroupMemberException("Not a member of the group");
        }

        Role currentRole = currentMembershipOpt.get().getRole();

        if (currentRole == Role.GROUP_LEADER && Objects.equals(currentUserId, memberUserId)) {
            throw new InvalidMembershipRemovalException("Group leader cannot remove themselves");
        }

        if (currentRole != Role.GROUP_LEADER && !Objects.equals(currentUserId, memberUserId)) {
            throw new InvalidMembershipRemovalException("Only group leader can remove other members");
        }
    }

    
    @Override
    public void validateChangeGroupMembershipRole(Long groupId, Long targetUserId, Role newRole) {

        var currentMembershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId());

        if (currentMembershipOpt.isEmpty()) {
            throw new NotAGroupMemberException("Not a member of the group");
        }

        if (currentMembershipOpt.get().getRole() != Role.GROUP_LEADER) {
            throw new UnauthorizedException("Only group leader can change roles");
        }

        if (newRole == null) {
            throw new IllegalArgumentException("Role must be provided");
        }

        var targetOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, targetUserId);
        if (targetOpt.isEmpty()) {
            throw new NotAGroupMemberException("Target user is not a member of the group");
        }

    }

    @Override
    public void validateTaskComment(Long groupId, Task task, String comment) {

        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new EntityNotFoundException("Task does not belong to the given group");
        }
        var membershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId())
                .orElseThrow(()-> new NotAGroupMemberException("You are not a member of this group"));
        boolean allowedByGroupRole = membershipOpt.getRole() == Role.GROUP_LEADER || membershipOpt.getRole() == Role.TASK_MANAGER;
        boolean isParticipant = task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getUser().getId().equals(effectiveCurrentUser.getUserId()));
        if (!allowedByGroupRole  && !isParticipant) {
            throw new NotAGroupMemberException("Not allowed to comment on this task");
        }
    }

    @Override
    public void validateTaskPatchComment(Long groupId, TaskComment existing, Long taskId, String comment){
        var task = existing.getTask();
        if (!Objects.equals(task.getId(), taskId) || !Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Comment does not belong to the given task/group");
        }

        if (!Objects.equals(existing.getCreator().getId(), effectiveCurrentUser.getUserId())) {
            throw new UnauthorizedException("Only the creator can edit this comment");
        }
    }

    @Override
    public void validateDeleteTask(Long groupId,TaskComment existing , Task fromExisting ,Long taskId) {
        if (!Objects.equals(fromExisting.getId(), taskId) || !Objects.equals(fromExisting.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Comment does not belong to the given task/group");
        }

        var membershipOpt = groupMembershipRepository.findByGroupIdAndUserId(groupId, effectiveCurrentUser.getUserId());
        boolean isCreator = Objects.equals(existing.getCreator().getId(), effectiveCurrentUser.getUserId());
        boolean isLeaderOrManager = membershipOpt.isPresent() && (
                membershipOpt.get().getRole() == Role.GROUP_LEADER ||
                        membershipOpt.get().getRole() == Role.TASK_MANAGER
        );

        if (!isCreator && !isLeaderOrManager) {
            throw new UnauthorizedException("Not allowed to delete this comment");
        }

    }

    private void doesTaskBelongToGroup(Task task, Long groupId){
        if (!Objects.equals(task.getGroup().getId(), groupId)) {
            throw new UnauthorizedException("Task does not belong to the group given");
        }
    }

    private void isUserTheReceiverOfTheInvitation(GroupInvitation invitation){
        if (!invitation.getUser().getId().equals(effectiveCurrentUser.getUserId())) {
            throw new UnauthorizedException("You are not the recipient of this invitation");
        }
    }

    private void isTheInvitationOnPendingStatus(GroupInvitation invitation){
        if (invitation.getInvitationStatus() != InvitationStatus.PENDING) {
            throw new io.github.balasis.taskmanager.context.base.exception.business.BusinessRuleException(
                    "This invitation has already been " + invitation.getInvitationStatus().name().toLowerCase());
        }
    }

    private void doesGroupNameAlreadyExists(Group group){
        boolean doesGroupExistWithSameName=groupRepository.existsByNameAndOwner_Id(
                group.getName(),effectiveCurrentUser.getUserId());
        if(doesGroupExistWithSameName){
            throw new GroupDuplicateException("You already have a group with that name");
        }
    }

    private void isGroupNameEmpty(Group group){
        if (group.getName() != null && group.getName().isBlank()) {
            throw new InvalidFieldValueException("Name cannot be empty");
        }
    }

    private void doesAnyOtherGroupNameAlreadyExists(Long id, Group group){
        boolean doesGroupExistWithSameName = groupRepository.existsByNameAndOwner_IdAndIdNot(
                group.getName(), effectiveCurrentUser.getUserId(),id);
        if (doesGroupExistWithSameName) {
            throw new GroupDuplicateException("You already have a group with that name");
        }
    }

    private void validateToBeSetAssigneesForTask(Long groupId, Set<Long> assignedIds){
        if (assignedIds != null && !assignedIds.isEmpty()) {
            for (Long assignedId : assignedIds){
                GroupMembership assignedMembership = groupMembershipRepository
                        .findByGroupIdAndUserId(groupId, assignedId)
                        .orElseThrow(() -> new NotAGroupMemberException("Some of the Assigned users are" +
                                " no longer part of the group"));
                isUserRoleEnoughToBeAnAssignee(assignedMembership);
            }
        }
    }

    private void validateToBeSetReviewersForTask(Long groupId, Set<Long> reviewerIds ){
        if (reviewerIds != null && !reviewerIds.isEmpty()) {
            for (Long reviewerId : reviewerIds){
                GroupMembership reviewerMembership = groupMembershipRepository
                        .findByGroupIdAndUserId(groupId, reviewerId)
                        .orElseThrow(() -> new NotAGroupMemberException("Some of the Reviewers are no longer part of the group"));
                isUserRoleEnoughToBeAReviewer(reviewerMembership);
            }
        }
    }

    private GroupMembership isUserToBeSetAssigneeMemberOfGroup(Long groupId,Long userId){
        return groupMembershipRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NotAGroupMemberException("User to be set as assignee is not a member of the group"));
    }

    private void isUserRoleEnoughToBeAnAssignee(GroupMembership membership){
        if (!rolePolicyService.canBeAssignee(membership.getRole())) {
            throw new InvalidRoleException("User to be set as assignee has insufficient role. Allowed roles: "
                    + rolePolicyService.getAllowedAssigneeRoles());
        }
    }

    private void isUserAlreadyAssigneeInTask(Task task,Long userId){
        boolean alreadyAssignee= task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.ASSIGNEE && tp.getUser().getId().equals(userId));
        if (alreadyAssignee) {
            throw new InvalidFieldValueException("User is already an assignee of this task");
        }
    }

    private void isUserAlreadyReviewerInTask(Task task,Long userId){
        boolean alreadyReviewer= task.getTaskParticipants().stream()
                .anyMatch(tp -> tp.getTaskParticipantRole() == TaskParticipantRole.REVIEWER && tp.getUser().getId().equals(userId));
        if (alreadyReviewer) {
            throw new InvalidFieldValueException("User is already a reviewer of this task");
        }
    }

    private void isUserRoleEnoughToBeAReviewer(GroupMembership membership){
        if (!rolePolicyService.canBeReviewer(membership.getRole())) {
            throw new InvalidRoleException("User to be set as reviewer has insufficient role." +
                    " Allowed roles: " + rolePolicyService.getAllowedReviewerRoles());
        }
    }

    private GroupMembership isUserToBeSetReviewerMemberOfGroup(Long groupId, Long userId){
        return groupMembershipRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new NotAGroupMemberException("User to be set as reviewer is not a member of the group"));
    }


    private void isFileEmpty(MultipartFile file){
        if (file == null || file.isEmpty()) {
            throw new InvalidFieldValueException("File cannot be empty");
        }
    }

    private void doesTaskParticipantBelongsToTask(Long taskParticipantId , Task task ){
       if ( task.getTaskParticipants().stream().
                noneMatch(tp -> tp.getId().equals(taskParticipantId))){
           throw new TaskParticipantNotFoundException("TaskParticipant doesn't exist in the task");
       }
    }


    private void doesTheFileExistInGroup(Task task, Long fileId){
        boolean fileExists = task.getCreatorFiles().stream().anyMatch(f -> f.getId().equals(fileId));
        if (!fileExists) {
            throw new InvalidFieldValueException("File does not exist in the task");
        }
    }

    private void doesAssigneeFileExistInTask(Task task, Long fileId) {
        boolean fileExists = task.getAssigneeFiles().stream().anyMatch(f -> f.getId().equals(fileId));
        if (!fileExists) {
            throw new InvalidFieldValueException("Assignee file does not exist in the task");
        }
    }

    private void ensureMaxFiles(int currentCount, int max) {
        if (currentCount >= max) {
            throw new InvalidFieldValueException("Maximum " + max + " files allowed");
        }
    }




    private void isToBeInvitedUserExists(GroupInvitation groupInvitation){
        if(!userRepository.existsById(groupInvitation.getUser().getId())){
            throw new UserNotFoundException("User with id: " + groupInvitation.getUser().getId()
                    + " doesnt exist");
        }
    }

    private void isToBeInvitedUserAlreadyInGroup(GroupInvitation groupInvitation){
        if (groupMembershipRepository.existsByGroupIdAndUserId(
                groupInvitation.getGroup().getId(),groupInvitation.getUser().getId()
        )){throw new GroupMemberDuplicateException("User is already in group");}
    }

    private void isTheRoleChosenValidAccordingToAppLogic(GroupInvitation groupInvitation){
        Role roleOfInviter = groupMembershipRepository.findByGroupIdAndUserId(
                groupInvitation.getGroup().getId(),
                effectiveCurrentUser.getUserId()
        ).orElseThrow(() -> new NotAGroupMemberException("")).getRole();

        if (groupInvitation.getUserToBeInvitedRole() == null){
            throw new InvalidRoleAssignmentException("You should assign a role in the invitation");
        }

        if (groupInvitation.getUserToBeInvitedRole() == Role.GROUP_LEADER){
            throw new InvalidRoleAssignmentException("You may not assign a group leader through an invitation");
        }

        if ( !(groupInvitation.getUserToBeInvitedRole() == Role.MEMBER || groupInvitation.getUserToBeInvitedRole() == Role.GUEST)
            && ( !roleOfInviter.equals(Role.GROUP_LEADER) )
        ){
            throw new InvalidRoleAssignmentException("Only group leader can invite with any other role than member or guest");
        }

    }

    private void doesInvitationAlreadyExists(GroupInvitation groupInvitation){
        // only consider a duplicate if there is already a PENDING invitation for same user+group
        if (groupInvitationRepository.existsByUser_IdAndGroup_IdAndInvitationStatus(
                groupInvitation.getUser().getId(), groupInvitation.getGroup().getId(), InvitationStatus.PENDING
        )){
            throw new GroupInviteDuplicateException("Group invitation already exists and is pending");
        }
    }

}
