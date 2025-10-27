package io.github.balasis.taskmanager.context.base.service;

import io.github.balasis.taskmanager.context.base.component.BaseComponent;
import io.github.balasis.taskmanager.context.base.exception.notfound.EntityNotFoundException;
import io.github.balasis.taskmanager.context.base.model.BaseModel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public abstract class BasicServiceImpl<T extends BaseModel, E extends EntityNotFoundException> extends BaseComponent implements BaseService<T, Long> {
    public abstract JpaRepository<T, Long> getRepository();

    public abstract Class<E> getNotFoundExceptionClass();

    public abstract String getModelName();

    @Override
    public T create(final T item) {
        return getRepository().save(item);
    }

    @Override
    public T get(final Long id) {
        return getRepository().findById(id)
                .orElseThrow(() -> createNotFoundException(
                        getModelName() + " with ID " + id + " not found."));
    }

    @Override
    public void update(final T item) {
        if (!getRepository().existsById(item.getId())) {
            throw createNotFoundException(
                    getModelName() + " with ID " + item.getId() + " not found.");
        }
        getRepository().save(item);
    }

    @Override
    public void delete(final Long id) {
        if (!getRepository().existsById(id)) {
            throw createNotFoundException(
                    getModelName() + " with ID " + id + " not found.");
        }
        getRepository().deleteById(id);
    }

    @Override
    public List<T> findAll() {
        return getRepository().findAll();
    }

    @Override
    public boolean exists(final T item) {
        return getRepository().existsById(item.getId());
    }

    private E createNotFoundException(String message) {
        try {
            return getNotFoundExceptionClass()
                    .getConstructor(String.class)
                    .newInstance(message);
        } catch (Exception e) {
            throw new EntityNotFoundException("Entity " + message);
        }
    }
}
