package io.github.balasis.taskmanager.engine.core.transfer;

import java.io.InputStream;

// internal transfer object for streaming file downloads. the InputStream
// comes from blob storage and gets piped straight to the HTTP response —
// we never buffer the whole file in memory.
public record TaskFileDownload(InputStream content, String filename, long size) {}
