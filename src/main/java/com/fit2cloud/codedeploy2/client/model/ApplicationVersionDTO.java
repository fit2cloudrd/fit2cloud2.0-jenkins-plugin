package com.fit2cloud.codedeploy2.client.model;


public class ApplicationVersionDTO extends ApplicationVersion {
    private String applicationName;
    private String environmentValueId;

    public String getEnvironmentValueId() {
        return environmentValueId;
    }

    public void setEnvironmentValueId(String environmentValueId) {
        this.environmentValueId = environmentValueId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
