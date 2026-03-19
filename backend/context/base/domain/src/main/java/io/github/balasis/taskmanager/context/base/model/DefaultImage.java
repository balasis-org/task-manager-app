package io.github.balasis.taskmanager.context.base.model;

import io.github.balasis.taskmanager.shared.enums.BlobContainerType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

// system-provided default images for profiles and groups. these get uploaded
// to blob storage at startup by DefaultImageBootstrap, then their blob names
// are stored here. when a user or group hasnt set a custom image we pick one
// of these at random.
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "default_images", indexes = {
        @Index(name = "idx_di_type", columnList = "type")
})
public class DefaultImage extends BaseModel{
    @Enumerated(EnumType.STRING)
    @Column
    private BlobContainerType type;
    @Column(length = 300)
    private String fileName;
}
