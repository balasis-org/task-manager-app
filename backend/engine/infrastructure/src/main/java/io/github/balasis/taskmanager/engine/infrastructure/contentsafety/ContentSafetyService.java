package io.github.balasis.taskmanager.engine.infrastructure.contentsafety;

import java.io.InputStream;

public interface ContentSafetyService {
    ModerationResult analyze(InputStream input);
}
