package io.github.balasis.taskmanager.context.base.service;

import java.util.List;

public interface BaseService<T, K> {
    T create(final T item);

    void update(T item);

    void delete(K id);

    T get(K id);

    List<T> findAll();

    boolean exists(T item);
}
