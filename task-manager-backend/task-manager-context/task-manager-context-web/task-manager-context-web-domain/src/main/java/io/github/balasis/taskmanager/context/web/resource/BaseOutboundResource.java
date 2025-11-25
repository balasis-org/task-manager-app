package io.github.balasis.taskmanager.context.web.resource;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BaseOutboundResource implements Serializable {
    protected Long id;
}
