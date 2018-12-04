package com.fit2cloud.codedeploy2;

import com.fit2cloud.codedeploy2.client.Fit2cloudClient;
import com.fit2cloud.codedeploy2.client.model.*;
import com.fit2cloud.codedeploy2.oss.AWSS3Client;
import com.fit2cloud.codedeploy2.oss.AliyunOSSClient;
import com.fit2cloud.codedeploy2.oss.NexusUploader;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.DirScanner;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class F2CCodeDeployPublisher extends Publisher {
    private static final String LOG_PREFIX = "[FIT2CLOUD 代码部署]";
    private final String f2cEndpoint;
    private final String f2cAccessKey;
    private final String f2cSecretKey;
    private final String workspaceId;
    private final String applicationId;
    private final String applicationSettingId;
    private final String applicationRepositoryId;
    private final String clusterId;
    private final String clusterRoleId;
    private final String cloudServerId;
    private final String deployPolicy;
    private final String applicationVersionName;
    private final boolean nexusChecked;
    private final boolean ossChecked;
    private final boolean s3Checked;
    private final boolean autoDeploy;
    private final String includes;
    private final String excludes;
    private final String appspecFilePath;
    private final String description;
    private final boolean waitForCompletion;
    private final Long pollingTimeoutSec;
    private final Long pollingFreqSec;


    private PrintStream logger;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public F2CCodeDeployPublisher(String f2cEndpoint,
                                  String f2cAccessKey,
                                  String f2cSecretKey,
                                  String applicationId,
                                  String applicationRepositoryId,
                                  String clusterId,
                                  String clusterRoleId,
                                  String workspaceId,
                                  String applicationSettingId,
                                  String cloudServerId,
                                  String deployPolicy,
                                  String applicationVersionName,
                                  boolean waitForCompletion,
                                  boolean nexusChecked,
                                  boolean ossChecked,
                                  boolean s3Checked,
                                  boolean autoDeploy,
                                  Long pollingTimeoutSec,
                                  Long pollingFreqSec,
                                  String includes,
                                  String excludes,
                                  String appspecFilePath,
                                  String description) {
        this.f2cEndpoint = f2cEndpoint;
        this.f2cAccessKey = f2cAccessKey;
        this.f2cSecretKey = f2cSecretKey;
        this.applicationId = applicationId;
        this.clusterId = clusterId;
        this.clusterRoleId = clusterRoleId;
        this.workspaceId = workspaceId;
        this.applicationSettingId = applicationSettingId;
        this.cloudServerId = cloudServerId;
        this.applicationRepositoryId = applicationRepositoryId;
        this.applicationVersionName = applicationVersionName;
        this.deployPolicy = deployPolicy;
        this.nexusChecked = nexusChecked;
        this.ossChecked = ossChecked;
        this.s3Checked = s3Checked;
        this.autoDeploy = autoDeploy;
        this.includes = includes;
        this.excludes = excludes;
        this.appspecFilePath = StringUtils.isBlank(appspecFilePath) ? "appspec.yml" : appspecFilePath;
        this.description = description;
        this.pollingFreqSec = pollingFreqSec;
        this.pollingTimeoutSec = pollingTimeoutSec;
        this.waitForCompletion = waitForCompletion;


    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.logger = listener.getLogger();

        int builtNumber = build.getNumber();
        String projectName = build.getProject().getName();

        final boolean buildFailed = build.getResult() == Result.FAILURE;
        if (buildFailed) {
            log("Skipping CodeDeploy publisher as build failed");
            return true;
        }
        final Fit2cloudClient fit2cloudClient = new Fit2cloudClient(this.f2cAccessKey, this.f2cSecretKey, this.f2cEndpoint);

        log("开始校验参数...");
        try {
            boolean findWorkspace = false;
            List<Workspace> workspaces = fit2cloudClient.getWorkspace();
            for (Workspace workspace : workspaces) {
                if (workspace.getId().equals(this.workspaceId)) {
                    findWorkspace = true;
                }
            }
            if (!findWorkspace) {
                throw new CodeDeployException("工作空间不存在！");
            }

            boolean findApplication = false;
            List<ApplicationDTO> applications = fit2cloudClient.getApplications(this.workspaceId);
            for (ApplicationDTO applicationDTO : applications) {
                if (applicationDTO.getId().equals(this.applicationId)) {
                    findApplication = true;
                }
            }
            if (!findApplication) {
                throw new CodeDeployException("应用不存在！");
            }
            boolean findApplicationSetting = false;
            List<ApplicationSetting> applicationSettings = fit2cloudClient.getApplicationSettings(this.applicationId);
            for (ApplicationSetting applicationSetting : applicationSettings) {
                if (applicationSetting.getId().equals(this.applicationSettingId)) {
                    findApplicationSetting = true;
                }
            }
            if (!findApplicationSetting) {
                throw new CodeDeployException("应用设置不存在！");
            }

            if (autoDeploy) {
                boolean findCluster = false;
                List<ClusterDTO> clusters = fit2cloudClient.getClusters(this.workspaceId);
                for (ClusterDTO clusterDTO : clusters) {
                    if (clusterDTO.getId().equals(this.clusterId)) {
                        findCluster = true;
                    }
                }
                if (!findCluster) {
                    throw new CodeDeployException("集群不存在! ");
                }


                List<ClusterRole> clusterRoles = fit2cloudClient.getClusterRoles(this.workspaceId, this.clusterId);

                if (clusterRoles.size() == 0) {
                    throw new CodeDeployException("此集群下主机组为空！");
                }

                if (!clusterRoleId.equalsIgnoreCase("ALL")) {
                    boolean findClusterRole = false;
                    for (ClusterRole clusterRole : clusterRoles) {
                        if (clusterRole.getId().equals(this.clusterRoleId)) {
                            findClusterRole = true;
                        }
                    }
                    if (!findClusterRole) {
                        throw new CodeDeployException("主机组不存在! ");
                    }
                }

                List<CloudServer> cloudServers = fit2cloudClient.getCloudServers(this.workspaceId, this.clusterRoleId, this.clusterId);
                if (cloudServers.size() == 0) {
                    throw new CodeDeployException("此主机组下主机为空！");
                }
                if (!cloudServerId.equalsIgnoreCase("ALL")) {
                    boolean findCLoudServer = false;
                    for (CloudServer cloudServer : cloudServers) {
                        if (cloudServer.getId().equals(this.cloudServerId)) {
                            findCLoudServer = true;
                        }
                    }
                    if (!findCLoudServer) {
                        throw new CodeDeployException("主机组不存在! ");
                    }
                }

            }
        } catch (Exception e) {
            log(e.getMessage());
            return false;
        }


        FilePath workspace = build.getWorkspace();

        File zipFile = null;
        String zipFileName = null;
        String newAddress = null;
        try {
            zipFileName = projectName + "-" + builtNumber + ".zip";
            String includesNew = Utils.replaceTokens(build, listener, this.includes);
            String excludesNew = Utils.replaceTokens(build, listener, this.excludes);
            String appspecFilePathNew = Utils.replaceTokens(build, listener, this.appspecFilePath);
            zipFile = zipFile(zipFileName, workspace, includesNew, excludesNew, appspecFilePathNew);
        } catch (Exception e) {
            log("生成ZIP包失败: " + e.getMessage());
            return false;
        }
        ApplicationSetting applicationSetting = null;
        String newAppVersion = null;
        try {
            newAppVersion = Utils.replaceTokens(build, listener, this.applicationVersionName);
            applicationSetting = findApplicationSetting(this.applicationId);
            String path = applicationSetting.getPath();
            String repType = applicationSetting.getRepositoryType();
            log("仓库类型为: " + repType);
            log("上传路径为: " + path);

            switch (applicationSetting.getRepositoryType()) {
                case ArtifactType.OSS:
                    log("开始上传zip文件到OSS服务器");
                    //getBucketLocation
                    String expFP = Utils.replaceTokens(build, listener, zipFile.toString());

                    if (expFP != null) {
                        expFP = expFP.trim();
                    }

                    // Resolve virtual path
                    String expVP = path;
                    if (Utils.isNullOrEmpty(expVP)) {
                        expVP = null;
                    }
                    if (!Utils.isNullOrEmpty(expVP) && !expVP.endsWith(Utils.FWD_SLASH)) {
                        expVP = expVP.trim() + Utils.FWD_SLASH;
                    }

                    String bucketName = applicationSetting.getRepositoryAddr().replace("bucket:", "");
                    try {
                        int filesUploaded = AliyunOSSClient.upload(build, listener,
                                applicationSetting.getAccessId(),
                                applicationSetting.getAccessPassword(),
                                ".aliyuncs.com",
                                bucketName, expFP, expVP);
                        if (filesUploaded > 0) {
                            log("上传Artifacts到阿里云OSS成功!");
                        }
                    } catch (Exception e) {
                        log("上传Artifact到阿里云OSS失败，错误消息如下:");
                        log(e.getMessage());
                        e.printStackTrace(this.logger);
                        return false;
                    }
                    log("上传zip文件到oss服务器成功!");
                    if (expVP == null) {
                        newAddress = zipFile.getName();
                    } else {
                        if (expVP.endsWith("/")) {
                            newAddress = path + zipFileName;
                        } else {
                            newAddress = path + "/" + zipFile.getName();
                        }
                    }
                    log("文件路径" + newAddress);
                    break;
                case ArtifactType.S3:
                    log("开始上传zip文件到OSS服务器");
                    //getBucketLocation
                    String expF = Utils.replaceTokens(build, listener, zipFile.toString());

                    if (expF != null) {
                        expF = expF.trim();
                    }

                    // Resolve virtual path
                    String expV = path;
                    if (Utils.isNullOrEmpty(expV)) {
                        expV = null;
                    }
                    if (!Utils.isNullOrEmpty(expV) && !expV.endsWith(Utils.FWD_SLASH)) {
                        expV = expV.trim() + Utils.FWD_SLASH;
                    }

                    String bkname = applicationSetting.getRepositoryAddr().replace("bucket:", "");
                    try {
                        int filesUploaded = AWSS3Client.upload(build, listener,
                                applicationSetting.getAccessId(),
                                applicationSetting.getAccessPassword(),
                                null,
                                bkname, expF, expV);
                        if (filesUploaded > 0) {
                            log("上传Artifacts到亚马逊S3成功!");
                        }
                    } catch (Exception e) {
                        log("上传Artifact到亚马逊S3失败，错误消息如下:");
                        log(e.getMessage());
                        e.printStackTrace(this.logger);
                        return false;
                    }
                    log("上传zip文件到亚马逊S3成功!");
                    if (expV == null) {
                        newAddress = zipFile.getName();
                    } else {
                        if (expV.endsWith("/")) {
                            newAddress = path + zipFileName;
                        } else {
                            newAddress = path + "/" + zipFile.getName();
                        }
                    }
                    log("文件路径" + newAddress);
                    break;
                case ArtifactType.NEXUS:
                    String repoAddr = applicationSetting.getRepositoryAddr();
                    String key = applicationSetting.getPath();

                    String newKey = key.replace(repoAddr + "/", "");
                    String[] splitArr = newKey.split("/");
                    String artifactId = splitArr[splitArr.length - 1];
                    StringBuilder sb = new StringBuilder();
                    int i = 0;
                    for (String str : splitArr) {
                        if (i < splitArr.length - 1) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(str);
                            i++;
                        }
                    }
                    String nexusGroupIdNew = sb.toString();
                    String nexusArtifactIdNew = artifactId;
                    String nexusArtifactVersionNew = newAppVersion;

                    log("开始上传zip文件到nexus服务器");
                    try {
                        newAddress = NexusUploader.upload(zipFile, applicationSetting.getAccessId(), applicationSetting.getAccessPassword(), applicationSetting.getRepositoryAddr(), nexusGroupIdNew, nexusArtifactIdNew, String.valueOf(build.getNumber()), "zip", nexusArtifactVersionNew);
                    } catch (Exception e) {
                        log("上传文件到 Nexus 服务器失败！错误消息如下:");
                        log(e.getMessage());
                        e.printStackTrace(this.logger);
                        return false;
                    }
                    log("上传zip文件到nexus服务器成功!");
                    break;
                default:
                    log("仓库类型不支持!");
                    return false;
            }
        } catch (Exception e) {
            log("上传zip失败:" + e.getMessage());
            return false;
        }
        ApplicationVersion appVersion = null;
        try {
            log("注册应用版本中...");
            ApplicationVersionDTO applicationVersion = new ApplicationVersionDTO();
            applicationVersion.setApplicationId(this.applicationId);
            applicationVersion.setName(newAppVersion);
            applicationVersion.setEnvironmentValueId(applicationSetting.getEnvironmentValueId());
            applicationVersion.setApplicationRepositoryId(applicationSetting.getRepositoryId());
            applicationVersion.setLocation(newAddress);
            appVersion = fit2cloudClient.createApplicationVersion(applicationVersion, this.workspaceId);
        } catch (Exception e) {
            log("版本注册失败！ 原因：" + e.getMessage());
            return false;
        }
        log("注册版本成功！");

        ApplicationDeployment applicationDeploy = null;
        try {
            log("创建代码部署任务...");
            if (this.autoDeploy) {
                ApplicationDeployment applicationDeployment = new ApplicationDeployment();
                applicationDeployment.setClusterId(this.clusterId);
                applicationDeployment.setClusterRoleId(this.clusterRoleId);
                applicationDeployment.setCloudServerId(this.cloudServerId);
                applicationDeployment.setApplicationVersionId(appVersion.getId());
                applicationDeployment.setPolicy(this.deployPolicy);
                applicationDeployment.setDescription("Jenkins 触发");
                applicationDeploy = fit2cloudClient.createApplicationDeployment(applicationDeployment, this.workspaceId);
            }
        } catch (Exception e) {
            log("创建代码部署任务失败: " + e.getMessage());
            return false;
        }

        try {
            int i = 0;
            if (this.autoDeploy && this.waitForCompletion) {
                log("执行代码部署...");
                while (true) {
                    Thread.sleep(1000 * pollingFreqSec);
                    ApplicationDeployment applicationDeployment = fit2cloudClient.getApplicationDeployment(applicationDeploy.getId());
                    if (applicationDeployment.getStatus().equalsIgnoreCase("success")
                            || applicationDeployment.getStatus().equalsIgnoreCase("fail")) {
                        log("部署完成！");
                        if (applicationDeployment.getStatus().equalsIgnoreCase("success")) {
                            log("部署结果: 成功");
                        } else {
                            throw new Exception("部署任务执行失败，具体结果请登录FIT2CLOUD控制台查看！");
                        }
                        break;
                    } else {
                        log("部署任务运行中...");
                    }
                }
                if (pollingFreqSec * ++i > pollingTimeoutSec) {
                    throw new Exception("部署超时,请查看FIT2CLOUD控制台！");
                }
            }
        } catch (Exception e) {
            log("执行代码部署失败: " + e.getMessage());
            return false;
        }


        return true;
    }

    private File zipFile(String zipFileName, FilePath sourceDirectory, String includesNew, String excludesNew, String appspecFilePathNew) throws IOException, InterruptedException, IllegalArgumentException {
        FilePath appspecFp = new FilePath(sourceDirectory, appspecFilePathNew);

        log("指定 appspecPath ::::: " + appspecFp.toURI().getPath());
        if (appspecFp.exists()) {
            if (!"appspec.yml".equals(appspecFilePathNew)) {
                FilePath appspecDestFP = new FilePath(sourceDirectory, "appspec.yml");
                log("目标 appspecPath  ::::: " + appspecDestFP.toURI().getPath());
                appspecFp.copyTo(appspecDestFP);
            }
            log("成功添加appspec文件");
        } else {
            throw new IllegalArgumentException("没有找到对应的appspec.yml文件！");
        }
        File zipFile = new File("/tmp/" + zipFileName);
        final boolean fileCreated = zipFile.createNewFile();
        if (!fileCreated) {
            log("Zip文件已存在，开始覆盖 : " + zipFile.getPath());
        }

        log("生成Zip文件 : " + zipFile.getAbsolutePath());

        FileOutputStream outputStream = new FileOutputStream(zipFile);
        try {
            String allIncludes = includesNew + ",appspec.yml";
            sourceDirectory.zip(
                    outputStream,
                    new DirScanner.Glob(allIncludes, excludesNew)
            );
        } finally {
            outputStream.close();
        }
        return zipFile;
    }


    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public FormValidation doCheckAccount(
                @QueryParameter String f2cAccessKey,
                @QueryParameter String f2cSecretKey,
                @QueryParameter String f2cEndpoint) {
            if (StringUtils.isEmpty(f2cAccessKey)) {
                return FormValidation.error("FIT2CLOUD ConsumerKey不能为空！");
            }
            if (StringUtils.isEmpty(f2cSecretKey)) {
                return FormValidation.error("FIT2CLOUD SecretKey不能为空！");
            }
            if (StringUtils.isEmpty(f2cEndpoint)) {
                return FormValidation.error("FIT2CLOUD EndPoint不能为空！");
            }
            try {
                Fit2cloudClient fit2cloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                fit2cloudClient.checkUser();
            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("验证FIT2CLOUD帐号成功！");
        }


        public ListBoxModel doFillWorkspaceIdItems(@QueryParameter String f2cAccessKey,
                                                   @QueryParameter String f2cSecretKey,
                                                   @QueryParameter String f2cEndpoint) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择工作空间","");
            try {
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                List<Workspace> list = fit2CloudClient.getWorkspace();
                if (list != null && list.size() > 0) {
                    for (Workspace c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
//            		e.printStackTrace();
//                return FormValidation.error(e.getMessage());
            }
            return items;
        }

        public ListBoxModel doFillApplicationIdItems(@QueryParameter String f2cAccessKey,
                                                     @QueryParameter String f2cSecretKey,
                                                     @QueryParameter String f2cEndpoint,
                                                     @QueryParameter String workspaceId) {
            ListBoxModel items = new ListBoxModel();
            try {
                List<ApplicationDTO> list = new ArrayList<>();
                items.add("请选择应用","");
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                if (workspaceId != null && !workspaceId.equals("")) {
                    list = fit2CloudClient.getApplications(workspaceId);
                }
                if (list != null && list.size() > 0) {
                    for (Application c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
//            		e.printStackTrace();
//                return FormValidation.error(e.getMessage());
            }
            return items;
        }

        public ListBoxModel doFillApplicationSettingIdItems(@QueryParameter String f2cAccessKey,
                                                            @QueryParameter String f2cSecretKey,
                                                            @QueryParameter String f2cEndpoint,
                                                            @QueryParameter String applicationId) {
            ListBoxModel items = new ListBoxModel();
            try {
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                items.add("请选择环境","");
                List<ApplicationSetting> list = fit2CloudClient.getApplicationSettings(applicationId);
                if (list != null && list.size() > 0) {
                    for (ApplicationSetting c : list) {
                        if (c.getEnvironmentValueId().equalsIgnoreCase("all")) {
                            c.setEnvValue("全部环境");
                        }
                        items.add(c.getEnvValue() + "---" + c.getRepositoryType(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
//            		e.printStackTrace();
//                return FormValidation.error(e.getMessage());
            }
            return items;
        }


        public ListBoxModel doFillClusterIdItems(@QueryParameter String f2cAccessKey,
                                                 @QueryParameter String f2cSecretKey,
                                                 @QueryParameter String f2cEndpoint,
                                                 @QueryParameter String workspaceId,
                                                 @QueryParameter String applicationId,
                                                 @QueryParameter String applicationSettingId) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择集群","");

            try {
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                List<ClusterDTO> list = fit2CloudClient.getClusters(workspaceId);

                final Fit2cloudClient fit2cloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                ApplicationDTO applicationDTO = null;
                ApplicationSetting applicationSetting = null;
                List<ApplicationDTO> applications = fit2cloudClient.getApplications(workspaceId);
                for (ApplicationDTO app : applications) {
                    if (app.getId().equalsIgnoreCase(applicationId)) {
                        applicationDTO = app;
                    }
                }
                List<ApplicationSetting> applicationSettings = fit2cloudClient.getApplicationSettings(applicationId);
                for (ApplicationSetting appst : applicationSettings) {
                    if (appst.getId().equalsIgnoreCase(applicationSettingId)) {
                        applicationSetting = appst;
                    }
                }
                String envValueId = applicationSetting.getEnvironmentValueId();
                String businessValueId = applicationDTO.getBusinessValueId();

                if (list != null && list.size() > 0) {
                    for (ClusterDTO c : list) {
                        if ((businessValueId == null || businessValueId.equalsIgnoreCase(c.getSystemValueId()))
                                && (envValueId.equalsIgnoreCase("ALL") || envValueId.equalsIgnoreCase(c.getEnvValueId()))) {
                            items.add(c.getName(), String.valueOf(c.getId()));
                        }
                    }
                }
            } catch (Exception e) {
//            		e.printStackTrace();
//                return FormValidation.error(e.getMessage());
            }
            return items;
        }

        public ListBoxModel doFillClusterRoleIdItems(@QueryParameter String f2cAccessKey,
                                                     @QueryParameter String f2cSecretKey,
                                                     @QueryParameter String f2cEndpoint,
                                                     @QueryParameter String workspaceId,
                                                     @QueryParameter String clusterId) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择主机组","");

            try {
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                List<ClusterRole> list = fit2CloudClient.getClusterRoles(workspaceId, clusterId);
                if (list != null && list.size() > 0) {
                    items.add("全部主机组", "ALL");
                    for (ClusterRole c : list) {
                        items.add(c.getName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
//            		e.printStackTrace();
//                return FormValidation.error(e.getMessage());
            }
            return items;
        }

        public ListBoxModel doFillCloudServerIdItems(@QueryParameter String f2cAccessKey,
                                                     @QueryParameter String f2cSecretKey,
                                                     @QueryParameter String f2cEndpoint,
                                                     @QueryParameter String workspaceId,
                                                     @QueryParameter String clusterId,
                                                     @QueryParameter String clusterRoleId) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择主机","");
            try {
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                List<CloudServer> list = fit2CloudClient.getCloudServers(workspaceId, clusterRoleId, clusterId);
                if (list != null && list.size() > 0) {
                    items.add("全部主机", "ALL");
                    for (CloudServer c : list) {
                        items.add(c.getInstanceName(), String.valueOf(c.getId()));
                    }
                }
            } catch (Exception e) {
//            		e.printStackTrace();
//                return FormValidation.error(e.getMessage());
            }
            return items;
        }

        public ListBoxModel doFillDeployPolicyItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("全部同时部署", "all");
            items.add("半数分批部署", "harf");
            items.add("单台依次部署", "sigle");
            return items;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindParameters(this);
            save();
            return super.configure(req, formData);
        }


        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            super(F2CCodeDeployPublisher.class);
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "FIT2CLOUD 代码部署";
        }


    }

    private ApplicationSetting findApplicationSetting(String applicationId) {
        ApplicationSetting applicationSetting = null;
        final Fit2cloudClient fit2cloudClient = new Fit2cloudClient(this.f2cAccessKey, this.f2cSecretKey, this.f2cEndpoint);
        List<ApplicationSetting> applicationSettings = fit2cloudClient.getApplicationSettings(applicationId);
        for (ApplicationSetting appst : applicationSettings) {
            if (appst.getId().equalsIgnoreCase(this.applicationSettingId)) {
                applicationSetting = appst;
            }
        }
        return applicationSetting;
    }


    public String getF2cEndpoint() {
        return f2cEndpoint;
    }

    public String getF2cAccessKey() {
        return f2cAccessKey;
    }

    public String getF2cSecretKey() {
        return f2cSecretKey;
    }

    public String getApplicationRepositoryId() {
        return applicationRepositoryId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public boolean isNexusChecked() {
        return nexusChecked;
    }

    public boolean isOssChecked() {
        return ossChecked;
    }

    public boolean isS3Checked() {
        return s3Checked;
    }

    public boolean isAutoDeploy() {
        return autoDeploy;
    }

    public String getApplicationSettingId() {
        return applicationSettingId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getClusterRoleId() {
        return clusterRoleId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getCloudServerId() {
        return cloudServerId;
    }

    public String getDeployPolicy() {
        return deployPolicy;
    }

    public String getApplicationVersionName() {
        return applicationVersionName;
    }

    public String getIncludes() {
        return includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public String getAppspecFilePath() {
        return appspecFilePath;
    }

    public String getDescription() {
        return description;
    }

    public boolean isWaitForCompletion() {
        return waitForCompletion;
    }

    public Long getPollingTimeoutSec() {
        return pollingTimeoutSec;
    }

    public Long getPollingFreqSec() {
        return pollingFreqSec;
    }

    private void log(String msg) {
        logger.println(LOG_PREFIX + msg);
    }


}
