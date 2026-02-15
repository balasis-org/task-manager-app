package io.github.balasis.taskmanager.context.web.mapper.outbound;

import java.util.Set;

public interface BaseOutboundMapper<D,R> {
    R toResource(D domain);

    Set<R> toResources(Set<D> domains);
}
