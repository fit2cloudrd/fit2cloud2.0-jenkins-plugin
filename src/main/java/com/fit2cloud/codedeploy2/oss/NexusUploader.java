package com.fit2cloud.codedeploy2.oss;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;

public class NexusUploader {

	public static String upload(File uploadFile, String userName, String password, String targetUrl, String groupId, String artifactId, String classifier, String extension, String version) throws Exception {
	    RepositorySystem system = newRepositorySystem();
	    RepositorySystemSession session = newSession(system);

	    Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
	    artifact = artifact.setFile(uploadFile);

	    // add authentication to connect to remove repository
	    Authentication authentication = new Authentication(userName, password);

	    // creates a remote repo at the given URL to deploy to
		RemoteRepository distRepo = new RemoteRepository("id", "default", targetUrl).setAuthentication(authentication);

	    DeployRequest deployRequest = new DeployRequest();
	    deployRequest.addArtifact(artifact);
	    deployRequest.setRepository(distRepo);

	    system.deploy(session, deployRequest);

	    if(!targetUrl.endsWith("/")) {
	    	targetUrl += "/";
	    }

		String url = targetUrl + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "-" + classifier + ".zip";
	    return url;
	}

	private static RepositorySystem newRepositorySystem() {
		RepositorySystem system = new RepositorySystemBuilder().build();
		return system;
	}

	private static RepositorySystemSession newSession(RepositorySystem system) {
		MavenRepositorySystemSession session = new MavenRepositorySystemSession();
	    LocalRepository localRepo = new LocalRepository("target/local-repo");
	    session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));
	    return session;
	}

}
