package com.fit2cloud.codedeploy2.client.model;

import java.io.Serializable;

public class ApplicationRepositorySetting implements Serializable {

    private String id;


    private String applicationId;


    private String repositoryId;


    private String envId;


    private static final long serialVersionUID = 1L;


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id == null ? null : id.trim();
    }


    public String getApplicationId() {
        return applicationId;
    }


    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId == null ? null : applicationId.trim();
    }


    public String getRepositoryId() {
        return repositoryId;
    }


    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId == null ? null : repositoryId.trim();
    }


    public String getEnvId() {
        return envId;
    }


    public void setEnvId(String envId) {
        this.envId = envId == null ? null : envId.trim();
    }
}