package io.github.balasis.taskmanager.context.web.mapper;

import java.util.List;

public interface BaseOutboundMapper<D,R> {
    R toResource(D domain);

    List<R> toResources(List<D> domains);
}
