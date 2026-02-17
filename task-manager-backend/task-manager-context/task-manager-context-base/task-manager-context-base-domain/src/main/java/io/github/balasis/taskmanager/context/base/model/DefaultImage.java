package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "default_images")
public class DefaultImage extends BaseModel{
    @Enumerated(EnumType.STRING)
    @Column
    private BlobContainerType type;
    @Column(length = 300)
    private String fileName;
}
