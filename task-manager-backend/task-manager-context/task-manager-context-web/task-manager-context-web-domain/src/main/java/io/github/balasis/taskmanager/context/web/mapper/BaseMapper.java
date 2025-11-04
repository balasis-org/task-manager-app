package io.github.balasis.taskmanager.context.web.mapper;

import java.util.List;

public interface BaseMapper<D, R> {
    D toDomain(R resource);

    List<D> toDomains(List<R> resources);

    R toResource(D domain);

    List<R> toResources(List<D> domains);
}
