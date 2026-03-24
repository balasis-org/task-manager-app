package io.github.balasis.taskmanager.engine.infrastructure.bootstrap;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Arrays;

// three atomic flags: imagesReady (DefaultImageBootstrap done), dataReady (DataLoader done),
// queryCacheReady (QueryCacheWarmUp done). StartupBlockingFilter returns 503 until all are true.
// if the DataLoader profile isnt active, dataReady starts as true.
@Component
public class StartupGate {
    private final AtomicBoolean imagesReady = new AtomicBoolean(false);
    private final AtomicBoolean dataReady;
    private final AtomicBoolean queryCacheReady = new AtomicBoolean(false);

    public StartupGate(Environment env) {
        boolean loaderActive = Arrays.asList(env.getActiveProfiles()).contains("DataLoader");

        this.dataReady = new AtomicBoolean(!loaderActive);
    }

    public void markImagesReady() { imagesReady.set(true); }
    public void markDataReady()  { dataReady.set(true); }
    public void markQueryCacheReady() { queryCacheReady.set(true); }
    public boolean isReady() { return imagesReady.get() && dataReady.get() && queryCacheReady.get(); }
}
