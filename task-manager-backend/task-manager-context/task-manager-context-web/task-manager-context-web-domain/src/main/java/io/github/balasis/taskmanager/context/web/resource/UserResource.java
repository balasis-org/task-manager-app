package io.github.balasis.taskmanager.context.web.resource;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserResource extends BaseResource{
    private String email;
    private String name;
}
