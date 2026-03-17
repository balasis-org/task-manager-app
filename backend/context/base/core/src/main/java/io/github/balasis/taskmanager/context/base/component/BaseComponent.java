package io.github.balasis.taskmanager.context.base.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// shared base for services and components. provides a logger and a streaming
// file transfer method that enforces a deadline so one slow download cant
// hold a thread forever. the timeout is computed by PlanLimits based on
// the file size and the downloader's subscription tier.
public abstract class BaseComponent {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    // streams bytes from in to out with a hard time limit.
    // if the transfer exceeds timeoutMs we abort rather than letting the
    // thread sit blocked indefinitely. this is our defense against slow
    // clients or broken connections clogging up the thread pool.
    protected static void transferWithTimeout(InputStream in, OutputStream out, long timeoutMs) throws IOException {
        byte[] buf = new byte[8192];
        long deadline = System.currentTimeMillis() + timeoutMs;
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
            if (System.currentTimeMillis() > deadline) {
                throw new IOException("Download aborted, transfer exceeded " + (timeoutMs / 1000) + "s timeout");
            }
        }
    }
}
