package io.github.balasis.taskmanager.context.web.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class GroupResource extends BaseResource{

    @NotBlank(message = "name is mandatory")
    private String name;

    private String description;

    private UserResource owner;
}
