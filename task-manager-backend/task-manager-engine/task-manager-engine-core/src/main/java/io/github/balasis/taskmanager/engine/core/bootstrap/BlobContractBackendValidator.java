package io.github.balasis.taskmanager.engine.core.bootstrap;


import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.contracts.enums.BlobContainerType;
import io.github.balasis.taskmanager.contracts.enums.BlobDefaultImageContainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * NOTE:
 * This validation relies on Hibernate using
 * PhysicalNamingStrategyStandardImpl.
 * If a different naming strategy is configured, this check
 * no longer guarantees physical table existence.
 * (We might swap to db queries later on if we deem that we
 * need 100% safety)
 */
@Component
@RequiredArgsConstructor
public class BlobContractBackendValidator extends BaseComponent {

    private final EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
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
