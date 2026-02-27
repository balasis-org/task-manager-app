package io.github.balasis.taskmanager.context.base.component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BaseComponent {
    protected Logger logger = LoggerFactory.getLogger(getClass());

//    @PostConstruct
//    public void init() {
//        logger.trace("Loaded {}.", getClass());
//    }
//
//    @PreDestroy
//    public void destroy() {
//        logger.trace("{} is about to be destroyed.", getClass().getName());
//    }

    /**
     * Streams bytes from {@code in} to {@code out} in 8 KB chunks, aborting
     * if the total transfer exceeds {@code timeoutMs} milliseconds.
     *
     * <p>This protects backend resources (thread, DB connection, blob stream)
     * from being held indefinitely by a slow or stalled client.
     *
     * @throws IOException if the timeout is exceeded or an I/O error occurs
     */
    protected static void transferWithTimeout(InputStream in, OutputStream out, long timeoutMs) throws IOException {
        byte[] buf = new byte[8192];
        long deadline = System.currentTimeMillis() + timeoutMs;
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
            if (System.currentTimeMillis() > deadline) {
                throw new IOException("Download aborted — transfer exceeded " + (timeoutMs / 1000) + "s timeout");
            }
        }
    }
}
