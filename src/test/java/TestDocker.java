import com.alibaba.fastjson.JSONObject;
import com.fit2cloud.codedeploy2.CodeDeployException;
import com.fit2cloud.codedeploy2.client.model.ApplicationRepository;
import com.fit2cloud.codedeploy2.oss.HarborClient;
import com.fit2cloud.codedeploy2.oss.NexusUploader;
import hudson.FilePath;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

public class TestDocker {
    @Test
    public void testBuild() throws Exception {
        NexusUploader.upload(new File("D:\\下载\\test-harbor.zip"), "admin", "admin123", "http://10.1.11.208:8081/nexus/content/repositories/releases/", "com.wisonic", "test-harbor", "1", "zip", "1.0");
    }

    @Test
    public void testPush() throws CodeDeployException, IOException, URISyntaxException {
        FilePath filePath = new FilePath(new File("/tmp/argo-demo"));
        ApplicationRepository appRepo = new ApplicationRepository();
        appRepo.setAccessId("test");
        appRepo.setAccessPassword("Calong@2015");
        appRepo.setRepository("https://192.168.123.5/test");
        String imagePath = HarborClient.push(filePath, appRepo, "http://192.168.123.111:2375", "Dockerfile", System.out, "test", "test");
        System.out.println(imagePath);
    }

}
