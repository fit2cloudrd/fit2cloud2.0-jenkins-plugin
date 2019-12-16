package com.fit2cloud.codedeploy2.oss;

import com.fit2cloud.codedeploy2.Utils;
import com.fit2cloud.codedeploy2.client.model.ApplicationRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author shaochuan.wu
 */
public class Nexus3Uploader {
    private static final String COMPONENT_URL = "service/rest/v1/components?repository=";

    public static String upload(File file, ApplicationRepository appRepo, String groupId,
                                String artifactId, String version, String buildNumber, String extension) throws IOException {
        String repoAddr = appRepo.getRepository();
        String server = StringUtils.substringBefore(repoAddr, "repository");
        String repoName = StringUtils.substringAfterLast(repoAddr, "/");
        String url = server + COMPONENT_URL + repoName;
        String auth = Utils.getBasicAuth(appRepo.getAccessId(), appRepo.getAccessPassword());
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", "Basic " + auth);
        FileBody fileBody = new FileBody(file);
        HttpEntity entity = MultipartEntityBuilder.create().
                setCharset(StandardCharsets.UTF_8)
                .addTextBody("maven2.groupId", groupId)
                .addTextBody("maven2.artifactId", artifactId)
                .addTextBody("maven2.version", version)
                .addTextBody("maven2.asset1.classifier",buildNumber)
                .addBinaryBody("maven2.asset1", fileBody.getInputStream())
                .addTextBody("maven2.asset1.extension", extension).build();
        post.setEntity(entity);
        client.execute(post);
        if (!repoAddr.endsWith("/")) {
            repoAddr += "/";
        }
        return repoAddr + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "-" + buildNumber + ".zip";
    }
}
