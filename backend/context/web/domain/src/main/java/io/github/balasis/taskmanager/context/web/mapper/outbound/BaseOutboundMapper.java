package io.github.balasis.taskmanager.context.web.mapper.outbound;

import java.util.Set;

// generic mapper contract: domain D → resource R. all outbound mappers extend this
// so controllers can consistently call toResource / toResources on any entity type.
public interface BaseOutboundMapper<D,R> {
    R toResource(D domain);

    Set<R> toResources(Set<D> domains);
}
