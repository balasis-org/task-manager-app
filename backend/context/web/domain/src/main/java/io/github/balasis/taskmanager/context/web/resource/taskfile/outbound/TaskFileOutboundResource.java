package io.github.balasis.taskmanager.context.web.resource.taskfile.outbound;

import io.github.balasis.taskmanager.context.web.resource.BaseOutboundResource;
import io.github.balasis.taskmanager.engine.core.dto.FileReviewInfoDto;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TaskFileOutboundResource extends BaseOutboundResource {
    private String name;
    private Long fileSize;
    private String uploadedByName;
    private Instant createdAt;
    private List<FileReviewInfoDto> reviews;
}
