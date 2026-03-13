package io.github.balasis.taskmanager.context.web.resource.taskcomment.inbound;

import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskCommentInboundResource extends BaseInboundResource {
    @NotBlank(message = "Comment cannot be blank")
    @Size(max = 400, message = "Comment must be at most 400 characters")
    private String comment;
}
