package io.github.balasis.taskmanager.context.web.resource;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

// base for all outbound DTOs — every response includes the entity id.
@Getter
@Setter
public class BaseOutboundResource implements Serializable {
    protected Long id;
}
