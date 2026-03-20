package io.github.balasis.taskmanager.engine.infrastructure.email;

// Centralised plain-text email templates.
// Each message ends with a direct link so the recipient can jump straight into the app.
public final class TaskEmailTemplates {

    private TaskEmailTemplates() {}

    /* task notification (manual email button) */

    public static String notifySubject(String taskTitle) {
        return "Task notification: " + taskTitle;
    }

    public static String notifyBody(String callerName, String taskTitle, String groupName,
                                     Long groupId, Long taskId, String customNote, String appUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append(callerName)
          .append(" wants your attention on the task '")
          .append(taskTitle)
          .append("' in group '")
          .append(groupName)
          .append("'.");

        if (customNote != null && !customNote.isBlank()) {
            sb.append("\n\nNote from ").append(callerName).append(":\n").append(customNote);
        }

        sb.append("\n\n--- Open the task ---\n")
          .append(taskLink(appUrl, groupId, taskId))
          .append(optOutFooter());

        return sb.toString();
    }

    /* group invitation */

    public static String inviteSubject(String groupName) {
        return "Group invitation: " + groupName;
    }

    public static String inviteBody(String groupName, String inviterName, String comment) {
        StringBuilder sb = new StringBuilder();
        sb.append("You have been invited to join the group '")
          .append(groupName)
          .append("' by ")
          .append(inviterName)
          .append(".");

        if (comment != null && !comment.isBlank()) {
            sb.append("\n\nInviter's comment: ").append(comment);
        }

        sb.append("\n\nLog in to view and respond to your invitations.")
          .append(optOutFooter());
        return sb.toString();
    }

    /* helpers */

    private static String taskLink(String appUrl, Long groupId, Long taskId) {
        String base = (appUrl == null || appUrl.isBlank()) ? "http://localhost:5173" : appUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/group/" + groupId + "/task/" + taskId;
    }

    private static String optOutFooter() {
        return "\n\n---\nIn case you want to avoid receiving emails, "
             + "you may go to Settings and disable Email notifications.";
    }
}
