package io.github.balasis.taskmanager.engine.core.dto;

public record EffectiveFileLimitsDto(int maxCreatorFiles, int maxAssigneeFiles, long maxFileSizeBytes) {}
