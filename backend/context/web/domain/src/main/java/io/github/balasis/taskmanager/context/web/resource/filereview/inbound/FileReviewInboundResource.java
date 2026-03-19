package io.github.balasis.taskmanager.context.web.resource.filereview.inbound;

import io.github.balasis.taskmanager.context.base.enumeration.FileReviewDecision;
import io.github.balasis.taskmanager.context.web.resource.BaseInboundResource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileReviewInboundResource extends BaseInboundResource {
    @NotNull(message = "Review status is required")
    private FileReviewDecision status;

    @Size(max = 200, message = "Note must be at most 200 characters")
    private String note;
}
