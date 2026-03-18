package io.github.balasis.taskmanager.engine.infrastructure.bootstrap;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Arrays;

// two atomic flags: imagesReady (DefaultImageBootstrap done) and dataReady (DataLoader done).
// StartupBlockingFilter returns 503 until both are true.
// if the DataLoader profile isnt active, dataReady starts as true.
@Component
public class StartupGate {
    private final AtomicBoolean imagesReady = new AtomicBoolean(false);
    private final AtomicBoolean dataReady;

    public StartupGate(Environment env) {
        boolean loaderActive = Arrays.asList(env.getActiveProfiles()).contains("DataLoader");

        this.dataReady = new AtomicBoolean(!loaderActive);
    }

    public void markImagesReady() { imagesReady.set(true); }
    public void markDataReady()  { dataReady.set(true); }
    public boolean isReady() { return imagesReady.get() && dataReady.get(); }
}
