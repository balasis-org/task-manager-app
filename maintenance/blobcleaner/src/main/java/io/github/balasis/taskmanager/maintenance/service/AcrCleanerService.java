package io.github.balasis.taskmanager.maintenance.service;

import com.azure.containers.containerregistry.ContainerRegistryClient;
import com.azure.containers.containerregistry.models.ArtifactManifestProperties;
import io.github.balasis.taskmanager.maintenance.base.BaseComponent;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

// keeps the last 2 container images per ACR repo and deletes older ones.
// only runs in prod — dev builds don't push to ACR.
@Service
@Profile("prod-azuresql")
@AllArgsConstructor
public class AcrCleanerService extends BaseComponent {

    private static final int KEEP_LAST_N = 2;
    private static final List<String> REPOSITORIES = List.of(
            "taskmanager-backend",
            "taskmanager-maintenance"
    );

    private final ContainerRegistryClient registryClient;

    public void cleanOldImages() {
        for (String repo : REPOSITORIES) {
            cleanRepository(repo);
        }
    }

    private void cleanRepository(String repositoryName) {
        logger.info("ACR cleanup: scanning repository '{}'", repositoryName);

        List<ArtifactManifestProperties> manifests;
        try {
            manifests = registryClient
                    .getRepository(repositoryName)
                    .listManifestProperties()
                    .stream()
                    .sorted(Comparator.comparing(
                            ArtifactManifestProperties::getCreatedOn,
                            Comparator.reverseOrder()))
                    .toList();
        } catch (Exception e) {
            logger.error("ACR cleanup: failed to list manifests for '{}': {}",
                    repositoryName, e.getMessage());
            return;
        }

        if (manifests.size() <= KEEP_LAST_N) {
            logger.info("ACR cleanup: '{}' has {} image(s), nothing to delete (keeping last {}).",
                    repositoryName, manifests.size(), KEEP_LAST_N);
            return;
        }

        List<ArtifactManifestProperties> toDelete = manifests.subList(KEEP_LAST_N, manifests.size());

        int deleted = 0;
        int failed = 0;
        for (ArtifactManifestProperties manifest : toDelete) {
            String digest = manifest.getDigest();
            List<String> tags = manifest.getTags();
            try {
                registryClient
                        .getRepository(repositoryName)
                        .getArtifact(digest)
                        .delete();
                deleted++;
                logger.info("ACR cleanup: deleted '{}' digest={} tags={}",
                        repositoryName, digest, tags);
            } catch (Exception e) {
                failed++;
                logger.error("ACR cleanup: failed to delete '{}' digest={}: {}",
                        repositoryName, digest, e.getMessage());
            }
        }

        logger.info("ACR cleanup: '{}' finished. kept={}, deleted={}, failed={}",
                repositoryName, KEEP_LAST_N, deleted, failed);
    }
}
