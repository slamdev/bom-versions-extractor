package com.github.slamdev.bomversionsextractor;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Extractor {

    private final RepositorySystem repositorySystem;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repos;
    private final List<DefaultArtifact> boms;

    public Extractor(List<String> repos, List<String> boms, Path cacheDir) {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(final Class<?> type, final Class<?> impl, final Throwable exception) {
                throw new IllegalStateException(exception);
            }
        });
        repositorySystem = locator.getService(RepositorySystem.class);

        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        LocalRepository localRepo = new LocalRepository(cacheDir.toFile());
        session = MavenRepositorySystemUtils.newSession();
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(false, false));
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));

        this.repos = repos.stream()
                .map(url -> new RemoteRepository.Builder(url, "default", url))
                .map(RemoteRepository.Builder::build)
                .collect(Collectors.toList());

        this.boms = boms.stream()
                .map(DefaultArtifact::new)
                .collect(Collectors.toList());
    }

    public List<ArtifactInfo> run() {
        return boms.stream()
                .map(a -> new ArtifactDescriptorRequest(a, repos, null))
                .map(r -> readArtifactDescriptor(repositorySystem, session, r))
                .map(ArtifactDescriptorResult::getManagedDependencies)
                .flatMap(Collection::stream)
                .map(Dependency::getArtifact)
                .map(a -> new ArtifactInfo(a.getGroupId(), a.getArtifactId(), a.getVersion()))
                .distinct()
                .sorted(Comparator
                        .comparing(ArtifactInfo::getGroup)
                        .thenComparing(ArtifactInfo::getVersion))
                .collect(Collectors.toList());
    }

    private ArtifactDescriptorResult readArtifactDescriptor(RepositorySystem repositorySystem, RepositorySystemSession session, ArtifactDescriptorRequest request) {
        try {
            return repositorySystem.readArtifactDescriptor(session, request);
        } catch (ArtifactDescriptorException e) {
            throw new IllegalStateException(e);
        }
    }
}
