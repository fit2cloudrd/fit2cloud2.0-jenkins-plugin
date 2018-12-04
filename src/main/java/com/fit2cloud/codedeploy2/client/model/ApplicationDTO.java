package com.fit2cloud.codedeploy2.client.model;


public class ApplicationDTO extends Application {
    private String businessValueId;
    private String organizationName;
    private String workspaceName;
    private Integer countVersion;


    public Integer getCountVersion() {
        return countVersion;
    }

    public void setCountVersion(Integer countVersion) {
        this.countVersion = countVersion;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }


    public String getBusinessValueId() {
        return businessValueId;
    }

    public void setBusinessValueId(String businessValueId) {
        this.businessValueId = businessValueId;
    }
}
