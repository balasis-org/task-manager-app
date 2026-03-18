package io.github.balasis.taskmanager.context.web.resource;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

// marker base for all inbound DTOs — SanitizingRequestBodyAdvice
// auto-sanitizes String fields on any class extending this.
@Getter
@Setter
public class BaseInboundResource implements Serializable {
}
