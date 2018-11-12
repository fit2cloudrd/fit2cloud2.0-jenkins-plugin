package com.fit2cloud.codedeploy.oss;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import java.io.File;

public class NexusUploader {
	
	public static String upload(File uploadFile, String userName, String password, String targetUrl, String groupId, String artifactId, String classifier, String extension, String version) throws Exception {
	    RepositorySystem system = newRepositorySystem();
	    RepositorySystemSession session = newSession(system);

	    Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
	    artifact = artifact.setFile(uploadFile);

	    // add authentication to connect to remove repository
	    Authentication authentication = new AuthenticationBuilder().addUsername(userName).addPassword(password).build();

	    // creates a remote repo at the given URL to deploy to
	    RemoteRepository distRepo = new RemoteRepository.Builder("id", "default", targetUrl).setAuthentication(authentication).build();

	    DeployRequest deployRequest = new DeployRequest();
	    deployRequest.addArtifact(artifact);
	    deployRequest.setRepository(distRepo);

	    system.deploy(session, deployRequest);
	    if(!targetUrl.endsWith("/")) {
	    	targetUrl += "/";
	    }
	    
	    String url = targetUrl + groupId.replace(".", "/") + "/"+artifactId+"/"+version+"/"+artifactId+"-"+version+"-"+classifier+".zip";
	    return url;
	}

	private static RepositorySystem newRepositorySystem() {
	    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
	    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
	    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
	    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
	    return locator.getService(RepositorySystem.class);
	}

	private static RepositorySystemSession newSession(RepositorySystem system) {
	    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
	    LocalRepository localRepo = new LocalRepository("target/local-repo");
	    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
	    return session;
	}
}
