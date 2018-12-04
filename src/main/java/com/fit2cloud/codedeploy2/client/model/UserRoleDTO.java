package com.fit2cloud.codedeploy2.client.model;

public class UserRoleDTO {

    private String id;
    private String name;
    private String desc;
    private String parentId;
    private Boolean isSwitch = true;

    public Boolean getSwitch() {
        return isSwitch;
    }

    public void setSwitch(Boolean aSwitch) {
        isSwitch = aSwitch;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
