package com.fit2cloud.codedeploy2;

import com.fit2cloud.codedeploy2.client.Fit2cloudClient;
import com.fit2cloud.codedeploy2.client.model.*;
import com.fit2cloud.codedeploy2.oss.AWSS3Client;
import com.fit2cloud.codedeploy2.oss.AliyunOSSClient;
import com.fit2cloud.codedeploy2.oss.ArtifactoryUploader;
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
    private final boolean autoDeploy;
    private final String includes;
    private final String excludes;
    private final String appspecFilePath;
    private final String description;
    private final boolean waitForCompletion;
    private final Long pollingTimeoutSec;
    private final Long pollingFreqSec;
    private final String nexusGroupId;
    private final String nexusArtifactId;
    private final String nexusArtifactVersion;
    private final boolean nexusChecked;
    private final boolean ossChecked;
    private final boolean s3Checked;
    private final boolean artifactoryChecked;



    private final String path;
    //上传到阿里云参数
    private final String objectPrefixAliyun;
    //上传到亚马逊参数
    private final String objectPrefixAWS;
    private final String repositorySettingId;
    private final String artifactType;


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
                                  boolean artifactoryChecked,
                                  boolean autoDeploy,
                                  Long pollingTimeoutSec,
                                  Long pollingFreqSec,
                                  String includes,
                                  String excludes,
                                  String appspecFilePath,
                                  String description,
                                  String artifactType,
                                  String repositorySettingId,
                                  String objectPrefixAliyun,
                                  String objectPrefixAWS,
                                  String path,
                                  String nexusGroupId,
                                  String nexusArtifactId,
                                  String nexusArtifactVersion) {
        this.f2cEndpoint = f2cEndpoint;
        this.f2cAccessKey = f2cAccessKey;
        this.artifactType = StringUtils.isBlank(artifactType) ? ArtifactType.NEXUS : artifactType;
        this.repositorySettingId = repositorySettingId;
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
        this.autoDeploy = autoDeploy;
        this.includes = includes;
        this.excludes = excludes;
        this.appspecFilePath = StringUtils.isBlank(appspecFilePath) ? "appspec.yml" : appspecFilePath;
        this.description = description;
        this.pollingFreqSec = pollingFreqSec;
        this.pollingTimeoutSec = pollingTimeoutSec;
        this.waitForCompletion = waitForCompletion;
        this.objectPrefixAliyun = objectPrefixAliyun;
        this.objectPrefixAWS = objectPrefixAWS;
        this.path = path;
        this.nexusGroupId = nexusGroupId;
        this.nexusArtifactId = nexusArtifactId;
        this.nexusArtifactVersion = nexusArtifactVersion;
        this.nexusChecked = artifactType.equals(ArtifactType.NEXUS) ? true : false;
        this.artifactoryChecked = artifactType.equals(ArtifactType.ARTIFACTORY) ? true : false;
        this.ossChecked = artifactType.equals(ArtifactType.OSS) ? true : false;
        this.s3Checked = artifactType.equals(ArtifactType.S3) ? true : false;
        ;


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


        // 查询仓库
        ApplicationRepository applicationRepository = null;
        ApplicationRepositorySetting repSetting = null;
        ApplicationRepository rep = null;
        try {
            ApplicationDTO app = null;
            List<ApplicationDTO> applicationDTOS = fit2cloudClient.getApplications(workspaceId);
            for (ApplicationDTO applicationDTO : applicationDTOS) {
                if (applicationDTO.getId().equals(this.applicationId)) {
                    app = applicationDTO;
                }
            }
            if (app != null) {
                for (ApplicationRepositorySetting setting : app.getApplicationRepositorySettings()) {
                    if (setting.getId().equals(repositorySettingId)) {
                        repSetting = setting;
                    }
                }
            }
            if (repSetting != null) {
                List<ApplicationRepository> repositories = fit2cloudClient.getApplicationRepositorys(workspaceId);
                for (ApplicationRepository re : repositories) {
                    if (re.getId().equals(repSetting.getRepositoryId())) {
                        rep = re;
                    }
                }
            }

            if (rep != null) {
                applicationRepository = rep;
            }

            String repoType = applicationRepository.getType();
            if (!artifactType.equalsIgnoreCase(repoType)) {
                log("所选仓库与 \"Zip文件上传设置\"中的类型设置不匹配!");
                return false;
            }

        } catch (Exception e) {
            log("加载仓库失败！" + e.getMessage());
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


            switch (artifactType) {
                case ArtifactType.OSS:
                    log("开始上传zip文件到OSS服务器");
                    //getBucketLocation
                    String expFP = Utils.replaceTokens(build, listener, zipFile.toString());

                    if (expFP != null) {
                        expFP = expFP.trim();
                    }

                    // Resolve virtual path
                    String expVP = Utils.replaceTokens(build, listener, objectPrefixAliyun);
                    if (Utils.isNullOrEmpty(expVP)) {
                        expVP = null;
                    }
                    if (!Utils.isNullOrEmpty(expVP) && !expVP.endsWith(Utils.FWD_SLASH)) {
                        expVP = expVP.trim() + Utils.FWD_SLASH;
                    }
                    try {
                        int filesUploaded = AliyunOSSClient.upload(build, listener,
                                applicationRepository.getAccessId(),
                                applicationRepository.getAccessPassword(),
                                ".aliyuncs.com",
                                applicationRepository.getRepository().replace("bucket:", ""), expFP, expVP, zipFile);
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
                        newAddress = objectPrefixAliyun + "/" + zipFile.getName();
                    }
                    log("文件路径" + newAddress);
                    break;
                case ArtifactType.ARTIFACTORY:
                    log("开始上传zip文件到Artifactory服务器");
                    if (StringUtils.isBlank(path)) {
                        log("请输入上传至 Artifactory 的 Path");
                        return false;
                    }
                    String pathNew = Utils.replaceTokens(build, listener, path);
                    try {

                        String r = applicationRepository.getRepository();
                        String server = r.substring(0, r.indexOf("/artifactory"));
                        newAddress = ArtifactoryUploader.uploadArtifactory(zipFile, server.trim(),
                                applicationRepository.getAccessId(), applicationRepository.getAccessPassword(), r, pathNew);
                    } catch (Exception e) {
                        log("上传文件到 Artifactory 服务器失败！错误消息如下:");
                        log(e.getMessage());
                        e.printStackTrace(this.logger);
                        return false;
                    }
                    log("上传zip文件到Artifactory服务器成功!");
                    break;
                case ArtifactType.NEXUS:
                    if (StringUtils.isBlank(nexusArtifactId) || StringUtils.isBlank(nexusGroupId) || StringUtils.isBlank(nexusArtifactVersion)) {
                        log("请输入上传至 Nexus 的 GroupId、 ArtifactId 和 NexusArtifactVersion");
                        return false;
                    }
                    String nexusGroupIdNew = Utils.replaceTokens(build, listener, nexusGroupId);
                    String nexusArtifactIdNew = Utils.replaceTokens(build, listener, nexusArtifactId);
                    String nexusArtifactVersionNew = Utils.replaceTokens(build, listener, nexusArtifactVersion);

                    log("开始上传zip文件到nexus服务器");
                    try {
                        newAddress = NexusUploader.upload(zipFile, applicationRepository.getAccessId(), applicationRepository.getAccessPassword(), applicationRepository.getRepository(),
                                nexusGroupIdNew, nexusArtifactIdNew, String.valueOf(builtNumber), "zip", nexusArtifactVersionNew);
                    } catch (Exception e) {
                        log("上传文件到 Nexus 服务器失败！错误消息如下:");
                        log(e.getMessage());
                        e.printStackTrace(this.logger);
                        return false;
                    }
                    log("上传zip文件到nexus服务器成功!");
                    break;
                case ArtifactType.S3:
                    log("开始上传zip文件到AWS服务器");
                    //getBucketLocation
                    String expFPAws = Utils.replaceTokens(build, listener, zipFile.toString());

                    if (expFPAws != null) {
                        expFPAws = expFPAws.trim();
                    }

                    // Resolve virtual path
                    String expVPAws = Utils.replaceTokens(build, listener, objectPrefixAWS);
                    if (Utils.isNullOrEmpty(expVPAws)) {
                        expVPAws = null;
                    }
                    if (!Utils.isNullOrEmpty(expVPAws) && !expVPAws.endsWith(Utils.FWD_SLASH)) {
                        expVPAws = expVPAws.trim() + Utils.FWD_SLASH;
                    }
                    try {
                        AWSS3Client.upload(build, listener,
                                applicationRepository.getAccessId(),
                                applicationRepository.getAccessPassword(),
                                null,
                                applicationRepository.getRepository(), expFPAws, expVPAws, zipFile);
                        log("上传Artifacts到亚马逊AWS成功!");
                    } catch (Exception e) {
                        log("上传Artifact到亚马逊AWS失败，错误消息如下:");
                        log(e.getMessage());
                        e.printStackTrace(this.logger);
                        return false;
                    }
                    log("上传zip文件到亚马逊AWS服务器成功!");
                    if (expVPAws == null) {
                        newAddress = zipFile.getName();
                    } else {
                        newAddress = objectPrefixAWS + "/" + zipFile.getName();
                    }
                    log("文件路径:" + newAddress);
                    break;
                default:
                    log("暂时不支持 " + artifactType + " 类型制品库");
                    return false;
            }


        } catch (Exception e) {
            log("生成ZIP包失败: " + e.getMessage());
            return false;
        } finally {
            if(zipFile != null && zipFile.exists()){
                try {
                    log("删除 Zip 文件 " + zipFile.getAbsolutePath());
                    zipFile.delete();
                }catch (Exception e){
                }
            }
        }


        ApplicationVersion appVersion = null;
        try {
            log("注册应用版本中...");
            String newAppVersion = Utils.replaceTokens(build, listener, this.applicationVersionName);
            ApplicationVersionDTO applicationVersion = new ApplicationVersionDTO();
            applicationVersion.setApplicationId(this.applicationId);
            applicationVersion.setName(newAppVersion);
            assert repSetting != null;
            applicationVersion.setEnvironmentValueId(repSetting.getEnvId());
            applicationVersion.setApplicationRepositoryId(repSetting.getRepositoryId());
            applicationVersion.setLocation(newAddress);
            appVersion = fit2cloudClient.createApplicationVersion(applicationVersion, this.workspaceId);
        } catch (Exception e) {
            log("版本注册失败！ 原因：" + e.getMessage());
            return false;
        }
        log("注册版本成功！");

        ApplicationDeployment applicationDeploy = null;
        try {
            if (this.autoDeploy) {
                log("创建代码部署任务...");
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
            items.add("请选择工作空间", "");
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
                items.add("请选择应用", "");
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

        public ListBoxModel doFillRepositorySettingIdItems(@QueryParameter String f2cAccessKey,
                                                           @QueryParameter String f2cSecretKey,
                                                           @QueryParameter String f2cEndpoint,
                                                           @QueryParameter String workspaceId,
                                                           @QueryParameter String applicationId) {
            ListBoxModel items = new ListBoxModel();
            try {
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                items.add("请选择环境", "");
                List<ApplicationDTO> applicationDTOS = fit2CloudClient.getApplications(workspaceId);

                ApplicationDTO application = null;

                for (ApplicationDTO applicationDTO : applicationDTOS) {
                    if (applicationDTO.getId().equals(applicationId)) {
                        application = applicationDTO;
                    }
                }

                assert application != null;
                List<ApplicationRepositorySetting> list = application.getApplicationRepositorySettings();
                List<ApplicationRepository> applicationRepositories = fit2CloudClient.getApplicationRepositorys(workspaceId);
                List<TagValue> envs = fit2CloudClient.getEnvList();

                if (list != null && list.size() > 0) {
                    for (ApplicationRepositorySetting c : list) {
                        ApplicationRepository repository = null;
                        for (ApplicationRepository applicationRepository : applicationRepositories) {
                            if (applicationRepository.getId().equals(c.getRepositoryId())) {
                                repository = applicationRepository;
                            }
                        }
                        String envName = null;
                        for (TagValue env : envs) {
                            if (env.getId().equals(c.getEnvId())) {
                                envName = env.getTagValueAlias();
                            }
                            if (c.getEnvId().equalsIgnoreCase("ALL")) {
                                envName = "全部环境";
                            }
                        }


                        assert repository != null;
                        items.add(envName + "---" + repository.getType(), String.valueOf(c.getId()));
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
                                                 @QueryParameter String repositorySettingId) {
            ListBoxModel items = new ListBoxModel();
            items.add("请选择集群", "");

            try {
                Fit2cloudClient fit2CloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                List<ClusterDTO> list = fit2CloudClient.getClusters(workspaceId);

                final Fit2cloudClient fit2cloudClient = new Fit2cloudClient(f2cAccessKey, f2cSecretKey, f2cEndpoint);
                ApplicationDTO applicationDTO = null;
                List<ApplicationDTO> applications = fit2cloudClient.getApplications(workspaceId);
                for (ApplicationDTO app : applications) {
                    if (app.getId().equalsIgnoreCase(applicationId)) {
                        applicationDTO = app;
                    }
                }
                ApplicationRepositorySetting applicationRepositorySetting = null;
                List<ApplicationRepositorySetting> repositorySettings = applicationDTO.getApplicationRepositorySettings();
                for (ApplicationRepositorySetting appst : repositorySettings) {
                    if (appst.getId().equalsIgnoreCase(repositorySettingId)) {
                        applicationRepositorySetting = appst;
                    }
                }

                String envValueId = applicationRepositorySetting.getEnvId();
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
            items.add("请选择主机组", "");

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
            items.add("请选择主机", "");
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

    public boolean isArtifactoryChecked() {
        return artifactoryChecked;
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

    public String getRepositorySettingId() {
        return repositorySettingId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public String getObjectPrefixAliyun() {
        return objectPrefixAliyun;
    }

    public String getObjectPrefixAWS() {
        return objectPrefixAWS;
    }

    public String getPath() {
        return path;
    }
    public String getNexusGroupId() {
        return nexusGroupId;
    }

    public String getNexusArtifactId() {
        return nexusArtifactId;
    }

    public String getNexusArtifactVersion() {
        return nexusArtifactVersion;
    }


}
