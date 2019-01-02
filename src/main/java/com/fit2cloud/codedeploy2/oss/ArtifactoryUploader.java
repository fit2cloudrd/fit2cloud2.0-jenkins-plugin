package com.fit2cloud.codedeploy2.oss;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClient;
import org.jfrog.artifactory.client.model.File;

/**
 * Created by linjinbo on 2017/8/30.
 */
public class ArtifactoryUploader {
    public static String uploadArtifactory(java.io.File file,String server,String username,String password,String repo,String path){
        Artifactory artifactory = ArtifactoryClient.create(server, username, password);
        int index = repo.indexOf("artifactory");
        String source = repo.substring(index);
        StringBuilder filePath = new StringBuilder(path);
        filePath.append("/").append(file.getName());
        File result = artifactory.repository(source).upload(filePath.toString(),file).doUpload();
        artifactory.close();
        return result.getUri();
    }
}
