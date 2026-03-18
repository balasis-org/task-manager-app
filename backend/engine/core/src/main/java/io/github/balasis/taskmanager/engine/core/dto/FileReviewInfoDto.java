package io.github.balasis.taskmanager.engine.core.dto;

import io.github.balasis.taskmanager.context.base.enumeration.FileReviewDecision;

import java.time.Instant;

// embedded inside GroupFileDto — one per reviewer who has reviewed the file
public record FileReviewInfoDto(
        FileReviewDecision status,
        String note,
        String reviewerName,
        Long reviewerId,
        Instant createdAt
) {}
