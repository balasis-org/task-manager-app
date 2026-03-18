package io.github.balasis.taskmanager.engine.core.dto;

// resolved file limits after applying the group owner's subscription plan.
// the controller sends this to the frontend so it can show max-file-count
// indicators and validate uploads client-side before hitting the server.
public record EffectiveFileLimitsDto(int maxCreatorFiles, int maxAssigneeFiles, long maxFileSizeBytes) {}
