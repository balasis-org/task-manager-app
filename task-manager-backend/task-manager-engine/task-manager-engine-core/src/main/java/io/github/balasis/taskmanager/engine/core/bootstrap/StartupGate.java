package io.github.balasis.taskmanager.engine.core.bootstrap;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Arrays;

@Component
public class StartupGate {
    private final AtomicBoolean imagesReady = new AtomicBoolean(false);
    private final AtomicBoolean dataReady;

    public StartupGate(Environment env) {
        boolean loaderActive = Arrays.asList(env.getActiveProfiles()).contains("DataLoader");
        // if DataLoader profile is active, dataReady starts false and will be set true when DataLoader finishes.
        // if not active, we don't wait for it.
        this.dataReady = new AtomicBoolean(!loaderActive);
    }

    public void markImagesReady() { imagesReady.set(true); }
    public void markDataReady()  { dataReady.set(true); }
    public boolean isReady() { return imagesReady.get() && dataReady.get(); }
}