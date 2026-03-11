package io.github.balasis.taskmanager.context.base.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BaseComponent {
    protected Logger logger = LoggerFactory.getLogger(getClass());

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
