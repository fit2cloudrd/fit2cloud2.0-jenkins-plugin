package com.fit2cloud.codedeploy2.oss;

import com.alibaba.fastjson.JSONObject;
import com.fit2cloud.codedeploy2.CodeDeployException;
import com.fit2cloud.codedeploy2.client.model.ApplicationRepository;
import hudson.FilePath;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HarborClient {
    private static final Pattern PATTERN = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
    private static SSLContext sslContext;

    static {
        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, (x509Certificates, s) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            e.printStackTrace();
        }
    }

    public static String push(FilePath workspace, ApplicationRepository appRepo, String dockerHost,
                              String dockerFile, PrintStream logger, String imageName, String imageTag) throws CodeDeployException, IOException, URISyntaxException {
        logger.println("开始生成镜像...");
        String imagePath;
        CloseableHttpClient client;
        InputStream streamInput;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader = null;
        String tmpTarFilePath = null;
        try {
            client = HttpClientBuilder.create()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();
//        第一步，先将用户指定的目录达成tar包，放到临时目录下/tmp/
            tmpTarFilePath = "/tmp/" + imageName + "-" + imageTag + ".tar";
            File tmpTarFile = new File(tmpTarFilePath);
            tar(new File(workspace.getRemote()), new File(tmpTarFilePath));
//            第二步，调用docker接口，打成镜像
            String repository = appRepo.getRepository().replace("/api/v2.0/projects","");
            if (!repository.endsWith("/")) {
                repository += "/";
            }
            String repoPath = StringUtils.substringAfter(repository, "//");
            imagePath = repoPath + imageName + ":" + imageTag;
            URI imageBuildUri = new URIBuilder(dockerHost)
                    .setPath("build")
                    .addParameter("t", imagePath)
                    .addParameter("dockerfile", dockerFile)
                    .build();

            HttpPost httpPost = new HttpPost(imageBuildUri);
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-tar");
            InputStreamEntity imageFis = new InputStreamEntity(new FileInputStream(tmpTarFile));
            httpPost.setEntity(imageFis);

//            调用docker打包时，返回的是trunked类型的响应，所以这边要循环的读取
            CloseableHttpResponse buildRes = client.execute(httpPost);
            streamInput = buildRes.getEntity().getContent();
            inputStreamReader = new InputStreamReader(streamInput);
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String returnString = unicodeToString(line);
                JSONObject jsonObject = JSONObject.parseObject(returnString);
                String output = jsonObject.getString("stream");
                if (StringUtils.isNotBlank(output)) {
                    if (output.endsWith("\n")) {
                        logger.print(output);
                    } else {
                        logger.println(output);
                    }
                }
                JSONObject errorDetail = jsonObject.getJSONObject("errorDetail");
                if (null != errorDetail) {
                    logger.println(errorDetail.getString("message"));
                    throw new CodeDeployException("镜像生成失败！");
                }
            }
            bufferedReader.close();
            logger.println("镜像生成成功！");
            logger.println("推送镜像到仓库...");
//          第三步，上传镜像到仓库
            URI imagePushUri = new URIBuilder(dockerHost)
                    .setPath("images/" + repoPath + imageName + "/push")
                    .addParameter("tag", imageTag)
                    .build();
            String registryAuthHeader = new RegistryAuthHeader(appRepo.getAccessId(), appRepo.getAccessPassword()).genAuthHeader();
            HttpPost pushImageReq = new HttpPost(imagePushUri);
            pushImageReq.addHeader("X-Registry-Auth", registryAuthHeader);
            CloseableHttpResponse pushRes = client.execute(pushImageReq);
            streamInput = pushRes.getEntity().getContent();
            inputStreamReader = new InputStreamReader(streamInput);
            bufferedReader = new BufferedReader(inputStreamReader);
//            这边有个坑，如果你不等到他输出完的话是推送不到仓库里的，尽管给你返回了成功
            while ((line = bufferedReader.readLine()) != null) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                String returnString = unicodeToString(line);
                JSONObject jsonObject = JSONObject.parseObject(returnString);
                JSONObject errorDetail = jsonObject.getJSONObject("errorDetail");
                if (null != errorDetail) {
                    logger.println(errorDetail.getString("message"));
                    throw new CodeDeployException("镜像推送失败！");
                }
            }
            bufferedReader.close();
            if (HttpStatus.SC_OK != pushRes.getStatusLine().getStatusCode()) {
                HttpEntity entity = pushRes.getEntity();
                throw new CodeDeployException(EntityUtils.toString(entity));
            }
            logger.println("镜像推送成功！");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }finally {
            if (StringUtils.isNotBlank(tmpTarFilePath)) {
                File tmpFile = new File(tmpTarFilePath);
                if (tmpFile.exists()) {
                    boolean ignored = tmpFile.delete();
                }
            }
            if (null != bufferedReader) {
                bufferedReader.close();
            }
        }
        return imagePath;
    }

    private static String unicodeToString(String str) {

        Matcher matcher = PATTERN.matcher(str);
        char ch;
        while (matcher.find()) {
            String group = matcher.group(2);
            ch = (char) Integer.parseInt(group, 16);
            String group1 = matcher.group(1);
            str = str.replace(group1, ch + "");
        }
        return str;
    }

    public static void tar(File source, File dest) {
        FileOutputStream out = null;
        TarArchiveOutputStream tarOut = null;

        try {
            out = new FileOutputStream(dest);
            tarOut = new TarArchiveOutputStream(out);
            //解决文件名过长
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            for (File file : Objects.requireNonNull(source.listFiles())) {
                tarPack(file, tarOut, "");
            }
            tarOut.flush();
            tarOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (tarOut != null) {
                    tarOut.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void tarPack(File source, TarArchiveOutputStream tarOut, String parentPath) {
        if (source.isDirectory()) {
            tarDir(source, tarOut, parentPath);
        } else if (source.isFile()) {
            tarFile(source, tarOut, parentPath);
        }
    }

    private static void tarFile(File source, TarArchiveOutputStream tarOut, String parentPath) {
        TarArchiveEntry entry = new TarArchiveEntry(parentPath + source.getName());
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            entry.setSize(source.length());
            tarOut.putArchiveEntry(entry);
            fis = new FileInputStream(source);
            bis = new BufferedInputStream(fis);
            IOUtils.copy(bis, tarOut);
            bis.close();
            tarOut.closeArchiveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private static void tarDir(File sourceDir, TarArchiveOutputStream tarOut, String parentPath) {
        //归档空目录
        if (Objects.requireNonNull(sourceDir.listFiles()).length < 1) {
            TarArchiveEntry entry = new TarArchiveEntry(parentPath + sourceDir.getName() + "/");
            try {
                tarOut.putArchiveEntry(entry);
                tarOut.closeArchiveEntry();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //递归 归档
        for (File file : Objects.requireNonNull(sourceDir.listFiles())) {
            tarPack(file, tarOut, parentPath + sourceDir.getName() + "/");
        }
    }

    private static class RegistryAuthHeader {
        private String username;
        private String password;

        public RegistryAuthHeader(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String genAuthHeader() {
            return Base64.encodeBase64String(JSONObject.toJSONString(this).getBytes(StandardCharsets.UTF_8));
        }
    }
}
