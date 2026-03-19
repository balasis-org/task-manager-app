package io.github.balasis.taskmanager.engine.core.bootstrap;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.shared.enums.BlobContainerType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

// startup cross-check between the BlobContainerType enum (in the shared module)
// and the actual JPA metamodel. if someone adds a new BlobContainerType but forgets
// to create the matching entity/column, this fails fast at boot instead of blowing up
// at runtime when a file upload hits that container.
@Component
@RequiredArgsConstructor
public class BlobContractBackendValidator extends BaseComponent {

    private final EntityManager entityManager;

    // runs at HIGHEST_PRECEDENCE so it blows up before anything else touches blob storage
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void validate() {
        for (BlobContainerType type : BlobContainerType.values()) {
            validateEntity(type);
            validateColumn(type);
        }
        validateDefaults();
    }

    private void validateEntity(BlobContainerType type) {
        boolean tableExists = entityManager.getMetamodel()
                .getEntities()
                .stream()
                .map(EntityType::getJavaType)
                .map(clazz -> clazz.getAnnotation(Table.class))
                .filter(Objects::nonNull)
                .map(Table::name)
                .anyMatch(name -> name.equalsIgnoreCase(type.getTableName()));
        if (!tableExists) {
            throw new IllegalStateException(
                   "Blob contract invalid: no entity mapped to table "
                            + type.getTableName()
            );
        }
    }

    private void validateColumn(BlobContainerType type) {

        EntityType<?> entity = entityManager.getMetamodel()
                .getEntities()
                .stream()
                .map(EntityType::getJavaType)
                .filter(clazz -> {
                    Table table = clazz.getAnnotation(Table.class);
                    return table != null
                            && table.name().equalsIgnoreCase(type.getTableName());
                })
                .findFirst()
                .map(entityManager.getMetamodel()::entity)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Blob contract invalid: no entity mapped to table "
                                        + type.getTableName()
                        )
                );

        boolean columnExists = entity.getAttributes()
                .stream()
                .anyMatch(a -> a.getName().equals(type.getColumnName()));

        if (!columnExists) {
            throw new IllegalStateException(
                    "Blob contract invalid: column '"
                            + type.getColumnName()
                            + "' not found in table "
                            + type.getTableName()
            );
        }
    }

    private void validateDefaults() {
        EntityType<?> entity = entityManager.getMetamodel()
                .getEntities()
                .stream()
                .map(EntityType::getJavaType)
                .filter(clazz -> {
                    Table table = clazz.getAnnotation(Table.class);
                    return table != null
                            && table.name().equalsIgnoreCase("default_images");
                })
                .findFirst()
                .map(entityManager.getMetamodel()::entity)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Default blob contract invalid: no entity mapped to table default_images"
                        )
                );

        boolean hasTypeColumn = entity.getAttributes()
                .stream()
                .anyMatch(a -> a.getName().equals("type"));

        boolean hasFileNameColumn = entity.getAttributes()
                .stream()
                .anyMatch(a -> a.getName().equals("fileName"));

        if (!hasTypeColumn || !hasFileNameColumn) {
            throw new IllegalStateException(
                    "Default blob contract invalid: table default_images must contain columns [type, fileName]"
            );
        }
    }

}
