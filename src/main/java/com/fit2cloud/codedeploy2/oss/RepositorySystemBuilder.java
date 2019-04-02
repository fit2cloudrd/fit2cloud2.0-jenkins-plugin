package com.fit2cloud.codedeploy2.oss;


import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.*;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;


final class RepositorySystemBuilder {

    public RepositorySystem build() {
        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(
            RepositoryConnectorFactory.class,
            FileRepositoryConnectorFactory.class
        );
        locator.addService(
            RepositoryConnectorFactory.class,
            AsyncRepositoryConnectorFactory.class
        );
        locator.addService(
            WagonProvider.class,
                PlexusWagonProvider.class
        );
        locator.addService(
            WagonConfigurator.class,
            PlexusWagonConfigurator.class
        );
        locator.addService(
            RepositoryConnectorFactory.class,
            WagonRepositoryConnectorFactory.class
        );
        locator.addService(
            RepositorySystem.class,
            DefaultRepositorySystem.class
        );
        locator.addService(
            VersionResolver.class,
            DefaultVersionResolver.class
        );
        locator.addService(
            VersionRangeResolver.class,
            DefaultVersionRangeResolver.class
        );
        locator.addService(
            ArtifactDescriptorReader.class,
            DefaultArtifactDescriptorReader.class
        );
        final RepositorySystem system =
            locator.getService(RepositorySystem.class);
        if (system == null) {
            throw new IllegalStateException("failed to get service");
        }
        return system;
    }

}