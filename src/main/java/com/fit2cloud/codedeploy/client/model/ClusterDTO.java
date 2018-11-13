package com.fit2cloud.codedeploy.client.model;


public class ClusterDTO extends Cluster {
    private Integer countClusterRole;
    private Integer countServer;
    private String organizationName;
    private String organizationId;
    private String workspaceName;
    private String ansibleGroupId;
    private String systemValueId;
    private String envValueId;

    public String getSystemValueId() {
        return systemValueId;
    }

    public void setSystemValueId(String systemValueId) {
        this.systemValueId = systemValueId;
    }

    public String getEnvValueId() {
        return envValueId;
    }

    public void setEnvValueId(String envValueId) {
        this.envValueId = envValueId;
    }

    public String getAnsibleGroupId() {
        return ansibleGroupId;
    }

    public void setAnsibleGroupId(String ansibleGroupId) {
        this.ansibleGroupId = ansibleGroupId;
    }

    public Integer getCountClusterRole() {
        return countClusterRole;
    }

    public void setCountClusterRole(Integer countClusterRole) {
        this.countClusterRole = countClusterRole;
    }

    public Integer getCountServer() {
        return countServer;
    }

    public void setCountServer(Integer countServer) {
        this.countServer = countServer;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
}
