package io.github.balasis.taskmanager.engine.infrastructure.textanalytics;

// input document for text analytics: id is the comment PK as string, text is the comment body
public record CommentDocument(String id, String text) {}
