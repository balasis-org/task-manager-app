package io.github.balasis.taskmanager.engine.core.transfer;

public record TaskFileDownload(byte[] content, String filename) {}