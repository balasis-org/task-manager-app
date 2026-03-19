package io.github.balasis.taskmanager.engine.infrastructure.contentsafety;

import java.io.InputStream;

// thin interface for Azure Content Safety image analysis.
// impl in prod/dev calls the real API; no-op in arena profiles.
public interface ContentSafetyService {
    ModerationResult analyze(InputStream input);
}
