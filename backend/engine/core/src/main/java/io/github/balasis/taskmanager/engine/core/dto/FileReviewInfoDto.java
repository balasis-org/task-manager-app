package io.github.balasis.taskmanager.engine.core.dto;

import io.github.balasis.taskmanager.context.base.enumeration.FileReviewDecision;

import java.time.Instant;

public record FileReviewInfoDto(
        FileReviewDecision status,
        String note,
        String reviewerName,
        Long reviewerId,
        Instant createdAt
) {}
