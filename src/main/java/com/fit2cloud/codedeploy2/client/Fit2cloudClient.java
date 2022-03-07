package com.fit2cloud.codedeploy2.client;

import com.alibaba.fastjson.JSON;
import com.fit2cloud.codedeploy2.client.model.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.*;

public class Fit2cloudClient {


    private static final String ACCEPT = "application/json;charset=UTF-8";
    private static final Integer CONNECT_TIME_OUT = 10000;
    private static final Integer CONNECT_REQUEST_TIME_OUT = 10000;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private HttpClient httpClient;

    public Fit2cloudClient(String accessKey, String secretKey, String endpoint) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endpoint = endpoint;
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectTimeout(CONNECT_TIME_OUT)
                .setConnectionRequestTimeout(CONNECT_REQUEST_TIME_OUT).build();
        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig).build();
    }

    public void checkUser() {
        Result getUserResult = call(ApiUrlConstants.USER_INFO, RequestMethod.GET);
        if (!getUserResult.isSuccess()) {
            throw new Fit2CloudException(getUserResult.getMessage());
        }
    }


    public List<Workspace> getWorkspace() {
        Result getUserResult = call(ApiUrlConstants.USER_INFO, RequestMethod.GET);
        User user = JSON.parseObject(getUserResult.getData(), User.class);
        Result userPermissinResult = call(ApiUrlConstants.USER_PERMISSION_LIST + "/" + user.getId(), RequestMethod.GET);
        List<UserRoleDTO> userRoleDTOS = JSON.parseArray(userPermissinResult.getData(), UserRoleDTO.class);
        List<Workspace> workspaces = new ArrayList<Workspace>();
        for (UserRoleDTO userRoleDTO : userRoleDTOS) {
            if (userRoleDTO.getParentId() != null) {
                Workspace workspace = new Workspace();
                workspace.setId(userRoleDTO.getId());
                workspace.setName(userRoleDTO.getName());
                workspaces.add(workspace);
            }
        }
        return workspaces;
    }


    public List<ApplicationRepository> getApplicationRepositorys(String workspaceId) {
        long currentPage = 1L;
        long pageSize = 100L;
        long pageCount;
        List<ApplicationRepository> applicationRepositories = new ArrayList<ApplicationRepository>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("sourceId", workspaceId);
        do {
            Result result = call(ApiUrlConstants.REPOSITORY_LIST + "/" + currentPage + "/" + pageSize, RequestMethod.POST, new HashMap(), headers);
            Page page = JSON.parseObject(result.getData(), Page.class);
            String listJson = JSON.toJSONString(page.getListObject());
            List<ApplicationRepository> appReps = JSON.parseArray(listJson, ApplicationRepository.class);
            applicationRepositories.addAll(appReps);
            pageCount = page.getPageCount();
            currentPage++;
        } while (pageCount >= currentPage);
        return applicationRepositories;
    }

    public List<TagValue> getEnvList() {
        Result result = call(ApiUrlConstants.APPLICATION_ENV_LIST, RequestMethod.GET);
        return JSON.parseArray(result.getData(), TagValue.class);
    }


    public List<ApplicationDTO> getApplications(String workspaceId) {
        long currentPage = 1L;
        long pageSize = 100L;
        long pageCount;
        List<ApplicationDTO> applications = new ArrayList<ApplicationDTO>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("sourceId", workspaceId);


        do {
            Result result = call(ApiUrlConstants.APPLICATION_LIST + "/" + currentPage + "/" + pageSize, RequestMethod.POST, new HashMap<String, Object>(), headers);
            Page page = JSON.parseObject(result.getData(), Page.class);
            String listJson = JSON.toJSONString(page.getListObject());
            List<ApplicationDTO> apps = JSON.parseArray(listJson, ApplicationDTO.class);
            applications.addAll(apps);
            pageCount = page.getPageCount();
            currentPage++;
        } while (pageCount >= currentPage);
        return applications;
    }


    public List<ClusterDTO> getClusters(String workspaceId) {
        long currentPage = 1L;
        long pageSize = 100L;
        long pageCount;
        List<ClusterDTO> clusters = new ArrayList<ClusterDTO>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("sourceId", workspaceId);
        do {
            Result result = call(ApiUrlConstants.CLUSTER_LIST + "/" + currentPage + "/" + pageSize, RequestMethod.POST, new HashMap<String, Object>(), headers);
            Page page = JSON.parseObject(result.getData(), Page.class);
            String listJson = JSON.toJSONString(page.getListObject());
            List<ClusterDTO> clusts = JSON.parseArray(listJson, ClusterDTO.class);
            clusters.addAll(clusts);
            pageCount = page.getPageCount();
            currentPage++;
        } while (pageCount >= currentPage);
        return clusters;
    }


    public List<ApplicationSetting> getApplicationSettings(String applicationId) {
        List<ApplicationSetting> applicationSettings = new ArrayList<>();
        Result result = call(ApiUrlConstants.APPLICATION_SETTING_LIST + "?appId=" + applicationId, RequestMethod.GET);
        if (result.isSuccess()) {
            applicationSettings = JSON.parseArray(result.getData(), ApplicationSetting.class);
        }
        return applicationSettings;
    }


    public List<ClusterRole> getClusterRoles(String workspaceId, String clusterId) {
        long currentPage = 1L;
        long pageSize = 100L;
        long pageCount;
        List<ClusterRole> clusterRoles = new ArrayList<ClusterRole>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("sourceId", workspaceId);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("clusterId", clusterId);
        do {
            Result result = call(ApiUrlConstants.CLUSTER_ROLE_LIST + "/" + currentPage + "/" + pageSize, RequestMethod.POST, params, headers);
            Page page = JSON.parseObject(result.getData(), Page.class);
            String listJson = JSON.toJSONString(page.getListObject());
            List<ClusterRole> clusrs = JSON.parseArray(listJson, ClusterRole.class);
            clusterRoles.addAll(clusrs);
            pageCount = page.getPageCount();
            currentPage++;
        } while (pageCount >= currentPage);
        return clusterRoles;
    }

    public List<CloudServer> getCloudServers(String workspaceId, String clusterRoleId, String clusterId) {
        long currentPage = 1L;
        long pageSize = 100L;
        long pageCount;
        List<CloudServer> cloudServers = new ArrayList<CloudServer>();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("sourceId", workspaceId);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("clusterRoleId", clusterRoleId);
        params.put("clusterId", clusterId);

        do {
            Result result = call(ApiUrlConstants.SERVER_LIST + "/" + currentPage + "/" + pageSize, RequestMethod.POST, params, headers);
            Page page = JSON.parseObject(result.getData(), Page.class);
            String listJson = JSON.toJSONString(page.getListObject());
            List<CloudServer> clds = JSON.parseArray(listJson, CloudServer.class);
            cloudServers.addAll(clds);
            pageCount = page.getPageCount();
            currentPage++;
        } while (pageCount >= currentPage);
        return cloudServers;
    }

    public ApplicationDeployment createApplicationDeployment(ApplicationDeployment applicationDeployment, String workspaceId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("sourceId", workspaceId);
        Result result = call(ApiUrlConstants.APPLICATION_DEPLOY_SAVE, RequestMethod.POST, applicationDeployment, headers);
        return JSON.parseObject(result.getData(), ApplicationDeployment.class);
    }

    public ApplicationDeployment getApplicationDeployment(String applicationDeploymentId) {
        Result result = call(ApiUrlConstants.APPLICATION_SETTING_GET + "?applicationDeploymentId=" + applicationDeploymentId, RequestMethod.GET);
        return JSON.parseObject(result.getData(), ApplicationDeployment.class);
    }


    public ApplicationVersion createApplicationVersion(ApplicationVersionDTO applicationVersion, String workspaceId) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("sourceId", workspaceId);
        Result result = call(ApiUrlConstants.APPLICATION_VERSION_SAVE, RequestMethod.POST, applicationVersion, headers);
        return JSON.parseObject(result.getData(), ApplicationVersion.class);
    }


    private Result call(String url, RequestMethod requestMethod) {
        return call(url, requestMethod, null, null);
    }

    private Result call(String url, RequestMethod requestMethod, Object params, Map<String, String> headers) {
        url = this.endpoint + "/" + url;
        String responseJson = null;
        try {
            if (requestMethod == RequestMethod.GET) {
                HttpGet httpGet = new HttpGet(url);
                auth(httpGet);
                HttpResponse response = httpClient.execute(httpGet);

                HttpEntity httpEntity = response.getEntity();

                responseJson = EntityUtils.toString(httpEntity);
            } else {
                HttpPost httpPost = new HttpPost(url);
                if (headers != null && headers.size() > 0) {
                    for (String key : headers.keySet()) {
                        httpPost.addHeader(key, headers.get(key));
                    }
                }
                if (params != null) {
                    StringEntity stringEntity = new StringEntity(JSON.toJSONString(params), "UTF-8");
                    httpPost.setEntity(stringEntity);
                }
                auth(httpPost);
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity httpEntity = response.getEntity();
                responseJson = EntityUtils.toString(httpEntity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Result result = JSON.parseObject(responseJson, Result.class);
        if (!result.isSuccess()) {
            throw new Fit2CloudException(result.getMessage());
        }
        return JSON.parseObject(responseJson, Result.class);
    }

    private void auth(HttpRequestBase httpRequestBase) {
        httpRequestBase.addHeader("Accept", ACCEPT);
        httpRequestBase.addHeader("accessKey", accessKey);
        String signature;
        try {
            signature = aesEncrypt(accessKey + "|" + UUID.randomUUID().toString() + "|" + System.currentTimeMillis(), secretKey, accessKey);
        } catch (Exception e) {
            throw new Fit2CloudException("签名失败: " + e.getMessage());
        }
        httpRequestBase.addHeader("signature", signature);
    }


    private static String aesEncrypt(String src, String secretKey, String iv) throws Exception {
        byte[] raw = secretKey.getBytes("UTF-8");
        SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv1);
        byte[] encrypted = cipher.doFinal(src.getBytes("UTF-8"));
        return Base64.encodeBase64String(encrypted);

    }
}

class ApiUrlConstants {
    public static final String USER_INFO = "dashboard/user/info";
    public static final String APPLICATION_REPOSITORY_LIST = "devops/application/repository/list";
    public static final String APPLICATION_SETTING_LIST = "devops/application/setting/list";
    public static final String APPLICATION_SETTING_GET = "devops/application/deploy/get";
    public static final String USER_PERMISSION_LIST = "dashboard/user/switch/source";
    public static final String REPOSITORY_LIST = "devops/repository/list";
    public static final String APPLICATION_LIST = "devops/application/list";
    public static final String CLUSTER_LIST = "devops/cluster/list";
    public static final String CLUSTER_ROLE_LIST = "devops/clusterRole/list";
    public static final String SERVER_LIST = "devops/server/list";
    public static final String APPLICATION_VERSION_SAVE = "devops/application/version/save";
    public static final String APPLICATION_DEPLOY_SAVE = "devops/application/deploy/save";
    public static final String APPLICATION_ENV_LIST = "devops/application/setting/env/list";
}

enum RequestMethod {
    GET, POST
}
