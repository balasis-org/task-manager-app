package io.github.balasis.taskmanager.context.base.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users")
public class User extends BaseModel{
    @Column(nullable = false)
    private String azureId;
    @Column(nullable = false)
    private String email;
    @Column
    private String name;
}
