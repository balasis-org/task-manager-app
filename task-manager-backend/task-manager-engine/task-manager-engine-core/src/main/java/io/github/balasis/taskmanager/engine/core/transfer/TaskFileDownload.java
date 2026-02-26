package io.github.balasis.taskmanager.engine.core.transfer;

import java.io.InputStream;

public record TaskFileDownload(InputStream content, String filename, long size) {}